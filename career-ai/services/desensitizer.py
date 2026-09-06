"""脱敏服务：调用模型前对学生隐私数据进行脱敏。

Demo 实现：仅对常见长数字（学号/身份证）与手机号做掩码；
后续迭代（替换点）：接入命名实体识别与统一脱敏规则库。
"""

from __future__ import annotations

import re

# 稳定性（2026-09）：扩展常见 PII 形态；顺序：强特征优先，避免学号规则误杀普通数字。
# 说明：\b\d{8,12}\b 仍可能误伤订单号等，调用方仅对“用户自由文本”使用，结构化数字字段不走 desensitize。
_PATTERNS = [
    (re.compile(r"(?<!\d)1[3-9]\d(?:[-\s]?\d{4}){2}(?!\d)"), "***"),  # 手机号（含分隔符，不截断长数字）
    (re.compile(r"\b\d{17}[\dXx]\b"), "***"),                 # 18 位身份证（含 X 尾号）
    (re.compile(r"\b\d{15}\b"), "***"),                       # 15 位身份证
    (re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"), "***"),  # 邮箱
    (re.compile(r"\b\d{8,12}\b"), "***"),                     # 学号等长数字
]


def desensitize(text: str) -> str:
    """对文本中的敏感数字做掩码，返回脱敏后的文本。"""
    if not text:
        return text
    for pattern, repl in _PATTERNS:
        text = pattern.sub(repl, text)
    return text


__all__ = ["desensitize"]
