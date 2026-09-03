"""生涯咨询：面向学生的生涯问答对话服务。

流程：脱敏 → 拼接系统提示词 + 历史对话 → 调用大模型 → 返回回答。
边界：不连接 MySQL、不持有学生身份信息；大模型失败抛 LlmError 由调用方处理。
"""

from __future__ import annotations

from services.desensitizer import mask_free_text
from services.llm_gateway import generate

_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「生涯咨询助手」，面向在校大学生提供生涯发展、专业选择、"
    "职业方向与学习规划方面的咨询。请只依据学生提供的信息给出客观、建设性的建议，"
    "不虚构事实、不给出医疗或法律等专业意见。用简洁的中文回答，必要时分点说明。"
)


def chat(question: str, context: dict | None = None, *, user_ref: str | None = None) -> str:
    """生成一次生涯咨询回答。

    :param question: 学生本次提问
    :param context: 可选上下文（directionId / goalSummary），来自 ChatRequest.context
    :param user_ref: 脱敏用户引用（写入 ai_call_log）
    :return: 模型回答文本
    """
    messages = [{"role": "system", "content": _SYSTEM_PROMPT}]
    # 自由文本截断到咨询上限 1500 字 + 脱敏（spec 4.2）
    messages.append({"role": "user", "content": mask_free_text(_build_prompt(question, context), limit=1500)})
    # max_tokens 需覆盖推理模型的 reasoning 预算，否则推理占满后 content 为空（deepseek-v4-flash 实测）
    return generate(messages, temperature=0.7, max_tokens=2000, scene="career_chat", user_ref=user_ref)


def _build_prompt(question: str, context: dict | None) -> str:
    """把可选上下文拼入提问（Demo：仅追加方向与目标摘要）。"""
    if not context:
        return question
    parts = [question]
    if context.get("directionId"):
        parts.append(f"当前关注方向：{context['directionId']}")
    if context.get("goalSummary"):
        parts.append(f"当前目标摘要：{context['goalSummary']}")
    return "\n".join(parts)


__all__ = ["chat"]
