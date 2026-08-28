"""推荐解释：根据结构化评分生成自然语言推荐解释。

流程：拼接系统提示词（prompts/recommendation_explainer.txt）+ 结构化数据 → 调用大模型 → 校验输出。
边界（与《具体实现细节_MVP_V1.0.md》一致）：
  - 只接收最小化结构化输入，不持有学生身份信息（student_id 可为空）；
  - AI 只生成“受约束的解释”，严格依据输入数据中的数值与事实，不虚构；
  - 大模型失败/输出不合法时抛 HTTPException(502)，由 career-core 侧回退规则模板。
"""

from __future__ import annotations

import json
from pathlib import Path

from fastapi import HTTPException

from providers.deepseek import DEFAULT_MODEL, LlmError
from services.llm_gateway import generate

# 读取系统提示词模板；缺失时使用内置兜底文案，保证服务可独立运行
_PROMPT_PATH = Path(__file__).resolve().parent.parent / "prompts" / "recommendation_explainer.txt"

_FALLBACK_SYSTEM_PROMPT = (
    "你是生涯规划系统中的“推荐解释生成器”。请基于给定的结构化推荐数据，生成一段"
    "简洁、客观、贴合数据的中文推荐理由，只依据输入中的数值与事实，不得虚构或夸大。"
    "直接输出中文正文，不要输出 JSON 或任何额外说明。"
)

_SYSTEM_PROMPT = (
    _PROMPT_PATH.read_text(encoding="utf-8")
    if _PROMPT_PATH.exists()
    else _FALLBACK_SYSTEM_PROMPT
)

MIN_REASON_LEN = 20
MAX_REASON_LEN = 500


def explain(request) -> "ExplainResponse":
    """根据请求中的方向评分数据，调用大模型生成推荐解释。

    请求模型见 api/main.py 的 ExplainRequest（含 direction 的评分/匹配/差距/人格标签）。
    输出模型 ExplainResponse：reason（自然语言理由）、model（所用模型名）。
    """
    from api.main import ExplainResponse  # 避免循环导入：模型定义在 api.main

    user_prompt = _build_user_prompt(request)

    try:
        content = generate(
            [
                {"role": "system", "content": _SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.7,
            max_tokens=1500,
            scene="recommendation_explain",
            user_ref=str(request.student_id) if request.student_id is not None else None,
        )
    except LlmError as exc:
        # Demo 边界：AI 失败由后端（career-core）回退规则模板，此处抛 502
        raise HTTPException(status_code=502, detail=f"推荐解释生成失败：{exc}") from exc

    if not _validate(content):
        raise HTTPException(status_code=502, detail="大模型返回的推荐解释不符合要求（长度/格式）")

    return ExplainResponse(reason=content, model=DEFAULT_MODEL)


def _build_user_prompt(request) -> str:
    """把结构化评分数据拼成易读的 user 提示词。"""
    d = request.direction
    lines = [
        f"方向名称：{d.name}",
        f"方向类型：{d.type or '未分类'}",
        f"综合匹配度：{round(d.score * 100)}%",
        f"各维度匹配：{_fmt_map(d.matches)}",
        f"各维度差距：{_fmt_map(d.gaps)}",
        f"方向霍兰德人格标签：{', '.join(d.personality_tags) if d.personality_tags else '无'}",
        f"学生霍兰德人格类型：{', '.join(request.personality) if request.personality else '未测评'}",
        "请据此生成推荐理由。",
    ]
    return "\n".join(lines)


def _fmt_map(mapping) -> str:
    if not mapping:
        return "无"
    return "；".join(f"{k}={round(v * 100)}%" for k, v in mapping.items())


def _validate(content: str) -> bool:
    """校验输出：非空、长度在合理区间、不含非法 JSON 包装。"""
    if not content:
        return False
    if len(content) < MIN_REASON_LEN or len(content) > MAX_REASON_LEN:
        return False
    stripped = content.strip()
    if stripped.startswith("{") or stripped.startswith("```"):
        return False
    return True


# ---------------------------------------------------------------- 批量推荐解释（Apifox /api/v1/ai/recommendation/explain）
_BATCH_SYSTEM_PROMPT = (
    "你是生涯规划系统中的「推荐解释生成器」。请基于给定的画像维度得分与候选方向列表，"
    "为每个候选方向生成通俗解释。只输出 JSON，不要输出任何额外文字或 Markdown 代码块。"
    "JSON 结构固定为：{\"explanations\": [{\"directionId\": 字符串, \"summary\": 通俗解释, "
    "\"confidenceText\": 可信程度文字, \"disclaimer\": \"智能生成，供探索参考\"}]}。"
    "必须为每个候选方向各生成一条解释，directionId 与输入保持一致，summary 简洁客观、"
    "只依据输入数值，不得虚构。"
)


def explain_batch(profile: dict | None, results: list[dict], *, run_id: str | None = None,
                  user_ref: str | None = None) -> list[dict]:
    """为候选方向批量生成推荐解释，返回 explanations 列表。

    输入按 Apifox ExplainRequest：profile（六维得分）、results（[{directionId, score, rank}]）。
    输出每项含 directionId/summary/confidenceText/disclaimer。
    大模型失败抛 LlmError；输出不合法抛 ValueError（由路由映射为 503）。
    run_id/user_ref 写入 ai_call_log 用于归因（Demo 精简点：仅记 user_ref，批次号暂不入库）。
    """
    content = generate(
        [
            {"role": "system", "content": _BATCH_SYSTEM_PROMPT},
            {"role": "user", "content": _build_batch_prompt(profile, results)},
        ],
        temperature=0.7,
        # 推理模型需更大预算，避免 reasoning 占满后 content 为空（deepseek-v4-flash 实测）
        max_tokens=2000,
        scene="recommendation_explain",
        user_ref=user_ref,
    )
    return _parse_batch_json(content)


def _build_batch_prompt(profile: dict | None, results: list[dict]) -> str:
    """把画像维度得分与候选方向拼成易读的 user 提示词。"""
    lines = []
    if profile:
        dims = "；".join(f"{k}={round(v * 100)}%" for k, v in profile.items() if v is not None)
        lines.append(f"画像维度得分：{dims or '无'}")
    items = [f"- {r.get('directionId')}：得分 {r.get('score')}，排名 {r.get('rank')}" for r in results]
    lines.append("候选方向：" + ("\n".join(items) if items else "无"))
    lines.append("请为每个候选方向生成解释 JSON。")
    return "\n".join(lines)


def _parse_batch_json(content: str) -> list[dict]:
    """解析模型输出；校验 explanations 非空且逐条含 directionId/summary。"""
    text = content.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:]
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        raise ValueError("推荐解释输出不含合法 JSON")
    data = json.loads(text[start:end + 1])
    explanations = data.get("explanations")
    if not isinstance(explanations, list) or not explanations:
        raise ValueError("推荐解释输出缺少 explanations")
    for item in explanations:
        if not item.get("directionId") or not item.get("summary"):
            raise ValueError("推荐解释条目缺少 directionId/summary")
        item.setdefault("confidenceText", "数据基本完整，供参考")
        item.setdefault("disclaimer", "智能生成，供探索参考")
    return explanations


__all__ = ["explain", "explain_batch"]

