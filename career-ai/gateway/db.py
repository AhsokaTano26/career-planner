"""ai_call_log 落库（MySQL）。

边界（Demo 精简点）：career-ai 由「不连库」收紧为「仅写 ai_call_log 单表」，
不读任何业务表；连接失败/写入失败均 fail-open（告警一次，不阻断模型调用）。
表结构见 career-core/src/main/resources/db/admin-log.sql。
"""

from __future__ import annotations

import logging
import uuid
from typing import Any

from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

logger = logging.getLogger("gateway.db")

_INSERT_SQL = text(
    "INSERT INTO ai_call_log "
    "(id, request_id, user_ref, scene, model_name, prompt_version, duration_ms, "
    " status, token_estimate, request_hash, input_hash) "
    "VALUES (:id, :request_id, :user_ref, :scene, :model_name, :prompt_version, "
    " :duration_ms, :status, :token_estimate, :request_hash, :input_hash) "
    # fallback 场景：失败事件先落库、成功事件后到 → 按 uk_ai_request_id 覆盖为最终状态
    "ON DUPLICATE KEY UPDATE status=VALUES(status), duration_ms=VALUES(duration_ms), "
    "token_estimate=VALUES(token_estimate), model_name=VALUES(model_name)"
)

_engine: Engine | None = None
_warned = False


def get_engine() -> Engine | None:
    """按环境变量构建引擎；未配置 DB_* 或驱动缺失时返回 None（不落库）。"""
    global _engine
    if _engine is not None:
        return _engine
    host = _env("DB_HOST")
    name = _env("DB_NAME")
    if not host or not name:
        return None
    url = (
        f"mysql+pymysql://{_env('DB_USER', 'career')}:{_env('DB_PASSWORD', '')}"
        f"@{host}:{_env('DB_PORT', '3306')}/{name}?charset=utf8mb4"
    )
    try:
        _engine = create_engine(url, pool_pre_ping=True, pool_recycle=1800, future=True)
    except Exception as exc:  # noqa: BLE001 - fail-open
        _warn_once("ai_call_log 引擎创建失败，日志将不落库：%s" % exc)
        return None
    return _engine


def insert_ai_call_log(
    *,
    request_id: str,
    scene: str,
    status: str,
    model_name: str | None = None,
    user_ref: str | None = None,
    prompt_version: str | None = None,
    duration_ms: int | None = None,
    token_estimate: int | None = None,
    request_hash: str | None = None,
    input_hash: str | None = None,
) -> bool:
    """写入一条调用日志；成功 True，失败/未配置 False（fail-open，不抛异常）。"""
    engine = get_engine()
    if engine is None:
        return False
    row: dict[str, Any] = {
        "id": uuid.uuid4().hex[:32],
        "request_id": request_id[:64],
        "user_ref": (user_ref or None),
        "scene": scene,
        "model_name": model_name,
        "prompt_version": prompt_version,
        "duration_ms": duration_ms,
        "status": status,
        "token_estimate": token_estimate,
        "request_hash": request_hash,
        "input_hash": input_hash,
    }
    try:
        with engine.begin() as conn:
            conn.execute(_INSERT_SQL, row)
        return True
    except Exception as exc:  # noqa: BLE001 - fail-open
        _warn_once("ai_call_log 写入失败（request_id=%s）：%s" % (request_id, exc))
        return False


def _env(key: str, default: str | None = None) -> str | None:
    value = __import__("os").getenv(key, default)
    return value if value not in ("", None) else default


def _warn_once(message: str) -> None:
    global _warned
    if not _warned:
        _warned = True
        logger.warning(message)
