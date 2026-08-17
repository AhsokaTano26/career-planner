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


def summarize(review_content: dict, cycle: str, task_summary: str | None = None) -> dict:
    """生成阶段总结，返回 {summary, suggestions}。

    输入按 Apifox ReviewSummarizeRequest：reviewContent(ReviewContent)/cycle/taskSummary。
    """
    user_prompt = _build_prompt(review_content, cycle, task_summary)
    content = generate(
        [
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.5,
        # 推理模型需更大预算，避免 reasoning 占满后 content 为空（deepseek-v4-flash 实测）
        max_tokens=1500,
    )
    import json
    text = content.strip()
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        return {"summary": content, "suggestions": []}
    data = json.loads(text[start:end + 1])
    if not data.get("summary"):
        data["summary"] = content
    data.setdefault("suggestions", [])
    return data


def _build_prompt(review_content: dict, cycle: str, task_summary: str | None) -> str:
    """把复盘结构化内容拼成易读文本。"""
    parts = [f"复盘周期：{cycle or '未指定'}"]
    labels = [("done", "本阶段完成情况"), ("undone", "未完成情况及原因"),
              ("interest", "方向兴趣变化"), ("ability", "能力提升"), ("next", "下一步安排")]
    for key, label in labels:
        value = (review_content or {}).get(key)
        if value:
            parts.append(f"{label}：{value}")
    if task_summary:
        parts.append(f"任务完成情况：{task_summary}")
    return "\n".join(parts)


__all__ = ["summarize"]
