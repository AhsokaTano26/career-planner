"""跨服务接口冒烟测试（最基础用例）：验证 career-core 核心接口。

运行前提：career-core 已启动（http://127.0.0.1:8080）。
运行方式：python tests/smoke_api.py
"""
import json
import urllib.request

BASE = "http://127.0.0.1:8080/api/v1"

# 推荐接口已接入 career-ai 大模型生成解释（每条方向一次 LLM 调用），/latest 可能耗时数十秒，
# 因此请求超时放宽到 120s（原 5s 会因 LLM 延迟超时）。
TIMEOUT = 120


def get(path):
    with urllib.request.urlopen(BASE + path, timeout=TIMEOUT) as r:
        return r.status, json.loads(r.read().decode("utf-8"))


def post(path, body=None):
    data = json.dumps(body if body is not None else {}).encode("utf-8")
    req = urllib.request.Request(
        BASE + path, data=data, method="POST",
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
        return r.status, json.loads(r.read().decode("utf-8"))


def main():
    # 契约对齐（2026-08-15）：成功响应为裸业务对象（无 {code,message,data} 包装）
    checks = [
        ("GET  /students/me/profile/latest?studentId=1001 (有效)", *get("/students/me/profile/latest?studentId=1001"), lambda b: isinstance(b, dict) and "dimensions" in b),
        ("GET  /students/me/profile/latest?studentId=9999 (无效->404)", lambda: _status_get("/students/me/profile/latest?studentId=9999"), 404, lambda b: b.get("code") == 40400),
        ("POST /students/me/recommendations/runs", *post("/students/me/recommendations/runs?studentId=1001"), lambda b: isinstance(b, dict) and "runId" in b),
        ("GET  /students/me/recommendations/latest?studentId=1001", *get("/students/me/recommendations/latest?studentId=1001"), lambda b: isinstance(b, dict) and "results" in b),
        ("POST /students/me/plans/draft (调用推荐数据)", *post("/students/me/plans/draft?studentId=1001"), lambda b: isinstance(b, dict) and "goalSummary" in b),
    ]
    ok = True
    for item in checks:
        name = item[0]
        if callable(item[1]):
            status, body = item[1]()
            expect_status, check = item[2], item[3]
            passed = status == expect_status and check(body)
        else:
            status, body, check = item[1], item[2], item[3]
            passed = status == 200 and check(body)
        ok = ok and passed
        print(f"{'PASS' if passed else 'FAIL'}  {name}  status={status}")
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


def _status_get(path):
    import urllib.error
    try:
        return get(path)
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))


if __name__ == "__main__":
    raise SystemExit(main())
