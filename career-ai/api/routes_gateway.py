"""AI 网关对外路由（本计划新增，后续需同步 Apifox 文档）：

    POST /v1/chat/completions      OpenAI 兼容入口（career-core LlmGateway 与外部工具调用）
    POST /api/v1/gateway/generate  高层入口（带 scene 归因，返回网关元数据）

鉴权：配置 GATEWAY_API_KEY 后强制 Bearer 校验（Demo 精简点：静态密钥，单值）。
Demo 精简点 / 后续迭代替换位置：/v1/chat/completions 暂不支持 stream（stream=true 报 400）；
不支持的 OpenAI 参数（tools/logprobs 等）直接忽略。
"""

from __future__ import annotations

import hmac
import os
import time
import uuid
from typing import List, Literal, Optional

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, Field, model_validator

from gateway.client import GatewayError, get_gateway
from gateway.ratelimit import GatewayRateLimited

router = APIRouter(tags=["gateway"])

_DISCLAIMER = "智能生成，供探索参考"


# ---------------------------------------------------------------- 鉴权
def _require_gateway_key(authorization: Optional[str]) -> None:
    key = os.getenv("GATEWAY_API_KEY", "")
    # 稳定性：compare_digest 防计时侧信道；strip 容忍多余空格
    provided = (authorization or "").strip()
    if not key or not provided.startswith("Bearer ") or not hmac.compare_digest(
        provided[len("Bearer "):], key
    ):
        raise HTTPException(status_code=401, detail="网关密钥无效或缺失")


# ---------------------------------------------------------------- /v1/chat/completions（OpenAI 兼容）
class GatewayMessage(BaseModel):
    role: str = Field(max_length=32)
    # 稳定性：单条内容封顶，超长前置 400（此前透传 provider 后转 502，难区分输入非法与渠道故障）
    content: str = Field(min_length=1, max_length=30000)


class ChatCompletionRequest(BaseModel):
    model: Optional[str] = Field(default=None, max_length=64)  # 网关模型组名（如 default）；缺省用 default 组
    messages: List[GatewayMessage] = Field(min_length=1, max_length=50)
    temperature: float = Field(default=0.7, ge=0, le=2)
    max_tokens: int = Field(default=500, ge=1, le=8000)
    stream: bool = False
    user: Optional[str] = Field(default=None, max_length=64)  # 脱敏用户引用（写入 ai_call_log.user_ref)
    # 回答质量迭代：JSON mode 透传（LiteLLM response_format，如 {"type": "json_object"}）
    response_format: Optional[dict] = None

    @model_validator(mode="after")
    def _check_total_size(self):
        # 复审加固：单条 30k×50 条≈1.5M 无总上限→总负载封顶 120k 字符（超长前置 400）
        total = sum(len(m.content or "") for m in self.messages)
        if total > 120_000:
            raise ValueError("messages 总长度超限（120000 字符）")
        return self


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
            response_format=req.response_format,
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
    messages: List[GatewayMessage] = Field(min_length=1, max_length=50)
    scene: str = Field(default="gateway_api", max_length=64)
    modelGroup: Optional[str] = Field(default=None, max_length=64)
    temperature: float = Field(default=0.7, ge=0, le=2)
    maxTokens: int = Field(default=500, ge=1, le=8000)
    userRef: Optional[str] = Field(default=None, max_length=64)
    promptVersion: Optional[str] = Field(default=None, max_length=64)

    @model_validator(mode="after")
    def _check_total_size(self):
        total = sum(len(m.content or "") for m in self.messages)
        if total > 120_000:
            raise ValueError("messages 总长度超限（120000 字符）")
        return self


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
