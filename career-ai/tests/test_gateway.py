"""AI 网关测试：配置解析 / 限流 / 客户端主流程（mock Router）/ 日志回调 / 网关路由。

Demo 边界：全部离线 mock，不产生真实大模型调用与真实落库。
"""

from datetime import datetime
from types import SimpleNamespace

import pytest
from fastapi.testclient import TestClient

from api.main import app
from gateway.client import GatewayClient, GatewayError
from gateway.config import GatewayConfig, ModelGroup, load_config
from gateway.logging_callback import AiCallLogHandler
from gateway.ratelimit import GatewayRateLimited, TokenBucket

client = TestClient(app)


def _config(**overrides) -> GatewayConfig:
    values = dict(
        api_key="gw-test",
        groups=[ModelGroup(name="default", models=["deepseek/m1"], fallbacks=[])],
        rpm=100,
        timeout=5.0,
        max_retries=0,
    )
    values.update(overrides)
    return GatewayConfig(**values)


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


# ---------------------------------------------------------------- 配置解析
def test_配置解析_模型组JSON(monkeypatch):
    monkeypatch.setenv("GATEWAY_MODEL_GROUPS",
                       '[{"group":"default","models":["deepseek/m1"],"fallbacks":["openai/m2"]}]')
    cfg = load_config()
    assert cfg.groups[0].name == "default"
    assert cfg.groups[0].models == ["deepseek/m1"]
    assert cfg.groups[0].fallbacks == ["openai/m2"]


def test_配置解析_非法JSON回退默认组(monkeypatch):
    monkeypatch.setenv("GATEWAY_MODEL_GROUPS", "not-json")
    cfg = load_config()
    assert cfg.default_group() is not None
    assert cfg.groups[0].models


# ---------------------------------------------------------------- 限流
def test_令牌桶_超限抛429语义异常():
    bucket = TokenBucket(rpm=2)
    bucket.acquire()
    bucket.acquire()
    with pytest.raises(GatewayRateLimited):
        bucket.acquire()


# ---------------------------------------------------------------- 客户端主流程（mock Router）
def _fake_response(text=" 你好 ", model="deepseek/m1", total_tokens=42):
    return {"choices": [{"message": {"role": "assistant", "content": text}}],
            "model": model, "usage": {"total_tokens": total_tokens}}


def test_客户端成功调用_返回文本与元数据():
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(response=_fake_response())
    result = gw.generate([{"role": "user", "content": "hi"}], scene="career_chat", user_ref="u1")
    assert result.text == "你好"
    assert result.model == "deepseek/m1"
    assert result.total_tokens == 42
    assert result.request_id
    # 元数据透传到 completion（供日志回调归因）
    meta = gw._router.kwargs["metadata"]
    assert meta["scene"] == "career_chat"
    assert meta["user_ref"] == "u1"
    assert meta["request_id"] == result.request_id
    assert meta["input_hash"] and meta["request_hash"]


def test_客户端渠道全部失败抛GatewayError():
    gw = GatewayClient(config=_config())
    gw._router = _FakeRouter(error=RuntimeError("boom"))
    with pytest.raises(GatewayError):
        gw.generate([{"role": "user", "content": "hi"}])


def test_客户端超限直接抛429语义异常():
    gw = GatewayClient(config=_config(rpm=1))
    gw._bucket = TokenBucket(rpm=1)
    gw._bucket.acquire()
    with pytest.raises(GatewayRateLimited):
        gw.generate([{"role": "user", "content": "hi"}])


def test_路由构建_包含fallback部署():
    cfg = _config(groups=[ModelGroup(name="default", models=["deepseek/m1"], fallbacks=["openai/m2"])])
    gw = GatewayClient(config=cfg)
    names = [d["model_name"] for d in gw._router.model_list]
    assert "default" in names and "default-fallback" in names


# ---------------------------------------------------------------- 日志回调（mock 落库）
def test_日志回调_成功落库且同request_id去重(monkeypatch):
    recorded = []
    monkeypatch.setattr("gateway.logging_callback.insert_ai_call_log",
                        lambda **kw: recorded.append(kw) or True)
    handler = AiCallLogHandler()
    kwargs = {"model": "deepseek/m1",
              "litellm_params": {"metadata": {"request_id": "r1", "scene": "career_chat",
                                              "user_ref": "u1", "input_hash": "ih", "request_hash": "rh"}}}
    response = SimpleNamespace(usage=SimpleNamespace(prompt_tokens=10, completion_tokens=5))
    start, end = datetime.now(), datetime.now()
    handler.log_success_event(kwargs, response, start, end)
    handler.log_success_event(kwargs, response, start, end)
    assert len(recorded) == 1
    row = recorded[0]
    assert row["status"] == "SUCCESS"
    assert row["scene"] == "career_chat"
    assert row["token_estimate"] == 15
    assert row["model_name"] == "deepseek/m1"


def test_日志回调_失败超时状态(monkeypatch):
    recorded = []
    monkeypatch.setattr("gateway.logging_callback.insert_ai_call_log",
                        lambda **kw: recorded.append(kw) or True)
    handler = AiCallLogHandler()
    kwargs = {"exception": TimeoutError("timed out"),
              "litellm_params": {"metadata": {"request_id": "r2", "scene": "plan_generate"}}}
    handler.log_failure_event(kwargs, None, datetime.now(), datetime.now())
    assert recorded[0]["status"] == "TIMEOUT"


def test_日志回调_重试降级恢复记DEGRADED(monkeypatch):
    """失败事件先到、成功事件后到（fallback 生效）→ 最终落 DEGRADED 且只有一条。"""
    recorded = []
    monkeypatch.setattr("gateway.logging_callback.insert_ai_call_log",
                        lambda **kw: recorded.append(kw) or True)
    handler = AiCallLogHandler()
    meta = {"litellm_params": {"metadata": {"request_id": "r3", "scene": "career_chat"}}}
    response = SimpleNamespace(usage=SimpleNamespace(prompt_tokens=8, completion_tokens=4))
    start, end = datetime.now(), datetime.now()
    handler.log_failure_event({"exception": RuntimeError("boom"), **meta}, None, start, end)
    handler.log_success_event(meta, response, start, end)
    assert len(recorded) == 2
    assert recorded[0]["status"] == "FAILED"
    assert recorded[1]["status"] == "DEGRADED"
    assert recorded[1]["token_estimate"] == 12


# ---------------------------------------------------------------- 网关路由
@pytest.fixture()
def _gw_key(monkeypatch):
    monkeypatch.setenv("GATEWAY_API_KEY", "gw-test")


def test_路由_缺密钥401(_gw_key):
    resp = client.post("/v1/chat/completions", json={
        "messages": [{"role": "user", "content": "hi"}]})
    assert resp.status_code == 401
    resp = client.post("/v1/chat/completions", headers={"Authorization": "Bearer wrong"}, json={
        "messages": [{"role": "user", "content": "hi"}]})
    assert resp.status_code == 401


def test_路由_stream暂不支持400(_gw_key):
    resp = client.post("/v1/chat/completions", headers={"Authorization": "Bearer gw-test"}, json={
        "messages": [{"role": "user", "content": "hi"}], "stream": True})
    assert resp.status_code == 400


def test_路由_chat_completions成功(_gw_key, monkeypatch):
    from gateway.client import GenerateResult

    fake = GatewayClient(config=_config())
    fake._router = _FakeRouter(response=_fake_response())

    import api.routes_gateway as rg

    monkeypatch.setattr(rg, "get_gateway", lambda: fake)
    resp = client.post("/v1/chat/completions", headers={"Authorization": "Bearer gw-test"}, json={
        "model": "default",
        "messages": [{"role": "user", "content": "hi"}],
        "user": "student_ref_x",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["object"] == "chat.completion"
    assert body["choices"][0]["message"]["content"] == "你好"
    assert body["model"] == "deepseek/m1"
    assert body["usage"]["total_tokens"] == 42


def test_路由_generate成功(_gw_key, monkeypatch):
    from gateway.client import GenerateResult

    fake = SimpleNamespace(generate=lambda *a, **kw: GenerateResult(
        text="ok", model="deepseek/m1", request_id="req1", duration_ms=12, total_tokens=9))

    import api.routes_gateway as rg

    monkeypatch.setattr(rg, "get_gateway", lambda: fake)
    resp = client.post("/api/v1/gateway/generate", headers={"Authorization": "Bearer gw-test"}, json={
        "messages": [{"role": "user", "content": "hi"}],
        "scene": "career_chat",
        "userRef": "u1",
    })
    assert resp.status_code == 200
    body = resp.json()
    assert body["text"] == "ok"
    assert body["requestId"] == "req1"
    assert body["durationMs"] == 12
    assert body["totalTokens"] == 9


def test_健康检查含网关概要():
    resp = client.get("/health")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert "gateway" in body
