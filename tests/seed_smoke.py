# -*- coding: utf-8 -*-
"""调试种子冒烟:登录各角色并抽查列表接口是否返回数据。"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8080"


def login(username, password):
    req = urllib.request.Request(
        BASE + "/api/v1/auth/login",
        data=json.dumps({"account": username, "password": password}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            body = json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return None, e.code
    data = body.get("data") or {}
    token = next((v for k, v in data.items() if "token" in k.lower() and isinstance(v, str)), None)
    return token, 200


def get(path, token):
    req = urllib.request.Request(BASE + path, headers={"Authorization": "Bearer " + token})
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            body = json.loads(r.read().decode("utf-8"))
            return r.status, body
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8", "ignore"))
        except Exception:
            return e.code, {}


def check(label, username, password, paths):
    token, code = login(username, password)
    print(f"\n### {label} ({username}) login={code}")
    if not token:
        print("  !! 登录失败,跳过")
        return
    for p in paths:
        st, body = get(p, token)
        data = body.get("data")
        if isinstance(data, list):
            n = len(data)
        elif isinstance(data, dict):
            n = "obj"
        else:
            n = data
        flag = "OK" if st == 200 and data not in (None, [], {}) else ("EMPTY" if st == 200 else "ERR")
        print(f"  [{flag}] {st} {p}  -> data={n}")


check("学生", "2026011301", "202601", [
    "/api/v1/students/me/experiences",
    "/api/v1/students/me/recommendations/latest",
    "/api/v1/students/me/goals",
    "/api/v1/students/me/plans/latest",
    "/api/v1/students/me/tasks",
    "/api/v1/students/me/reminders",
    "/api/v1/students/me/favorites",
    "/api/v1/students/me/profile/latest",
    "/api/v1/assessment-sessions",
])

check("辅导员", "A2026001", "Adv@2026", [
    "/api/v1/advisor/students",
    "/api/v1/advisor/statistics",
])

check("管理员", "admin", "Admin@2026", [
    "/api/v1/admin/abilities",
    "/api/v1/admin/templates",
    "/api/v1/admin/weights",
    "/api/v1/admin/model-configs",
    "/api/v1/admin/prompts/scenes",
])
