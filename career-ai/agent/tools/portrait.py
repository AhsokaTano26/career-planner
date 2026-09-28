"""工具：获取学生画像快照（六维度分数 + 优势 + 总结）。

career-core 契约：画像接口为登录态用户维度（/api/v1/students/me/**），
数据范围由 AGENT_SERVICE_TOKEN（学生 JWT）决定，无需 studentId 参数。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_student_portrait() -> str:
    """获取当前学生画像快照，包含六维度分数（兴趣/价值/能力/学业/倾向/实践）、优势和总结。
    用于了解学生的个人特质和能力评估结果。"""
    try:
        data = await get_json("/api/v1/students/me/profile/latest")
        return desensitize(str(data))
    except Exception as e:  # noqa: BLE001
        return f"获取画像失败：{e}"
