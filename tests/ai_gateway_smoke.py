"""career-ai 网关 + 智能服务联调冒烟（仅标准库）。

运行前提：
- career-ai 已启动（http://127.0.0.1:8000），.env 配 GATEWAY_API_KEY
- career-core 已启动（http://127.0.0.1:8080），且配置相同 AI_GATEWAY_API_KEY
- MySQL career_core 可连（验 ai_call_log 落库）
运行方式：python3 tests/ai_gateway_smoke.py

无真实 LLM 密钥时的预期：LLM 调用走 502/503 降级，core 侧回退模板，不断言模型文本。
真 LLM 模式：CAREER_AI_REAL_LLM=1（需 career-ai 配真实 LLM_API_KEY），降级断言改为 200+非空内容断言。
"""
import json
import os
import urllib.request
import urllib.error

REAL_LLM = os.getenv("CAREER_AI_REAL_LLM") == "1"

AI = "http://127.0.0.1:8000"
CORE = "http://127.0.0.1:8080/api/v1"
GW_KEY = "career-dev-gateway-key-2026"
# 稳定性 2026-09：career-ai 的 AI 路由与网关同 Bearer 密钥（fail-closed）
AI_HDR = {"Authorization": "Bearer " + GW_KEY}
TIMEOUT = 60

ACCOUNT = "2026011310"
PASSWORD = "Smoke@2026"


def call(base, method, path, body=None, headers=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json"}
    h.update(headers or {})
    req = urllib.request.Request(base + path, data=data, method=method, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
            raw = r.read().decode("utf-8")
            try:
                return r.status, json.loads(raw)
            except Exception:
                return r.status, raw
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}


def main():
    results = []

    def check(name, cond, detail=""):
        results.append(bool(cond))
        print(f"{'PASS' if cond else 'FAIL'}  {name}  {detail}")

    gw = {"Authorization": "Bearer " + GW_KEY}

    # ---- A. career-ai 直调 ----
    s, b = call(AI, "GET", "/health")
    check("AI GET /health", s == 200 and b.get("status") == "ok"
          and b.get("gateway", {}).get("callLogDb") is True, f"status={s}")

    s, b = call(AI, "GET", "/metrics")
    check("AI /metrics无key->401", s == 401, f"status={s}")

    s, b = call(AI, "GET", "/metrics", headers=gw)
    check("AI GET /metrics", s == 200 and isinstance(b, str)
          and "career_ai_requests_total" in b, f"status={s}")

    s, b = call(AI, "POST", "/v1/chat/completions",
                {"messages": [{"role": "user", "content": "hi"}]})
    check("GW 无key->401", s == 401, f"status={s}")

    s, b = call(AI, "POST", "/v1/chat/completions",
                {"messages": [{"role": "user", "content": "hi"}]},
                {"Authorization": "Bearer wrong"})
    check("GW 错key->401", s == 401, f"status={s}")

    s, b = call(AI, "POST", "/v1/chat/completions",
                {"model": "default", "messages": [{"role": "user", "content": "hi"}],
                 "stream": True}, gw)
    check("GW stream=true->400", s == 400, f"status={s}")

    s, b = call(AI, "POST", "/api/v1/gateway/generate",
                {"messages": [{"role": "user", "content": "hi"}]})
    check("GW generate无key->401", s == 401, f"status={s}")

    # 无 LLM key：网关应 502（降级信号），且写 ai_call_log；真 LLM 模式下应 200 有文本
    s, b = call(AI, "POST", "/v1/chat/completions",
                {"model": "default", "messages": [{"role": "user", "content": "hi"}],
                 "max_tokens": 20}, gw)
    if REAL_LLM:
        ok = s == 200 and isinstance(b, dict) and bool(
            ((b.get("choices") or [{}])[0].get("message") or {}).get("content"))
        check("GW 真LLM调用", ok, f"status={s}")
    else:
        check("GW 无LLM->502降级", s == 502, f"status={s}")

    s, b = call(AI, "POST", "/api/v1/gateway/generate",
                {"messages": [{"role": "user", "content": "hi"}], "maxTokens": 20}, gw)
    if REAL_LLM:
        check("GW generate真LLM", s == 200 and bool(b.get("text")), f"status={s}")
    else:
        check("GW generate无LLM->502", s == 502, f"status={s}")

    # 稳定性 2026-09：AI 路由与网关同密钥，无 key 直接 401
    s, b = call(AI, "POST", "/api/v1/ai/chat",
                {"studentRef": "smoke", "sessionId": "s0", "question": "hi"})
    check("AI 无key->401", s == 401, f"status={s}")

    # 不调 LLM 的路径：转人工关键词
    s, b = call(AI, "POST", "/api/v1/ai/chat",
                {"studentRef": "smoke", "sessionId": "s1",
                 "question": "我最近很焦虑怎么办"},
                headers=AI_HDR)
    check("AI chat转人工", s == 200 and b.get("needsHumanSupport") is True,
          f"status={s}")

    s, b = call(AI, "GET", "/api/v1/ai/chat/history?page=&size=&sort=",
                headers=AI_HDR)
    check("AI chat/history空串兼容", s == 200 and "messageId" in b, f"status={s}")
    mid = b.get("messageId", "")

    s, b = call(AI, "POST", f"/api/v1/ai/chat/{mid}/feedback",
                {"feedbackType": "HELPFUL"},
                headers=AI_HDR)
    check("AI chat反馈", s == 200 and b.get("code") == "OK", f"status={s}")

    s, b = call(AI, "POST", "/api/v1/ai/chat/feedback", {"feedbackType": "NEUTRAL"},
                headers=AI_HDR)
    check("AI chat兜底反馈", s == 200 and b.get("code") == "OK", f"status={s}")

    s, b = call(AI, "POST", "/api/v1/ai/recommendation/explain",
                {"studentRef": "smoke", "ruleVersion": "v1", "profileVersion": 1,
                 "results": [{"directionId": "backend_dev", "score": 80, "rank": 1}]},
                headers=AI_HDR)
    if REAL_LLM:
        check("AI explain真LLM", s == 200 and bool(b.get("explanations")), f"status={s}")
    else:
        check("AI explain无LLM->503", s == 503, f"status={s}")

    s, b = call(AI, "POST", "/api/v1/ai/plan/generate",
                {"studentRef": "smoke", "directionId": "backend_dev",
                 "semester": "2026秋"},
                headers=AI_HDR)
    if REAL_LLM:
        check("AI plan真LLM", s == 200 and bool(b.get("goalSummary")), f"status={s}")
    else:
        check("AI plan无LLM->503", s == 503, f"status={s}")

    s, b = call(AI, "POST", "/api/v1/ai/review/summarize",
                {"studentRef": "smoke", "cycle": "month",
                 "reviewContent": {"done": "学了Python"}},
                headers=AI_HDR)
    if REAL_LLM:
        check("AI review真LLM", s == 200 and bool(b.get("summary")), f"status={s}")
    else:
        check("AI review无LLM->503", s == 503, f"status={s}")

    s, b = call(AI, "POST", "/v1/recommendation/explain",
                {"direction": {"direction_id": 1, "name": "后端开发", "score": 0.8}},
                headers=AI_HDR)
    if REAL_LLM:
        check("AI legacy explain真LLM", s == 200 and bool(b.get("reason")), f"status={s}")
    else:
        check("AI legacy explain无LLM->502", s == 502, f"status={s}")

    # ---- B. core→网关端到端（core 应回退而非 500） ----
    s, b = call(CORE, "POST", "/auth/login",
                {"account": ACCOUNT, "password": PASSWORD})
    if not (s == 200 and b.get("code") == "OK"):
        check("CORE 登录", False, f"status={s}")
        print("SOME FAILED"); return 1
    token = b["data"]["accessToken"]
    auth = {"Authorization": "Bearer " + token}

    s, b = call(CORE, "POST", "/ai/chat",
                {"sessionId": "smoke-e2e", "question": "后端方向本周学什么"},
                auth)
    data = (b.get("data") or {}) if isinstance(b, dict) else {}
    ok = s == 200 and b.get("code") == "OK" and isinstance(data.get("answer"), str)
    if REAL_LLM and ok:
        ok = "模板生成" not in data["answer"] and len(data["answer"]) > 20
    check("CORE /ai/chat回退" if not REAL_LLM else "CORE /ai/chat真文本", ok, f"status={s}")

    s, b = call(CORE, "POST", "/ai/recommendation/explain",
                {"ruleVersion": "v1", "profileVersion": 1,
                 "results": [{"directionId": "backend_dev", "score": 80, "rank": 1}]},
                auth)
    data = (b.get("data") or {}) if isinstance(b, dict) else {}
    ok = (s == 200 and b.get("code") == "OK"
          and isinstance(data.get("explanations"), list))
    if REAL_LLM and ok:
        ok = bool(data["explanations"]) and "模板生成" not in json.dumps(
            data["explanations"], ensure_ascii=False)
    check("CORE /ai/explain回退" if not REAL_LLM else "CORE /ai/explain真文本", ok,
          f"status={s}")

    s, b = call(CORE, "POST", "/ai/plan/generate",
                {"directionId": "backend_dev", "semester": "2026秋"}, auth)
    check("CORE /ai/plan回退", s == 200 and b.get("code") == "OK", f"status={s}")

    s, b = call(CORE, "POST", "/ai/review/summarize",
                {"cycle": "month", "reviewContent": {"done": "学了Python"}}, auth)
    check("CORE /ai/review回退", s == 200 and b.get("code") == "OK", f"status={s}")

    s, b = call(CORE, "GET", "/ai/chat/history", headers=auth)
    check("CORE /ai/history", s == 200 and b.get("code") == "OK", f"status={s}")

    print("ALL PASS" if all(results) else "SOME FAILED")
    return 0 if all(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
