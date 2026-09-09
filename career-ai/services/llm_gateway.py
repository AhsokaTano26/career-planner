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
    :param response_format: 透传 LiteLLM（如 {"type": "json_object"}），JSON 场景建议用 generate_json
    :raises LlmError: 调用失败时抛出，由上层决定回退策略。
    """
    scene = kwargs.pop("scene", "career_chat")
    user_ref = kwargs.pop("user_ref", None)
    result = get_gateway().generate(messages, scene=scene, user_ref=user_ref, **kwargs)
    return result.text


def generate_json(messages: list[dict], parse, *, repairs: int = 1, **kwargs) -> object:
    """JSON 场景统一入口：json_object 模式 + 输出校验失败时带错回修。

    :param parse: 输出解析函数（抛 ValueError 即触发回修）
    :param repairs: 最多回修次数（默认 1；纯离线测试桩同样适用）
    :raises LlmError: 渠道失败；ValueError: 回修后仍不合法
    """
    content = generate(messages, response_format={"type": "json_object"}, **kwargs)
    try:
        return parse(content)
    except ValueError:
        if repairs <= 0:
            raise
    retry_messages = list(messages) + [
        {"role": "user", "content": "上次输出不是合法 JSON（%s），请只输出合法 JSON，不要附加解释。" % _preview(content)},
    ]
    content = generate(retry_messages, response_format={"type": "json_object"}, **kwargs)
    return parse(content)


def _preview(content: str, limit: int = 200) -> str:
    text = (content or "").strip().replace("\n", " ")
    return text[:limit] if len(text) > limit else text


__all__ = ["generate", "generate_json", "LlmError", "GatewayError", "DEFAULT_MODEL"]
