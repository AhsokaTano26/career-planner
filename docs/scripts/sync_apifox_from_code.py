# -*- coding: utf-8 -*-
"""
根据 career-core 当前代码（springdoc 生成的 /v3/api-docs）同步 Apifox 接口。

前置条件：
  1. career-core 已引入 springdoc-openapi（见 pom.xml），且 SecurityConfig 已放行 /v3/api-docs/**；
  2. 后端已启动（默认 http://localhost:8080）；
  3. 已安装 apifox-cli 并登录。

做了什么：
  - 抓取 /v3/api-docs（代码即真相）
  - 仅保留范围内模块路径（学生画像 / 方向推荐 / 目标计划 / 阶段复盘 / AI 智能服务 / AI 生涯咨询）
  - 归一化每个接口的 tag 为单一正确模块名（springdoc 会把类级与方法级 @Tag 合并成双 tag，这里纠正为单 tag）
  - apifox import 覆盖更新对应模块

注意：
  - AI-Gateway 接口来自 career-ai（FastAPI），需另外抓取其 /openapi.json 并 import；本脚本只处理 career-core。
  - 导入后若历史旧路径仍残留（如重构前的 /api/v1/plans/{planId}），需在 Apifox 手动删除，保证「代码 == Apifox」。
"""
import json
import subprocess
import sys

PROJECT_ID = "8662286"
OAS_URL = "http://localhost:8080/v3/api-docs"
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"

# 范围内路径前缀
PREFIXES = [
    "/api/v1/students/me/profile",
    "/api/v1/profile-snapshots",
    "/api/v1/students/me/recommendations",
    "/api/v1/recommendation-runs",
    "/api/v1/recommendation-results",
    "/api/v1/students/me/goals",
    "/api/v1/students/me/plans",
    "/api/v1/students/me/tasks",
    "/api/v1/reviews",
    "/api/v1/students/me/reminders",
    "/api/v1/ai/",
]
MODULES = ["学生画像", "方向推荐", "目标计划", "阶段复盘", "AI 智能服务", "AI 生涯咨询"]


def module_for(path: str):
    if path.startswith("/api/v1/reviews"):
        return "阶段复盘"
    if "/ai/chat" in path:
        return "AI 生涯咨询"
    if path.startswith("/api/v1/ai/"):
        return "AI 智能服务"
    if path.startswith("/api/v1/students/me/profile") or path.startswith("/api/v1/profile-snapshots"):
        return "学生画像"
    if "/recommendation" in path:
        return "方向推荐"
    if (path.startswith("/api/v1/students/me/goals")
            or path.startswith("/api/v1/students/me/plans")
            or path.startswith("/api/v1/students/me/tasks")
            or path.startswith("/api/v1/students/me/reminders")):
        return "目标计划"
    return None


def fetch_oas():
    import urllib.request
    req = urllib.request.Request(OAS_URL, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read().decode("utf-8"))


def build_scope(oas):
    paths = {k: v for k, v in oas.get("paths", {}).items() if any(k.startswith(p) for p in PREFIXES)}
    for p, ops in paths.items():
        for mt, op in ops.items():
            if mt.lower() in ("get", "post", "put", "patch", "delete"):
                op["tags"] = [module_for(p)] if module_for(p) else op.get("tags", [])
    oas["paths"] = paths
    oas["tags"] = [{"name": x} for x in MODULES]
    return oas


def main():
    oas = fetch_oas()
    scope = build_scope(oas)
    out = r"D:\Zht20241287\career-planner\docs\openapi\career-core-scope.json"
    json.dump(scope, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"已生成范围内 spec：{out}（{len(scope['paths'])} 个路径）")
    args = [APIFOX_CMD, "import", "--project", PROJECT_ID, "--format", "openapi", "--file", out]
    print("执行：", " ".join(args))
    proc = subprocess.run(args, capture_output=True)
    print(proc.stdout.decode("utf-8-sig"))
    print("导入完成。请到 Apifox 核对并删除重构前的残留旧路径。", file=sys.stderr)


if __name__ == "__main__":
    main()
