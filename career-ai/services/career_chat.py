"""生涯咨询：面向学生的生涯问答对话服务。

流程：脱敏 → 拼接系统提示词 + 历史对话 → 调用大模型 → 返回回答。
边界：不连接 MySQL、不持有学生身份信息；大模型失败抛 LlmError 由调用方处理。
"""

from __future__ import annotations

from services.desensitizer import desensitize
from services.llm_gateway import generate

_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「生涯咨询助手」，面向在校大学生提供生涯发展、专业选择、"
    "职业方向与学习规划方面的咨询。请只依据学生提供的信息给出客观、建设性的建议，"
    "不虚构事实、不给出医疗或法律等专业意见。用简洁的中文回答，必要时分点说明。"
)


def chat(message: str, history: list[dict] | None = None) -> str:
    """生成一次生涯咨询回答。

    :param message: 学生本次提问
    :param history: 历史对话列表，每项含 role(user/assistant) 与 content
    :return: 模型回答文本
    """
    messages = [{"role": "system", "content": _SYSTEM_PROMPT}]
    for item in history or []:
        role = "assistant" if item.get("role") == "assistant" else "user"
        content = item.get("content", "")
        if content:
            messages.append({"role": role, "content": content})
    messages.append({"role": "user", "content": desensitize(message)})
    return generate(messages, temperature=0.7, max_tokens=600)


__all__ = ["chat"]
