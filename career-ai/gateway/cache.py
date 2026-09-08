"""进程内 TTL LRU 缓存（推荐解释 identical prompt 去重）。

Demo 精简点 / 后续迭代替换位置：
    - 仅单实例生效；多实例部署时换 Redis（key=sha256(prompt)，value=explanations JSON）；
    - 线程安全（Lock + OrderedDict），容量/TTL 由环境变量配置。
"""

from __future__ import annotations

import os
import threading
import time
from collections import OrderedDict
from typing import Any


class TTLCache:
    """线程安全 TTL LRU：get 未命中/过期返回 None；set 驱逐最久未用。"""

    def __init__(self, maxsize: int = 256, ttl: float = 3600.0) -> None:
        self.maxsize = max(1, int(maxsize))
        self.ttl = max(1.0, float(ttl))
        self._data: OrderedDict[str, tuple[float, Any]] = OrderedDict()
        self._lock = threading.Lock()

    def get(self, key: str) -> Any | None:
        now = time.monotonic()
        with self._lock:
            item = self._data.get(key)
            if item is None:
                return None
            expires_at, value = item
            if expires_at <= now:
                del self._data[key]
                return None
            self._data.move_to_end(key)
            return value

    def set(self, key: str, value: Any) -> None:
        with self._lock:
            self._data[key] = (time.monotonic() + self.ttl, value)
            self._data.move_to_end(key)
            while len(self._data) > self.maxsize:
                self._data.popitem(last=False)

    def clear(self) -> None:
        with self._lock:
            self._data.clear()

    def __len__(self) -> int:
        with self._lock:
            return len(self._data)


def _to_int(raw: str | None, default: int) -> int:
    try:
        return int(raw) if raw not in (None, "") else default
    except (TypeError, ValueError):
        return default


def new_explain_cache() -> TTLCache:
    """按环境变量构建推荐解释缓存（GATEWAY_EXPLAIN_CACHE=0 关闭）。"""
    return TTLCache(
        maxsize=_to_int(os.getenv("GATEWAY_EXPLAIN_CACHE_MAXSIZE"), 256),
        ttl=_to_int(os.getenv("GATEWAY_EXPLAIN_CACHE_TTL"), 3600),
    )


def explain_cache_enabled() -> bool:
    return os.getenv("GATEWAY_EXPLAIN_CACHE", "1") not in ("0", "false", "False", "")
