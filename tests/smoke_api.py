"""跨服务接口冒烟测试（JWT 契约）：验证 career-core 核心接口。

运行前提：career-core 已启动（http://127.0.0.1:8080），MySQL 已建库并执行种子。
运行方式：python3 tests/smoke_api.py

契约（2026-09，与 Apifox 线上一致）：
- 统一前缀 /api/v1，JWT 认证（login 拿 accessToken，其余带 Authorization 头）；
- 成功响应为包裹体 {code:"OK", message, data, traceId, timestamp}；
- 业务对象 null 字段不序列化（如画像 feedback 为空时 key 不出现）。
"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080/api/v1"
# 推荐/计划可能触发 career-ai 调用，超时放宽（无 AI 服务时后端走降级）
TIMEOUT = 120

# 白名单种子（db/data.sql）：用 WL003，避免与手工测试的 WL001 冲突；
# 若已注册则走登录分支。冒烟新密码固定，重复跑可直接登录。
STUDENT_NO = "2026011310"
STUDENT_NAME = "王芳"
INITIAL_PASSWORD = "202610"
SMOKE_PASSWORD = "Smoke@2026"


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}


def main():
    results = []

    def check(name, cond, detail=""):
        results.append(cond)
        print(f"{'PASS' if cond else 'FAIL'}  {name}  {detail}")

    # 1) 注册（幂等：已注册则登录）
    status, body = call("POST", "/auth/register", {
        "studentNo": STUDENT_NO, "name": STUDENT_NAME, "initialPassword": INITIAL_PASSWORD,
    })
    password = INITIAL_PASSWORD
    if status == 200 and body.get("code") == "OK":
        token = body["data"]["accessToken"]
        check("POST /auth/register", True, f"status={status}")
    else:
        for pwd in (SMOKE_PASSWORD, INITIAL_PASSWORD):
            status, body = call("POST", "/auth/login", {
                "account": STUDENT_NO, "password": pwd,
            })
            if status == 200 and body.get("code") == "OK":
                password = pwd
                break
        ok = status == 200 and body.get("code") == "OK"
        check("POST /auth/login（已注册分支）", ok, f"status={status} code={body.get('code')}")
        if not ok:
            print("SOME FAILED"); return 1
        token = body["data"]["accessToken"]

    # 1b) 首改密码门禁：新用户 passwordChangeRequired=true，业务接口会 403；
    # 按真实开户流程先改密再重新登录
    status, body = call("GET", "/students/me", token=token)
    if status == 403:
        status, body = call("PATCH", "/auth/me/password", {
            "oldPassword": password, "newPassword": SMOKE_PASSWORD,
        }, token=token)
        ok = status == 200 and body.get("code") == "OK"
        check("PATCH /auth/me/password（首改）", ok, f"status={status}")
        if not ok:
            print("SOME FAILED"); return 1
        status, body = call("POST", "/auth/login", {
            "account": STUDENT_NO, "password": SMOKE_PASSWORD,
        })
        token = body["data"]["accessToken"]
        check("POST /auth/login（改密后）", status == 200 and body.get("code") == "OK",
              f"status={status}")

    # 2) 我的档案
    status, body = call("GET", "/students/me", token=token)
    check("GET  /students/me", status == 200 and body.get("code") == "OK",
          f"status={status}")

    # 3) 画像刷新 + P1 断言：feedback 为空时 key 不得出现（NON_NULL）
    status, body = call("POST", "/students/me/profile/refresh", {}, token=token)
    data = body.get("data") if isinstance(body, dict) else None
    ok = (status == 200 and body.get("code") == "OK" and isinstance(data, dict)
          and isinstance(data.get("dimensions"), list) and "feedback" not in data)
    check("POST /students/me/profile/refresh（无feedback:null）", ok, f"status={status}")

    # 4) 画像版本列表
    status, body = call("GET", "/students/me/profile/versions", token=token)
    check("GET  /students/me/profile/versions", status == 200 and body.get("code") == "OK",
          f"status={status}")

    # 5) 推荐批次（方向种子为空时结果可为空，只断言接口 OK）
    status, body = call("POST", "/students/me/recommendations/runs",
                        {"requestId": "smoke-001"}, token=token)
    check("POST /students/me/recommendations/runs", status == 200 and body.get("code") == "OK",
          f"status={status} code={body.get('code')}")

    # 6) 推荐历史（分页包裹）
    status, body = call("GET", "/students/me/recommendations?page=1&size=5", token=token)
    check("GET  /students/me/recommendations", status == 200 and body.get("code") == "OK",
          f"status={status}")

    # 7) 计划草案（不用 AI，走模板降级）
    status, body = call("POST", "/students/me/plans/draft",
                        {"useAi": False, "requestId": "smoke-002"}, token=token)
    check("POST /students/me/plans/draft", status == 200 and body.get("code") == "OK",
          f"status={status} code={body.get('code')}")

    # 8) 未认证应 401（安全基线）
    status, body = call("GET", "/students/me")
    check("GET  /students/me 无token->401", status == 401, f"status={status}")

    print("ALL PASS" if all(results) else "SOME FAILED")
    return 0 if all(results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
