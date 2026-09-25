"""RAG 检索器抽象接口 + 嵌入提供者接口。

RAGRetriever：Agent 知识检索的统一抽象。
  - 当前实现：HybridRetriever（SQLite FTS5 BM25 + FastFeature 向量 + RRF）
  - 后续实现：VectorRetriever（接入真实嵌入模型 + 向量库）

BaseEmbeddingProvider：嵌入提供者抽象（迁移自 localrag-kit providers/base.py，MIT 协议）。
"""

from __future__ import annotations

from abc import ABC, abstractmethod

from agent.rag.models import RetrievedDoc


class RAGRetriever(ABC):
    """RAG 检索器抽象接口。"""

    @abstractmethod
    def retrieve(self, query: str, top_k: int = 5, mode: str = "hybrid") -> list[RetrievedDoc]:
        """检索与 query 最相关的文档。mode: hybrid / bm25 / vector。"""
        ...

    @abstractmethod
    def index(self, documents: list[dict]) -> None:
        """索引文档列表。每条 dict 需含 content 和 source 字段。"""
        ...


class BaseEmbeddingProvider(ABC):
    """嵌入提供者接口：文本 → 稠密向量。"""

    @property
    @abstractmethod
    def name(self) -> str:
        """提供者标识名。"""

    @property
    @abstractmethod
    def dimension(self) -> int:
        """输出向量维度。"""

    @abstractmethod
    def embed_text(self, text: str) -> list[float]:
        """为单条文本生成归一化稠密浮点向量。"""

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """批量生成向量，默认循环调用 embed_text。"""
        return [self.embed_text(t) for t in texts]

    def is_available(self) -> bool:
        """检查提供者后端是否可达。"""
        return True
