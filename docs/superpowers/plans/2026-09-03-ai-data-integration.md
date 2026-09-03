# AI 与核心领域能力整合 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI、测评、画像、推荐和规划能力以 JWT 安全、可构建的方式整合到当前工作台。

**Architecture:** 以 `main` 的认证、响应、MyBatis 和 Vue 路由为基线，精选移植 `origin/AI_and_DATA` 的领域模块。浏览器经 Spring API 访问业务能力；Spring 再经内部网关调用 `career-ai`，领域表由追加的 schema 初始化。

**Tech Stack:** Java 17、Spring Boot、Spring Security、MyBatis、MySQL 8、Vue 3、TypeScript、Vite、Vitest、Docker Compose。

**Spec:** `docs/superpowers/specs/2026-09-03-ai-data-integration-design.md`

## Global Constraints

- 学生身份仅从 JWT 获取，不能信任请求中的学生 ID。
- `career-ai` 仅由 Spring 经内部令牌调用，模型密钥只从环境变量读取。
- 数据库初始化仅追加表与种子数据，不删除既有生产数据。
- 迁移不包含 `tests/apifox-reports`、日志、个人工具配置或浏览器产物。
- 每项行为变更先写失败测试，确认失败后才写生产代码。

---

### Task 1: 建立可重复的领域数据库初始化

**Files:**
- Modify: `career-core/src/main/resources/application.yml`
- Create: `career-core/src/main/resources/db/{ai-chat,core-domains,seed-directions,seed-questionnaires}.sql`
- Test: `career-core/src/test/java/com/rickgao/careercore/CareerCoreApplicationTests.java`

**Interfaces:** 产生 `ai_chat_message`、`assessment_session`、`profile_snapshot`、`recommendation_run`、`student_goal`、`semester_plan` 等运行时表。

- [ ] Step 1: 在 `CareerCoreApplicationTests` 新增 `JdbcTemplate` 断言，查询 `assessment_session`、`profile_snapshot`、`ai_chat_message`；运行 `cd career-core && ./mvnw -Dtest=CareerCoreApplicationTests test`，确认因表不存在失败。
- [ ] Step 2: 从 AI 分支复制上述四个 SQL 文件，在 `spring.sql.init.schema-locations` 末尾追加 `db/ai-chat.sql,db/core-domains.sql`，保留既有 SQL 顺序。
- [ ] Step 3: 重跑启动测试，确认三个表可查询；提交 `feat: initialize AI domain schema`。

### Task 2: 整合 AI 网关与安全对话 API

**Files:**
- Create: `career-core/src/main/java/com/rickgao/careercore/modules/ai/**`
- Create: `career-core/src/main/resources/mapper/ai/{AiChatFeedbackMapper,AiChatMessageMapper}.xml`
- Modify: `career-core/src/main/java/com/rickgao/careercore/config/SecurityConfig.java`
- Test: `career-core/src/test/java/com/rickgao/careercore/modules/ai/AiServiceTest.java`

**Interfaces:** 产生 `POST /api/v1/ai/chat`、`GET /api/v1/ai/chat/history`、聊天反馈、推荐解释、计划生成与复盘总结 API。

- [ ] Step 1: 写 `AiServiceTest`，以用户 A/B 验证 A 不能读取或反馈 B 的消息，缺少内部令牌返回受控错误；运行 `cd career-core && ./mvnw -Dtest=AiServiceTest test`，确认因 `AiService` 不存在失败。
- [ ] Step 2: 精选复制 AI 分支 `modules/ai` 和 mapper XML；删除所有从请求体读取当前用户 ID 的路径，保留 `SecurityUtils.currentUserId()`；网关路径仅供内部服务调用。
- [ ] Step 3: 重跑 `AiServiceTest`，确认跨用户访问被拒绝；提交 `feat: integrate secured AI services`。

### Task 3: 整合测评和画像闭环

**Files:**
- Create: `career-core/src/main/java/com/rickgao/careercore/modules/{assessment,portrait}/**`
- Create: `career-core/src/main/resources/mapper/{assessment,portrait}/**.xml`
- Test: `career-core/src/test/java/com/rickgao/careercore/modules/assessment/AssessmentServiceTest.java`
- Test: `career-core/src/test/java/com/rickgao/careercore/modules/portrait/PortraitServiceTest.java`

**Interfaces:** 产生测评会话/评分 API 和 `/students/me/profile/latest|refresh|versions` 画像 API。

- [ ] Step 1: 写 `AssessmentServiceTest` 覆盖创建、保存答案、提交和仅本人读取分数；写 `PortraitServiceTest` 覆盖刷新后只返回当前学生快照；运行 `cd career-core && ./mvnw -Dtest=AssessmentServiceTest,PortraitServiceTest test`，确认失败。
- [ ] Step 2: 移植 assessment、portrait 与 mapper；控制器一律使用 `SecurityUtils.currentUserId()`，详情和反馈查询必须绑定快照所属学生。
- [ ] Step 3: 重跑两组测试，确认通过；提交 `feat: add assessment and portrait workflows`。

### Task 4: 整合推荐、方向收藏和规划任务

**Files:**
- Create: `career-core/src/main/java/com/rickgao/careercore/modules/{recommendation,planning}/**`
- Modify: `career-core/src/main/java/com/rickgao/careercore/modules/direction/**`
- Create: `career-core/src/main/resources/mapper/{recommendation,planning,direction}/**.xml`
- Test: `career-core/src/test/java/com/rickgao/careercore/modules/planning/PlanningServiceTest.java`

**Interfaces:** 产生推荐运行/反馈、目标、计划、任务打卡、复盘、提醒和指导申请 API。

- [ ] Step 1: 在 `PlanningServiceTest` 创建用户 A/B 的任务，验证 A 无法更新 B 的任务；验证 AI 总结只读取当前用户复盘；运行 `cd career-core && ./mvnw -Dtest=PlanningServiceTest test`，确认失败。
- [ ] Step 2: 移植 recommendation、planning 和方向收藏，所有按 ID 的 Mapper 查询均绑定 `student_id` 与当前 JWT 用户。
- [ ] Step 3: 重跑测试，确认跨用户操作被拒绝；提交 `feat: add recommendations and planning workflows`。

### Task 5: 整合管理员 AI 配置与问卷维护

**Files:**
- Create: `career-core/src/main/java/com/rickgao/careercore/modules/admin/{controller,dto,entity,mapper,service,vo}/**` 中模型、提示词、问卷、AI 日志文件
- Create: `career-core/src/main/resources/mapper/admin/{AdminModelConfigMapper,AdminPromptMapper,AdminQuestionnaireMapper,AiCallLogMapper}.xml`
- Test: `career-core/src/test/java/com/rickgao/careercore/modules/admin/service/impl/AdminQuestionnaireServiceImplTest.java`

**Interfaces:** 产生管理员模型配置、提示词、问卷、AI 调用日志 API。

- [ ] Step 1: 写测试验证管理员新增问卷后能创建版本，非管理员请求被权限层拒绝；运行 `cd career-core && ./mvnw -Dtest=AdminQuestionnaireServiceImplTest test`，确认失败。
- [ ] Step 2: 移植相关 controller/service/mapper/DTO/VO，沿用当前项目管理员权限与操作审计约定。
- [ ] Step 3: 重跑测试，确认通过；提交 `feat: add AI administration and questionnaires`。

### Task 6: 将学生工作台接入真实 AI 业务

**Files:**
- Modify: `fronted/src/api/request.ts`, `fronted/src/router/index.ts`, `fronted/src/layouts/DefaultLayout.vue`
- Create: `fronted/src/composables/{useAssessment,usePortrait,usePlanning}.ts`
- Create: `fronted/src/views/student/{StudentAssessmentPage,StudentPortraitPage,StudentPlanningPage}.vue`
- Test: `fronted/src/composables/usePlanning.test.ts`

**Interfaces:** 消费 `/api/v1/assessment-sessions`、`/students/me/profile/*`、`/students/me/plans/*`，提供无 JSON 文本框的学生业务页面。

- [ ] Step 1: 写 `usePlanning.test.ts`，断言任务更新只发送 `{ status, note }` 平面 DTO，成功后重新加载列表；运行 `cd fronted && npm run test -- usePlanning.test.ts`，确认失败。
- [ ] Step 2: 实现专用 composable 和页面，复用 BaseModal、BaseSelect、Toast，禁止暴露内部令牌和 JSON 输入框。
- [ ] Step 3: 重跑前端单测，确认通过；提交 `feat: connect student AI workflows`。

### Task 7: 接入管理端 AI 工作台并完成全量验证

**Files:**
- Create: `fronted/src/views/admin/AiPlaygroundPage.vue`
- Modify: `fronted/src/views/admin/AdminModuleView.vue`, `fronted/src/layouts/AdminLayout.vue`, `fronted/src/router/index.ts`
- Test: `fronted/src/views/admin/AiPlaygroundPage.test.ts`
- Modify: `docker-compose.yml`（只在健康依赖或环境变量缺失时）

**Interfaces:** 提供仅管理员可访问的模型、提示词、问卷和 AI 调试入口。

- [ ] Step 1: 写页面测试，断言请求带当前登录令牌，学生访问管理 AI 路由会重定向；运行 `cd fronted && npm run test -- AiPlaygroundPage.test.ts`，确认失败。
- [ ] Step 2: 移植 AI 调试业务部分，使用当前中文文案与通用组件；以路由元数据限制管理员访问。
- [ ] Step 3: 运行 `cd career-core && ./mvnw test && cd ../fronted && npm run test && npm run build && cd .. && docker compose config --quiet`，所有命令必须 exit 0；提交 `feat: complete AI data integration`。
