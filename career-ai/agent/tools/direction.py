"""工具：查职业方向列表与详情。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_career_directions() -> str:
    """获取全部职业方向列表（名称、类型、性格标签）。
    用于回答"有哪些职业方向"、"介绍一下方向"等问题。"""
    try:
        data = await get_json("/api/v1/directions")
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取方向列表失败：{e}"


@tool
async def get_direction_detail(direction_id: str) -> str:
    """获取单个职业方向详情，包含描述、能力要求、发展路径。
    用于深入了解某个方向的具体要求和发展前景。"""
    try:
        data = await get_json(f"/api/v1/directions/{direction_id}")
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取方向详情失败：{e}"
