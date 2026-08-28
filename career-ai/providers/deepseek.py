"""Deprecated：直连 DeepSeek Provider 已由 AI 网关（gateway/，基于 LiteLLM）取代。

Demo 兼容层：保留 LlmError / DEFAULT_MODEL / chat_completion 导出，内部转发网关，
使既有 `from providers.deepseek import ...` 的调用方无需改动。
后续迭代替换位置：确认无调用方后删除本文件，统一 import gateway.client。
"""

from __future__ import annotations

import os

from gateway.client import GatewayError, get_gateway

LlmError = GatewayError
DEFAULT_MODEL = os.getenv("LLM_MODEL", "deepseek-v4-flash")


def chat_completion(messages: list[dict], **kwargs) -> str:
    """兼容签名：转发网关统一入口（原直连 httpx 的实现已移除）。"""
    return get_gateway().generate(messages, **kwargs).text


__all__ = ["chat_completion", "LlmError", "DEFAULT_MODEL"]
