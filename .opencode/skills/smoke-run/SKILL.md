---
name: smoke-run
description: Use when verifying backend changes, AI gateway integration, or before reporting interface results; runs the standard smoke suites.
---

# Smoke Run Skill

只用仓库内现成脚本复跑，不得现写一次性脚本（结果不可比）。

## 1) 核心 JWT 契约：`python3 tests/smoke_api.py`

覆盖注册→首次改密→登录→画像/推荐/计划/401 基线共 10 项，`ALL PASS` 为通过标准。要求 core（8080）+ MySQL（3306）先行。

## 2) AI 网关：`python3 tests/ai_gateway_smoke.py`

直调 15 项（health/鉴权/限流/降级码/转人工/history/反馈）+ core 端到端 5 项回退。无真实 LLM key 时 `20 项 ALL PASS` 即通过（走 502/503 降级验证）；真 LLM 模式仅 `CAREER_AI_REAL_LLM=1 python3 tests/ai_gateway_smoke.py`，且 key 只放 gitignore 的 `career-ai/.env`（`LLM_API_KEY`），不得入库/进日志。

## 前置环境（Linux 容器）

- `source ~/devtools/env.sh`；core 启动环境必须带 `AI_GATEWAY_API_KEY`（与 career-ai `GATEWAY_API_KEY` 同值）；career-ai 启动必须在 `career-ai/` 目录下（`api` 包导入依赖当前目录）：`setsid ~/devtools/python/bin/python -m uvicorn api.main:app --host 127.0.0.1 --port 8000`，且构建与启动分两次调用（后台保活坑）。
- 报告口径：逐项报 HTTP 状态 + 结果；失败先看应用日志再回查代码。
