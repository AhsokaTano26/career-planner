"""持久化层：PostgreSQL checkpointing（短期记忆）+ Store（长期记忆）。

直接迁移自借鉴代码/agent-service-toolkit/src/memory/postgres.py（MIT 协议），
适配 career-ai 的配置读取方式（读 .env 环境变量）。
"""

from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.store.postgres import AsyncPostgresStore
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool

logger = logging.getLogger("memory.postgres")


def validate_postgres_config() -> None:
    """校验 PostgreSQL 配置完整性。"""
    required_vars = [
        "AGENT_PG_USER",
        "AGENT_PG_PASSWORD",
        "AGENT_PG_HOST",
        "AGENT_PG_PORT",
        "AGENT_PG_DB",
    ]
    missing = [v for v in required_vars if not os.getenv(v)]
    if missing:
        raise ValueError(
            f"缺少 PostgreSQL 配置：{', '.join(missing)}。"
            "使用 Agent 持久化前必须设置这些环境变量。"
        )


def get_postgres_connection_string() -> str:
    """构建 PostgreSQL 连接串。"""
    password = os.getenv("AGENT_PG_PASSWORD", "")
    return (
        f"postgresql://{os.getenv('AGENT_PG_USER')}:"
        f"{password}@"
        f"{os.getenv('AGENT_PG_HOST')}:{os.getenv('AGENT_PG_PORT')}/"
        f"{os.getenv('AGENT_PG_DB')}"
    )


@asynccontextmanager
async def get_postgres_saver():
    """初始化基于连接池的 PostgreSQL checkpointer（更弹性的连接）。"""
    validate_postgres_config()
    application_name = "career-ai-agent-saver"

    async with AsyncConnectionPool(
        get_postgres_connection_string(),
        min_size=1,
        max_size=5,
        # LangGraph 要求 autocommit=true + row_factory=dict_row
        kwargs={
            "autocommit": True,
            "row_factory": dict_row,
            "application_name": application_name,
        },
        check=AsyncConnectionPool.check_connection,
    ) as pool:
        try:
            checkpointer = AsyncPostgresSaver(pool)
            await checkpointer.setup()
            yield checkpointer
        finally:
            await pool.close()


@asynccontextmanager
async def get_postgres_store():
    """初始化基于连接池的 PostgreSQL Store（长期记忆）。"""
    validate_postgres_config()
    application_name = "career-ai-agent-store"

    async with AsyncConnectionPool(
        get_postgres_connection_string(),
        min_size=1,
        max_size=5,
        kwargs={
            "autocommit": True,
            "row_factory": dict_row,
            "application_name": application_name,
        },
        check=AsyncConnectionPool.check_connection,
    ) as pool:
        try:
            store = AsyncPostgresStore(pool)
            await store.setup()
            yield store
        finally:
            await pool.close()


__all__ = ["get_postgres_saver", "get_postgres_store", "validate_postgres_config"]
