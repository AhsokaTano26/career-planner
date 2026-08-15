"""AI 智能服务与 AI 生涯咨询路由（/api/v1/ai/*，按线上 Apifox 定义）。

边界（与 career-ai 一致）：本服务不连接 MySQL、不持久化身份信息；
chat 历史与反馈仅在进程内存中暂存（Demo 精简点，重启即清空）。
"""

from __future__ import annotations

import uuid
from datetime import datetime, timezone
from typing import List, Optional

from fastapi import APIRouter, Body, File, HTTPException, UploadFile
from pydantic import BaseModel

from providers.deepseek import DEFAULT_MODEL, LlmError
from services.career_chat import chat as chat_service
from services.pdf_parser import parse as pdf_parse
from services.plan_generator import generate_plan
from services.review_summarizer import summarize as review_summarize

router = APIRouter(prefix="/api/v1/ai", tags=["ai"])

# ---------------------------------------------------------------- 内存存储（Demo：career-ai 不连库）
_CHAT_HISTORY: list[dict] = []          # {messageId, sessionId, role, content, createdAt}
_CHAT_FEEDBACK: dict[str, dict] = {}    # messageId -> {feedbackType, comment}


# ---------------------------------------------------------------- 模型定义
class ChatRequest(BaseModel):
    message: str
    sessionId: Optional[str] = None
    history: Optional[List[dict]] = None


class ChatResponse(BaseModel):
    messageId: str
    reply: str
    model: str


class PdfParseResponse(BaseModel):
    filename: str
    raw_text: str
    courses: List[dict]


class PlanGenerateRequest(BaseModel):
    goalSummary: Optional[str] = None
    directionName: Optional[str] = None
    dimensions: Optional[dict] = None


class PlanGenerateResponse(BaseModel):
    goalSummary: str
    semesterGoals: List[dict]
    monthlyTasks: List[dict]
    notes: List[str]


class ReviewSummarizeRequest(BaseModel):
    rawText: str


class ReviewSummarizeResponse(BaseModel):
    summary: str
    suggestions: List[str]
    model: str


class ChatFeedbackRequest(BaseModel):
    feedbackType: str
    comment: Optional[str] = None


class ChatFeedbackResponse(BaseModel):
    messageId: str
    feedbackType: str
    comment: Optional[str] = None


# ---------------------------------------------------------------- 路由
@router.post("/chat", response_model=ChatResponse)
def ai_chat(req: ChatRequest) -> ChatResponse:
    """生涯咨询问答：生成一次回答并写入内存会话。"""
    message_id = uuid.uuid4().hex
    try:
        reply = chat_service(req.message, req.history)
    except LlmError as exc:
        raise HTTPException(status_code=502, detail=f"生涯咨询生成失败：{exc}") from exc

    _CHAT_HISTORY.append(
        {"messageId": message_id, "sessionId": req.sessionId, "role": "user",
         "content": req.message, "createdAt": _now()})
    _CHAT_HISTORY.append(
        {"messageId": message_id, "sessionId": req.sessionId, "role": "assistant",
         "content": reply, "createdAt": _now()})
    return ChatResponse(messageId=message_id, reply=reply, model=DEFAULT_MODEL)


@router.post("/pdf/parse", response_model=PdfParseResponse)
def ai_pdf_parse(file: UploadFile = File(...)) -> PdfParseResponse:
    """解析培养方案 PDF（Demo：提取可读文本，课程结构化留空）。"""
    data = file.file.read()
    result = pdf_parse(data, file.filename or "upload.pdf")
    return PdfParseResponse(filename=result["filename"], raw_text=result["raw_text"],
                            courses=result["courses"])


@router.post("/plan/generate", response_model=PlanGenerateResponse)
def ai_plan_generate(req: PlanGenerateRequest) -> PlanGenerateResponse:
    """生成学期计划草案。"""
    try:
        result = generate_plan(req.goalSummary, req.directionName, req.dimensions)
    except LlmError as exc:
        raise HTTPException(status_code=502, detail=f"计划生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=502, detail=f"计划生成输出不合法：{exc}") from exc
    return PlanGenerateResponse(
        goalSummary=result.get("goalSummary", ""),
        semesterGoals=result.get("semesterGoals", []),
        monthlyTasks=result.get("monthlyTasks", []),
        notes=result.get("notes", []),
    )


@router.post("/review/summarize", response_model=ReviewSummarizeResponse)
def ai_review_summarize(req: ReviewSummarizeRequest) -> ReviewSummarizeResponse:
    """生成阶段总结与调整建议。"""
    try:
        result = review_summarize(req.rawText)
    except LlmError as exc:
        raise HTTPException(status_code=502, detail=f"阶段总结生成失败：{exc}") from exc
    return ReviewSummarizeResponse(
        summary=result.get("summary", ""),
        suggestions=result.get("suggestions", []),
        model=DEFAULT_MODEL,
    )


@router.post("/recommendation/explain")
def ai_recommendation_explain(req: dict = Body(...)):
    """推荐解释（与 /v1/recommendation/explain 相同逻辑，按线上 /api/v1/ai 前缀暴露）。"""
    from api.main import ExplainRequest  # 延迟导入，避免循环依赖
    from services.recommendation_explainer import explain

    model_req = ExplainRequest(**req)
    return explain(model_req)


@router.get("/chat/history")
def ai_chat_history(sessionId: Optional[str] = None):
    """会话历史（进程内存，Demo 不持久化）。"""
    messages = [m for m in _CHAT_HISTORY if sessionId is None or m["sessionId"] == sessionId]
    return {"messages": messages}


@router.post("/chat/{messageId}/feedback", response_model=ChatFeedbackResponse)
def ai_chat_feedback(messageId: str, req: ChatFeedbackRequest) -> ChatFeedbackResponse:
    """回答反馈（进程内存，Demo 不持久化）。"""
    _CHAT_FEEDBACK[messageId] = {"feedbackType": req.feedbackType, "comment": req.comment}
    return ChatFeedbackResponse(messageId=messageId, feedbackType=req.feedbackType,
                                comment=req.comment)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()
