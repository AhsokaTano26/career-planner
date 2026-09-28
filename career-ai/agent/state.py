"""Agent 状态定义（参考借鉴代码/agent-service-toolkit 的 AgentState 模式）。
"""

from __future__ import annotations

from typing import Annotated, TypedDict

from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages
from typing_extensions import NotRequired


class AgentState(TypedDict, total=False):
    """Agent 图的共享状态。

    messages：LangGraph 消息列表（核心通道，add_messages 归并器按消息 id 追加/更新，
    各节点返回的 messages 增量不会覆盖历史——否则 model→tools→model 第二跳会丢失
    用户消息与带 tool_calls 的 assistant 消息，上游 DeepSeek 报
    "tool 消息必须响应前一条带 tool_calls 的消息"）。
    safety：安全节点结果 {needs_human_support, support_reason}。
    """

    messages: Annotated[list[BaseMessage], add_messages]
    safety: NotRequired[dict]
