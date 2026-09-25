"""Agent 状态定义（参考借鉴代码/agent-service-toolkit 的 AgentState 模式）。
"""

from __future__ import annotations

from typing import TypedDict

from langchain_core.messages import BaseMessage
from typing_extensions import NotRequired


class AgentState(TypedDict, total=False):
    """Agent 图的共享状态。

    messages：LangGraph 消息列表（核心通道，由 MessagesState 模式驱动）。
    safety：安全节点结果 {needs_human_support, support_reason}。
    """

    messages: list[BaseMessage]
    safety: NotRequired[dict]
