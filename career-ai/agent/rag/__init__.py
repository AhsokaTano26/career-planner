"""RAG 混合检索引擎（localrag-kit 迁移，MIT 协议）。

零模型、零外部服务、零云依赖：
  - SQLite FTS5 BM25 关键词检索
  - FastFeatureEmbeddingProvider（n-gram 特征哈希，384 维向量）
  - 倒数排名融合（RRF）
  - ContextSynthesizer（token 预算 + 来源引用）
"""

from __future__ import annotations

import os

from agent.rag.base import BaseEmbeddingProvider, RAGRetriever
from agent.rag.models import RetrievedDoc

__all__ = ["RAGRetriever", "BaseEmbeddingProvider", "RetrievedDoc", "get_retriever", "reset_retriever"]

_retriever: RAGRetriever | None = None


def get_retriever() -> RAGRetriever:
    """工厂方法：返回当前可用的检索器（进程级单例）。"""
    global _retriever
    if _retriever is None:
        from agent.rag.orchestrator import HybridRetriever

        _retriever = HybridRetriever(db_path=os.getenv("RAG_DB_PATH"))
    return _retriever


def reset_retriever() -> None:
    """重置单例（测试用）。"""
    global _retriever
    if _retriever is not None and hasattr(_retriever, "store"):
        _retriever.store.close()
    _retriever = None
