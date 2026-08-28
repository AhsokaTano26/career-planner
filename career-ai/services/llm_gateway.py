"""大模型网关（兼容层）：转发到 gateway.client（LiteLLM Router：多渠道/重试/降级/日志）。

Demo 精简点 / 后续迭代替换位置：本模块仅保留兼容导出（LlmError / DEFAULT_MODEL / generate），
新代码请直接 import gateway.client。
"""

from __future__ import annotations

import os

from gateway.client import GatewayError, get_gateway

LlmError = GatewayError
DEFAULT_MODEL = os.getenv("LLM_MODEL", "deepseek-v4-flash")


def generate(messages: list[dict], **kwargs) -> str:
    """统一入口：生成一次模型回复文本（透传 temperature/max_tokens 等参数）。

    :param scene: ai_call_log 场景归因（career_chat/plan_generate/recommendation_explain/review_summarize/gateway_api）
    :param user_ref: 脱敏用户引用，写入 ai_call_log.user_ref
    :raises LlmError: 调用失败时抛出，由上层决定回退策略。
    """
    scene = kwargs.pop("scene", "career_chat")
    user_ref = kwargs.pop("user_ref", None)
    result = get_gateway().generate(messages, scene=scene, user_ref=user_ref, **kwargs)
    return result.text


__all__ = ["generate", "LlmError", "GatewayError", "DEFAULT_MODEL"]
