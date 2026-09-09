"""推荐解释：根据结构化评分生成自然语言推荐解释。

流程：拼接系统提示词（prompts/recommendation_explainer.txt）+ 结构化数据 → 调用大模型 → 校验输出。
边界（与《具体实现细节_MVP_V1.0.md》一致）：
  - 只接收最小化结构化输入，不持有学生身份信息（student_id 可为空）；
  - AI 只生成“受约束的解释”，严格依据输入数据中的数值与事实，不虚构；
  - 大模型失败/输出不合法时抛 HTTPException(502)，由 career-core 侧回退规则模板。
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from fastapi import HTTPException

from gateway.cache import explain_cache_enabled, new_explain_cache
from providers.deepseek import DEFAULT_MODEL, LlmError
from services.desensitizer import desensitize
from services.llm_gateway import generate, generate_json

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
                  user_ref: str | None = None, model_group: str | None = None,
                  prompt_version: str | None = None,
                  temperature: float = 0.3) -> list[dict]:
    """为候选方向批量生成推荐解释，返回 explanations 列表。

    输入按 Apifox ExplainRequest：profile（六维得分）、results（[{directionId, score, rank}]）。
    输出每项含 directionId/summary/confidenceText/disclaimer。
    大模型失败抛 LlmError；输出不合法抛 ValueError（由路由映射为 503）。
    run_id/user_ref 写入 ai_call_log 用于归因（Demo 精简点：仅记 user_ref，批次号暂不入库）。
    生产化（2026-09）：identical prompt 命中进程内 LRU 缓存时直接返回，
    另落一条 model_name='cache'、token=0 的 SUCCESS 行（不耗额度）。
    复审 P0 修复：user_prompt 先过 desensitize（此前全程未脱敏）；缓存键绑定
    模型组/promptVersion/temperature（此前只 hash prompt，换模型命中旧答案）；
    缓存命中每次生成新 request_id（此前复用同一 cache-* id 致跨用户归因污染）。
    """
    user_prompt = desensitize(_build_batch_prompt(profile, results))
    cache_key = _cache_key(user_prompt, model_group, prompt_version, temperature)
    if explain_cache_enabled():
        hit = _EXPLAIN_CACHE.get(cache_key)
        if hit is not None:
            _log_cache_hit(user_ref)
            # 稳定性：缓存命中同样记 metrics（否则缓存率越高 Grafana 越失真）
            from gateway.metrics import metrics as _metrics

            _metrics.observe("recommendation_explain", "SUCCESS", 0.0, None)
            return [dict(item) for item in hit]
    content = generate_json(
        [
            {"role": "system", "content": _BATCH_SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ],
        _parse_batch_json,
        temperature=temperature,
        # 推理模型需更大预算，避免 reasoning 占满后 content 为空（deepseek-v4-flash 实测）
        max_tokens=2000,
        scene="recommendation_explain",
        user_ref=user_ref,
    )
    explanations = content
    if explain_cache_enabled():
        _EXPLAIN_CACHE.set(cache_key, [dict(item) for item in explanations])
    return explanations


_EXPLAIN_CACHE = new_explain_cache()


def reset_explain_cache() -> None:
    """测试用：清空推荐解释缓存。"""
    _EXPLAIN_CACHE.clear()


def _cache_key(user_prompt: str, model_group: str | None = None,
               prompt_version: str | None = None, temperature: float = 0.7) -> str:
    material = "recommendation_explain|%s|%s|%s|%s" % (
        model_group or "default", prompt_version or "", temperature, user_prompt)
    return hashlib.sha256(material.encode("utf-8")).hexdigest()


def _log_cache_hit(user_ref: str | None) -> None:
    """缓存命中落库：每次新 request_id（model_name='cache'），避免跨用户归因污染。"""
    import uuid

    from gateway.db import insert_ai_call_log  # 局部导入：避免循环

    insert_ai_call_log(
        request_id="cache-" + uuid.uuid4().hex[:16],
        scene="recommendation_explain",
        status="SUCCESS",
        model_name="cache",
        user_ref=user_ref,
        duration_ms=0,
        token_estimate=0,
    )


def _build_batch_prompt(profile: dict | None, results: list[dict]) -> str:
    """把画像维度得分与候选方向拼成易读的 user 提示词（含方向名/匹配Top3/差距Top2）。"""
    lines = []
    if profile:
        dims = "；".join(f"{_DIM_NAMES.get(k, k)}={_pct(v)}" for k, v in profile.items() if v is not None)
        lines.append(f"画像维度得分（0-100分）：{dims or '无'}")
    items = [_format_result(r) for r in results]
    lines.append("候选方向：" + ("\n".join(items) if items else "无"))
    lines.append("请为每个候选方向生成解释 JSON。解释须结合画像维度说明匹配原因，"
                 "引用方向的关键匹配/差距，不得虚构课程与就业数据。")
    return "\n".join(lines)


_DIM_NAMES = {
    "interest": "兴趣", "values": "价值观", "ability": "能力",
    "academic": "学业", "tendency": "倾向", "practice": "实践",
}


def _pct(value) -> str:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return "无"
    return f"{round(number * 100 if number <= 1 else number)}分"


def _format_result(result: dict) -> str:
    name = result.get("directionName") or result.get("directionId")
    head = f"- {name}：得分 {result.get('score')}，排名 {result.get('rank')}"
    matches = _top_map(result.get("matches"), 3)
    gaps = _top_map(result.get("gaps"), 2, reverse=True)
    tails = []
    if matches:
        tails.append("匹配：" + "、".join(matches))
    if gaps:
        tails.append("差距：" + "、".join(gaps))
    return head + ("（" + "；".join(tails) + "）" if tails else "")


def _top_map(mapping: dict | None, limit: int, reverse: bool = False) -> list[str]:
    if not isinstance(mapping, dict) or not mapping:
        return []
    items = []
    for key, value in mapping.items():
        try:
            number = float(value)
        except (TypeError, ValueError):
            continue
        items.append((key, number))
    items.sort(key=lambda kv: kv[1], reverse=not reverse)
    return [f"{_DIM_NAMES.get(k, k)}{_pct(v)}" for k, v in items[:limit]]


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

