"""工具：获取最新推荐运行结果（方向 + 分数 + 排名）。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_recommendation_results(student_id: str) -> str:
    """获取学生最新一次职业方向推荐结果，包含推荐方向列表、匹配分数和排名。
    用于回答"为什么推荐这个方向"、"我适合什么方向"等问题。"""
    try:
        data = await get_json("/api/v1/recommendation/latest", params={"studentId": student_id})
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取推荐结果失败：{e}"
