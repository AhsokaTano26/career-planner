"""网关生产化测试：fallback 链 / 预算 / 缓存 / 指标（全部离线 mock，不耗真实额度）。"""

import time

import pytest

from gateway.budget import DailyTokenBudget
from gateway.cache import TTLCache
from gateway.client import GatewayClient, GatewayError
from gateway.config import GatewayConfig, ModelGroup
from gateway.metrics import Metrics
from gateway.ratelimit import GatewayRateLimited


class _FakeRouter:
    def __init__(self, response=None, error=None):
        self.response = response
        self.error = error
        self.kwargs = None

    def completion(self, **kwargs):
        self.kwargs = kwargs
        if self.error is not None:
            raise self.error
        return self.response


def _config(**overrides):
    values = dict(
        api_key="gw-test",
        groups=[ModelGroup(name="default", models=["deepseek/m1"], fallbacks=["openai/m2"])],
        rpm=1000,
        timeout=5.0,
        max_retries=0,
    )
    values.update(overrides)
    return GatewayConfig(**values)


def _fake_response(text="ok", model="deepseek/m1", total_tokens=42):
    return {"choices": [{"message": {"role": "assistant", "content": text}}],
            "model": model, "usage": {"total_tokens": total_tokens}}


# ---------------------------------------------------------------- fallback
def test_路由fallback格式为组到备用组映射():
    gw = GatewayClient(config=_config())
    assert gw._router.fallbacks == [{"default": ["default-fallback"]}]


def test_fallback命中记DEGRADED后缀行(monkeypatch):
    recorded = []
    monkeypatch.setattr("gateway.db.insert_ai_call_log",
                        lambda **kw: recorded.append(kw) or True)
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(response=_fake_response(text="fb", model="openai/m2"))
    result = gw.generate([{"role": "user", "content": "hi"}], scene="career_chat")
    assert result.text == "fb"
    suffix_rows = [r for r in recorded if r["scene"] == "career_chat:fallback"]
    assert len(suffix_rows) == 1
    assert suffix_rows[0]["status"] == "DEGRADED"
    assert suffix_rows[0]["model_name"] == "openai/m2"


def test_主通道成功不记fallback行(monkeypatch):
    recorded = []
    monkeypatch.setattr("gateway.db.insert_ai_call_log",
                        lambda **kw: recorded.append(kw) or True)
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(response=_fake_response(model="deepseek/m1"))
    gw.generate([{"role": "user", "content": "hi"}], scene="career_chat")
    assert [r for r in recorded if "fallback" in r["scene"]] == []


def test_全部通道失败仍抛GatewayError():
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(error=RuntimeError("boom"))
    with pytest.raises(GatewayError):
        gw.generate([{"role": "user", "content": "hi"}])


# ---------------------------------------------------------------- 预算与钳制
def test_场景max_tokens钳制():
    gw = GatewayClient(config=_config(scene_max_tokens={"career_chat": 100}))
    gw._router = _FakeRouter(response=_fake_response())
    gw.generate([{"role": "user", "content": "hi"}], scene="career_chat", max_tokens=2000)
    assert gw._router.kwargs["max_tokens"] == 100


def test_未配置钳制保持原值():
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(response=_fake_response())
    gw.generate([{"role": "user", "content": "hi"}], scene="career_chat", max_tokens=2000)
    assert gw._router.kwargs["max_tokens"] == 2000


def test_单日预算超限抛429语义():
    budget = DailyTokenBudget({"career_chat": 100})
    budget.check("career_chat")
    budget.record("career_chat", 60)
    budget.check("career_chat")
    budget.record("career_chat", 50)
    with pytest.raises(GatewayRateLimited):
        budget.check("career_chat")


def test_客户端预算耗尽拒绝调用():
    gw = GatewayClient(config=_config(daily_token_budget={"career_chat": 42}))
    gw._router = _FakeRouter(response=_fake_response(total_tokens=42))
    # 预算按“预占 max_tokens”执行：首次预占 42 未超，成功结算 42；第二次预占即拒绝
    gw.generate([{"role": "user", "content": "hi"}], scene="career_chat", max_tokens=42)
    with pytest.raises(GatewayRateLimited):
        gw.generate([{"role": "user", "content": "hi"}], scene="career_chat", max_tokens=42)


# ---------------------------------------------------------------- 缓存
def test_TTL缓存_过期与LRU():
    cache = TTLCache(maxsize=2, ttl=1)
    cache.set("a", 1)
    cache.set("b", 2)
    assert cache.get("a") == 1
    cache.set("c", 3)  # 驱逐最久未用 b（a 刚被 get）
    assert cache.get("b") is None
    time.sleep(1.05)
    assert cache.get("a") is None


def test_推荐解释缓存_二次调用不调模型(monkeypatch):
    import services.recommendation_explainer as explainer

    explainer.reset_explain_cache()
    calls = []
    monkeypatch.setattr(explainer, "generate",
                        lambda *a, **k: calls.append(1) or '{"explanations": [{"directionId": "d1", "summary": "好"}]}')
    logged = []
    monkeypatch.setattr("gateway.db.insert_ai_call_log",
                        lambda **kw: logged.append(kw) or True)
    args = ({"interest": 0.8}, [{"directionId": "d1", "score": 80, "rank": 1}])
    first = explainer.explain_batch(*args, user_ref="u1")
    second = explainer.explain_batch(*args, user_ref="u1")
    assert len(calls) == 1
    assert first == second
    cache_rows = [r for r in logged if r.get("model_name") == "cache"]
    assert len(cache_rows) == 1
    assert cache_rows[0]["token_estimate"] == 0
    explainer.reset_explain_cache()


# ---------------------------------------------------------------- 指标
def test_指标计数与直方图渲染():
    m = Metrics()
    m.observe("career_chat", "SUCCESS", 0.3, 10)
    m.observe("career_chat", "FAILED", 65.0, None)
    text = m.render()
    assert 'career_ai_requests_total{scene="career_chat",status="SUCCESS"} 1' in text
    assert 'career_ai_requests_total{scene="career_chat",status="FAILED"} 1' in text
    assert 'career_ai_tokens_total{scene="career_chat"} 10' in text
    assert 'career_ai_duration_seconds_bucket{scene="career_chat",le="0.5"} 1' in text
    assert 'career_ai_duration_seconds_bucket{scene="career_chat",le="+Inf"} 2' in text


def test_metrics路由可达():
    import os
    from fastapi.testclient import TestClient

    from api.main import app

    key = os.getenv("GATEWAY_API_KEY", "test-gateway-key")
    # 无有效 key → 401（复审加固：指标不再裸奔；conftest 默认自动带头，故显式传空头测 401）
    assert TestClient(app).get("/metrics", headers={"Authorization": ""}).status_code == 401
    assert TestClient(app).get("/metrics", headers={"Authorization": "Bearer wrong"}).status_code == 401
    resp = TestClient(app).get("/metrics", headers={"Authorization": "Bearer " + key})
    assert resp.status_code == 200
    assert "career_ai_requests_total" in resp.text


# ---------------------------------------------------------------- AI 路由鉴权（稳定性 2026-09）
def test_AI路由_错密钥401():
    from fastapi.testclient import TestClient

    from api.main import app

    resp = TestClient(app).post(
        "/api/v1/ai/chat",
        headers={"Authorization": "Bearer wrong"},
        json={"studentRef": "s", "sessionId": "s1", "question": "hi"},
    )
    assert resp.status_code == 401


def test_AI路由_空密钥503拒绝服务(monkeypatch):
    from fastapi.testclient import TestClient

    from api.main import app

    monkeypatch.setenv("GATEWAY_API_KEY", "")
    resp = TestClient(app).post(
        "/api/v1/ai/chat",
        headers={"Authorization": ""},
        json={"studentRef": "s", "sessionId": "s1", "question": "hi"},
    )
    assert resp.status_code == 503
