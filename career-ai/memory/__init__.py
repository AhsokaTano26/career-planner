"""持久化层：checkpointing + Store 工厂。

迁移自借鉴代码/agent-service-toolkit/src/memory/__init__.py（MIT 协议）。
当前只支持 PostgreSQL（对应引入依赖与决策）；SQLite/MongoDB 后续按需。
"""

from __future__ import annotations

from contextlib import AbstractAsyncContextManager

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.store.postgres import AsyncPostgresStore

from memory.postgres import get_postgres_saver, get_postgres_store

__all__ = ["initialize_database", "initialize_store"]


def initialize_database() -> AbstractAsyncContextManager[AsyncPostgresSaver]:
    """初始化 PostgreSQL checkpointer（短期记忆/对话线程）。"""
    return get_postgres_saver()


def initialize_store() -> AbstractAsyncContextManager[AsyncPostgresStore]:
    """初始化 PostgreSQL Store（长期记忆/跨会话知识）。"""
    return get_postgres_store()
