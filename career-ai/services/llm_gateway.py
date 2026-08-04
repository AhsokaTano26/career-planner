"""大模型网关：统一封装外部大模型调用。

Demo 实现：直连 DeepSeek（OpenAI 兼容接口），模型名默认 deepseek-v4-pro。
后续迭代（替换点）：在 providers/ 下扩展多 Provider 路由、重试/熔断、限流与 ai_call_log 调用记录。
"""

from __future__ import annotations

from providers.deepseek import DEFAULT_MODEL, LlmError, chat_completion


def generate(messages: list[dict], **kwargs) -> str:
    """统一入口：生成一次模型回复。

    :raises LlmError: 调用失败时抛出，由上层决定回退策略。
    """
    return chat_completion(messages, **kwargs)


__all__ = ["generate", "LlmError", "DEFAULT_MODEL"]

