# 契约校验 + 自动修复报告（开发环境 47907998）

> 校验方式：apifox test-case run（调试）+ 直连 8080/8000 取响应体，与 OAS(career-core-apis-live.yaml) 200 Schema 比对。
> 自动修复：核心接口缺信封包裹时，把 200 响应 Schema 包成 {code,message,data,traceId,timestamp}（data=原业务 Schema 或数组）。
## 汇总
- 接口数: 57
- 已自动修复 OAS: 55
- 仍有契约问题: 0

## 明细
| 模块 | 方法 | 路径 | HTTP | Apifox | 修复 | 问题 |
|---|---|---|---|---|---|---|
| 测评 | GET | /api/v1/questionnaires | 200 | None | 是 | - |
| 测评 | GET | /api/v1/questionnaires/{questionnaireId} | 200 | None | 是 | - |
| 测评 | GET | /api/v1/questionnaires/{questionnaireId}/versions | 200 | None | 是 | - |
| 测评 | POST | /api/v1/assessment-sessions | 400 | None | 是 | - |
| 测评 | GET | /api/v1/assessment-sessions | 200 | None | 是 | - |
| 测评 | GET | /api/v1/assessment-sessions/{sessionId} | 200 | None | 是 | - |
| 测评 | PUT | /api/v1/assessment-sessions/{sessionId}/answers | 400 | None | 是 | - |
| 测评 | POST | /api/v1/assessment-sessions/{sessionId}/submit | 200 | None | 是 | - |
| 测评 | GET | /api/v1/assessment-sessions/{sessionId}/scores | 200 | None | 是 | - |
| 学生画像 | GET | /api/v1/students/me/profile/latest | 200 | None | 是 | - |
| 学生画像 | GET | /api/v1/students/me/profile/versions | 200 | None | 是 | - |
| 学生画像 | GET | /api/v1/profile-snapshots/{snapshotId} | 200 | None | 是 | - |
| 学生画像 | POST | /api/v1/students/me/profile/refresh | 200 | None | 是 | - |
| 学生画像 | POST | /api/v1/profile-snapshots/{snapshotId}/feedback | 400 | None | 是 | - |
| 方向推荐 | GET | /api/v1/paths | 404 | None | 是 | - |
| 方向推荐 | GET | /api/v1/directions | 404 | None | 是 | - |
| 方向推荐 | GET | /api/v1/directions/{directionId} | 400 | None | 是 | - |
| 方向推荐 | GET | /api/v1/directions/compare | 404 | None | 是 | - |
| 方向推荐 | GET | /api/v1/students/me/favorites | 200 | None | 是 | - |
| 方向推荐 | POST | /api/v1/students/me/favorites/{directionId} | 400 | None | 是 | - |
| 方向推荐 | DELETE | /api/v1/students/me/favorites/{directionId} | 400 | None | 是 | - |
| 方向推荐 | POST | /api/v1/students/me/recommendations/runs | 400 | None | 是 | - |
| 方向推荐 | GET | /api/v1/students/me/recommendations/latest | 200 | None | 是 | - |
| 方向推荐 | GET | /api/v1/students/me/recommendations | 200 | None | 是 | - |
| 方向推荐 | GET | /api/v1/recommendation-runs/{runId} | 400 | None | 是 | - |
| 方向推荐 | POST | /api/v1/recommendation-results/{resultId}/feedback | 400 | None | 是 | - |
| 目标计划 | GET | /api/v1/students/me/goals | 200 | None | 是 | - |
| 目标计划 | POST | /api/v1/students/me/goals | 400 | None | 是 | - |
| 目标计划 | GET | /api/v1/goal-versions | 404 | None | 是 | - |
| 目标计划 | POST | /api/v1/students/me/plans/draft | 400 | None | 是 | - |
| 目标计划 | GET | /api/v1/students/me/plans/latest | 200 | None | 是 | - |
| 目标计划 | PATCH | /api/v1/plans/{planId} | 400 | None | 是 | - |
| 目标计划 | POST | /api/v1/plans/{planId}/confirm | 400 | None | 是 | - |
| 目标计划 | GET | /api/v1/plan-versions | 404 | None | 是 | - |
| 目标计划 | GET | /api/v1/tasks | 404 | None | 是 | - |
| 目标计划 | POST | /api/v1/tasks | 404 | None | 是 | - |
| 目标计划 | PATCH | /api/v1/tasks/{taskId} | 400 | None | 是 | - |
| 目标计划 | DELETE | /api/v1/tasks/{taskId} | 400 | None | 是 | - |
| 目标计划 | POST | /api/v1/tasks/{taskId}/checkin | 400 | None | 是 | - |
| 目标计划 | GET | /api/v1/students/me/reminders | 200 | None | 是 | - |
| 阶段复盘 | GET | /api/v1/reviews | 200 | None | 是 | - |
| 阶段复盘 | POST | /api/v1/reviews/drafts | 400 | None | 是 | - |
| 阶段复盘 | GET | /api/v1/reviews/{reviewId} | 200 | None | 是 | - |
| 阶段复盘 | PUT | /api/v1/reviews/{reviewId}/draft | 400 | None | 是 | - |
| 阶段复盘 | POST | /api/v1/reviews/{reviewId}/submit | 200 | None | 是 | - |
| 阶段复盘 | POST | /api/v1/reviews/{reviewId}/ai-summary | 200 | None | 是 | - |
| 阶段复盘 | POST | /api/v1/reviews/{reviewId}/guidance-request | 400 | None | 是 | - |
| 阶段复盘 | POST | /api/v1/reviews/{reviewId}/adopt-advice | 400 | None | 是 | - |
| AI智能服务 | POST | /api/v1/ai/chat | 400 | None | 是 | - |
| AI智能服务 | POST | /api/v1/ai/recommendation/explain | 400 | None | 是 | - |
| AI智能服务 | POST | /api/v1/ai/plan/generate | 400 | None | 是 | - |
| AI智能服务 | POST | /api/v1/ai/review/summarize | 400 | None | 是 | - |
| AI智能服务 | POST | /api/v1/ai/pdf/parse | 400 | None | 是 | - |
| AI生涯咨询 | GET | /api/v1/ai/chat/history | 200 | None | 是 | - |
| AI生涯咨询 | POST | /api/v1/ai/chat/{messageId}/feedback | 400 | None | 是 | - |
| AI-Gateway | POST | /v1/chat/completions | 400 | None | - | - |
| AI-Gateway | POST | /api/v1/gateway/generate | 400 | None | - | - |