"""生涯咨询：面向学生的生涯问答对话服务。

流程：脱敏 → 拼接系统提示词 + 历史对话 → 调用大模型 → 返回回答。
边界：不连接 MySQL、不持有学生身份信息；大模型失败抛 LlmError 由调用方处理。
"""

from __future__ import annotations

from services.desensitizer import mask_free_text
from services.llm_gateway import generate
from services.prompt_loader import load_prompt

_SYSTEM_PROMPT = load_prompt(
    "career_chat.txt",
    "你是生涯规划系统中的「生涯咨询助手」，面向在校大学生提供生涯发展、专业选择、"
    "职业方向与学习规划方面的咨询。请只依据学生提供的信息给出客观、建设性的建议，"
    "不虚构事实、不给出医疗或法律等专业意见。用简洁的中文回答，必要时分点说明。",
)


_DIM_NAMES = {
    "interest": "兴趣", "values": "价值观", "ability": "能力",
    "academic": "学业", "tendency": "倾向", "practice": "实践",
}

_HISTORY_ROUNDS = 6
_HISTORY_CONTENT_LIMIT = 400


def chat(question: str, context: dict | None = None, *, user_ref: str | None = None,
         profile: dict | None = None, history: list[dict] | None = None) -> str:
    """生成一次生涯咨询回答。

    :param question: 学生本次提问
    :param context: 可选上下文（directionId / directionName / goalSummary），来自 ChatRequest.context
    :param user_ref: 脱敏用户引用（写入 ai_call_log）
    :param profile: 可选画像六维（0-1 或 0-100），拼入背景块
    :param history: 可选历史消息 [{role, content}]，取最近 6 轮回送
    :return: 模型回答文本
    """
    messages = [{"role": "system", "content": _SYSTEM_PROMPT}]
    background = _build_background(profile, context)
    user_text = mask_free_text(_build_prompt(question, context, background), limit=1500)
    for msg in _pack_history(history):
        messages.append({"role": msg["role"], "content": mask_free_text(msg["content"], limit=_HISTORY_CONTENT_LIMIT)})
    messages.append({"role": "user", "content": user_text})
    # max_tokens 需覆盖推理模型的 reasoning 预算，否则推理占满后 content 为空（deepseek-v4-flash 实测）
    return generate(messages, temperature=0.7, max_tokens=2000, scene="career_chat", user_ref=user_ref)


def _build_prompt(question: str, context: dict | None, background: str = "") -> str:
    """把可选上下文拼入提问（Demo：仅追加方向与目标摘要）。"""
    if not context and not background:
        return question
    parts = [question]
    if context:
        if context.get("directionName") or context.get("directionId"):
            parts.append(f"当前关注方向：{context.get('directionName') or context.get('directionId')}")
        if context.get("goalSummary"):
            parts.append(f"当前目标摘要：{context['goalSummary']}")
    text = "\n".join(parts)
    if background:
        text = "【学生背景】\n" + background + "\n【本次提问】\n" + text
    return text


def _build_background(profile: dict | None, context: dict | None) -> str:
    """画像六维中文名+得分（兼容 0-1 与 0-100 两种量纲）。"""
    if not profile:
        return ""
    dims = []
    for key, name in _DIM_NAMES.items():
        value = profile.get(key)
        if value is None:
            continue
        score = round(value * 100) if value <= 1 else round(value)
        dims.append(f"{name}{score}分")
    return "学生画像（0-100分）：" + "、".join(dims) if dims else ""


def _pack_history(history: list[dict] | None) -> list[dict]:
    """取最近 6 轮 user/assistant 消息（时间正序传入，直接截尾）。"""
    if not history:
        return []
    packed = [m for m in history if m.get("role") in ("user", "assistant") and (m.get("content") or "").strip()]
    return packed[-_HISTORY_ROUNDS * 2:]


__all__ = ["chat"]
