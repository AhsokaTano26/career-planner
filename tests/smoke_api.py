"""跨服务接口冒烟测试（最基础用例）：验证 career-core 核心接口。

运行前提：career-core 已启动（http://127.0.0.1:8080）。
运行方式：python tests/smoke_api.py
"""
import json
import urllib.request

BASE = "http://127.0.0.1:8080/api/v1"


def get(path):
    with urllib.request.urlopen(BASE + path, timeout=5) as r:
        return r.status, json.loads(r.read().decode("utf-8"))


def post(path):
    req = urllib.request.Request(BASE + path, data=b"", method="POST")
    with urllib.request.urlopen(req, timeout=5) as r:
        return r.status, json.loads(r.read().decode("utf-8"))


def main():
    checks = [
        ("GET  /profiles/latest?studentId=1001 (有效)", *get("/profiles/latest?studentId=1001")),
        ("GET  /profiles/latest?studentId=9999 (无效->空对象)", *get("/profiles/latest?studentId=9999")),
        ("POST /recommendations/run", *post("/recommendations/run?studentId=1001")),
        ("GET  /recommendations/latest?studentId=1001", *get("/recommendations/latest?studentId=1001")),
        ("POST /planning/plans/generate (调用推荐数据)", *post("/planning/plans/generate?studentId=1001")),
    ]
    ok = True
    for name, status, body in checks:
        passed = status == 200 and body.get("code") == 0
        ok = ok and passed
        print(f"{'PASS' if passed else 'FAIL'}  {name}  status={status} code={body.get('code')}")
    print("ALL PASS" if ok else "SOME FAILED")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
