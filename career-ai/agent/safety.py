"""Agent 安全节点：危机关键词检测。

与现有 career_chat 一致的危机关键词集合，命中时阻断 LLM 调用，
返回转人工提示并标记 needs_human_support=true。

参考：career-ai/api/routes_ai.py 的 _HUMAN_SUPPORT_KEYWORDS + _detect_human_support。
参考：借鉴代码/agent-service-toolkit 的 Safeguard 模式（关键词匹配替代 LLM 审核）。
"""

from __future__ import annotations

from langchain_core.messages import AIMessage

# 需转人工/专业机构的关键词（与现有 career_chat 一致）
HUMAN_SUPPORT_KEYWORDS = (
    "自杀",
    "自残",
    "抑郁",
    "焦虑",
    "心理疾病",
    "法律",
    "医疗",
    "诊断",
)

CRISIS_RESPONSE = (
    "该问题可能涉及心理健康、医疗或法律等专业领域，"
    "建议联系辅导员或专业机构获取帮助。"
)

SUPPORT_REASON = "涉及心理健康/医疗/法律等话题，建议转人工或专业机构"


def detect_crisis(text: str) -> bool:
    """检测文本是否涉及危机话题（关键词匹配）。"""
    return any(kw in (text or "") for kw in HUMAN_SUPPORT_KEYWORDS)


async def safety_check_node(state: dict) -> dict:
    """Agent 图的安全节点：检测用户输入是否涉及危机话题。

    命中则注入转人工提示（AIMessage），标记 safety.needs_human_support=True，
    不调用 LLM。安全路由据此直接跳到 END。
    """
    messages = state.get("messages", [])
    last_msg = messages[-1] if messages else None
    user_text = getattr(last_msg, "content", "") if last_msg else ""

    if detect_crisis(str(user_text)):
        return {
            "messages": [AIMessage(content=CRISIS_RESPONSE)],
            "safety": {"needs_human_support": True, "support_reason": SUPPORT_REASON},
        }
    return {"safety": {"needs_human_support": False, "support_reason": ""}}


__all__ = ["HUMAN_SUPPORT_KEYWORDS", "detect_crisis", "safety_check_node", "SUPPORT_REASON"]
