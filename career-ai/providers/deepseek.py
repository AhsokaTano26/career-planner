"""DeepSeek 大模型 Provider（OpenAI 兼容接口）。

调用外部大模型的唯一出口；当前模型名默认 deepseek-v4-pro（可用环境变量 LLM_MODEL 覆盖）。
配置项（环境变量）：
    LLM_API_KEY   必填，API Key
    LLM_BASE_URL  可选，OpenAI 兼容 Base URL，默认 https://api.deepseek.com
    LLM_MODEL     可选，模型名，默认 deepseek-v4-pro

后续迭代（替换点）：在此扩展重试、限流、超时分级，以及 ai_call_log 调用记录
（见《具体实现细节_MVP_V1.0.md》智能与导入域）。
"""

from __future__ import annotations

import os

import httpx

DEFAULT_MODEL = "deepseek-v4-pro"
DEFAULT_BASE_URL = "https://api.deepseek.com"
DEFAULT_TIMEOUT = 20.0  # 秒


class LlmError(RuntimeError):
    """大模型调用失败（网络/鉴权/返回结构异常）。"""


def chat_completion(
    messages: list[dict],
    *,
    model: str | None = None,
    base_url: str | None = None,
    api_key: str | None = None,
    temperature: float = 0.7,
    max_tokens: int = 500,
    timeout: float = DEFAULT_TIMEOUT,
) -> str:
    """调用 OpenAI 兼容的 chat/completions，返回模型回复文本。

    :raises LlmError: 未配置 Key、网络失败、HTTP 错误或返回结构异常时抛出。
    """
    model = model or os.getenv("LLM_MODEL", DEFAULT_MODEL)
    base = (base_url or os.getenv("LLM_BASE_URL", DEFAULT_BASE_URL)).rstrip("/")
    key = api_key or os.getenv("LLM_API_KEY", "")
    if not key:
        raise LlmError("未配置 LLM_API_KEY，无法调用大模型")

    url = f"{base}/chat/completions"
    payload = {
        "model": model,
        "messages": messages,
        "temperature": temperature,
        "max_tokens": max_tokens,
    }
    headers = {"Authorization": f"Bearer {key}", "Content-Type": "application/json"}

    try:
        with httpx.Client(timeout=timeout) as client:
            resp = client.post(url, json=payload, headers=headers)
        resp.raise_for_status()
        data = resp.json()
    except LlmError:
        raise
    except Exception as exc:  # 网络/超时/HTTP 状态等
        raise LlmError(f"调用大模型失败（{model}）：{exc}") from exc

    try:
        content = data["choices"][0]["message"]["content"]
        return str(content).strip()
    except (KeyError, IndexError, TypeError) as exc:
        raise LlmError(f"大模型返回结构异常：{data}") from exc
