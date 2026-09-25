"""SQLite 混合存储引擎（直接迁移自 localrag-kit storage/sqlite_store.py，MIT 协议）。

零外部服务：一个 SQLite 文件同时承载 FTS5 全文索引 + 稠密向量 BLOB + 文件元数据。
WAL 模式、外键、自动触发器同步 FTS5。
"""

from __future__ import annotations

import json
import sqlite3
import time
from collections.abc import Sequence
from pathlib import Path

from pydantic import BaseModel, Field

from agent.rag.models import Chunk, ChunkMetadata, FileType
from agent.rag.schema import CREATE_TABLES_SQL, CREATE_TRIGGERS_SQL
from agent.rag.vector_ops import (
    batch_cosine_similarities,
    deserialize_vector,
    serialize_vector,
)


class SearchResult(BaseModel):
    """检索查询结果，含分数与来源。"""

    chunk: Chunk
    score: float = Field(description="相关性分数（越高越好）")
    match_type: str = Field(description="检索机制：'bm25' / 'vector' / 'hybrid'")


class StoreStats(BaseModel):
    """聚合存储指标。"""

    total_files: int = 0
    total_chunks: int = 0
    total_vectorized_chunks: int = 0
    database_size_bytes: int = 0
    db_path: str = ""


class SQLiteStore:
    """管理文档、文本块、FTS5 全文索引与稠密向量嵌入。"""

    def __init__(self, db_path: str | Path):
        self.db_path = Path(db_path).resolve()
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._conn: sqlite3.Connection | None = None
        self._init_connection()

    def _init_connection(self):
        """配置 SQLite 连接的高性能并发 pragma。"""
        self._conn = sqlite3.connect(
            str(self.db_path),
            check_same_thread=False,
            timeout=30.0,
        )
        self._conn.row_factory = sqlite3.Row
        with self._conn:
            # WAL 模式支持高吞吐并发读写
            self._conn.execute("PRAGMA journal_mode = WAL;")
            self._conn.execute("PRAGMA synchronous = NORMAL;")
            self._conn.execute("PRAGMA foreign_keys = ON;")
            self._conn.execute("PRAGMA temp_store = MEMORY;")
            self._conn.execute("PRAGMA mmap_size = 268435456;")  # 256MB 内存映射
        self.init_db()

    def init_db(self):
        """创建表、索引与同步触发器（幂等）。"""
        with self._conn:
            self._conn.executescript(CREATE_TABLES_SQL)
            self._conn.executescript(CREATE_TRIGGERS_SQL)

    def close(self):
        """关闭底层 SQLite 连接。"""
        if self._conn:
            self._conn.close()
            self._conn = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    # -------------------------------------------------------------------------
    # 文档元数据管理与增量变更检测
    # -------------------------------------------------------------------------

    def upsert_file(
        self,
        *,
        relative_path: str,
        file_path: str = "",
        file_type: str = "text",
        sha256: str = "",
    ):
        """插入或更新一条文档记录。"""
        with self._conn:
            self._conn.execute(
                """
                INSERT INTO files (
                    relative_path, file_path, file_type, file_size_bytes,
                    sha256, line_count, last_modified, language, indexed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(relative_path) DO UPDATE SET
                    file_path = excluded.file_path,
                    file_type = excluded.file_type,
                    sha256 = excluded.sha256,
                    indexed_at = excluded.indexed_at;
                """,
                (
                    relative_path,
                    file_path,
                    file_type,
                    0,
                    sha256,
                    0,
                    0.0,
                    None,
                    time.time(),
                ),
            )

    def delete_file(self, relative_path: str):
        """删除文档并级联删除其所有文本块与 FTS 条目。"""
        with self._conn:
            self._conn.execute(
                "DELETE FROM chunks_fts WHERE relative_path = ?", (relative_path,)
            )
            self._conn.execute("DELETE FROM files WHERE relative_path = ?", (relative_path,))
    # -------------------------------------------------------------------------
    # 文本块与稠密向量管理
    # -------------------------------------------------------------------------

    def upsert_chunks(self, chunks: list[Chunk]):
        """批量插入或替换文本块及其向量嵌入。"""
        if not chunks:
            return

        rows = []
        for c in chunks:
            emb_blob = serialize_vector(c.embedding) if c.embedding else None
            extra_json = json.dumps(c.metadata.extra) if c.metadata.extra else None
            rows.append(
                (
                    c.metadata.chunk_id,
                    c.metadata.relative_path,
                    c.metadata.file_path,
                    c.metadata.file_type.value,
                    c.metadata.start_line,
                    c.metadata.end_line,
                    c.metadata.char_count,
                    c.metadata.estimated_tokens,
                    c.metadata.section_title,
                    c.text,
                    emb_blob,
                    extra_json,
                )
            )

        with self._conn:
            self._conn.executemany(
                """
                INSERT INTO chunks (
                    chunk_id, relative_path, file_path, file_type,
                    start_line, end_line, char_count, estimated_tokens,
                    section_title, text, embedding, extra_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(chunk_id) DO UPDATE SET
                    relative_path = excluded.relative_path,
                    file_path = excluded.file_path,
                    file_type = excluded.file_type,
                    start_line = excluded.start_line,
                    end_line = excluded.end_line,
                    char_count = excluded.char_count,
                    estimated_tokens = excluded.estimated_tokens,
                    section_title = excluded.section_title,
                    text = excluded.text,
                    embedding = excluded.embedding,
                    extra_json = excluded.extra_json;
                """,
                rows,
            )
            # 手动同步 FTS5（分字中文），替代触发器
            for c in chunks:
                cid = c.metadata.chunk_id
                self._conn.execute(
                    "DELETE FROM chunks_fts WHERE chunk_id = ?", (cid,)
                )
                segmented_text = self._segment_cjk_for_fts(c.text)
                segmented_title = self._segment_cjk_for_fts(c.metadata.section_title or "")
                self._conn.execute(
                    "INSERT INTO chunks_fts(chunk_id, relative_path, section_title, text) "
                    "VALUES (?, ?, ?, ?)",
                    (cid, c.metadata.relative_path, segmented_title, segmented_text),
                )

    def delete_chunks_for_file(self, relative_path: str):
        """删除某个来源文件关联的所有文本块（含 FTS 条目）。"""
        with self._conn:
            self._conn.execute(
                "DELETE FROM chunks_fts WHERE relative_path = ?", (relative_path,)
            )
            self._conn.execute("DELETE FROM chunks WHERE relative_path = ?", (relative_path,))

    def get_chunk(self, chunk_id: str) -> Chunk | None:
        """按 ID 获取单个文本块。"""
        cur = self._conn.execute("SELECT * FROM chunks WHERE chunk_id = ?", (chunk_id,))
        row = cur.fetchone()
        if not row:
            return None
        return self._row_to_chunk(row)

    def get_all_chunks(self) -> list[Chunk]:
        """获取全部文本块。"""
        cur = self._conn.execute("SELECT * FROM chunks")
        return [self._row_to_chunk(row) for row in cur.fetchall()]

    # -------------------------------------------------------------------------
    # 检索引擎（FTS5 BM25 + 稠密向量余弦相似度）
    # -------------------------------------------------------------------------

    def search_bm25(self, query: str, limit: int = 20) -> list[SearchResult]:
        """用 SQLite FTS5 执行 BM25 关键词检索。

        返回按 BM25 相关性排序的前 N 个文本块。
        """
        cleaned_query = self._sanitize_fts_query(query)
        if not cleaned_query:
            return []

        # FTS5 中 bm25() 返回负数（越负匹配越好），归一化为正分数
        sql = """
            SELECT c.*, bm25(chunks_fts) as rank
            FROM chunks_fts
            JOIN chunks c ON c.chunk_id = chunks_fts.chunk_id
            WHERE chunks_fts MATCH ?
            ORDER BY rank ASC
            LIMIT ?;
        """
        try:
            cur = self._conn.execute(sql, (cleaned_query, limit))
            rows = cur.fetchall()
        except sqlite3.OperationalError:
            # 查询语法错误时回退到简单 token 搜索
            fallback_query = " OR ".join(f'"{tok}"' for tok in query.split() if tok.isalnum())
            if not fallback_query:
                return []
            try:
                cur = self._conn.execute(sql, (fallback_query, limit))
                rows = cur.fetchall()
            except sqlite3.OperationalError:
                return []

        results = []
        for row in rows:
            chunk = self._row_to_chunk(row)
            raw_rank = row["rank"]
            # 负 BM25 分数归一化到 [0.0, 1.0]
            score = 1.0 / (1.0 + abs(raw_rank))
            results.append(SearchResult(chunk=chunk, score=score, match_type="bm25"))

        return results

    def search_vector(
        self,
        query_embedding: Sequence[float],
        limit: int = 20,
        min_similarity: float = 0.0,
    ) -> list[SearchResult]:
        """在所有存储的向量嵌入上执行稠密向量余弦相似度检索。"""
        if not query_embedding:
            return []

        cur = self._conn.execute("SELECT * FROM chunks WHERE embedding IS NOT NULL")
        rows = cur.fetchall()
        if not rows:
            return []

        candidate_chunks: list[Chunk] = []
        candidate_vectors: list[list[float]] = []

        for row in rows:
            chunk = self._row_to_chunk(row)
            if chunk.embedding:
                candidate_chunks.append(chunk)
                candidate_vectors.append(chunk.embedding)

        if not candidate_vectors:
            return []

        similarities = batch_cosine_similarities(query_embedding, candidate_vectors)

        scored_pairs = [
            (chunk, score)
            for chunk, score in zip(candidate_chunks, similarities)
            if score >= min_similarity
        ]
        scored_pairs.sort(key=lambda p: p[1], reverse=True)

        results = [
            SearchResult(chunk=chunk, score=float(score), match_type="vector")
            for chunk, score in scored_pairs[:limit]
        ]
        return results

    # -------------------------------------------------------------------------
    # 诊断与统计
    # -------------------------------------------------------------------------

    def get_stats(self) -> StoreStats:
        """计算数据库统计信息。"""
        file_count = self._conn.execute("SELECT count(*) FROM files").fetchone()[0]
        chunk_count = self._conn.execute("SELECT count(*) FROM chunks").fetchone()[0]
        vec_count = self._conn.execute(
            "SELECT count(*) FROM chunks WHERE embedding IS NOT NULL"
        ).fetchone()[0]

        size_bytes = 0
        if self.db_path.exists():
            size_bytes = self.db_path.stat().st_size
            wal_path = self.db_path.with_name(self.db_path.name + "-wal")
            if wal_path.exists():
                size_bytes += wal_path.stat().st_size

        return StoreStats(
            total_files=file_count,
            total_chunks=chunk_count,
            total_vectorized_chunks=vec_count,
            database_size_bytes=size_bytes,
            db_path=str(self.db_path),
        )

    # -------------------------------------------------------------------------
    # 辅助
    # -------------------------------------------------------------------------

    @staticmethod
    def _row_to_chunk(row: sqlite3.Row) -> Chunk:
        """把 SQLite 行还原为 Chunk Pydantic 模型。"""
        emb = deserialize_vector(row["embedding"]) if row["embedding"] else None
        extra = json.loads(row["extra_json"]) if row["extra_json"] else {}

        meta = ChunkMetadata(
            chunk_id=row["chunk_id"],
            file_path=row["file_path"],
            relative_path=row["relative_path"],
            file_type=FileType(row["file_type"]),
            start_line=row["start_line"],
            end_line=row["end_line"],
            char_count=row["char_count"],
            estimated_tokens=row["estimated_tokens"],
            section_title=row["section_title"],
            extra=extra,
        )
        return Chunk(text=row["text"], metadata=meta, embedding=emb)

    @staticmethod
    def _segment_cjk_for_fts(text: str) -> str:
        """在 CJK 字符之间插入空格，使 FTS5 unicode61 分词器能逐字索引中文。

        连续中文不加空格时，unicode61 会把整串当作一个 token，
        导致 "编程语言" 搜不到 "软件工程师需要掌握编程语言"。
        """
        result: list[str] = []
        for ch in text:
            if "\u4e00" <= ch <= "\u9fff":
                result.append(f" {ch} ")
            else:
                result.append(ch)
        return "".join(result)

    @staticmethod
    def _segment_cjk_for_fts(text: str) -> str:
        """在 CJK 字符之间插入空格，使 FTS5 unicode61 分词器能逐字索引中文。

        连续中文不加空格时，unicode61 会把整串当作一个 token，
        导致 "编程语言" 搜不到 "软件工程师需要掌握编程语言"。
        """
        result: list[str] = []
        for ch in text:
            if "\u4e00" <= ch <= "\u9fff":
                result.append(f" {ch} ")
            else:
                result.append(ch)
        return "".join(result)

    @staticmethod
    def _sanitize_fts_query(query: str) -> str:
        """把原始输入清洗为安全的 FTS5 MATCH 查询语法（支持中文逐字 AND 匹配）。"""
        # 先对查询做 CJK 分字
        segmented = SQLiteStore._segment_cjk_for_fts(query)
        tokens = []
        for word in segmented.split():
            clean = "".join(ch for ch in word if ch.isalnum() or ch in ("_", "-"))
            if clean:
                tokens.append(f'"{clean}"')
        if not tokens:
            return ""
        # 中文逐字用 AND 提高精度；英文词保持 OR（单个词已按空格分开）
        # 全部 AND 在混合中英文时也合理——要求所有词都出现
        return " AND ".join(tokens)
