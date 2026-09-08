"""复盘总结：根据阶段复盘文本生成总结与调整建议。

流程：拼接系统提示词 + 阶段复盘原文 → 调用大模型 → 返回总结与建议。
边界：大模型失败抛 LlmError 由调用方处理。
"""

from __future__ import annotations

from services.desensitizer import mask_free_text
from services.llm_gateway import generate

_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「阶段复盘总结器」。请阅读学生的阶段复盘内容，输出 JSON："
    "{\"summary\": 一段阶段总结, \"suggestions\": [若干调整建议]}。只输出 JSON，"
    "不要输出任何额外文字或 Markdown 代码块。"
)


def summarize(review_content: dict, cycle: str, task_summary: str | None = None,
              *, user_ref: str | None = None) -> dict:
    """生成阶段总结，返回 {summary, suggestions}。

    输入按 Apifox ReviewSummarizeRequest：reviewContent(ReviewContent)/cycle/taskSummary。
    user_ref 为脱敏用户引用（写入 ai_call_log）。
    """
    user_prompt = _build_prompt(review_content, cycle, task_summary)
    # 复盘为自由文本：截断到 2000 字 + 脱敏（spec 4.2「已脱敏复盘」）
    user_prompt = mask_free_text(user_prompt, limit=2000)
    content = generate(
        [
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.5,
        # 推理模型需更大预算，避免 reasoning 占满后 content 为空（deepseek-v4-flash 实测）
        max_tokens=1500,
        scene="review_summarize",
        user_ref=user_ref,
    )
    import json
    text = content.strip()
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        return {"summary": content, "suggestions": []}
    # 稳定性：模型截断/杂文本导致 JSON 非法时抛 ValueError，由路由映射为 503
    # （此前裸奔为 500，打破“失败一律可回退”约定）
    try:
        data = json.loads(text[start:end + 1])
    except (json.JSONDecodeError, ValueError) as exc:
        raise ValueError("复盘总结输出非合法 JSON：%s" % exc) from exc
    if not isinstance(data, dict):
        raise ValueError("复盘总结输出非 JSON 对象")
    if not data.get("summary"):
        data["summary"] = content
    suggestions = data.get("suggestions", [])
    # suggestions 必须为字符串列表；模型返回裸字符串/脏类型时归一化，避免出口 Pydantic 500
    if isinstance(suggestions, str):
        suggestions = [suggestions] if suggestions.strip() else []
    elif not isinstance(suggestions, list):
        suggestions = []
    data["suggestions"] = [str(s) for s in suggestions if s is not None][:10]
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
