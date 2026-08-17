"""计划生成：根据目标与画像生成学期计划草案。

流程：拼接系统提示词 + 目标/方向/维度 → 调用大模型 → 校验输出为结构化 PlanDraft。
边界：大模型失败抛 LlmError 由调用方（career-core）回退规则模板。
"""

from __future__ import annotations

import json

from services.llm_gateway import generate

_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「计划生成器」。请根据给定的目标方向、画像维度与目标摘要，"
    "生成一份一学期的计划草案。只输出 JSON，不要输出任何额外文字或 Markdown 代码块。"
    "JSON 结构固定为：{\"goalSummary\": 字符串, \"semesterGoals\": [{\"title\": 字符串, "
    "\"abilityTag\": 字符串}], \"monthlyTasks\": [{\"month\": \"YYYY-MM\", \"title\": 字符串, "
    "\"taskType\": \"LEARNING/PRACTICE/CAREER/REVIEW\", \"estimatedHours\": 数字}], "
    "\"notes\": [字符串]}。monthlyTasks 给出 4-6 个月度任务。"
)


def generate_plan(direction_id: str, semester: str, goal_summary: str | None = None,
                  template: dict | None = None) -> dict:
    """生成学期计划草案，返回结构化 dict（goalSummary/semesterGoals/monthlyTasks/notes）。

    输入按 Apifox PlanGenerateRequest：directionId/semester/goalSummary/template(PlanDraft)。
    """
    user_prompt = (
        f"方向编码：{direction_id or '未指定'}\n"
        f"学期：{semester or '未指定'}\n"
        f"目标摘要：{goal_summary or (template or {}).get('goalSummary') or '围绕目标方向打好基础，完成一个小项目'}\n"
        f"参考模板：{json.dumps(template or {}, ensure_ascii=False)}\n"
        "请生成计划草案 JSON。"
    )
    content = generate(
        [
            {"role": "system", "content": _SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.7,
        # 推理模型需更大预算，避免 reasoning 占满后 content 为空（deepseek-v4-flash 实测）
        max_tokens=2000,
    )
    return _normalize(_parse_json(content))


def _normalize(data: dict) -> dict:
    """补齐契约必填字段默认值，避免模型少返回字段导致契约校验失败（Demo 精简点）。"""
    if not data.get("goalSummary"):
        data["goalSummary"] = "围绕目标方向完成一学期学习与一个小项目"
    semester_goals = data.get("semesterGoals") or []
    data["semesterGoals"] = [
        {"title": (g or {}).get("title") or "未命名目标", "abilityTag": (g or {}).get("abilityTag")}
        for g in semester_goals if isinstance(g, dict)
    ]
    monthly_tasks = data.get("monthlyTasks") or []
    data["monthlyTasks"] = [
        {"month": (t or {}).get("month") or "2026-09",
         "title": (t or {}).get("title") or "学习任务",
         "taskType": (t or {}).get("taskType"),
         "estimatedHours": (t or {}).get("estimatedHours")}
        for t in monthly_tasks if isinstance(t, dict)
    ]
    data["notes"] = [str(n) for n in (data.get("notes") or [])]
    return data


def _parse_json(content: str) -> dict:
    """解析模型输出的 JSON；失败时抛 ValueError 由调用方回退模板。"""
    text = content.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:]
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        raise ValueError("计划生成输出不含合法 JSON")
    return json.loads(text[start:end + 1])


__all__ = ["generate_plan"]
