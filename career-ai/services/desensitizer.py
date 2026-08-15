"""脱敏服务：调用模型前对学生隐私数据进行脱敏。

Demo 实现：仅对常见长数字（学号/身份证）与手机号做掩码；
后续迭代（替换点）：接入命名实体识别与统一脱敏规则库。
"""

from __future__ import annotations

import re

# Demo 精简点：仅覆盖常见敏感数字形态，按需扩展
_PATTERNS = [
    (re.compile(r"1[3-9]\d{9}"), "***"),          # 手机号
    (re.compile(r"\b\d{15,18}\b"), "***"),        # 身份证号
    (re.compile(r"\b\d{8,12}\b"), "***"),         # 学号等长数字
]


def desensitize(text: str) -> str:
    """对文本中的敏感数字做掩码，返回脱敏后的文本。"""
    if not text:
        return text
    for pattern, repl in _PATTERNS:
        text = pattern.sub(repl, text)
    return text


__all__ = ["desensitize"]
