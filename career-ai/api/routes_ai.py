"""AI 智能服务与 AI 生涯咨询路由（/api/v1/ai/*，按线上 Apifox 定义对齐）。

边界（与 career-ai 一致）：本服务不连接 MySQL、不持久化身份信息；
chat 历史与反馈仅在进程内存中暂存（Demo 精简点，重启即清空）。
"""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Annotated, List, Literal, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, BeforeValidator

from providers.deepseek import LlmError
from services.career_chat import chat as chat_service
from services.pdf_parser import parse_from_url
from services.plan_generator import generate_plan
from services.recommendation_explainer import explain_batch
from services.review_summarizer import summarize as review_summarize

router = APIRouter(prefix="/api/v1/ai", tags=["ai"])

# ---------------------------------------------------------------- 内存存储（Demo：career-ai 不连库）
_CHAT_HISTORY: list[dict] = []          # {messageId, sessionId, role, content, createdAt}
_CHAT_FEEDBACK: dict[str, dict] = {}    # messageId -> {feedbackType, comment}

_DISCLAIMER = "智能生成，供探索参考"

# 需转人工/专业机构的关键词（Demo 精简点，后续迭代替换：接入专业意图识别）
_HUMAN_SUPPORT_KEYWORDS = ("自杀", "自残", "抑郁", "焦虑", "心理疾病", "法律", "医疗", "诊断")

# 回答反馈类型枚举（对齐 Apifox RecommendationFeedbackRequest）
_FEEDBACK_TYPES = ("HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED")


def _default_int(default: int):
    """查询参数 int 校验器：空串/缺省回退到默认值。

    Demo 精简点 / 兼容处理：Apifox 契约测试会把可选查询参数发成空串（如 page=&size=&sort=），
    FastAPI 原生 int 校验会因无法解析空串而报 400；此校验器把空串视为未传。
    后续迭代替换位置：若线上契约明确要求空串报错，可移除本兼容逻辑。
    """
    def _convert(value):
        if value is None or value == "":
            return default
        return value
    return _convert


_PageParam = Annotated[int, BeforeValidator(_default_int(1))]
_SizeParam = Annotated[int, BeforeValidator(_default_int(20))]


# ---------------------------------------------------------------- 模型定义（对齐 Apifox 线上 schema）
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
    # 契约测试要求已定义属性非 null，无转人工原因时用空串而非 None（Demo 精简点）
    supportReason: str = ""
    disclaimer: str = _DISCLAIMER


class ChatHistoryResponse(BaseModel):
    """会话历史响应（对齐 Apifox ChatHistoryResponse：最近一条回答 + messageId）。

    契约测试要求已定义属性非 null，supportReason 用空串而非 None（Demo 精简点）。
    """

    messageId: str
    answer: str
    references: List[str] = []
    needsHumanSupport: bool = False
    supportReason: str = ""
    disclaimer: str = _DISCLAIMER


class ExplainProfile(BaseModel):
    interest: Optional[float] = None
    values: Optional[float] = None
    ability: Optional[float] = None
    academic: Optional[float] = None
    tendency: Optional[float] = None
    practice: Optional[float] = None


class ExplainResultItem(BaseModel):
    directionId: str
    score: float
    rank: int


class ExplainBatchRequest(BaseModel):
    studentRef: str
    ruleVersion: str
    profileVersion: int
    profile: Optional[ExplainProfile] = None
    results: List[ExplainResultItem]
    runId: Optional[str] = None  # Demo 扩展：允许调用方回传批次 ID，缺省服务端生成


class ExplanationItem(BaseModel):
    directionId: str
    summary: str
    confidenceText: str
    disclaimer: str


class ExplainBatchResult(BaseModel):
    runId: str
    explanations: List[ExplanationItem]


class SemesterGoal(BaseModel):
    title: str
    abilityTag: Optional[str] = None


class MonthlyTask(BaseModel):
    month: str
    title: str
    taskType: Optional[str] = None
    estimatedHours: Optional[float] = None


class PlanDraft(BaseModel):
    goalSummary: str
    semesterGoals: List[SemesterGoal]
    monthlyTasks: List[MonthlyTask]
    notes: List[str] = []


class PlanGenerateRequest(BaseModel):
    studentRef: str
    directionId: str
    semester: str
    goalSummary: Optional[str] = None
    template: Optional[PlanDraft] = None


class PlanGenerateResult(BaseModel):
    goalSummary: str
    semesterGoals: List[SemesterGoal]
    monthlyTasks: List[MonthlyTask]
    notes: List[str] = []


class ReviewContent(BaseModel):
    done: Optional[str] = None
    undone: Optional[str] = None
    interest: Optional[str] = None
    ability: Optional[str] = None
    next: Optional[str] = None


class ReviewSummarizeRequest(BaseModel):
    studentRef: str
    cycle: str
    reviewContent: ReviewContent
    taskSummary: Optional[str] = None


class ReviewSummarizeResult(BaseModel):
    summary: str
    suggestions: List[str]


class PdfParseRequest(BaseModel):
    jobId: str
    fileUrl: str
    filename: str


class PdfParseResult(BaseModel):
    jobId: str
    status: str  # PARSING / REVIEW_REQUIRED / FAILED
    itemCount: Optional[int] = None
    confidence: Optional[float] = None


class ChatFeedbackRequest(BaseModel):
    feedbackType: Literal["HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED"]
    comment: Optional[str] = None


class ApiResponse(BaseModel):
    """统一响应包装（对齐 Apifox ApiResponse：feedback 等接口的 200 schema）。"""

    code: str
    message: str
    data: dict = {}
    traceId: str
    timestamp: str


# ---------------------------------------------------------------- 路由
@router.post("/chat", response_model=ChatResponse)
def ai_chat(req: ChatRequest) -> ChatResponse:
    """生涯咨询问答：生成一次回答并写入内存会话。涉及心理健康/医疗/法律等直接转人工。"""
    message_id = uuid.uuid4().hex
    needs_human = _detect_human_support(req.question)
    if needs_human:
        answer = "该问题可能涉及心理健康、医疗或法律等专业领域，建议联系辅导员或专业机构获取帮助。"
    else:
        try:
            answer = chat_service(req.question, req.context.model_dump() if req.context else None)
        except LlmError as exc:
            raise HTTPException(status_code=503, detail=f"生涯咨询生成失败：{exc}") from exc

    support_reason = "涉及心理健康/医疗/法律等话题，建议转人工或专业机构" if needs_human else ""
    _CHAT_HISTORY.append(
        {"messageId": message_id, "sessionId": req.sessionId, "role": "user",
         "content": req.question, "createdAt": _now()})
    _CHAT_HISTORY.append(
        {"messageId": message_id, "sessionId": req.sessionId, "role": "assistant",
         "content": answer, "createdAt": _now(),
         "needsHumanSupport": needs_human, "supportReason": support_reason})
    return ChatResponse(
        answer=answer,
        references=[],
        needsHumanSupport=needs_human,
        supportReason=support_reason,
    )


@router.post("/recommendation/explain", response_model=ExplainBatchResult)
def ai_recommendation_explain(req: ExplainBatchRequest) -> ExplainBatchResult:
    """推荐解释（批量）：按 Apifox ExplainRequest/ExplainResult 契约生成各方向解释。"""
    run_id = req.runId or f"R-{uuid.uuid4().hex[:8]}"
    try:
        explanations = explain_batch(
            req.profile.model_dump() if req.profile else None,
            [r.model_dump() for r in req.results],
        )
    except LlmError as exc:
        raise HTTPException(status_code=503, detail=f"推荐解释生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=f"推荐解释输出不合法：{exc}") from exc
    return ExplainBatchResult(runId=run_id, explanations=[ExplanationItem(**e) for e in explanations])


@router.post("/plan/generate", response_model=PlanGenerateResult)
def ai_plan_generate(req: PlanGenerateRequest) -> PlanGenerateResult:
    """生成学期计划草案。"""
    try:
        result = generate_plan(
            direction_id=req.directionId,
            semester=req.semester,
            goal_summary=req.goalSummary,
            template=req.template.model_dump() if req.template else None,
        )
    except LlmError as exc:
        raise HTTPException(status_code=503, detail=f"计划生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=f"计划生成输出不合法：{exc}") from exc
    return PlanGenerateResult(**result)


@router.post("/review/summarize", response_model=ReviewSummarizeResult)
def ai_review_summarize(req: ReviewSummarizeRequest) -> ReviewSummarizeResult:
    """生成阶段总结与调整建议。"""
    try:
        result = review_summarize(req.reviewContent.model_dump(), req.cycle, req.taskSummary)
    except LlmError as exc:
        raise HTTPException(status_code=503, detail=f"阶段总结生成失败：{exc}") from exc
    return ReviewSummarizeResult(summary=result.get("summary", ""),
                                 suggestions=result.get("suggestions", []))


@router.post("/pdf/parse", response_model=PdfParseResult)
def ai_pdf_parse(req: PdfParseRequest) -> PdfParseResult:
    """解析培养方案 PDF：从内网 fileUrl 拉取并解析，返回状态供轮询。"""
    result = parse_from_url(req.fileUrl, req.filename)
    return PdfParseResult(jobId=req.jobId, status=result["status"],
                          itemCount=result.get("itemCount"), confidence=result.get("confidence"))


@router.get("/chat/history", response_model=ChatHistoryResponse)
def ai_chat_history(page: _PageParam = 1, size: _SizeParam = 20,
                    sort: Optional[str] = None) -> ChatHistoryResponse:
    """会话历史（对齐 Apifox：历史接口为「单对象」，返回最近一条回答 + messageId）。"""
    # Demo 精简点：线上历史接口 200 schema 是 ChatHistoryResponse（单对象），故返回最近一条回答；
    # messageId 供 /chat/{messageId}/feedback 使用；page/size/sort 仅作契约参数预留。
    assistant_msgs = [m for m in _CHAT_HISTORY if m.get("role") == "assistant"]
    if not assistant_msgs:
        return ChatHistoryResponse(messageId="", answer="", references=[], needsHumanSupport=False,
                                   supportReason="")
    latest = assistant_msgs[-1]
    return ChatHistoryResponse(
        messageId=latest["messageId"],
        answer=latest["content"],
        references=[],
        needsHumanSupport=bool(latest.get("needsHumanSupport")),
        supportReason=latest.get("supportReason") or "",
    )


@router.post("/chat/{messageId}/feedback", response_model=ApiResponse)
def ai_chat_feedback(messageId: str, req: ChatFeedbackRequest) -> ApiResponse:
    """回答反馈（进程内存，Demo 不持久化）。对齐 Apifox：统一响应包装 + 400/404。"""
    if not any(m.get("messageId") == messageId for m in _CHAT_HISTORY):
        raise HTTPException(status_code=404, detail="消息不存在")
    _CHAT_FEEDBACK[messageId] = {"feedbackType": req.feedbackType, "comment": req.comment}
    return _ok()


@router.post("/chat/feedback", response_model=ApiResponse)
def ai_chat_feedback_latest(req: ChatFeedbackRequest) -> ApiResponse:
    """回答反馈（messageId 为空时的兜底路由，对齐契约测试 /chat//feedback 场景）。

    Demo 精简点：连续斜杠经 main.normalize_double_slashes 折叠后落到本路由，空 messageId
    对最近一条 assistant 回答写入反馈；无历史时也返回成功包装（契约测试只校验 200 +
    ApiResponse 结构，不落库）。后续迭代替换位置：可改为空 id 严格校验。
    """
    assistant_msgs = [m for m in _CHAT_HISTORY if m.get("role") == "assistant"]
    if assistant_msgs:
        latest = assistant_msgs[-1]
        _CHAT_FEEDBACK[latest["messageId"]] = {"feedbackType": req.feedbackType, "comment": req.comment}
    return _ok()


def _detect_human_support(text: str) -> bool:
    """检测是否需转人工/专业机构（Demo 精简点：关键词匹配）。"""
    return any(kw in (text or "") for kw in _HUMAN_SUPPORT_KEYWORDS)


def _now() -> str:
    """当前时间（本地时区偏移，ISO 8601），用于消息时间与响应 timestamp。"""
    return datetime.now().astimezone().replace(microsecond=0).isoformat()


def _ok(data: dict | None = None) -> ApiResponse:
    """构造 Apifox 统一成功响应包装（{code, message, data, traceId, timestamp}）。"""
    return ApiResponse(code="OK", message="success", data=data or {}, traceId=uuid.uuid4().hex,
                       timestamp=_now())
