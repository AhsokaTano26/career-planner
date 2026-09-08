"""脱敏服务：调用模型前对学生隐私数据进行脱敏。

正则规则移植自开源项目（MIT 协议，署名参考）：
  - neednlab/cn_pii_anonymization: 中国大陆 PII 正则识别器（手机号/身份证/护照/邮箱）
  - fighting41love/funNLP & cocoNLP: 手机号/身份证抽取正则
Demo 精简点 / 后续迭代替换位置：当前为纯正则方案；命名实体识别（姓名/地址）
接入 Presidio 等为后续迭代，与《具体实现细节_MVP_V1.0.md》4.2「用正则遮蔽」一致。
"""

from __future__ import annotations

import logging
import re
from collections import Counter

logger = logging.getLogger("services.desensitizer")

# 类型化掩码规则：按 (掩码类型, 正则, 占位符) 组织。
# 顺序：长/精确模式优先（身份证 → 手机 → 学号），避免被更短的兜底规则误吞。
# 边界统一用环视 (?<!\d)/(?!\d) 而非 \b：Python re 中汉字属 \w，
# 「是2026011301」里汉字与数字之间无 \b 边界，\b 方案在中文语境下会静默失效。
_RULES: list[tuple[str, re.Pattern[str], str]] = [
    # 邮箱
    ("邮箱", re.compile(r"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}"), "[邮箱]"),
    # 身份证号：18 位（含出生日期段校验，末位 X）或 15 位旧版
    ("身份证号", re.compile(
        r"(?<!\d)(?:\d{6}(?:18|19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]"
        r"|\d{15})(?!\d)"), "[身份证号]"),
    # 手机号：可选 +86/0086/86 前缀与 -/空格 分隔符容错，11 位
    ("手机号", re.compile(
        r"(?<!\d)(?:\+?86[\s\-]?)?0?1[3-9]\d[\s\-]?\d{4}[\s\-]?\d{4}(?!\d)"), "[手机号]"),
    # 学号：8-12 位数字兜底（白名单学号如 2026011301 共 10 位）
    ("学号", re.compile(r"(?<!\d)\d{8,12}(?!\d)"), "[学号]"),
]


def desensitize_detailed(text: str) -> tuple[str, dict[str, int]]:
    """脱敏并返回掩码类型命中计数。

    :return: (脱敏后文本, {掩码类型: 命中次数})，满足 spec 4.2「替换为占位符并记录掩码类型」
    """
    if not text:
        return text, {}
    out = text
    hits: Counter[str] = Counter()
    for name, pattern, repl in _RULES:
        out, n = pattern.subn(repl, out)
        if n:
            hits[name] += n
    return out, dict(hits)


def desensitize(text: str) -> str:
    """兼容旧调用：仅返回脱敏后文本。"""
    out, _ = desensitize_detailed(text)
    return out


def truncate(text: str, limit: int) -> str:
    """超长自由文本截断到业务上限（spec 4.2：复盘 2000 字、咨询 1500 字）。"""
    if text is None:
        return text
    return text[:limit] if len(text) > limit else text


def mask_free_text(text: str | None, *, limit: int | None = None) -> str | None:
    """脱敏 + 可选截断 + 命中数日志：各 AI 能力自由文本入口统一调用。

    Demo 精简点：命中数仅写入应用日志；ai_call_log 无命中数字段（加列属跨服务变更），
    后续迭代替换位置：将其一并写入 ai_call_log.user_ref 同级元数据供「脱敏命中数」指标。
    """
    if text is None:
        return text
    if limit is not None:
        text = truncate(text, limit)
    masked, hits = desensitize_detailed(text)
    if hits:
        logger.info("脱敏命中: %s", hits)
    return masked


__all__ = ["desensitize", "desensitize_detailed", "truncate", "mask_free_text"]
