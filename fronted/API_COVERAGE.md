# 前端接口接入状态

本前端按远程分支的实际实现接入，而非按旧 OpenAPI 原型推断。

| 能力 | 后端分支与实际接口 | 前端状态 |
| --- | --- | --- |
| 登录、注册、登出 | `career-core(back-end)`：`/auth/login`、`/auth/register`、`/auth/logout` | 已接入 JWT |
| 当前学生档案 | `career-core(back-end)`：`/students/me`、`/students/me/completeness` | 已接入 |
| 经历管理 | `career-core(back-end)`：`/students/me/experiences` | 已接入查询、新增、删除 |
| 辅导员查询 | `main`：`/advisor/students`、`/advisor/attention`、`/advisor/statistics` | 接口已合并；前端读取接入待继续完善 |
| 学生画像、推荐、计划 | `AI_and_DATA` / `feature/student-profile-algorithm` | 不在已鉴权业务后端中，页面明确标记为待服务合并 |
| AI 咨询、计划、复盘、推荐解释 | `AI_and_DATA/career-ai` | 不从浏览器直连；接口要求 `X-Internal-Token`，应由 Spring Boot 网关调用 |
| 管理端 | `main`：`/admin/users`、`/admin/whitelist`、`/admin/relations`、`/admin/directions`、`/admin/abilities`、`/admin/templates`、`/admin/curricula/jobs`、`/admin/weights`、`/admin/exports`、`/admin/logs/operations` | 已接入读取列表；问卷和模型提示词尚无后端控制器 |

## 配置

开发环境需要把 `VITE_API_BASE_URL` 指向 Spring Boot 的 `/api/v1` 根路径，例如：

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

响应按后端统一格式处理：`{ code: "OK", message, data, traceId, timestamp }`。访问令牌仅存储在当前浏览器会话中。
