"""工具：获取最近一次测评的维度得分。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_assessment_scores(student_id: str) -> str:
    """获取学生最近一次生涯测评的六维度得分。
    用于了解学生的兴趣、能力、学业等测评结果。"""
    try:
        data = await get_json(
            "/api/v1/assessment/sessions/latest", params={"studentId": student_id}
        )
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取测评得分失败：{e}"
