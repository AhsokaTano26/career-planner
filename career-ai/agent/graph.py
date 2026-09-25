"""Agent 图定义：LangGraph StateGraph 驱动的生涯对话体。

图流程：
    safety → (crisis?) → END（返回转人工提示）
           ↓ (safe)
          model → (tool_calls?) → tools → model → ... → END
                 ↓ (no tool_calls)
                END

参考：借鉴代码/agent-service-toolkit/src/agents/research_assistant.py（StateGraph 模式）
参考：借鉴代码/agent-service-toolkit/src/agents/rag_assistant.py（带安全检查的图）
"""

from __future__ import annotations

from typing import Literal

from langchain_core.messages import AIMessage, SystemMessage
from langchain_core.runnables import RunnableConfig, RunnableLambda
from langgraph.graph import END, StateGraph
from langgraph.prebuilt import ToolNode

from agent.llm import get_agent_model
from agent.prompts import SYSTEM_PROMPT
from agent.safety import safety_check_node
from agent.state import AgentState
from agent.tools import ALL_TOOLS


async def call_model(state: AgentState, config: RunnableConfig) -> AgentState:
    """模型节点：拼系统提示 + 绑定工具 + 调用 LLM（经 LiteLLM 网关）。"""
    model_name = config.get("configurable", {}).get("model")
    model = get_agent_model(model_name)
    bound = model.bind_tools(ALL_TOOLS)
    preprocessor = RunnableLambda(
        lambda s: [SystemMessage(content=SYSTEM_PROMPT)] + s["messages"],
        name="StateModifier",
    )
    response = await (preprocessor | bound).ainvoke(state, config)
    if state.get("safety", {}).get("needs_human_support"):
        # 安全节点已阻断，不应走到这里，但兜底跳过
        return {"messages": []}
    return {"messages": [response]}


def route_after_safety(state: AgentState) -> Literal["model", "end"]:
    """安全路由：命中危机话题直接结束（安全节点已注入转人工提示）。"""
    if state.get("safety", {}).get("needs_human_support"):
        return "end"
    return "model"


def route_after_model(state: AgentState) -> Literal["tools", "end"]:
    """模型路由：有工具调用则执行工具，否则结束。"""
    last = state["messages"][-1]
    if isinstance(last, AIMessage) and last.tool_calls:
        return "tools"
    return "end"


def build_graph():
    """构建并返回未编译的 StateGraph（lifespan 中注入 checkpointer 后 compile）。"""
    graph = StateGraph(AgentState)
    graph.add_node("safety", safety_check_node)
    graph.add_node("model", call_model)
    graph.add_node("tools", ToolNode(ALL_TOOLS))

    graph.set_entry_point("safety")
    graph.add_conditional_edges(
        "safety", route_after_safety, {"model": "model", "end": END}
    )
    graph.add_edge("tools", "model")
    graph.add_conditional_edges(
        "model", route_after_model, {"tools": "tools", "end": END}
    )
    return graph


# 未编译的图（用于 lifespan 注入 checkpointer 后再 compile）
agent_graph = build_graph()

__all__ = ["build_graph", "agent_graph", "call_model", "route_after_safety", "route_after_model"]
