"""AI 网关对外路由（本计划新增，后续需同步 Apifox 文档）：

    POST /v1/chat/completions      OpenAI 兼容入口（career-core LlmGateway 与外部工具调用）
    POST /api/v1/gateway/generate  高层入口（带 scene 归因，返回网关元数据）

鉴权：配置 GATEWAY_API_KEY 后强制 Bearer 校验（Demo 精简点：静态密钥，单值）。
Demo 精简点 / 后续迭代替换位置：/v1/chat/completions 暂不支持 stream（stream=true 报 400）；
不支持的 OpenAI 参数（tools/logprobs 等）直接忽略。
"""

from __future__ import annotations

import os
import time
import uuid
from typing import List, Literal, Optional

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field

from gateway.client import GatewayError, get_gateway
from gateway.ratelimit import GatewayRateLimited

router = APIRouter(tags=["gateway"])

_DISCLAIMER = "智能生成，供探索参考"


# ---------------------------------------------------------------- 鉴权
def _require_gateway_key(authorization: Optional[str]) -> None:
    key = os.getenv("GATEWAY_API_KEY", "")
    if key and authorization != f"Bearer {key}":
        raise HTTPException(status_code=401, detail="网关密钥无效或缺失")


# ---------------------------------------------------------------- /v1/chat/completions（OpenAI 兼容）
class GatewayMessage(BaseModel):
    role: str
    content: str


class ChatCompletionRequest(BaseModel):
    model: Optional[str] = None          # 网关模型组名（如 default）；缺省用 default 组
    messages: List[GatewayMessage] = Field(min_length=1)
    temperature: float = 0.7
    max_tokens: int = 500
    stream: bool = False
    user: Optional[str] = None           # 脱敏用户引用（写入 ai_call_log.user_ref）


@router.post("/v1/chat/completions")
def chat_completions(req: ChatCompletionRequest,
                     authorization: Optional[str] = Header(default=None)) -> dict:
    """OpenAI 兼容聊天补全（非流式）。model 传网关模型组名，未识别时回退 default 组。"""
    _require_gateway_key(authorization)
    if req.stream:
        raise HTTPException(status_code=400, detail="网关暂不支持 stream=true（Demo 精简点）")
    try:
        result = get_gateway().generate(
            [m.model_dump() for m in req.messages],
            scene="gateway_api",
            user_ref=req.user,
            temperature=req.temperature,
            max_tokens=req.max_tokens,
            model_group=req.model,
        )
    except GatewayRateLimited as exc:
        raise HTTPException(status_code=429, detail=str(exc)) from exc
    except GatewayError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return {
        "id": f"chatcmpl-{result.request_id}",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": result.model,
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": result.text},
            "finish_reason": "stop",
        }],
        # Demo 精简点：仅回填 total_tokens（prompt/completion 明细后续按需拆分）
        "usage": {"prompt_tokens": None, "completion_tokens": None,
                  "total_tokens": result.total_tokens},
    }


# ---------------------------------------------------------------- /api/v1/gateway/generate（高层）
class GatewayGenerateRequest(BaseModel):
    messages: List[GatewayMessage] = Field(min_length=1)
    scene: str = "gateway_api"
    modelGroup: Optional[str] = None
    temperature: float = 0.7
    maxTokens: int = 500
    userRef: Optional[str] = None
    promptVersion: Optional[str] = None


class GatewayGenerateResponse(BaseModel):
    text: str
    model: Optional[str] = None
    requestId: str
    durationMs: int
    totalTokens: Optional[int] = None
    disclaimer: str = _DISCLAIMER


@router.post("/api/v1/gateway/generate", response_model=GatewayGenerateResponse)
def gateway_generate(req: GatewayGenerateRequest,
                     authorization: Optional[str] = Header(default=None)) -> GatewayGenerateResponse:
    """网关高层入口：带场景归因，返回文本 + 网关元数据（模型/耗时/token）。"""
    _require_gateway_key(authorization)
    try:
        result = get_gateway().generate(
            [m.model_dump() for m in req.messages],
            scene=req.scene,
            user_ref=req.userRef,
            temperature=req.temperature,
            max_tokens=req.maxTokens,
            model_group=req.modelGroup,
            prompt_version=req.promptVersion,
        )
    except GatewayRateLimited as exc:
        raise HTTPException(status_code=429, detail=str(exc)) from exc
    except GatewayError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    return GatewayGenerateResponse(
        text=result.text,
        model=result.model,
        requestId=result.request_id,
        durationMs=result.duration_ms,
        totalTokens=result.total_tokens,
    )


__all__ = ["router", "GatewayGenerateResponse"]
