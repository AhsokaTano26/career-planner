"""生涯规划智能服务（career-ai）入口。

启动（在 career-ai 目录下执行）：
    uvicorn api.main:app --host 127.0.0.1 --port 8000

边界：本服务不连接 MySQL、不持有学生身份信息（student_id 可选留空）、不写入正式业务表；
只接收最小化结构化输入并返回校验后的 JSON。大模型失败时由 career-core 回退规则模板。
"""

from __future__ import annotations

from pathlib import Path
from typing import Dict, List, Optional

from dotenv import load_dotenv
from fastapi import FastAPI
from pydantic import BaseModel

# 加载 career-ai/.env（如存在）
load_dotenv(Path(__file__).resolve().parent.parent / ".env")

app = FastAPI(title="career-ai", version="0.1.0")


# ---------------------------------------------------------------- 模型定义
class Direction(BaseModel):
    """单条推荐方向的结构化评分数据（来自 career-core 规则引擎）。"""

    direction_id: int
    name: str
    type: Optional[str] = None
    score: float = 0.0
    rank: Optional[int] = None
    personality_tags: Optional[List[str]] = None  # 方向霍兰德标签（RIASEC）
    matches: Optional[Dict[str, float]] = None    # 各维度匹配度（0-1）
    gaps: Optional[Dict[str, float]] = None       # 各维度差距（0-1，供“建议加强”提示）


class ExplainRequest(BaseModel):
    """推荐解释请求：最小化结构化输入，不包含学生身份信息。"""

    student_id: Optional[int] = None  # Demo 边界：可为空，避免持有身份信息
    personality: Optional[List[str]] = None  # 学生霍兰德人格类型（RIASEC）
    direction: Direction


class ExplainResponse(BaseModel):
    """推荐解释响应：自然语言理由 + 所用模型名。"""

    reason: str
    model: str


# ---------------------------------------------------------------- 路由
@app.post("/v1/recommendation/explain", response_model=ExplainResponse)
def recommendation_explain(req: ExplainRequest) -> ExplainResponse:
    """根据结构化评分数据生成自然语言推荐解释（调用大模型，失败抛 502 由调用方回退）。"""
    from services.recommendation_explainer import explain

    return explain(req)


@app.get("/health")
def health() -> Dict[str, str]:
    """健康检查。"""
    return {"status": "ok", "service": "career-ai"}
