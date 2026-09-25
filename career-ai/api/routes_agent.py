"""Agent 路由的增强版：带 lifespan 编译图注入与反馈/历史端点。

（注：本文件为 api/routes_agent.py 的扩展版本，由 main.py 引用。）
"""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Annotated, List, Literal, Optional

from fastapi import APIRouter, HTTPException
from langchain_core.messages import HumanMessage
from langchain_core.runnables import RunnableConfig
from pydantic import BaseModel, BeforeValidator

router = APIRouter(prefix="/api/v1/ai/agent", tags=["agent"])

_DISCLAIMER = "智能生成，供探索参考"


def _default_int(default: int):
    """查询参数 int 校验器：空串/缺省回退到默认值。"""

    def _convert(value):
        if value is None or value == "":
            return default
        return value

    return _convert


_PageParam = Annotated[int, BeforeValidator(_default_int(1))]
_SizeParam = Annotated[int, BeforeValidator(_default_int(20))]


# ---------------------------------------------------------------- 模型定义
class ChatContext(BaseModel):
    directionId: Optional[str] = None
    goalSummary: Optional[str] = None


class ChatRequest(BaseModel):
    studentRef: str
    sessionId: str
    question: str
    context: Optional[ChatContext] = None


class ChatResponse(BaseModel):
    answer: str
    references: List[str] = []
    needsHumanSupport: bool = False
    supportReason: str = ""
    disclaimer: str = _DISCLAIMER


class ChatHistoryResponse(BaseModel):
    messageId: str
    answer: str
    references: List[str] = []
    needsHumanSupport: bool = False
    supportReason: str = ""
    disclaimer: str = _DISCLAIMER


class ChatFeedbackRequest(BaseModel):
    feedbackType: Literal["HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED"]
    comment: Optional[str] = None


class ApiResponse(BaseModel):
    code: str
    message: str
    data: dict = {}
    traceId: str
    timestamp: str


# ---------------------------------------------------------------- 内存存储（Agent 侧的反馈暂存）
_AGENT_HISTORY: list[dict] = []
_AGENT_FEEDBACK: dict[str, dict] = {}

# 编译后的 Agent 图（lifespan 注入）
_compiled_agent = None


def set_compiled_agent(agent) -> None:
    """lifespan 初始化时注入编译后的 Agent 图。"""
    global _compiled_agent
    _compiled_agent = agent


def _get_compiled_agent():
    return _compiled_agent


# ---------------------------------------------------------------- 路由
@router.post("/invoke", response_model=ChatResponse)
async def agent_invoke(req: ChatRequest) -> ChatResponse:
    """智能生涯对话体：工具调用 + RAG + 多步推理。"""
    from agent.safety import CRISIS_RESPONSE, SUPPORT_REASON, detect_crisis  # noqa: PLC0415

    # 危机话题快速拦截（图内 safety 节点也会再检查一次）
    if detect_crisis(req.question):
        return ChatResponse(
            answer=CRISIS_RESPONSE,
            needsHumanSupport=True,
            supportReason=SUPPORT_REASON,
        )

    agent = _get_compiled_agent()
    if agent is None:
        raise HTTPException(status_code=503, detail="Agent 未初始化（PostgreSQL 未就绪）")

    config = RunnableConfig(
        configurable={
            "thread_id": req.sessionId,
            "user_id": req.studentRef,
        }
    )

    try:
        result = await agent.ainvoke(
            {"messages": [HumanMessage(content=req.question)]},
            config=config,
        )
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=503, detail=f"Agent 调用失败：{e}") from e

    messages = result.get("messages", [])
    safety = result.get("safety", {})
    last_msg = messages[-1] if messages else None
    answer = str(getattr(last_msg, "content", "") or "")

    needs_human = bool(safety.get("needs_human_support", False))
    support_reason = str(safety.get("support_reason", "") or "") if needs_human else ""

    # 记录会话历史（进程内存，与现有 career_chat 一致的 Demo 模式）
    message_id = uuid.uuid4().hex
    _AGENT_HISTORY.append(
        {
            "messageId": message_id,
            "sessionId": req.sessionId,
            "role": "assistant",
            "content": answer,
            "createdAt": _now(),
            "needsHumanSupport": needs_human,
            "supportReason": support_reason,
        }
    )

    return ChatResponse(
        answer=answer,
        needsHumanSupport=needs_human,
        supportReason=support_reason,
    )


@router.get("/chat/history", response_model=ChatHistoryResponse)
def agent_chat_history(
    page: _PageParam = 1, size: _SizeParam = 20, sort: Optional[str] = None
) -> ChatHistoryResponse:
    """Agent 会话历史（对齐现有 /chat/history 单对象契约）。"""
    assistant_msgs = [m for m in _AGENT_HISTORY if m.get("role") == "assistant"]
    if not assistant_msgs:
        return ChatHistoryResponse(
            messageId="", answer="", references=[], needsHumanSupport=False, supportReason=""
        )
    latest = assistant_msgs[-1]
    return ChatHistoryResponse(
        messageId=latest["messageId"],
        answer=latest["content"],
        references=[],
        needsHumanSupport=bool(latest.get("needsHumanSupport")),
        supportReason=latest.get("supportReason") or "",
    )


@router.post("/chat/{messageId}/feedback", response_model=ApiResponse)
def agent_chat_feedback(messageId: str, req: ChatFeedbackRequest) -> ApiResponse:
    """Agent 回答反馈（进程内存）。"""
    if not any(m.get("messageId") == messageId for m in _AGENT_HISTORY):
        raise HTTPException(status_code=404, detail="消息不存在")
    _AGENT_FEEDBACK[messageId] = {"feedbackType": req.feedbackType, "comment": req.comment}
    return _ok()


def _now() -> str:
    return datetime.now().astimezone().replace(microsecond=0).isoformat()


def _ok(data: dict | None = None) -> ApiResponse:
    return ApiResponse(
        code="OK",
        message="success",
        data=data or {},
        traceId=uuid.uuid4().hex,
        timestamp=_now(),
    )


__all__ = ["router", "set_compiled_agent", "ChatRequest", "ChatResponse"]
