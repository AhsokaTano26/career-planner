"""Agent 工具集：调用 career-core API 获取学生数据 + RAG 知识检索。

每个工具的返回数据先经 desensitize() 脱敏（姓名/学号/手机/邮箱 → 占位符），
再作为 ToolMessage 进入 LLM 上下文。
"""

from __future__ import annotations

from agent.tools.assessment import get_assessment_scores
from agent.tools.direction import get_career_directions, get_direction_detail
from agent.tools.knowledge import search_knowledge
from agent.tools.planning import get_plan_progress
from agent.tools.portrait import get_student_portrait
from agent.tools.recommendation import get_recommendation_results

ALL_TOOLS = [
    get_student_portrait,
    get_assessment_scores,
    get_recommendation_results,
    get_plan_progress,
    get_career_directions,
    get_direction_detail,
    search_knowledge,
]

__all__ = ["ALL_TOOLS"]
