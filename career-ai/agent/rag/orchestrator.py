"""混合检索编排器（适配迁移自 localrag-kit retrieval/orchestrator.py，MIT 协议）。

适配说明：
  - 保留 retrieve()（支持 hybrid / bm25 / vector 三种模式 + RRF 融合）
  - 去掉 ask() / stream_ask()——LLM 生成由 Agent 图自己处理
  - 新增 index() 接口——接收 list[dict]（content + source），内部转 Chunk 后 upsert
  - 实现 RAGRetriever 抽象接口
"""

from __future__ import annotations

import hashlib
import os

from agent.rag.base import RAGRetriever
from agent.rag.local_hasher import FastFeatureEmbeddingProvider
from agent.rag.models import Chunk, ChunkMetadata, FileType, RetrievedDoc
from agent.rag.rrf import reciprocal_rank_fusion
from agent.rag.sqlite_store import SQLiteStore


class HybridRetriever(RAGRetriever):
    """SQLite FTS5 BM25 + FastFeature 向量 + RRF 融合的混合检索器。

    零外部服务、零模型下载、零云依赖。
    """

    def __init__(self, db_path: str | None = None):
        db_path = db_path or os.getenv("RAG_DB_PATH", "./data/rag_store.db")
        self.store = SQLiteStore(db_path)
        self.embedder = FastFeatureEmbeddingProvider()  # 零模型零下载

    def retrieve(self, query: str, top_k: int = 5, mode: str = "hybrid") -> list[RetrievedDoc]:
        """按 mode 执行检索：hybrid（BM25 + 向量 + RRF）/ bm25 / vector。"""
        if mode == "bm25":
            results = self.store.search_bm25(query, limit=top_k)
        elif mode == "vector":
            qv = self.embedder.embed_text(query)
            results = self.store.search_vector(qv, limit=top_k)
        else:
            # 混合检索：两个引擎各取 top_k*2，再用 RRF 融合
            bm25_res = self.store.search_bm25(query, limit=top_k * 2)
            qv = self.embedder.embed_text(query)
            vec_res = self.store.search_vector(qv, limit=top_k * 2)

            if not vec_res:
                results = bm25_res[:top_k]
            elif not bm25_res:
                results = vec_res[:top_k]
            else:
                results = reciprocal_rank_fusion(bm25_res, vec_res, limit=top_k)

        return [
            RetrievedDoc(
                content=r.chunk.text,
                source=r.chunk.metadata.relative_path,
                score=r.score,
                match_type=r.match_type,
            )
            for r in results
        ]

    def index(self, documents: list[dict]) -> None:
        """索引文档列表。每条 dict 需含 content 和 source 字段（可选 section_title）。"""
        if not documents:
            return

        # 按 source 分组，同 source 替换旧块
        source_groups: dict[str, list[dict]] = {}
        for doc in documents:
            source = str(doc.get("source", "") or "unknown")
            source_groups.setdefault(source, []).append(doc)

        for source, docs in source_groups.items():
            # 先删除该 source 的旧块
            self.store.delete_chunks_for_file(source)

            # upsert 文档元数据（SHA-256 变更检测）
            combined_text = "\n".join(str(d.get("content", "")) for d in docs)
            sha256 = hashlib.sha256(combined_text.encode("utf-8")).hexdigest()
            self.store.upsert_file(
                relative_path=source,
                file_path=source,
                file_type=self._detect_file_type(source),
                sha256=sha256,
            )

            chunks = []
            for i, doc in enumerate(docs):
                content = str(doc.get("content", ""))
                if not content.strip():
                    continue
                chunk_id = hashlib.sha256(
                    f"{source}:{i}:{content[:64]}".encode("utf-8")
                ).hexdigest()[:16]

                meta = ChunkMetadata(
                    chunk_id=chunk_id,
                    file_path=source,
                    relative_path=source,
                    file_type=self._detect_file_type(source),
                    start_line=i + 1,
                    end_line=i + 1,
                    char_count=len(content),
                    estimated_tokens=0,
                    section_title=str(doc.get("section_title", "")) or None,
                    extra={k: v for k, v in doc.items() if k not in ("content", "source")},
                )

                # 生成嵌入
                embedding = self.embedder.embed_text(content)

                chunks.append(Chunk(text=content, metadata=meta, embedding=embedding))

            if chunks:
                self.store.upsert_chunks(chunks)

    @staticmethod
    def _detect_file_type(path: str) -> FileType:
        """按扩展名推断文件类型。"""
        lower = path.lower()
        if lower.endswith((".md", ".markdown", ".mdx")):
            return FileType.MARKDOWN
        if lower.endswith((".pdf",)):
            return FileType.PDF
        if lower.endswith((".html", ".htm", ".xhtml")):
            return FileType.HTML
        if lower.endswith((".txt", ".rst", ".csv", ".tsv", ".log")):
            return FileType.TEXT
        return FileType.TEXT

    def close(self):
        """关闭底层 SQLite 连接。"""
        self.store.close()
