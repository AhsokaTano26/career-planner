"""五个板块接口全覆盖测试：学生画像 / 方向推荐 / 目标计划 / AI智能服务 / AI生涯咨询。

运行前提：career-core(8080) 与 career-ai(8000) 已启动。
输出：每个接口的 HTTP 状态 + 关键字段；404 表示未实现。
"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080/api/v1"
TIMEOUT = 120


def req(method, path, body=None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    r = urllib.request.Request(BASE + path, data=data, method=method,
                               headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(r, timeout=TIMEOUT) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8")
        try:
            return e.code, json.loads(raw) if raw else None
        except Exception:
            return e.code, raw


results = []


def check(name, status, body, expect=None, key=None):
    ok = True
    detail = ""
    if expect is not None and status != expect:
        ok = False
        detail = f"期望{expect}"
    if key is not None:
        has = isinstance(body, dict) and key in body
        if not has:
            ok = False
        detail += f" 字段[{key}]={'有' if has else '无'}"
    tag = "PASS" if ok else "FAIL"
    results.append((tag, name, status, body, detail))
    print(f"[{tag}] {name}  status={status} {detail}")
    if tag == "FAIL":
        print(f"        body={json.dumps(body, ensure_ascii=False)[:300]}")


print("=" * 70)
print("一、学生画像（5 个）")
print("=" * 70)
s, b = req("GET", "/students/me/profile/latest?studentId=1001")
check("GET /students/me/profile/latest?studentId=1001", s, b, 200, "dimensions")
s, b = req("GET", "/students/me/profile/versions?studentId=1001")
check("GET /students/me/profile/versions?studentId=1001", s, b, 200)
s, b = req("GET", "/profile-snapshots/1?studentId=1001")
check("GET /profile-snapshots/{snapshotId}", s, b, 200)
s, b = req("POST", "/students/me/profile/refresh?studentId=1001")
check("POST /students/me/profile/refresh", s, b, 200)
s, b = req("POST", "/profile-snapshots/1/feedback?studentId=1001", {"feedbackType": "MATCH", "comment": "测试"})
check("POST /profile-snapshots/{snapshotId}/feedback", s, b, 200)

print("=" * 70)
print("二、方向推荐（5 个）")
print("=" * 70)
s, run = req("POST", "/students/me/recommendations/runs?studentId=1001")
check("POST /students/me/recommendations/runs", s, run, 200, "runId")
run_id = run.get("runId") if isinstance(run, dict) else None
s, b = req("GET", f"/recommendation-runs/{run_id}?studentId=1001")
check("GET /recommendation-runs/{runId}", s, b, 200, "status")
result_id = None
if isinstance(b, dict) and isinstance(b.get("results"), list) and b["results"]:
    result_id = b["results"][0].get("directionId") or b["results"][0].get("id")
s, b = req("GET", "/students/me/recommendations/latest?studentId=1001")
check("GET /students/me/recommendations/latest", s, b, 200, "results")
s, b = req("GET", "/students/me/recommendations?studentId=1001")
check("GET /students/me/recommendations（历史）", s, b, 200)
# 反馈接口的 resultId 在契约中是字符串方向ID，这里尝试；若 4xx 再提示
s, b = req("POST", f"/recommendation-results/{result_id or 1}/feedback?studentId=1001",
           {"feedbackType": "HELPFUL", "comment": "测试"})
check("POST /recommendation-results/{resultId}/feedback", s, b)

print("=" * 70)
print("三、目标计划（14 个中已实现 1 个，其余探测）")
print("=" * 70)
s, b = req("POST", "/students/me/plans/draft?studentId=1001")
check("POST /students/me/plans/draft", s, b, 200, "goalSummary")
probe = [
    ("GET", "/students/me/plans/latest"), ("GET", "/students/me/goals"),
    ("POST", "/students/me/goals", {"goal": "x"}), ("GET", "/students/me/reminders"),
    ("GET", "/tasks"), ("POST", "/tasks", {"title": "x"}), ("GET", "/goal-versions"),
    ("GET", "/plan-versions"), ("PATCH", "/plans/1", {}), ("POST", "/plans/1/confirm"),
    ("PATCH", "/tasks/1", {}), ("DELETE", "/tasks/1"), ("POST", "/tasks/1/checkin"),
]
for m, p, *body in probe:
    s, b = req(m, p, body[0] if body else None)
    check(f"{m} {p}", s, b)

print("=" * 70)
print("四、AI 智能服务（5 个，career-core 侧）")
print("=" * 70)
ai = [
    ("POST", "/ai/chat", {"message": "你好"}), ("POST", "/ai/pdf/parse", {"content": "x"}),
    ("POST", "/ai/plan/generate", {"goal": "x"}),
    ("POST", "/ai/recommendation/explain", {"directionId": "1"}),
    ("POST", "/ai/review/summarize", {"content": "x"}),
]
for m, p, body in ai:
    s, b = req(m, p, body)
    check(f"{m} {p}", s, b)

print("=" * 70)
print("五、AI 生涯咨询（2 个）")
print("=" * 70)
s, b = req("GET", "/ai/chat/history")
check("GET /ai/chat/history", s, b)
s, b = req("POST", "/ai/chat/1/feedback", {"feedbackType": "good"})
check("POST /ai/chat/{messageId}/feedback", s, b)

print("=" * 70)
ok = all(r[0] == "PASS" for r in results)
print(f"总计：{sum(1 for r in results if r[0]=='PASS')} PASS / {sum(1 for r in results if r[0]=='FAIL')} FAIL")
print("判定：ALL PASS" if ok else "SOME FAILED（含未实现 404）")
