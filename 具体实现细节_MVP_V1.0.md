# 计算机与软件大类新生生涯规划系统：具体实现细节（MVP）

本文在《需求规格说明书》和《开发设计说明书》基础上，将 MVP 细化为可直接拆分开发任务的实现约定。范围保持为“注册—测评—画像—推荐—目标计划—任务—复盘—指导”闭环；方向排序由规则引擎决定，AI 只生成受约束的解释、草案和总结。

## 一、总体交付边界

- 单体业务服务：`career-core`（Spring Boot）是唯一的鉴权、规则、事务和正式数据写入入口。
- 智能服务：`career-ai`（FastAPI）不连接 MySQL、不持有学生身份信息、不写入正式业务表；只接收最小化结构化输入并返回校验后的 JSON。
- Web：一个 Vue 3 应用按角色加载学生、辅导员、管理员布局；Nginx 同域代理 `/api`，浏览器不直接访问 FastAPI。
- 所有配置对象（问卷、方向、权重、能力标签、任务模板、提示词）采用“草稿—发布—停用”版本状态；业务结果保存所使用版本，历史不得被覆盖。

```text
Browser → Nginx → Spring Boot → MySQL
                    │
                    └── internal HTTP (HMAC + request-id) → FastAPI → LLM Provider
```

## 二、后端（Spring Boot）

### 2.1 工程与横切实现

采用 Java 21、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、MySQL 8。包名按 `modules/<domain>` 组织，每个领域固定包含 `controller`、`application`、`domain`、`infrastructure`、`dto` 五层。

```text
career-core/
  common/                 # ApiResponse、异常、错误码、分页、TraceId
  security/               # JWT filter、CurrentUser、RBAC、数据范围校验
  integration/ai/         # WebClient、AI DTO、签名、超时与降级适配器
  modules/
    auth student assessment profile career recommendation
    planning review advisor curriculum admin audit
  resources/db/migration/ # V1__init.sql、V2__seed_config.sql ...
```

- 统一响应：`{ code, message, data, traceId, timestamp }`；`traceId` 从 `X-Request-Id` 获取或服务端生成，并写入 MDC、审计日志和 AI 调用记录。
- 统一异常：参数错误返回 `VALIDATION_ERROR`/400，资源不存在 404，状态不允许 409，数据范围不足 403；不得将数据库或模型错误原文返回客户端。
- 写接口接收 `Idempotency-Key`（UUID）。服务端以 `user_id + endpoint + key` 建唯一记录；同一成功请求返回第一次响应，处理中返回 409，过期记录可按 24 小时清理。
- 使用 `@Valid` 和 Bean Validation 做入参校验；跨对象、状态和权限校验放在 Application Service。数据库约束作为最终防线。
- 审计采用异步事件 `AuditEvent` 写入 `audit_log`；但“查看学生详情、导出、配置发布、AI 调用”在响应前必须成功入队。审计失败记录告警，不中断普通读取。

### 2.2 身份、授权与数据范围

1. 注册只接受白名单中的 `student_no + verify_code`，`SELECT ... FOR UPDATE` 锁定白名单记录后校验未使用，再创建用户、授权记录和学生档案，并标记白名单已使用。
2. 密码使用 BCrypt（cost 12）；连续 5 次失败后锁定 15 分钟。登录成功后签发 30 分钟 access token 与 7 天 refresh token；refresh token 仅存哈希且可撤销。
3. JWT 内仅放 `userId`、角色和 `tokenVersion`，不放学号或个人资料。密码修改、禁用账号、角色变更时递增 `tokenVersion`。
4. 所有学生资源均通过 `student_profile.user_id = currentUser.id` 过滤。辅导员查看学生资源前必须调用 `AdvisorScopeService.assertAssigned(advisorId, studentId)`；管理员的高风险操作另要求 `ADMIN` 权限。
5. 访问学生详情与导出前记录对象类型、对象 ID、查询范围和结果数量；导出文件生成临时地址，10 分钟失效。

### 2.3 核心数据表与约束

主键统一使用 `BIGINT` 雪花 ID；时间以 UTC 写库、API 使用 ISO 8601；所有业务表至少有 `created_at`、`updated_at`、`created_by`、`updated_by`，逻辑删除表有 `deleted`。

| 领域 | 关键表与实现约束 |
| --- | --- |
| 账号 | `sys_user(student_no unique, password_hash, status, token_version)`；`sys_role`、`sys_user_role`；`student_whitelist(student_no unique, verify_code_hash, used_at)`；`consent_record(user_id, consent_version)` |
| 测评与画像 | `assessment_session(student_id, questionnaire_version_id, status, request_id unique)`；`assessment_answer(session_id, question_id unique)`；`assessment_score`；`profile_snapshot(student_id, source_session_id, dimension_json, completeness, version_no)` |
| 方向与规则 | `career_path`、`career_direction(code unique, status, version_no)`、`direction_dimension_weight(direction_id, dimension_code, target_score, weight)`、`direction_ability_requirement`；仅 `PUBLISHED` 版本可被读取和推荐 |
| 推荐 | `recommendation_run(student_id, profile_snapshot_id, rule_version, status)`、`recommendation_result(run_id, direction_id, score, rank, breakdown_json, explanation_json)`；`(run_id, direction_id)` 唯一 |
| 计划闭环 | `student_goal`、`goal_version`、`semester_plan`、`plan_version`、`plan_task`、`task_checkin`、`stage_review`；正式计划只引用已确认的目标版本 |
| 智能与导入 | `ai_task(request_id unique, type, status, input_hash, prompt_version, model_name)`、`ai_call_log`；`curriculum_import_task`、`curriculum_import_item`、`curriculum_version`、`course`、`course_ability_tag` |

关键索引：`assessment_session(student_id, status)`、`profile_snapshot(student_id, created_at desc)`、`recommendation_run(student_id, created_at desc)`、`plan_task(plan_id, status, due_date)`、`advisor_student_relation(advisor_id, student_id unique)`、`audit_log(operator_id, created_at)`。外键用于强关联的明细数据；审计日志与历史版本只保留逻辑引用，避免删除链式失败。

### 2.4 测评、画像与推荐

**测评状态机**：`DRAFT → IN_PROGRESS → SUBMITTED → SCORED`；已提交会话不可改答案。如需重测，新建会话并保留旧版本。自动保存仅允许前两种状态。

提交时在一个事务中校验必答题、批量写入答案、计算各维得分、置为 `SCORED` 并创建画像快照。六维画像 JSON 固定为兴趣、价值观、能力基础、学业基础、发展倾向、经历；每维保存 `rawScore`、`normalizedScore`、`evidenceCount`。完整度为已完成必填字段与必答题占比。

推荐计算在 `RecommendationService.run(studentId, profileSnapshotId)` 中执行：

1. 过滤：只选已发布、适用专业匹配、路径符合学生选择且满足必要条件的方向。
2. 计分：对存在数据的维度计算 `match = clamp(1 - abs(student - target), 0, 1)`；类别题按命中 1、相邻 0.5、其他 0；能力差距写入 `gap`，不直接淘汰。缺失维度从分母剔除后重新归一化。
3. 排序：`score = Σ(weight × match) / Σ(availableWeight) - penalty`，四舍五入为 0–100；按分数、方向编码稳定排序，取前 3–5 项。
4. 可信度：完整度低于 70% 为 `LOW`；第一二名差小于 5 分或关键维度冲突为 `MEDIUM`；其余 `HIGH`。该字段只展示为“探索参考程度”，不称“成功率”。
5. 保存 `breakdown_json`（权重、匹配值、差距、过滤原因）和所有输入版本；结果生成后异步创建 AI 解释任务。AI 失败时由后端把结构化得分套入规则模板。

### 2.5 目标、计划、任务与复盘

- 学生只能同时拥有一个 `ACTIVE` 主目标和一个 `ACTIVE` 备选目标。修改目标创建 `goal_version`，必须填写变更原因，旧版置为 `SUPERSEDED`。
- 生成计划时先创建 `ai_task`，从目标、已发布任务模板和已审核课程能力标签拼装输入。返回内容仅作为 `DRAFT` 计划版本；学生确认后事务性地将该版本置 `CONFIRMED`，替换当前正式计划。
- 任务状态允许：`NOT_STARTED → IN_PROGRESS → COMPLETED`；任何非完成状态可转 `POSTPONED` 或 `ABANDONED`，后二者必须填写原因。每次变更写 `task_checkin`，不覆盖历史。
- 复盘支持草稿；提交后生成 AI 总结与下一阶段草案。学生确认“采纳”时，服务端根据草案创建新的计划版本，不让 AI 直接修改任务表。

### 2.6 API 清单与约定

所有接口前缀为 `/api/v1`，分页使用 `page`（从 1 开始）、`size`（最大 100）、`sort`。涉及异步生成或导入的接口返回 `202` 和任务 ID，可轮询任务状态。

| 领域 | 主要端点 |
| --- | --- |
| 认证 | `POST /auth/register`、`/auth/login`、`/auth/refresh`、`/auth/logout`、`PATCH /auth/password` |
| 学生/测评 | `GET/PATCH /students/me`、`GET /questionnaires/active`、`POST /assessments`、`PATCH /assessments/{id}/answers`、`POST /assessments/{id}/submit`、`GET /profiles/latest` |
| 方向/推荐 | `GET /paths`、`GET /directions`、`GET /directions/{id}`、`POST /recommendations/runs`、`GET /recommendations/runs/{id}`、`POST /recommendations/results/{id}/feedback` |
| 计划闭环 | `GET/POST /goals`、`POST /plans/generate`、`GET/PATCH /plans/{id}`、`POST /plans/{id}/confirm`、`PATCH /tasks/{id}/status`、`POST /reviews`、`POST /reviews/{id}/generate`、`POST /reviews/{id}/adopt` |
| 辅导员 | `GET /advisor/students`、`GET /advisor/students/{id}`、`POST /advisor/students/{id}/comments`、`GET /advisor/statistics` |
| 管理 | `GET/POST/PATCH /admin/questionnaires`、`/directions`、`/abilities`、`/templates`、`/users`；`POST /admin/curricula/imports`、`GET/PATCH /admin/curricula/imports/{id}/items`、`POST /admin/curricula/{id}/publish` |
| 通用 | `GET /ai-tasks/{id}`、`GET /audit-logs`、`POST /exports` |

## 三、前端（Vue 3）

### 3.1 技术组织

使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、ECharts、Axios。开启 `strict: true`；接口类型由后端 OpenAPI 生成或维护于 `src/types/api.ts`，页面禁止 `any`。

```text
src/
  api/            # 按领域封装，统一返回 Promise<ApiResponse<T>>
  components/     # AssessmentStepper、ProfileRadar、DirectionCompare 等
  composables/    # useAutoSave、useAsyncTask、usePermission
  layouts/        # StudentLayout、AdvisorLayout、AdminLayout
  router/         # 路由元数据与前置守卫
  stores/         # auth、config、notification
  views/student | advisor | admin
  utils/          # request、validation、format、storage
```

### 3.2 认证、路由与请求

- `authStore` 只保存用户摘要、角色、access token 与过期时间；refresh token 使用 HttpOnly、Secure、SameSite=Lax Cookie，不进入 Pinia 或 localStorage。
- Axios 请求拦截器带 `Authorization`、`X-Request-Id` 和写操作的 `Idempotency-Key`。401 时只允许一个刷新请求，其余请求排队；刷新失败清空状态并跳转 `/login?redirect=`。
- 路由 `meta.roles` 控制体验层跳转：学生 `/student/*`，辅导员 `/advisor/*`，管理员 `/admin/*`；后端仍为最终权限判断。403、404、断网、加载和空数据都有独立状态组件。
- 不在前端判断学生是否可访问他人数据，也不在浏览器计算正式推荐分数。

### 3.3 学生端关键页面

1. **首次流程首页**：根据档案、测评、推荐、目标、计划、复盘状态生成步骤卡；每步只给一个主要操作入口，进度来自后端。
2. **学生信息与测评**：`AssessmentStepper` 按题组分页；切页或失焦后 800ms 防抖保存，显示“保存中/已保存/保存失败”。提交前展示未答题定位，提交后只读。
3. **画像与推荐**：雷达图采用 0–100 统一刻度；方向卡展示排序、依据、优势、待补足能力、可信度及“智能生成，供探索参考”。比较页最多选择两个方向，字段固定为适配点、差距、相关课程、推荐任务。
4. **目标、计划与任务**：目标编辑器要求主/备选唯一；计划草案可逐项编辑、删除或新增，确认前必须至少保留一个目标和一个任务。任务看板按状态分列，移动端改为筛选列表，状态更新使用乐观更新并在失败时回滚。
5. **复盘与咨询**：复盘分步保存草稿；AI 输出以卡片显示，并只有显式“采纳并生成新计划”按钮。咨询会话默认仅保留当前会话；高风险提示使用固定前端安全文案，不能被模型内容覆盖。

### 3.4 辅导员、管理端与复用组件

- 辅导员列表由服务端筛选、排序和分页；支持路径、方向、未设目标、超期未复盘、申请指导等组合筛选。学生详情为只读时间线，提交指导意见后刷新，不直接编辑学生数据。
- 管理端配置页统一使用“草稿编辑 → 校验 → 二次确认发布”。已发布版本不可原地修改，只能复制为新草稿。
- PDF 导入页支持上传、任务轮询、逐行审核、来源页码和原文片段展开；仅全部必填审核通过后允许发布。
- `AuditTable`、`AsyncTaskPanel`、`EmptyState`、`ErrorState`、`ConfirmPublishDialog` 应作为公共组件，统一处理分页、轮询、权限提示与二次确认。

### 3.5 前端质量门槛

- 表单规则与后端同名字段保持一致，但前端校验仅改善体验；提交错误映射到具体字段。
- 首屏按角色路由懒加载；ECharts 组件按需导入；列表查询取消过期请求。
- 以 360px 宽度验证学生端，辅导员/管理端以 1280px 宽度验证；不允许横向页面滚动或仅鼠标可完成的关键操作。
- Vitest 覆盖自动保存、路由权限、任务状态回滚和表单校验；Playwright 覆盖注册至复盘的主流程与辅导员越权提示。

## 四、AI（FastAPI）

### 4.1 服务边界与安全通信

FastAPI 使用 Python 3.11、FastAPI、Pydantic v2、httpx、PyMuPDF/pdfplumber。仅暴露在 Docker 内部网络，接受 Spring Boot 的 `X-Request-Id`、时间戳和 HMAC 签名；签名过期 5 分钟或 nonce 重放即拒绝。服务禁止使用 MySQL 凭据。

```text
career-ai/
  api/             # recommendation.py、plan.py、review.py、chat.py、pdf.py
  schemas/         # 输入/输出 Pydantic 模型
  services/        # desensitizer、llm_gateway、各业务编排器、pdf_parser
  providers/       # OpenAI-compatible 等供应商适配器
  prompts/         # <capability>/<version>.yaml
  validators/      # 输出、风险、注入与长度校验
  tests/
```

### 4.2 统一调用管线

1. 校验 HMAC、请求大小（JSON 最大 64KB；PDF 最大 20MB）和允许字段。
2. 对自由文本截断到业务上限（复盘 2,000 字、咨询 1,500 字），用正则遮蔽手机号、邮箱、学号、身份证号；命中后替换为占位符并记录掩码类型，不保留原文日志。
3. 仅将 `student_ref`、标准化维度、方向 ID、已审核课程/任务、脱敏文本传入提示词。姓名、学号、联系方式、地址、完整辅导员意见一律拒绝。
4. 加载发布状态提示词与 JSON Schema，调用供应商；连接/读取超时分别为 5 秒/20 秒，最多重试一次且仅重试可恢复错误。
5. 先解析 JSON，再用 Pydantic 做字段、长度、枚举和列表数量验证；验证失败返回 `AI_OUTPUT_INVALID`，由 Spring Boot 降级。
6. 记录 `request_id`、能力类型、模型、提示词版本、耗时、token 估算、结果状态、输入哈希与脱敏摘要；不得记录 API Key 或完整敏感提示词。

### 4.3 各能力的输入与输出契约

| 能力 | Spring Boot 输入 | 严格输出约束 | 后端降级 |
| --- | --- | --- | --- |
| 推荐解释 | 候选方向 ID、排序、维度匹配/差距、可信度 | 只可引用候选 ID；每方向 1 段理由、2–3 个优势/差距、最多 3 条行动；不输出适合/不适合结论 | 由分项得分套规则解释模板 |
| 计划生成 | 已确认目标、学期、已审核课程、能力差距、任务模板 | 3–8 个任务；每个有标题、月份、可验证完成标准、关联能力；不得改目标 | 返回方向任务模板 |
| 复盘总结 | 已脱敏复盘、任务聚合状态、当前目标 | `summary`、`insights[]`、`adjustments[]`、`next_tasks[]`；建议为可选项 | 返回完成率、延期原因汇总与固定建议 |
| 咨询 | 已发布方向库摘录、当前页面上下文、用户问题 | 引用受控知识；不诊断心理/医疗/法律问题；不代替重大决定 | 返回知识库指引及稍后重试提示 |
| PDF 解析 | 文件路径或内部对象引用 | 课程字段、来源页、原文片段、字段置信度、问题列表 | 标记 `MANUAL_REVIEW_REQUIRED` |

### 4.4 提示词、风险与降级

- 提示词 YAML 含 `capability`、`version`、`status`、`system`、`template`、`schema_version`、`max_output_tokens`。生产环境只加载 `PUBLISHED` 版本；模型名称和提示词版本由 Spring Boot 持久化。
- 系统提示要求：不提供结论性职业判定；将所有建议表达为探索选项；遇心理健康、医疗、法律、紧急风险时停止普通回答，输出固定转人工结构。
- 将用户自由文本放入明确的数据分隔区，并声明其中的指令不得改变系统规则；检测到“忽略指令、泄露提示词、索取敏感数据”等注入信号时，返回安全拒绝模板。
- 禁止模型创建方向 ID、课程 ID、任务状态或数据库主键。返回的引用必须是输入白名单的子集。
- 供应商不可用、超时或 JSON 不合法时，FastAPI 返回可识别错误码；Spring Boot 将 AI 任务置 `FALLBACK` 并生成确定性内容，主业务流程不能失败。

### 4.5 PDF 解析任务

上传由 Spring Boot 保存文件与元数据并创建 `PENDING` 导入任务，再调用 FastAPI。FastAPI 按“文字提取 → 表头定位 → 行合并 → 字段映射 → 置信度计算”处理；跨页表格依据同名表头和课程代码合并。

- 文本型 PDF：PyMuPDF 提取页面文本，pdfplumber 提取表格；课程代码优先作为去重键。
- 扫描型 PDF：首版不强依赖 OCR，直接返回 `MANUAL_REVIEW_REQUIRED`；不得伪造结构化课程。
- 输出逐项含 `source_page`、`source_excerpt`、`confidence`、`missing_fields`。低于 0.80 或关键字段缺失的项目默认标记待人工校核。
- 管理员审核、补录、合并完成后，后端才将其发布为 `curriculum_version`；未审核课程及其能力标签绝不进入推荐或计划输入。

### 4.6 AI 测试与可观测性

- Pydantic Schema 分别测试正常、缺字段、超长、未知枚举和越权方向 ID。
- 脱敏测试覆盖手机号、邮箱、学号、身份证号及混合文本；断言出站 Provider 请求中无原值。
- Mock Provider 测试超时、429、非 JSON、提示注入、高风险问题和一次重试后的规则降级。
- `/health/live` 只验证进程，`/health/ready` 验证提示词与 Provider 配置；指标至少包括请求数、成功率、P95 时延、重试数、降级数、脱敏命中数和估算 token。

## 五、联调顺序与验收

1. 先完成 Flyway 基线、认证/白名单、学生档案、问卷与确定性计分；以 Mock AI 验证接口。
2. 完成方向配置、画像快照与不依赖 AI 的推荐结果；教师审核权重和方向矩阵。
3. 接入 AI 解释、计划与复盘，并验证超时、无效 JSON、敏感信息和降级路径。
4. 完成辅导员数据范围、管理发布流、PDF 人工审核流，最后接入 Nginx、HTTPS、备份恢复和审计检查。

发布前必须通过：学生无法读取他人数据；辅导员无法读取非负责学生；模型请求不含直接身份信息；AI 故障时主闭环可用；推荐能追溯到画像、规则和方向版本；已执行一次空库恢复演练。
