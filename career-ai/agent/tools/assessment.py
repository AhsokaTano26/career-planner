"""工具：获取最近一次测评的维度得分。

career-core 契约：测评会话为登录态用户维度；先取会话列表找最近一次已完成会话，
再查该会话的六维度得分（/api/v1/assessment-sessions/**）。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.tools.career_core_client import get_json
from services.desensitizer import desensitize


@tool
async def get_assessment_scores() -> str:
    """获取当前学生最近一次生涯测评的六维度得分。
    用于了解学生的兴趣、能力、学业等测评结果。"""
    try:
        sessions = await get_json("/api/v1/assessment-sessions")
        if not isinstance(sessions, list):
            return desensitize(str(sessions))
        completed = [s for s in sessions if s.get("status") == "COMPLETED"]
        if not completed:
            return "暂无已完成的测评会话"
        latest = max(completed, key=lambda s: s.get("finishedAt") or "")
        scores = await get_json(f"/api/v1/assessment-sessions/{latest['id']}/scores")
        return desensitize(f"会话={latest.get('id')} 问卷={latest.get('questionnaireName')} 完成于={latest.get('finishedAt')} 得分={scores}")
    except Exception as e:  # noqa: BLE001
        return f"获取测评得分失败：{e}"
