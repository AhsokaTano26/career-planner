"""career-ai 入口（含 Agent 模块 lifespan 集成）。

启动（在 career-ai 目录下执行）：
    uvicorn api.main:app --host 127.0.0.1 --port 8000

边界：本服务不连接 MySQL、不持有学生身份信息（student_id 可选留空）、不写入正式业务表；
只接收最小化结构化输入并返回校验后的 JSON。大模型失败时由 career-core 回退规则模板。
"""

from __future__ import annotations

import logging
import re
import uuid as _uuid
from contextlib import asynccontextmanager
from datetime import datetime as _datetime
from pathlib import Path
from typing import Dict, List, Optional

from dotenv import load_dotenv
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, PlainTextResponse
from pydantic import BaseModel

# 加载 career-ai/.env（如存在）
load_dotenv(Path(__file__).resolve().parent.parent / ".env")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """启动时预热网关 + 初始化 Agent（checkpointer + store + RAG + 编译图）。

    稳定性（2026-09）：GATEWAY_API_KEY 为空 = 未配置内部密钥 → fail-closed 拒绝启动，
    杜绝 AI 路由无鉴权裸奔（此前空 key 自动放行）。
    Agent/网关初始化失败不阻塞启动（fail-open），调用时按需报错。
    """
    import os

    if not os.getenv("GATEWAY_API_KEY", ""):
        raise RuntimeError("GATEWAY_API_KEY 未配置：AI 路由要求内部鉴权，拒绝无密钥启动")

    # 1. 预热 LiteLLM 网关（现有逻辑）
    try:
        from gateway.client import get_gateway

        gw = get_gateway()
        groups = [g.name for g in gw.config.groups]
        logging.getLogger("gateway").info("AI 网关就绪：模型组=%s rpm=%s", groups, gw.config.rpm)
    except Exception as exc:  # noqa: BLE001 - fail-open
        logging.getLogger("gateway").warning("AI 网关初始化失败（调用时将报错）：%s", exc)

    # 2. 初始化 Agent 持久化 + RAG + 编译图（新）
    agent_saver_cm = None
    agent_store_cm = None
    compiled_agent = None
    try:
        from memory import initialize_database, initialize_store

        agent_saver_cm = initialize_database()
        agent_store_cm = initialize_store()
        saver = await agent_saver_cm.__aenter__()
        store = await agent_store_cm.__aenter__()

        if hasattr(saver, "setup"):
            await saver.setup()
        if hasattr(store, "setup"):
            await store.setup()

        from agent.graph import agent_graph

        compiled_agent = agent_graph.compile(checkpointer=saver, store=store)

        from api.routes_agent import set_compiled_agent

        set_compiled_agent(compiled_agent)
        logging.getLogger("agent").info("Agent 图已编译（PostgreSQL checkpointing + Store 就绪）")
    except Exception as exc:  # noqa: BLE001 - fail-open（PostgreSQL 未启动不阻塞其他服务）
        logging.getLogger("agent").warning(
            "Agent 初始化失败（/api/v1/ai/agent/invoke 将返回 503）：%s", exc
        )
        compiled_agent = None

    # 3. 初始化 RAG 混合检索器（进程级单例）
    try:
        from agent.rag import get_retriever

        retriever = get_retriever()
        stats = retriever.store.get_stats()
        logging.getLogger("agent.rag").info(
            "RAG 检索器就绪：chunks=%s db=%s", stats.total_chunks, stats.db_path
        )
    except Exception as exc:  # noqa: BLE001 - fail-open
        logging.getLogger("agent.rag").warning("RAG 初始化失败（search_knowledge 将降级）：%s", exc)

    yield

    # 清理：关闭 Agent 持久化上下文
    if agent_store_cm is not None:
        try:
            await agent_store_cm.__aexit__(None, None, None)
        except Exception:  # noqa: BLE001
            pass
    if agent_saver_cm is not None:
        try:
            await agent_saver_cm.__aexit__(None, None, None)
        except Exception:  # noqa: BLE001
            pass


app = FastAPI(title="career-ai", version="0.3.0", lifespan=lifespan)


@app.middleware("http")
async def normalize_double_slashes(request: Request, call_next):
    """请求路径归一化（Demo 兼容点 / 后续迭代替换位置）。

    Apifox 契约测试在路径变量为空时会请求带连续斜杠的 URL（如 /api/v1/ai/chat//feedback），
    FastAPI 路由不匹配空路径段 → 404。此中间件把连续斜杠折叠为单斜杠，命中 Controller 的
    兜底路由（/chat/feedback）；对 queryString 无影响。
    """
    path = request.scope.get("path", "")
    if "//" in path:
        normalized = re.sub(r"/{2,}", "/", path)
        request.scope["path"] = normalized
        request.scope["raw_path"] = normalized.encode("utf-8")
    return await call_next(request)


# AI 智能服务 + AI 生涯咨询路由（/api/v1/ai/*）
from api.routes_ai import router as ai_router  # noqa: E402

app.include_router(ai_router)

# AI 网关路由（/v1/chat/completions、/api/v1/gateway/generate）
from api.routes_gateway import router as gateway_router  # noqa: E402

app.include_router(gateway_router)

# Agent 路由（/api/v1/ai/agent/*）—— 智能生涯对话体
from api.routes_agent import router as agent_router  # noqa: E402

app.include_router(agent_router)


# ---------------------------------------------------------------- 统一错误响应（对齐 Apifox ErrorResponse）
_ERROR_CODE_BY_STATUS = {
    400: "VALIDATION_ERROR",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    409: "CONFLICT",
    422: "VALIDATION_ERROR",
    429: "RATE_LIMITED",
    502: "BAD_GATEWAY",
    503: "SERVICE_UNAVAILABLE",
}


def _now_local() -> str:
    return _datetime.now().astimezone().replace(microsecond=0).isoformat()


@app.exception_handler(HTTPException)
async def http_exception_handler(_request: Request, exc: HTTPException):
    """HTTPException → {code, message, data, traceId, timestamp}（对齐 Apifox ErrorResponse）。"""
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "code": _ERROR_CODE_BY_STATUS.get(exc.status_code, "ERROR"),
            "message": str(exc.detail),
            "data": {},
            "traceId": _uuid.uuid4().hex,
            "timestamp": _now_local(),
        },
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_request: Request, exc: RequestValidationError):
    """Pydantic 校验失败 → 400（对齐 Apifox：请求参数校验失败 400）。"""
    details = [
        {
            "field": ".".join(str(p) for p in err.get("loc", []) if p not in ("body", "query", "path")),
            "message": err.get("msg", ""),
        }
        for err in exc.errors()
    ]
    return JSONResponse(
        status_code=400,
        content={
            "code": "VALIDATION_ERROR",
            "message": "请求参数校验失败",
            "data": details,
            "traceId": _uuid.uuid4().hex,
            "timestamp": _now_local(),
        },
    )


# ---------------------------------------------------------------- 模型定义
class Direction(BaseModel):
    """单条推荐方向的结构化评分数据（来自 career-core 规则引擎）。"""

    direction_id: int
    name: str
    type: Optional[str] = None
    score: float = 0.0
    rank: Optional[int] = None
    personality_tags: Optional[List[str]] = None
    matches: Optional[Dict[str, float]] = None
    gaps: Optional[Dict[str, float]] = None


class ExplainRequest(BaseModel):
    """推荐解释请求：最小化结构化输入，不包含学生身份信息。"""

    student_id: Optional[int] = None
    personality: Optional[List[str]] = None
    direction: Direction


class ExplainResponse(BaseModel):
    """推荐解释响应：自然语言理由 + 所用模型名。"""

    reason: str
    model: str


# ---------------------------------------------------------------- 路由
@app.post("/v1/recommendation/explain", response_model=ExplainResponse)
def recommendation_explain(req: ExplainRequest,
                           authorization: Optional[str] = Header(default=None)) -> ExplainResponse:
    """根据结构化评分数据生成自然语言推荐解释（调用大模型，失败抛 502 由调用方回退）。"""
    import os

    from fastapi import HTTPException as _HTTPException

    key = os.getenv("GATEWAY_API_KEY", "")
    if not key:
        raise _HTTPException(status_code=503, detail="AI 服务未配置内部密钥，拒绝服务")
    if authorization != f"Bearer {key}":
        raise _HTTPException(status_code=401, detail="内部密钥无效或缺失")
    from services.recommendation_explainer import explain

    return explain(req)
@app.get("/health")
def health() -> Dict[str, object]:
    """健康检查（含网关 + Agent + RAG 概要）。"""
    summary: Dict[str, object] = {"status": "ok", "service": "career-ai"}
    try:
        from gateway.client import get_gateway
        from gateway.db import get_engine

        gw = get_gateway()
        summary["gateway"] = {
            "groups": [g.name for g in gw.config.groups],
            "rpm": gw.config.rpm,
            "timeout": gw.config.timeout,
            "maxRetries": gw.config.max_retries,
            "callLogDb": get_engine() is not None,
        }
    except Exception as exc:  # noqa: BLE001 - 健康检查不因网关未就绪而失败
        summary["gateway"] = {"status": "unavailable", "reason": str(exc)}

    # Agent 状态
    try:
        from api.routes_agent import _get_compiled_agent

        summary["agent"] = {
            "available": _get_compiled_agent() is not None,
        }
    except Exception as exc:  # noqa: BLE001
        summary["agent"] = {"available": False, "reason": str(exc)}

    # RAG 状态
    try:
        from agent.rag import get_retriever

        stats = get_retriever().store.get_stats()
        summary["rag"] = {
            "chunks": stats.total_chunks,
            "vectorized": stats.total_vectorized_chunks,
            "db": stats.db_path,
        }
    except Exception as exc:  # noqa: BLE001
        summary["rag"] = {"status": "unavailable", "reason": str(exc)}

    return summary


@app.get("/metrics", response_class=PlainTextResponse)
def prometheus_metrics(authorization: Optional[str] = Header(default=None)) -> str:
    """Prometheus 指标（career_ai_requests_total / tokens / duration 直方图）。

    Grafana 抓取示例：job=career-ai, targets=['127.0.0.1:8000'], metrics_path=/metrics；
    p95 = histogram_quantile(0.95, sum(rate(career_ai_duration_seconds_bucket[5m])) by (le, scene))。
    Demo 精简点：进程内累计（多 worker 各自累计，抓取端 sum 聚合）。
    复审加固：/metrics 同网关 Bearer 鉴权（此前无鉴权暴露请求量/token 量）。
    """
    import hmac
    import os

    key = os.getenv("GATEWAY_API_KEY", "")
    provided = (authorization or "").strip()
    if not key or not provided.startswith("Bearer ") or not hmac.compare_digest(
        provided[len("Bearer "):], key
    ):
        raise HTTPException(status_code=401, detail="指标接口需内部密钥")
    from gateway.metrics import metrics

    return metrics.render()
