"""工具：获取学生画像快照（六维度分数 + 优势 + 总结）。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_student_portrait(student_id: str) -> str:
    """获取学生画像快照，包含六维度分数（兴趣/价值/能力/学业/倾向/实践）、优势和总结。
    用于了解学生的个人特质和能力评估结果。"""
    try:
        data = await get_json("/api/v1/portrait/latest", params={"studentId": student_id})
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取画像失败：{e}"
