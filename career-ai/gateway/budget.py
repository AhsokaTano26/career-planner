"""网关单日 token 预算（进程内累计，fail-open）。

规则：
    - 按 scene 累计当日成功调用的 total_tokens；key 支持具体 scene 与通配 "*"（总额度）；
    - 任一命中额度超限 → 抛 GatewayRateLimited（映射 HTTP 429，core 侧走现有 FALLBACK 模板）；
    - 每日零点（本地时区日期变化）自动清零；未配置额度 = 不限（零行为变化）。

Demo 精简点 / 后续迭代替换位置：仅单实例生效；多实例换 Redis 计数。
"""

from __future__ import annotations

import threading
from datetime import date

from gateway.ratelimit import GatewayRateLimited


class DailyTokenBudget:
    """线程安全单日预算账本。

    并发安全（2026-09 稳定性）：调用前 reserve(max_tokens) 预占额度，
    调用后 settle(预占, 实际) 多退少补；纯 check→record 模式仍保留兼容
    （单线程/测试用），生产路径一律走预占。
    """

    def __init__(self, budgets: dict[str, int] | None = None) -> None:
        self._budgets = dict(budgets or {})
        self._spent: dict[str, int] = {}
        self._reserved: dict[str, int] = {}
        self._day: str = date.today().isoformat()
        self._lock = threading.Lock()

    def reconfigure(self, budgets: dict[str, int] | None) -> None:
        with self._lock:
            self._budgets = dict(budgets or {})

    def check(self, scene: str) -> None:
        """调用前检查；超限抛 GatewayRateLimited（兼容旧语义：预占 0）。"""
        self.reserve(scene, 0)

    def reserve(self, scene: str, amount: int) -> None:
        """预占额度；超限抛 GatewayRateLimited。"""
        amount = max(0, int(amount or 0))
        with self._lock:
            self._rollover_locked()
            for key in (scene, "*"):
                limit = self._budgets.get(key)
                if limit and self._spent.get(key, 0) + self._reserved.get(key, 0) + amount > limit:
                    raise GatewayRateLimited(
                        "超出网关单日 token 预算（%s 上限 %d，已用 %d，预占 %d）"
                        % ("总额度" if key == "*" else "场景 " + scene, limit,
                           self._spent.get(key, 0), self._reserved.get(key, 0))
                    )
            if amount:
                for key in (scene, "*"):
                    if key in self._budgets:
                        self._reserved[key] = self._reserved.get(key, 0) + amount

    def record(self, scene: str, tokens: int | None) -> None:
        """调用后累计（tokens 为 None/0 不累计）。"""
        self.settle(scene, 0, tokens)

    def settle(self, scene: str, reserved: int, actual: int | None) -> None:
        """结算：释放预占，按实际累计。"""
        reserved = max(0, int(reserved or 0))
        actual = max(0, int(actual or 0))
        with self._lock:
            self._rollover_locked()
            for key in (scene, "*"):
                if key in self._budgets:
                    self._reserved[key] = max(0, self._reserved.get(key, 0) - reserved)
                    if actual:
                        self._spent[key] = self._spent.get(key, 0) + actual

    def spent(self, scene: str) -> int:
        with self._lock:
            return self._spent.get(scene, 0)

    def _rollover_locked(self) -> None:
        today = date.today().isoformat()
        if today != self._day:
            self._day = today
            # 复审修复：跨零点同时清预占（此前只清 spent，残留 reserved 次日误 429 永不恢复）。
            # 无 Redis 方案下的说明：预占为纯内存态，进程崩溃即清零，不存在跨进程泄漏；
            # 进程内泄漏只可能来自 reserve 后未 settle 的代码路径——生产路径 generate() 的
            # 成功/限流/异常三分支均已 settle，新增路径必须同样保证（见 client.generate）。
            self._spent = {}
            self._reserved = {}

    def outstanding(self) -> dict[str, int]:
        """测试/排障用：当前未结算预占快照。"""
        with self._lock:
            return dict(self._reserved)
