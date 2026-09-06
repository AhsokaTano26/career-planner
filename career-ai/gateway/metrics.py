"""Prometheus 文本格式指标（零依赖，手写 exposition）。

指标：
    - career_ai_requests_total{scene,status}：请求计数；
    - career_ai_tokens_total{scene}：累计 token；
    - career_ai_duration_seconds：按 scene 分桶的延迟直方图（桶边界固定，p95 由 Grafana 的 histogram_quantile 算）。

抓取：GET /metrics（见 api/main.py）。Grafana 侧示例：
    histogram_quantile(0.95, sum(rate(career_ai_duration_seconds_bucket[5m])) by (le, scene))

Demo 精简点 / 后续迭代替换位置：进程内累计（多 worker 各自累计，抓取端 sum 聚合即可）；
直方图分桶固定；后续若引入 prometheus_client 可整体替换本模块（接口 observe/render 不变）。
"""

from __future__ import annotations

import threading
from collections import defaultdict

_BUCKETS = (0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, float("inf"))

# 复审加固：scene 基数上限（调用方可传任意 scene 串，无上限则恶意/误传高基数 scene
# 致内存持续膨胀；超限统一归入 overflow，Grafana 可据此告警排查调用方）。
_MAX_SCENES = 200
_OVERFLOW_SCENE = "overflow"


class Metrics:
    """线程安全指标注册表。"""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._requests: dict[tuple[str, str], int] = defaultdict(int)
        self._tokens: dict[str, int] = defaultdict(int)
        self._hist: dict[str, list[int]] = defaultdict(lambda: [0] * len(_BUCKETS))
        self._scenes: set[str] = set()

    def observe(self, scene: str, status: str, duration_s: float, tokens: int | None) -> None:
        # 稳定性：scene 进 label 前消毒（外部可传任意串，含引号/换行会破坏 exposition）
        scene = _escape(scene)
        status = _escape(status)
        with self._lock:
            if scene not in self._scenes:
                if len(self._scenes) >= _MAX_SCENES:
                    scene = _OVERFLOW_SCENE
                else:
                    self._scenes.add(scene)
            self._requests[(scene, status)] += 1
            if tokens:
                self._tokens[scene] += int(tokens)
            for i, bound in enumerate(_BUCKETS):
                if duration_s <= bound:
                    self._hist[scene][i] += 1

    def render(self) -> str:
        with self._lock:
            lines = [
                "# HELP career_ai_requests_total 网关请求计数",
                "# TYPE career_ai_requests_total counter",
            ]
            for (scene, status), count in sorted(self._requests.items()):
                lines.append(
                    'career_ai_requests_total{scene="%s",status="%s"} %d' % (scene, status, count)
                )
            lines += [
                "# HELP career_ai_tokens_total 累计 token",
                "# TYPE career_ai_tokens_total counter",
            ]
            for scene, total in sorted(self._tokens.items()):
                lines.append('career_ai_tokens_total{scene="%s"} %d' % (scene, total))
            lines += [
                "# HELP career_ai_duration_seconds 请求延迟直方图",
                "# TYPE career_ai_duration_seconds histogram",
            ]
            for scene, buckets in sorted(self._hist.items()):
                # observe() 存的已是累计值（obs 落入 bound>=value 的所有桶），直接输出不再累加
                for bound, count in zip(_BUCKETS, buckets):
                    le = "+Inf" if bound == float("inf") else ("%g" % bound)
                    lines.append(
                        'career_ai_duration_seconds_bucket{scene="%s",le="%s"} %d'
                        % (scene, le, count)
                    )
                lines.append('career_ai_duration_seconds_count{scene="%s"} %d' % (scene, buckets[-1]))
            return "\n".join(lines) + "\n"

    def reset(self) -> None:
        with self._lock:
            self._requests.clear()
            self._tokens.clear()
            self._hist.clear()
            self._scenes.clear()


def _escape(value: str) -> str:
    """Prometheus label 值转义（\ → \\、" → \"、换行 → 空格），超长截断。"""
    text = (value or "unknown")[:64]
    return text.replace("\\", "\\\\").replace('"', '\\"').replace("\n", " ").replace("\r", " ")


metrics = Metrics()
