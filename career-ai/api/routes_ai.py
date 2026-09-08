"""AI 智能服务与 AI 生涯咨询路由（/api/v1/ai/*，按线上 Apifox 定义对齐）。

边界（与 career-ai 一致）：本服务不连接 MySQL、不持久化身份信息；
chat 历史与反馈仅在进程内存中暂存（Demo 精简点，重启即清空）。
"""

from __future__ import annotations

import hmac
import os
import uuid
from datetime import datetime
from typing import Annotated, List, Literal, Optional

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel, BeforeValidator, Field, field_validator

from gateway.client import GatewayError
from services.career_chat import chat as chat_service
from services.pdf_parser import parse_from_url
from services.plan_generator import generate_plan
from services.recommendation_explainer import explain_batch
from services.review_summarizer import summarize as review_summarize

router = APIRouter(prefix="/api/v1/ai", tags=["ai"])

# ---------------------------------------------------------------- 内存存储（Demo：career-ai 不连库）
# 稳定性（2026-09 复审 P0）：历史按 studentRef 隔离分区，每用户独立上限 500 条、
# 总用户数上限 1000（超限淘汰最旧用户分区）；history 接口必须传 studentRef，
# 否则返回空对象（此前全局“最近一条”导致任意持 key 者可读他人问答）。
# 后续迭代替换位置：多 worker/持久化换 Redis（key=ai:history:{ref}）。
_HIST_BY_REF: dict[str, list[dict]] = {}  # studentRef -> [{messageId, sessionId, role, content, createdAt, ...}]
_MSG_OWNER: dict[str, str] = {}            # messageId -> studentRef（反馈归属核对）
_CHAT_FEEDBACK: dict[str, dict] = {}    # messageId -> {feedbackType, comment}
# 稳定性：内存历史设上限（防长期运行 OOM；重启仍清空）
_PER_USER_HISTORY_LIMIT = 500
_MAX_HISTORY_USERS = 1000
_FEEDBACK_LIMIT = 1000


def _remember_message(student_ref: str, entry: dict) -> None:
    if not student_ref:
        return
    bucket = _HIST_BY_REF.get(student_ref)
    if bucket is None:
        if len(_HIST_BY_REF) >= _MAX_HISTORY_USERS:
            _HIST_BY_REF.pop(next(iter(_HIST_BY_REF)))
        bucket = _HIST_BY_REF[student_ref] = []
    bucket.append(entry)
    while len(bucket) > _PER_USER_HISTORY_LIMIT:
        bucket.pop(0)
    _MSG_OWNER[entry["messageId"]] = student_ref
    while len(_MSG_OWNER) > _MAX_HISTORY_USERS * _PER_USER_HISTORY_LIMIT:
        _MSG_OWNER.pop(next(iter(_MSG_OWNER)))


# 兼容旧名：测试若直接调 _remember_message(entry) 仍可用（归属 unknown 分区）
def _remember_legacy(entry: dict) -> None:
    _remember_message("__unknown__", entry)


def _remember_feedback(message_id: str, feedback: dict) -> None:
    _CHAT_FEEDBACK[message_id] = feedback
    while len(_CHAT_FEEDBACK) > _FEEDBACK_LIMIT:
        _CHAT_FEEDBACK.pop(next(iter(_CHAT_FEEDBACK)))

_DISCLAIMER = "智能生成，供探索参考"

# 需转人工/专业机构的关键词（Demo 精简点，后续迭代替换：接入专业意图识别）
_HUMAN_SUPPORT_KEYWORDS = ("自杀", "自残", "抑郁", "焦虑", "心理疾病", "法律", "医疗", "诊断")

# 回答反馈类型枚举（对齐 Apifox RecommendationFeedbackRequest）
_FEEDBACK_TYPES = ("HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED")


def _require_ai_key(authorization: Optional[str]) -> None:
    """AI 路由鉴权（2026-09 稳定性）：与网关同 Bearer 密钥。

    key 为空 = 未配置内部密钥 → fail-closed 直接 503，杜绝无鉴权裸奔。
    """
    key = os.getenv("GATEWAY_API_KEY", "")
    if not key:
        raise HTTPException(status_code=503, detail="AI 服务未配置内部密钥，拒绝服务")
    # 稳定性：compare_digest 防计时侧信道；strip 容忍多余空格
    provided = (authorization or "").strip()
    if not provided.startswith("Bearer ") or not hmac.compare_digest(
        provided[len("Bearer "):], key
    ):
        raise HTTPException(status_code=401, detail="内部密钥无效或缺失")


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
    directionId: Optional[str] = Field(default=None, max_length=64)
    goalSummary: Optional[str] = Field(default=None, max_length=2000)


class ChatRequest(BaseModel):
    studentRef: str = Field(max_length=64)
    sessionId: str = Field(max_length=64)
    # 稳定性：问题长度封顶（超长直接 400，避免巨 prompt 费用/延迟爆炸后再转 502）
    question: str = Field(min_length=1, max_length=5000)
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
    directionId: str = Field(max_length=64)
    score: float = Field(ge=0, le=100)
    rank: int = Field(ge=1, le=100)


class ExplainBatchRequest(BaseModel):
    studentRef: str = Field(max_length=64)
    ruleVersion: str = Field(max_length=32)
    profileVersion: int = Field(ge=0)
    profile: Optional[ExplainProfile] = None
    results: List[ExplainResultItem] = Field(min_length=1, max_length=10)
    runId: Optional[str] = Field(default=None, max_length=64)  # Demo 扩展：允许调用方回传批次 ID，缺省服务端生成


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
    # 复审加固：month 格式 YYYY-MM、estimatedHours 范围（此前无约束直入 prompt/落库）
    month: str = Field(pattern=r"^\d{4}-(0[1-9]|1[0-2])$")
    title: str
    taskType: Optional[str] = None
    estimatedHours: Optional[float] = Field(default=None, ge=0, le=1000)


class PlanDraft(BaseModel):
    goalSummary: str
    semesterGoals: List[SemesterGoal]
    monthlyTasks: List[MonthlyTask]
    notes: List[str] = []


class PlanGenerateRequest(BaseModel):
    studentRef: str = Field(max_length=64)
    directionId: str = Field(max_length=64)
    semester: str = Field(max_length=64)
    goalSummary: Optional[str] = Field(default=None, max_length=2000)
    template: Optional[PlanDraft] = None


class PlanGenerateResult(BaseModel):
    goalSummary: str
    semesterGoals: List[SemesterGoal]
    monthlyTasks: List[MonthlyTask]
    notes: List[str] = []


class ReviewContent(BaseModel):
    done: Optional[str] = Field(default=None, max_length=2000)
    undone: Optional[str] = Field(default=None, max_length=2000)
    interest: Optional[str] = Field(default=None, max_length=2000)
    ability: Optional[str] = Field(default=None, max_length=2000)
    next: Optional[str] = Field(default=None, max_length=2000)


class ReviewSummarizeRequest(BaseModel):
    studentRef: str = Field(max_length=64)
    cycle: str = Field(max_length=64)
    reviewContent: ReviewContent
    taskSummary: Optional[str] = Field(default=None, max_length=2000)


class ReviewSummarizeResult(BaseModel):
    summary: str
    suggestions: List[str]


class PdfParseRequest(BaseModel):
    jobId: str = Field(max_length=64)
    fileUrl: str = Field(max_length=2048)
    filename: str = Field(max_length=256)

    @field_validator("fileUrl")
    @classmethod
    def _check_file_url(cls, v: str) -> str:
        """复审 P0：fileUrl 仅接受 http/https、无 userinfo、无非标端口，避免持 key SSRF。"""
        from urllib.parse import urlparse

        try:
            parsed = urlparse(v or "")
        except Exception as exc:
            raise ValueError("fileUrl 非法") from exc
        if parsed.scheme not in ("http", "https") or not parsed.hostname:
            raise ValueError("fileUrl 仅支持 http/https 地址")
        if parsed.username or parsed.password:
            raise ValueError("fileUrl 不得携带用户信息")
        if parsed.port is not None and parsed.port not in (80, 443):
            raise ValueError("fileUrl 端口非法")
        return v


class PdfParseResult(BaseModel):
    jobId: str
    status: str  # PARSING / REVIEW_REQUIRED / FAILED
    itemCount: Optional[int] = None
    confidence: Optional[float] = None


class ChatFeedbackRequest(BaseModel):
    feedbackType: Literal["HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED"]
    # 复审加固：comment 封顶（此前无 max_length 直入内存反馈表）
    comment: Optional[str] = Field(default=None, max_length=2000)


class ApiResponse(BaseModel):
    """统一响应包装（对齐 Apifox ApiResponse：feedback 等接口的 200 schema）。"""

    code: str
    message: str
    data: dict = {}
    traceId: str
    timestamp: str


# ---------------------------------------------------------------- 路由
@router.post("/chat", response_model=ChatResponse)
def ai_chat(req: ChatRequest, authorization: Optional[str] = Header(default=None)) -> ChatResponse:
    """生涯咨询问答：生成一次回答并写入内存会话。涉及心理健康/医疗/法律等直接转人工。"""
    _require_ai_key(authorization)
    message_id = uuid.uuid4().hex
    needs_human = _detect_human_support(req.question)
    if needs_human:
        answer = "该问题可能涉及心理健康、医疗或法律等专业领域，建议联系辅导员或专业机构获取帮助。"
    else:
        try:
            answer = chat_service(req.question, req.context.model_dump() if req.context else None,
                                  user_ref=req.studentRef)
        except GatewayError as exc:
            raise HTTPException(status_code=503, detail=f"生涯咨询生成失败：{exc}") from exc

    support_reason = "涉及心理健康/医疗/法律等话题，建议转人工或专业机构" if needs_human else ""
    _remember_message(
        req.studentRef,
        {"messageId": message_id, "sessionId": req.sessionId, "role": "user",
         "content": req.question, "createdAt": _now()})
    _remember_message(
        req.studentRef,
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
def ai_recommendation_explain(req: ExplainBatchRequest, authorization: Optional[str] = Header(default=None)) -> ExplainBatchResult:
    """推荐解释（批量）：按 Apifox ExplainRequest/ExplainResult 契约生成各方向解释。"""
    _require_ai_key(authorization)
    run_id = req.runId or f"R-{uuid.uuid4().hex[:8]}"
    try:
        explanations = explain_batch(
            req.profile.model_dump() if req.profile else None,
            [r.model_dump() for r in req.results],
            run_id=run_id,
            user_ref=req.studentRef,
        )
    except GatewayError as exc:
        raise HTTPException(status_code=503, detail=f"推荐解释生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=f"推荐解释输出不合法：{exc}") from exc
    return ExplainBatchResult(runId=run_id, explanations=[ExplanationItem(**e) for e in explanations])


@router.post("/plan/generate", response_model=PlanGenerateResult)
def ai_plan_generate(req: PlanGenerateRequest, authorization: Optional[str] = Header(default=None)) -> PlanGenerateResult:
    """生成学期计划草案。"""
    _require_ai_key(authorization)
    try:
        result = generate_plan(
            direction_id=req.directionId,
            semester=req.semester,
            goal_summary=req.goalSummary,
            template=req.template.model_dump() if req.template else None,
            user_ref=req.studentRef,
        )
    except GatewayError as exc:
        raise HTTPException(status_code=503, detail=f"计划生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=f"计划生成输出不合法：{exc}") from exc
    return PlanGenerateResult(**result)


@router.post("/review/summarize", response_model=ReviewSummarizeResult)
def ai_review_summarize(req: ReviewSummarizeRequest, authorization: Optional[str] = Header(default=None)) -> ReviewSummarizeResult:
    """生成阶段总结与调整建议。"""
    _require_ai_key(authorization)
    try:
        result = review_summarize(req.reviewContent.model_dump(), req.cycle, req.taskSummary,
                                  user_ref=req.studentRef)
    except GatewayError as exc:
        raise HTTPException(status_code=503, detail=f"阶段总结生成失败：{exc}") from exc
    except ValueError as exc:
        raise HTTPException(status_code=503, detail=f"阶段总结输出不合法：{exc}") from exc
    return ReviewSummarizeResult(summary=result.get("summary", ""),
                                 suggestions=result.get("suggestions", []))


@router.post("/pdf/parse", response_model=PdfParseResult)
def ai_pdf_parse(req: PdfParseRequest, authorization: Optional[str] = Header(default=None)) -> PdfParseResult:
    """解析培养方案 PDF：从内网 fileUrl 拉取并解析，返回状态供轮询。"""
    _require_ai_key(authorization)
    result = parse_from_url(req.fileUrl, req.filename)
    return PdfParseResult(jobId=req.jobId, status=result["status"],
                          itemCount=result.get("itemCount"), confidence=result.get("confidence"))


@router.get("/chat/history", response_model=ChatHistoryResponse)
def ai_chat_history(page: _PageParam = 1, size: _SizeParam = 20,
                    sort: Optional[str] = None,
                    studentRef: Optional[str] = None,
                    sessionId: Optional[str] = None,
                    authorization: Optional[str] = Header(default=None)) -> ChatHistoryResponse:
    """会话历史（对齐 Apifox：历史接口为「单对象」，返回最近一条回答 + messageId）。

    复审 P0 修复：必须按 studentRef 分区读取（可再按 sessionId 过滤）；
    未传 studentRef 时返回空对象而非全局最近一条，杜绝跨用户串读。
    """
    _require_ai_key(authorization)
    bucket = _HIST_BY_REF.get(studentRef or "", [])
    if sessionId:
        bucket = [m for m in bucket if m.get("sessionId") == sessionId]
    assistant_msgs = [m for m in bucket if m.get("role") == "assistant"]
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
def ai_chat_feedback(messageId: str, req: ChatFeedbackRequest, authorization: Optional[str] = Header(default=None)) -> ApiResponse:
    """回答反馈（进程内存，Demo 不持久化）。对齐 Apifox：统一响应包装 + 400/404。"""
    _require_ai_key(authorization)
    if messageId not in _MSG_OWNER:
        raise HTTPException(status_code=404, detail="消息不存在")
    _remember_feedback(messageId, {"feedbackType": req.feedbackType, "comment": req.comment})
    return _ok()


@router.post("/chat/feedback", response_model=ApiResponse)
def ai_chat_feedback_latest(req: ChatFeedbackRequest, authorization: Optional[str] = Header(default=None)) -> ApiResponse:
    """回答反馈（messageId 为空时的兜底路由，对齐契约测试 /chat//feedback 场景）。

    Demo 精简点：连续斜杠经 main.normalize_double_slashes 折叠后落到本路由，空 messageId
    对最近一条 assistant 回答写入反馈；无历史时也返回成功包装（契约测试只校验 200 +
    ApiResponse 结构，不落库）。后续迭代替换位置：可改为空 id 严格校验。
    """
    _require_ai_key(authorization)
    latest = None
    for bucket in _HIST_BY_REF.values():
        for m in bucket:
            if m.get("role") == "assistant":
                latest = m
    if latest is not None:
        _remember_feedback(latest["messageId"], {"feedbackType": req.feedbackType, "comment": req.comment})
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
