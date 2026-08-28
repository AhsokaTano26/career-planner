"""进程内令牌桶限流（rpm）。

Demo 边界：仅网关级全局限流，单实例生效；后续迭代替换位置：
    - 多实例部署时改为 Redis 分布式限流；
    - 若引入按用户/按密钥配额，在此扩展维度。
"""

from __future__ import annotations

import threading
import time


class GatewayRateLimited(RuntimeError):
    """超过限流阈值（对应 HTTP 429 语义）。"""


class TokenBucket:
    """线程安全令牌桶：容量 rpm，按 rpm/60 每秒匀速补充。"""

    def __init__(self, rpm: int) -> None:
        self.capacity = max(1, int(rpm))
        self._tokens = float(self.capacity)
        self._refill_per_sec = self.capacity / 60.0
        self._updated = time.monotonic()
        self._lock = threading.Lock()

    def acquire(self) -> None:
        """取一枚令牌；不足时抛 GatewayRateLimited。"""
        with self._lock:
            now = time.monotonic()
            self._tokens = min(float(self.capacity), self._tokens + (now - self._updated) * self._refill_per_sec)
            self._updated = now
            if self._tokens < 1.0:
                raise GatewayRateLimited("请求过于频繁，超过网关限流阈值（rpm=%d）" % self.capacity)
            self._tokens -= 1.0
