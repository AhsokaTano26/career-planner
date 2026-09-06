---
description: Run the standard backend + AI gateway smoke suites in an isolated subagent
agent: build
subtask: true
---

Read the skill definition at `.opencode/skills/smoke-run/SKILL.md` and follow it.

Run in order (repo root, Linux container):

1. `source ~/devtools/env.sh` then `python3 tests/smoke_api.py` (10 items, ALL PASS to pass; requires core :8080 + MySQL :3306).
2. `python3 tests/ai_gateway_smoke.py` (20 items ALL PASS to pass in downgrade mode without a real LLM key).

Report each item's HTTP status and result. On failure read the app log first, then the code. Do NOT write ad-hoc scripts; reuse the repo scripts. Real-LLM mode (`CAREER_AI_REAL_LLM=1`) only on explicit user request; keys live only in gitignored `career-ai/.env`.
