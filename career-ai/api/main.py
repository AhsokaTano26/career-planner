"""生涯规划智能服务（career-ai）入口。

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
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel

# 加载 career-ai/.env（如存在）
load_dotenv(Path(__file__).resolve().parent.parent / ".env")


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """启动时预热网关（Demo 精简点：失败不阻塞启动，调用时报错）。"""
    try:
        from gateway.client import get_gateway

        gw = get_gateway()
        groups = [g.name for g in gw.config.groups]
        logging.getLogger("gateway").info("AI 网关就绪：模型组=%s rpm=%s", groups, gw.config.rpm)
    except Exception as exc:  # noqa: BLE001 - fail-open
        logging.getLogger("gateway").warning("AI 网关初始化失败（调用时将报错）：%s", exc)
    yield


app = FastAPI(title="career-ai", version="0.2.0", lifespan=lifespan)


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
            "data": [],
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
def recommendation_explain(req: ExplainRequest) -> ExplainResponse:
    """根据结构化评分数据生成自然语言推荐解释（调用大模型，失败抛 502 由调用方回退）。"""
    from services.recommendation_explainer import explain

    return explain(req)


@app.get("/health")
def health() -> Dict[str, object]:
    """健康检查（含网关概要：模型组 / rpm / 日志落库配置）。"""
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
    return summary
