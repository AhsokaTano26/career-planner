"""LiteLLM 日志回调：把每次成功/失败调用写入 ai_call_log。

注册方式：client 构建 Router 时将本 Handler 实例追加到 litellm.callbacks。
Demo 精简点 / 后续迭代替换位置：
    - Router 重试/降级会为同一 request_id 触发多个事件，进程内状态机 + db 层 upsert
      保证最终只留一条（多实例部署时需改 Redis，见 db.py 边界说明）；
    - fallback 生效时最终状态记 DEGRADED；实测 litellm 1.98 Router 重试/降级不触发 failure
      回调（仅最终结果触发），DEGRADED 为防御路径（若回调行为变化即生效），当前多为 SUCCESS。
"""

from __future__ import annotations

import logging
import threading
from datetime import datetime

from litellm.integrations.custom_logger import CustomLogger

from gateway.db import insert_ai_call_log

logger = logging.getLogger("gateway.callback")


class AiCallLogHandler(CustomLogger):
    """成功/失败事件 → ai_call_log 单条落库（fail-open）。

    Router 重试/降级会为同一 request_id 触发多个事件，落库规则：
        - 失败事件先到 → 记 FAILED/TIMEOUT；
        - 成功事件后到（fallback 生效）→ 覆盖为 DEGRADED（db 层 upsert）；
        - 成功事件先到 → 已记 SUCCESS，后续同 id 事件忽略。
    """

    def __init__(self) -> None:
        super().__init__()
        self._seen: dict[str, str] = {}
        self._lock = threading.Lock()

    # ---------------------------------------------------------------- LiteLLM 事件
    def log_success_event(self, kwargs, response_obj, start_time, end_time) -> None:
        self._handle(kwargs, response_obj, start_time, end_time, status="SUCCESS")

    async def async_log_success_event(self, kwargs, response_obj, start_time, end_time) -> None:
        self._handle(kwargs, response_obj, start_time, end_time, status="SUCCESS")

    def log_failure_event(self, kwargs, response_obj, start_time, end_time) -> None:
        self._handle(kwargs, response_obj, start_time, end_time, status=self._failure_status(kwargs))

    async def async_log_failure_event(self, kwargs, response_obj, start_time, end_time) -> None:
        self._handle(kwargs, response_obj, start_time, end_time, status=self._failure_status(kwargs))

    # ---------------------------------------------------------------- 内部处理
    def _handle(self, kwargs, response_obj, start_time, end_time, *, status: str) -> None:
        try:
            meta = self._metadata(kwargs)
            request_id = meta.get("request_id") or str(kwargs.get("litellm_call_id") or "")
            if not request_id:
                return
            with self._lock:
                seen = self._seen.get(request_id)
                if seen == "SUCCESS" or seen == status:
                    return
                # 重试/降级恢复的成功记 DEGRADED，保留「发生过失败」的可观测信息
                effective = "DEGRADED" if (status == "SUCCESS" and seen in ("FAILED", "TIMEOUT")) else status
                self._seen[request_id] = effective
            duration_ms = self._duration_ms(start_time, end_time)
            insert_ai_call_log(
                request_id=request_id,
                scene=str(meta.get("scene") or "unknown"),
                status=effective,
                model_name=self._model_name(kwargs),
                user_ref=meta.get("user_ref"),
                prompt_version=meta.get("prompt_version"),
                duration_ms=duration_ms,
                token_estimate=self._token_estimate(response_obj),
                request_hash=meta.get("request_hash"),
                input_hash=meta.get("input_hash"),
            )
        except Exception as exc:  # noqa: BLE001 - fail-open
            logger.warning("ai_call_log 回调处理异常：%s", exc)

    def _metadata(self, kwargs) -> dict:
        params = kwargs.get("litellm_params") or {}
        meta = params.get("metadata")
        return meta if isinstance(meta, dict) else {}

    def _model_name(self, kwargs) -> str | None:
        model = kwargs.get("model") or (kwargs.get("litellm_params") or {}).get("model")
        return str(model)[:64] if model else None

    def _duration_ms(self, start_time, end_time) -> int | None:
        if isinstance(start_time, datetime) and isinstance(end_time, datetime):
            return int((end_time - start_time).total_seconds() * 1000)
        return None

    def _token_estimate(self, response_obj) -> int | None:
        usage = getattr(response_obj, "usage", None) or (
            response_obj.get("usage") if isinstance(response_obj, dict) else None
        )
        if usage is None:
            return None
        prompt = getattr(usage, "prompt_tokens", None)
        if prompt is None and isinstance(usage, dict):
            prompt = usage.get("prompt_tokens")
        completion = getattr(usage, "completion_tokens", None)
        if completion is None and isinstance(usage, dict):
            completion = usage.get("completion_tokens")
        if prompt is None and completion is None:
            return None
        return int(prompt or 0) + int(completion or 0)

    def _failure_status(self, kwargs) -> str:
        exc = kwargs.get("exception")
        if exc is None:
            info = (kwargs.get("litellm_params") or {}).get("exception_info") or {}
            exc = info.get("exception") if isinstance(info, dict) else None
        text = ("%s %s" % (type(exc).__name__, exc)).lower() if exc else ""
        return "TIMEOUT" if ("timeout" in text or "timed out" in text) else "FAILED"
