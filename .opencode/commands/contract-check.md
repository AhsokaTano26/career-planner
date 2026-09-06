---
description: Check interface changes against the contract docs and Apifox yaml
agent: plan
subtask: true
---

Read the skill definition at `.opencode/skills/api-contract/SKILL.md` and follow it.

Check in order against `@docs/接口设计.md`, `@docs/openapi/career-core-apis.yaml`, `@docs/本地实现vs线上核对报告.md`:

1. Prefix `/api/v1`, JWT (except login), envelope `{code,message,data,traceId,timestamp}` with `code=OK`.
2. No bare `null` in business fields: DTO/record must carry `@JsonInclude(JsonInclude.Include.NON_NULL)`, empty states as `""`/`[]`/defaults.
3. Any enum/status/field add/remove must update the yaml + reconciliation report, not just Java.

Output: pass/fail per item with file:line evidence. Do not edit code in this command.
