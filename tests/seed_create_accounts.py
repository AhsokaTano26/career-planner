# -*- coding: utf-8 -*-
"""调试用账号创建(纯标准库,无需第三方依赖)。

流程:
  1) 先执行 db/dev/seed-debug-users.sql(插入辅导员白名单 WL900)
  2) 本脚本调用 POST /api/v1/auth/register 注册 3 名学生 + 1 名辅导员
     - 学生用白名单已有校验码(即初始密码)
     - 辅导员用 WL900 的校验码 Adv@2026
  3) 再执行 db/dev/seed-debug.sql(将辅导员角色改为 ADVISOR 并写入业务种子)

注册接口会产生合法 BCrypt 密码,无需手动算哈希。重复执行安全(409 跳过)。
"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"
REG = BASE + "/api/v1/auth/register"

STUDENTS = [
    {"studentNo": "2026011301", "name": "李明", "className": "计科2601", "verifyCode": "202601"},
    {"studentNo": "2026011309", "name": "张同学", "className": "计科2601", "verifyCode": "202609"},
    {"studentNo": "2026011310", "name": "王芳", "className": "软工2601", "verifyCode": "202610"},
]
ADVISOR = {"studentNo": "A2026001", "name": "陈辅导员", "className": "辅导员办公室", "verifyCode": "Adv@2026"}


def post(payload):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(REG, data=data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8", "ignore"))


def register(label, payload):
    code, body = post(payload)
    if isinstance(body, dict):
        msg = body.get("message") or body.get("code") or ""
    else:
        msg = str(body)[:120]
    print(f"[{label}] {payload['studentNo']:>10} -> HTTP {code} {msg}")


def main():
    print("== 注册学生 ==")
    for s in STUDENTS:
        register("学生", s)
    print("== 注册辅导员 ==")
    register("辅导员", ADVISOR)
    print("\n账号清单(用户名 / 密码):")
    for s in STUDENTS:
        print(f"  学生  {s['studentNo']} / {s['verifyCode']}")
    print(f"  辅导员 A2026001 / {ADVISOR['verifyCode']}")
    print("  管理员 admin / Admin@2026 (已存在)")


if __name__ == "__main__":
    main()
