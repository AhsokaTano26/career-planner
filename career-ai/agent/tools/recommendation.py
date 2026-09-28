"""工具：获取最新推荐运行结果（方向 + 分数 + 排名）。

career-core 契约：推荐接口为登录态用户维度（/api/v1/students/me/recommendations/latest），
数据范围由 AGENT_SERVICE_TOKEN（学生 JWT）决定，无需 studentId 参数。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_recommendation_results() -> str:
    """获取当前学生最新一次职业方向推荐结果，包含推荐方向列表、匹配分数和排名。
    用于回答"为什么推荐这个方向"、"我适合什么方向"等问题。"""
    try:
        data = await get_json("/api/v1/students/me/recommendations/latest")
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取推荐结果失败：{e}"
