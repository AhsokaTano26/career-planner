"""网关配置：从环境变量解析渠道凭据、模型组、限流与重试参数。

渠道凭据解析（Demo 精简点 / 后续迭代替换位置）：
    deepseek/*   取 LLM_API_KEY / LLM_BASE_URL
    openai/*     取 OPENAI_API_KEY（LiteLLM 标准环境变量）
    anthropic/*  取 ANTHROPIC_API_KEY
    其他 provider 交由 LiteLLM 按其环境变量约定解析（api_key=None）
"""

from __future__ import annotations

import json
import os
from dataclasses import dataclass, field


@dataclass
class ModelGroup:
    """一个业务模型组：models 内多渠道负载均衡，fallbacks 为整组降级链。"""

    name: str
    models: list[str] = field(default_factory=list)
    fallbacks: list[str] = field(default_factory=list)


@dataclass
class GatewayConfig:
    api_key: str
    groups: list[ModelGroup]
    rpm: int
    timeout: float
    max_retries: int
    # 生产化（2026-09）：按 scene 的 max_tokens 上限 + 单日 token 预算（见 .env.example）。
    # 缺省不限（零行为变化）；超预算由 client 抛 GatewayRateLimited（映射 429）。
    scene_max_tokens: dict[str, int] = field(default_factory=dict)
    daily_token_budget: dict[str, int] = field(default_factory=dict)

    def group(self, name: str) -> ModelGroup | None:
        for g in self.groups:
            if g.name == name:
                return g
        return None

    def default_group(self) -> ModelGroup | None:
        return self.group("default") or (self.groups[0] if self.groups else None)


def load_config() -> GatewayConfig:
    """读取环境变量并构建配置；非法 JSON / 空模型组回退到 DeepSeek 单渠道默认值。"""
    groups = _parse_groups(os.getenv("GATEWAY_MODEL_GROUPS", ""))
    return GatewayConfig(
        api_key=os.getenv("GATEWAY_API_KEY", ""),
        groups=groups,
        rpm=_to_int(os.getenv("GATEWAY_RPM"), 60),
        timeout=_to_float(os.getenv("GATEWAY_TIMEOUT"), 30.0),
        max_retries=_to_int(os.getenv("GATEWAY_MAX_RETRIES"), 2),
        scene_max_tokens=_parse_int_map(os.getenv("GATEWAY_SCENE_MAX_TOKENS", "")),
        daily_token_budget=_parse_int_map(os.getenv("GATEWAY_DAILY_TOKEN_BUDGET", "")),
    )


def _parse_int_map(raw: str) -> dict[str, int]:
    """解析 {"scene": 整数} JSON；非法/非正数条目丢弃（fail-open，不阻断启动）。"""
    if not raw or not raw.strip():
        return {}
    try:
        data = json.loads(raw)
    except (json.JSONDecodeError, TypeError, ValueError):
        return {}
    out: dict[str, int] = {}
    if isinstance(data, dict):
        for key, value in data.items():
            try:
                number = int(value)
            except (TypeError, ValueError):
                continue
            if key and number > 0:
                out[str(key)] = number
    return out


def channel_credentials(model: str) -> tuple[str | None, str | None]:
    """按模型串前缀返回 (api_key, api_base)；未识别的 provider 返回 (None, None)。"""
    provider = model.split("/", 1)[0].lower() if "/" in model else ""
    if provider == "deepseek":
        return os.getenv("LLM_API_KEY") or None, os.getenv("LLM_BASE_URL") or None
    if provider == "openai":
        return os.getenv("OPENAI_API_KEY") or None, None
    if provider == "anthropic":
        return os.getenv("ANTHROPIC_API_KEY") or None, None
    return None, None


def _parse_groups(raw: str) -> list[ModelGroup]:
    if raw.strip():
        try:
            data = json.loads(raw)
            groups = [
                ModelGroup(
                    name=str(item.get("group", "")).strip(),
                    models=[str(m) for m in item.get("models", []) if str(m).strip()],
                    fallbacks=[str(m) for m in item.get("fallbacks", []) if str(m).strip()],
                )
                for item in data
                if isinstance(item, dict)
            ]
            groups = [g for g in groups if g.name and g.models]
            if groups:
                return groups
        except (json.JSONDecodeError, TypeError, ValueError):
            pass
    # 缺省：DeepSeek 单渠道（与 LLM_MODEL 对齐）
    return [ModelGroup(name="default", models=[f"deepseek/{os.getenv('LLM_MODEL', 'deepseek-v4-flash')}"])]


def _to_int(raw: str | None, default: int) -> int:
    try:
        return int(raw) if raw not in (None, "") else default
    except (TypeError, ValueError):
        return default


def _to_float(raw: str | None, default: float) -> float:
    try:
        return float(raw) if raw not in (None, "") else default
    except (TypeError, ValueError):
        return default
