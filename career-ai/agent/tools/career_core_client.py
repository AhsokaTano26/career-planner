"""career-core API HTTP 客户端：Agent 工具反调 career-core 的统一出口。

鉴权：Bearer Token（AGENT_SERVICE_TOKEN，和网关 GATEWAY_API_KEY 一致的服务间鉴权模式）。
脱敏：所有工具返回数据在调用方（各工具模块）先 desensitize() 再进 LLM 上下文。
"""

from __future__ import annotations

import logging
import os

import httpx

logger = logging.getLogger("agent.tools.career_core_client")

_client: httpx.AsyncClient | None = None


def _get_client() -> httpx.AsyncClient:
    """获取进程级 HTTP 客户端（懒加载）。"""
    global _client
    if _client is None:
        base_url = os.getenv("CAREER_CORE_BASE_URL", "http://127.0.0.1:8080")
        token = os.getenv("AGENT_SERVICE_TOKEN", "")
        _client = httpx.AsyncClient(
            base_url=base_url,
            headers={"Authorization": f"Bearer {token}"},
            timeout=10.0,
        )
    return _client


async def get_json(path: str, params: dict | None = None) -> dict:
    """GET career-core API 并返回 data 字段（统一响应包装 {code, message, data, ...}）。

    :param path: API 路径，如 /api/v1/portrait/latest
    :param params: 查询参数
    :return: 响应的 data 字典
    :raises httpx.HTTPStatusError: HTTP 状态码 >= 400 时抛出
    """
    client = _get_client()
    resp = await client.get(path, params=params)
    resp.raise_for_status()
    body = resp.json()
    # 统一响应包装：{code, message, data, traceId, timestamp}
    if isinstance(body, dict) and "data" in body:
        return body["data"]
    return body


def close_client() -> None:
    """关闭 HTTP 客户端（lifespan 清理用）。"""
    global _client
    if _client is not None:
        import anyio

        try:
            anyio.from_thread.run(_client.aclose)
        except Exception:  # noqa: BLE001
            pass
        _client = None


__all__ = ["get_json", "close_client"]
