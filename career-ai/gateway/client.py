"""网关统一入口：generate() 一次调用完成 限流 → 路由/重试/降级 → 结果提取。

复用 LiteLLM：Router 负责多渠道负载均衡、num_retries 指数退避重试、fallbacks 整组降级；
AiCallLogHandler 回调负责 ai_call_log 落库。业务层只依赖本模块与 GatewayError。
"""

from __future__ import annotations

import hashlib
import json
import time
import uuid
from dataclasses import dataclass

import litellm
from litellm import Router

from gateway.config import GatewayConfig, channel_credentials, load_config
from gateway.logging_callback import AiCallLogHandler
from gateway.ratelimit import GatewayRateLimited, TokenBucket


class GatewayError(RuntimeError):
    """网关调用失败（渠道全部不可用 / 返回结构异常 / 未配置渠道）。"""


@dataclass
class GenerateResult:
    text: str
    model: str | None
    request_id: str
    duration_ms: int
    total_tokens: int | None


class GatewayClient:
    """线程安全；FastAPI 多 worker 下每进程一个实例（见 get_gateway()）。"""

    def __init__(self, config: GatewayConfig | None = None) -> None:
        self.config = config or load_config()
        self._bucket = TokenBucket(self.config.rpm)
        self._handler = AiCallLogHandler()
        callbacks = list(litellm.callbacks or [])
        if not any(isinstance(c, AiCallLogHandler) for c in callbacks):
            litellm.callbacks = callbacks + [self._handler]
        self._router = self._build_router()

    def generate(
        self,
        messages: list[dict],
        *,
        scene: str = "career_chat",
        user_ref: str | None = None,
        temperature: float = 0.7,
        max_tokens: int = 500,
        model_group: str | None = None,
        prompt_version: str | None = None,
    ) -> GenerateResult:
        """统一生成入口。

        :raises gateway.ratelimit.GatewayRateLimited: 超过 rpm 限流（映射 429）。
        :raises GatewayError: 渠道全部失败 / 返回结构异常。
        """
        group = model_group or self._default_group_name()
        request_id = uuid.uuid4().hex
        input_hash = self._sha256(json.dumps(messages, ensure_ascii=False, sort_keys=True))
        request_hash = self._sha256("%s|%s|%s" % (scene, group, input_hash))
        metadata = {
            "request_id": request_id,
            "scene": scene,
            "user_ref": user_ref,
            "prompt_version": prompt_version,
            "request_hash": request_hash,
            "input_hash": input_hash,
        }
        self._bucket.acquire()
        started = time.perf_counter()
        try:
            response = self._router.completion(
                model=group,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens,
                num_retries=self.config.max_retries,
                timeout=self.config.timeout,
                metadata=metadata,
            )
        except GatewayRateLimited:
            raise
        except Exception as exc:  # noqa: BLE001 - 统一转 GatewayError
            raise GatewayError("调用大模型失败（group=%s）：%s" % (group, exc)) from exc
        duration_ms = int((time.perf_counter() - started) * 1000)
        text = self._extract_text(response)
        return GenerateResult(
            text=text,
            model=self._model_used(response),
            request_id=request_id,
            duration_ms=duration_ms,
            total_tokens=self._total_tokens(response),
        )

    # ---------------------------------------------------------------- Router 构建
    def _build_router(self) -> Router:
        model_list: list[dict] = []
        fallbacks: list[dict] = []
        for g in self.config.groups:
            for m in g.models:
                model_list.append(self._deployment(g.name, m))
            if g.fallbacks:
                fallback_name = "%s-fallback" % g.name
                for m in g.fallbacks:
                    model_list.append(self._deployment(fallback_name, m))
                fallbacks.append({"default_model": fallback_name})
        if not model_list:
            raise GatewayError("网关未配置任何模型渠道（检查 GATEWAY_MODEL_GROUPS）")
        return Router(model_list=model_list, fallbacks=fallbacks or None, num_retries=self.config.max_retries)

    def _deployment(self, model_name: str, model: str) -> dict:
        api_key, api_base = channel_credentials(model)
        params: dict = {"model": model, "timeout": self.config.timeout}
        if api_key:
            params["api_key"] = api_key
        if api_base:
            params["api_base"] = api_base
        return {"model_name": model_name, "litellm_params": params}

    # ---------------------------------------------------------------- 响应提取
    def _extract_text(self, response) -> str:
        try:
            return str(response["choices"][0]["message"]["content"]).strip()
        except (KeyError, IndexError, TypeError) as exc:
            raise GatewayError("大模型返回结构异常：%s" % (response,)) from exc

    def _model_used(self, response) -> str | None:
        model = getattr(response, "model", None) or (
            response.get("model") if isinstance(response, dict) else None
        )
        return str(model)[:64] if model else None

    def _total_tokens(self, response) -> int | None:
        usage = getattr(response, "usage", None) or (
            response.get("usage") if isinstance(response, dict) else None
        )
        if usage is None:
            return None
        total = getattr(usage, "total_tokens", None)
        if total is None and isinstance(usage, dict):
            total = usage.get("total_tokens")
        return int(total) if total is not None else None

    def _default_group_name(self) -> str:
        group = self.config.default_group()
        if group is None:
            raise GatewayError("网关未配置模型组（检查 GATEWAY_MODEL_GROUPS）")
        return group.name

    @staticmethod
    def _sha256(text: str) -> str:
        return hashlib.sha256(text.encode("utf-8")).hexdigest()


_client: GatewayClient | None = None


def get_gateway() -> GatewayClient:
    """进程级单例；首次调用时按环境变量构建（测试可先 reset_gateway()）。"""
    global _client
    if _client is None:
        _client = GatewayClient()
    return _client


def reset_gateway() -> None:
    """测试/热更新用：丢弃单例，下次 get_gateway() 重建。"""
    global _client
    _client = None
