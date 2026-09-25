"""LLM 适配层：返回指向 LiteLLM 网关的 LangChain ChatOpenAI 实例。

核心设计：career-ai 的网关（POST /v1/chat/completions）是 OpenAI 兼容格式，
因此 LangGraph 的 LangChain 模型可以直接指向网关 URL——Agent 的 LLM 调用
经网关享受限流（rpm）、多渠道重试/降级、ai_call_log 调用日志。

参考：借鉴代码/agent-service-toolkit/src/core/llm.py 的 get_model() 工厂模式。
"""

from __future__ import annotations

import os
from functools import cache

from langchain_openai import ChatOpenAI


@cache
def get_agent_model(model_name: str | None = None) -> ChatOpenAI:
    """返回指向 LiteLLM 网关的 ChatOpenAI 实例（进程级缓存）。

    网关是 OpenAI 兼容的（POST /v1/chat/completions），所以用 ChatOpenAI 即可。
    :param model_name: 模型名，缺省用 LLM_MODEL（默认 deepseek-v4-flash）。
    """
    base_url = os.getenv("AGENT_GATEWAY_BASE_URL", "http://127.0.0.1:8000/v1")
    api_key = os.getenv("GATEWAY_API_KEY", "")
    model = model_name or os.getenv("LLM_MODEL", "deepseek-v4-flash")
    return ChatOpenAI(
        model=model,
        temperature=0.7,
        streaming=False,  # 先非流式
        openai_api_base=base_url,
        openai_api_key=api_key,
        max_retries=2,
        timeout=30,
    )


__all__ = ["get_agent_model"]
