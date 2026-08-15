"""复盘总结：根据阶段复盘文本生成总结与调整建议。

流程：拼接系统提示词 + 阶段复盘原文 → 调用大模型 → 返回总结与建议。
边界：大模型失败抛 LlmError 由调用方处理。
"""

from __future__ import annotations

from services.llm_gateway import generate

_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「阶段复盘总结器」。请阅读学生的阶段复盘内容，输出 JSON："
    "{\"summary\": 一段阶段总结, \"suggestions\": [若干调整建议]}。只输出 JSON，"
    "不要输出任何额外文字或 Markdown 代码块。"
)


def summarize(raw_text: str) -> dict:
    """生成阶段总结，返回 {summary, suggestions}。"""
    content = generate(
        [
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": raw_text or ""},
        ],
        temperature=0.5,
        max_tokens=500,
    )
    import json
    text = content.strip()
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        return {"summary": content, "suggestions": []}
    return json.loads(text[start:end + 1])


__all__ = ["summarize"]
