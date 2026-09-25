# Memory — 生涯规划系统（career-planner）

> 本文件为项目级记忆，始终加载。聚焦 AI 服务（career-ai）部分，并给出全局上下文。
> AI 服务的深度规范在 `career-ai/AGENTS.md`（编辑该目录代码时自动加载）。

## 项目概述
生涯规划系统：**三层微服务架构**。
- **career-core**（Java / Spring Boot 3.3.4 / MyBatis，宿主 `:8080`）— 主后端：认证 / 学生画像 / 测评 / 职业方向 / 推荐 / 计划 / 辅导员 / 管理后台 + AI 集成（`modules/ai/`）。
- **career-ai**（Python 3.12 / FastAPI / LiteLLM，Docker 内网 `:8000`）— **统一 LLM 网关** + AI 能力服务：多渠道 / 重试 / 降级 / 限流 / 调用日志。
- **fronted**（Vue 3 + Vite + TS，`:5173`，`/api` 代理到 8080）— 前端。⚠️ 目录拼写为 `fronted`（无 e），非 `frontend`。

完整目录结构见 @docs/guides/项目结构.md

## 服务间通信
- 前端 → career-core（`:8080`，JWT 认证）。
- career-core → career-ai：经 `LlmGateway`（Spring `RestClient`）调 `POST /v1/chat/completions`，`Authorization: Bearer GATEWAY_API_KEY`，带 `X-Request-Id`。
- career-ai → DeepSeek（OpenAI 兼容 API，`https://api.deepseek.com`，模型 `deepseek-v4-flash`），经 LiteLLM `Router` 路由。
- **AI 调用统一走 career-ai 网关，career-core 不直连 LLM**；AI 失败时 career-core 回退规则模板。
- 两侧各自写 `ai_call_log`（fail-open）。

## 开发环境
- 工作目录：`D:\Zht20241287\career-planner`（Windows）。
- 工具链：JDK 25（必须 25，否则 `class file version 69.0`）、Maven 3.9.16、MySQL 8.4、Python 3.12、Node ≥ 20。
- 本地启动：`start-all.bat`（各服务独立窗口）或 `docker compose up`。
- Docker Compose：`mysql` + `career-ai`（内网，健康检查 `/health`）+ `app`（宿主 `:8080`）。
- CI/CD：`.github/workflows/docker-publish.yml`（push main 自动构建并推送 Docker Hub；career-ai 镜像单独构建）。
- 接口文档 / 契约测试：**Apifox**（`docs/openapi/career-core-apis-live.yaml`，以 Apifox 线上为准）。

## 关键约定
- API 统一响应：`{code, message, data, traceId, timestamp}`，对齐 Apifox `ErrorResponse`。
- 改接口前先对照 `docs/openapi/career-core-apis-live.yaml`（以 Apifox 线上为准）。
- 数据库 schema 以 `career-core/src/main/resources/db/schema.sql` 为准（启动幂等执行）。
- career-core 新增模块遵循分层：`controller + dto + entity + mapper + service(impl) + vo`。
- Git：主分支 `main`，提交信息中文；`career-ai/.env`、`application-local.yml` 等已 gitignore。
- 中文优先：文档、记忆、提交信息用中文；代码标识符用英文。

## 操作者工作范围

当前操作者属于 **AI 与数据组**。Command Code 聚焦以下职责区域，不修改其他组（平台与业务后端组、前端与部署组）的代码，除非操作者特别指示。

### AI 组职责
推荐评分、学生画像、AI 网关、脱敏、生成任务、PDF 解析。

### In-scope（AI 组可直接修改）

| 范围 | 路径 | 说明 |
|------|------|------|
| FastAPI 服务 | `career-ai/` | 整个目录（gateway/、services/、api/、prompts/、providers/、tests/） |
| AI 集成模块 | `career-core/.../modules/ai/` | 整个模块（LlmGateway、AiService、Desensitizer、AiCallLogWriter、AiController、GatewayController 及 dto/entity/mapper/vo） |
| 推荐评分逻辑 | `modules/recommendation/service/RecommendationService.java` | 仅评分方法：`score()`、`DEFAULT_WEIGHTS`、`reasons()`、`strengths()`、`gaps()`、`semesterActions()`、`parseProfile()`、`parseTarget()` 及 `createRun()` 评分流程 |
| 学生画像生成 | `modules/portrait/service/PortraitService.java` | 仅生成方法：`refresh()`、`estimateFromProfile()`、`buildSummary()`、`topDims()`、`parseDimensions()` |
| AI 域配置语义 | `modules/admin/` 中 `AdminModelConfig`/`AdminPrompt`/`AdminWeight`/`AiCallLog` | AI 组 owns 语义和逻辑（默认值、权重定义、提示词版本）；纯 CRUD 脚手架不动 |

### Out-of-scope（不修改，除非特别指示）

- **后端组**：`modules/assessment/`、`modules/student/`、`modules/advisor/`、`modules/auth/`、`modules/planning/`、`modules/direction/`、`common/`、`config/`、`security/`；以及 recommendation/portrait/admin 模块中的 CRUD 和持久化部分。
- **前端组**：`fronted/`（整个前端）、`Dockerfile`、`docker-compose.yml`、`deploy/`。

## 实现进度
见 @IMPLEMENTATION_PROGRESS.md

## 团队记忆分层
- 项目级（本文件）：全局架构 / 环境 / 跨服务约定。
- AI 服务级（`career-ai/AGENTS.md`）：网关架构 / 隐私红线 / Prompt 管理 / AI 最佳实践约定。
- AI 详解（`career-ai/docs/ai-dev-conventions.md`）：流式 / RAG / 缓存 / 评估等实现方案（按需加载）。
