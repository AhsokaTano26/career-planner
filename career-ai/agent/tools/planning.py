"""工具：查当前学期计划与任务完成进度。

career-core 契约：计划为登录态用户维度（/api/v1/students/me/plans/**）。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_plan_progress() -> str:
    """获取当前学生最新一版学期计划：目标摘要、学期目标与月度任务清单。
    用于回答"我这个学期的计划进行得怎么样"、"接下来该做什么"等问题。"""
    try:
        data = await get_json("/api/v1/students/me/plans/latest")
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取计划进度失败：{e}"
