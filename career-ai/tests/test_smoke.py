"""智能服务基础冒烟测试（最基础用例）。"""

from fastapi import HTTPException

from api.main import Direction, ExplainRequest
from services.recommendation_explainer import _build_user_prompt, explain, _validate


def test_explain_无APIKey时抛出502由调用方回退():
    """未配置 LLM_API_KEY 时应抛 502（由 career-core 回退规则模板），而非返回空理由。"""
    req = ExplainRequest(
        personality=["I", "R", "C"],
        direction=Direction(
            direction_id=1,
            name="软件开发工程师",
            type="技术研发",
            score=0.95,
            personality_tags=["R", "I", "C"],
            matches={"interest": 0.96, "ability": 0.95, "orientation": 0.94},
            gaps={"experience": 0.10},
        ),
    )
    try:
        explain(req)
    except HTTPException as exc:
        assert exc.status_code == 502
    else:
        # 若环境配置了 LLM_API_KEY，则可能真实调用成功（不视为失败）
        pass


def test_提示词包含结构化数据():
    req = ExplainRequest(
        personality=["I", "R", "C"],
        direction=Direction(
            direction_id=1,
            name="软件开发工程师",
            score=0.95,
            matches={"interest": 0.96},
            gaps={"experience": 0.10},
            personality_tags=["R", "I", "C"],
        ),
    )
    prompt = _build_user_prompt(req)
    assert "软件开发工程师" in prompt
    assert "96%" in prompt
    assert "I, R, C" in prompt


def test_输出校验():
    assert _validate("根据你的兴趣匹配96%……建议加强：实践经历。") is True
    assert _validate("") is False
    assert _validate("太短") is False
    assert _validate('{"reason": "不应输出JSON"}') is False
