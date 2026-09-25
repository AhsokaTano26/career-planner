"""工具：查当前学期计划与任务完成进度。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_plan_progress(student_id: str) -> str:
    """获取学生当前学期的计划、月度任务与完成进度。
    用于回答"我这个学期的计划进行得怎么样"、"接下来该做什么"等问题。"""
    try:
        data = await get_json(
            "/api/v1/planning/semesters/current", params={"studentId": student_id}
        )
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取计划进度失败：{e}"
