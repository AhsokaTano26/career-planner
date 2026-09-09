"""网关统一入口：generate() 一次调用完成 限流 → 路由/重试/降级 → 结果提取。

复用 LiteLLM：Router 负责多渠道负载均衡、num_retries 指数退避重试、fallbacks 整组降级；
AiCallLogHandler 回调负责 ai_call_log 落库。业务层只依赖本模块与 GatewayError。
"""

from __future__ import annotations

import hashlib
import json
import threading
import time
import uuid
from dataclasses import dataclass

import litellm
from litellm import Router

from gateway.config import GatewayConfig, channel_credentials, load_config
from gateway.budget import DailyTokenBudget
from gateway.logging_callback import AiCallLogHandler
from gateway.metrics import metrics
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
        self._budget = DailyTokenBudget(self.config.daily_token_budget)
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
        response_format: dict | None = None,
    ) -> GenerateResult:
        """统一生成入口。

        :raises gateway.ratelimit.GatewayRateLimited: 超过 rpm 限流 / 单日 token 预算（映射 429）。
        :raises GatewayError: 渠道全部失败 / 返回结构异常。
        """
        group = model_group or self._default_group_name()
        # 生产化：按 scene 钳制 max_tokens（GATEWAY_SCENE_MAX_TOKENS，未配置不限）
        effective_max_tokens = self._clamp_max_tokens(scene, max_tokens)
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
        started = time.perf_counter()
        try:
            self._bucket.acquire()
            # 预算预占（防 check-then-act 并发超限，多退少补见 settle）
            self._budget.reserve(scene, effective_max_tokens)
            completion_kwargs: dict = dict(
                model=group,
                messages=messages,
                temperature=temperature,
                max_tokens=effective_max_tokens,
                num_retries=self.config.max_retries,
                timeout=self.config.timeout,
                metadata=metadata,
            )
            # JSON mode：仅透传给真实 Router；_FakeRouter（测试桩）按 **kwargs 接收无影响
            if response_format is not None:
                completion_kwargs["response_format"] = response_format
            response = self._router.completion(**completion_kwargs)
        except GatewayRateLimited:
            metrics.observe(scene, "RATE_LIMITED", time.perf_counter() - started, None)
            raise
        except Exception as exc:  # noqa: BLE001 - 统一转 GatewayError
            self._budget.settle(scene, effective_max_tokens, 0)
            metrics.observe(scene, "FAILED", time.perf_counter() - started, None)
            raise GatewayError("调用大模型失败（group=%s）：%s" % (group, exc)) from exc
        duration_s = time.perf_counter() - started
        duration_ms = int(duration_s * 1000)
        try:
            text = self._extract_text(response)
            model_used = self._model_used(response)
            total_tokens = self._total_tokens(response)
        except GatewayError:
            # 渠道返回 200 但结构异常：同样释放预算预占并记 FAILED（此前完全隐身）
            self._budget.settle(scene, effective_max_tokens, 0)
            metrics.observe(scene, "FAILED", duration_s, None)
            raise
        status = "SUCCESS"
        # fallback 实际生效 → 显式记一条 DEGRADED + scene 后缀（回调只记了 SUCCESS，upsert 覆盖为最终态）
        if self._used_fallback(group, model_used):
            status = "DEGRADED"
            self._log_fallback(request_id, scene, user_ref, prompt_version, model_used,
                               duration_ms, total_tokens, request_hash, input_hash)
        self._budget.settle(scene, effective_max_tokens, total_tokens)
        metrics.observe(scene, status, duration_s, total_tokens)
        return GenerateResult(
            text=text,
            model=model_used,
            request_id=request_id,
            duration_ms=duration_ms,
            total_tokens=total_tokens,
        )

    # ---------------------------------------------------------------- Router 构建
    def _build_router(self) -> Router:
        model_list: list[dict] = []
        fallbacks: list[dict] = []
        for g in self.config.groups:
            for m in g.models:
                model_list.append(self._deployment(g.name, m))
            if g.fallbacks:
                # LiteLLM Router 语义：{模型组: [备用组]}；备用部署挂在 "<组>-fallback" 名下
                fallback_name = "%s-fallback" % g.name
                for m in g.fallbacks:
                    model_list.append(self._deployment(fallback_name, m))
                fallbacks.append({g.name: [fallback_name]})
        if not model_list:
            raise GatewayError("网关未配置任何模型渠道（检查 GATEWAY_MODEL_GROUPS）")
        return Router(model_list=model_list, fallbacks=fallbacks or None, num_retries=self.config.max_retries)

    def _clamp_max_tokens(self, scene: str, requested: int) -> int:
        """按 scene 钳制 max_tokens（GATEWAY_SCENE_MAX_TOKENS，未配置不限）。"""
        cap = self.config.scene_max_tokens.get(scene)
        if cap and requested > cap:
            return cap
        return requested

    def _used_fallback(self, group: str, model_used: str | None) -> bool:
        """实际命中的模型是否在该组的 fallback 名单里。

        复审加固（LiteLLM 官方 Router 语义对照）：先看命中是否属于主组部署——
        属于则必非降级；再看是否属于 fallback 部署——属于则为降级；上游改名导致
        两边都对不上时宁可漏记不误记（DEGRADED 漏记只影响可观测，不影响正确性）。
        归一化（小写+去 provider 前缀）兼容 deepseek-chat vs deepseek/deepseek-chat。
        """
        if not model_used:
            return False
        cfg_group = self.config.group(group)
        if cfg_group is None or not cfg_group.fallbacks:
            return False
        used = self._normalize_model(model_used)
        if any(used == self._normalize_model(m) for m in cfg_group.models):
            return False
        return any(used == self._normalize_model(fb) for fb in cfg_group.fallbacks)

    @staticmethod
    def _normalize_model(model: str) -> str:
        name = (model or "").strip().lower()
        if "/" in name:
            name = name.split("/", 1)[1]
        return name

    def _log_fallback(self, request_id: str, scene: str, user_ref: str | None,
                      prompt_version: str | None, model_used: str | None, duration_ms: int,
                      total_tokens: int | None, request_hash: str, input_hash: str) -> None:
        """fallback 生效 → 显式落 DEGRADED + scene 后缀（同 request_id upsert 覆盖回调的 SUCCESS 行）。"""
        from gateway.db import insert_ai_call_log  # 局部导入：避免模块循环

        insert_ai_call_log(
            request_id=request_id,
            scene=("%s:fallback" % scene)[:32],
            status="DEGRADED",
            model_name=model_used,
            user_ref=user_ref,
            prompt_version=prompt_version,
            duration_ms=duration_ms,
            token_estimate=total_tokens,
            request_hash=request_hash,
            input_hash=input_hash,
        )

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
_client_lock = threading.Lock()


def get_gateway() -> GatewayClient:
    """进程级单例；首次调用时按环境变量构建（测试可先 reset_gateway()）。

    稳定性：双检锁，并发首次调用不再建出多实例（此前可双 Router + 双 callback 注册）。
    """
    global _client
    if _client is None:
        with _client_lock:
            if _client is None:
                _client = GatewayClient()
    return _client


def reset_gateway() -> None:
    """测试/热更新用：丢弃单例，下次 get_gateway() 重建。"""
    global _client
    _client = None
