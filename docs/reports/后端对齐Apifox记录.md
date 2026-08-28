# 后端对齐 Apifox 线上定义 · 核对与改造记录

> 对照对象：Apifox 项目 8662286 主分支（122 接口 / 16 目录，2026-08-15 拉取）
> 数据源：`docs/openapi/career-core-apis-live.yaml`（最新导出）
> 核对对象：`career-core` 已实现的 3 个模块（画像 / 推荐 / 计划）
> 说明：后端当前仅实现 3 个模块，线上共 16 个目录；改造范围 = **已实现模块与线上定义的路径/结构对齐**，不新增未实现模块。

---

## 一、核对结论总览

- **推荐模块**：✅ 字段与线上一致，可放心对接（唯一小项：历史接口返回 List vs 线上单对象，需确认契约期望）。
- **画像模块**：✅ 字段一致，但需给 `ProfileSnapshotDto` 补 `@JsonInclude(NON_NULL)`，避免 `feedback: null` 触发契约报错（与推荐模块保持同一套修复）。
- **计划模块**：❌ 差异最大。`Plan/Goal/Task/Reminder` 四类响应结构、`GoalRequest` 请求体均与线上不一致，需按线上 Schema 重构（详见第四节）。

---

## 二、推荐模块（Recommendation）

### 2.1 字段核对（✅ 一致）

| 线上字段 | 本地 `RecommendationResultDto` | 一致 |
|---------|-------------------------------|------|
| directionId (string 方向编码) | `String directionId` | ✅ |
| rank (int) | `int rank` | ✅ |
| score (number 0-100) | `double score`（内部 0-1 → 百分制） | ✅ |
| confidence (HIGH/MEDIUM/LOW) | `String confidence` | ✅ |
| reasons (string[]) | `List<String> reasons` | ✅ |
| strengths (string[]) | `List<String> strengths` | ✅ |
| gaps (string[]) | `List<String> gaps` | ✅ |
| semesterActions (string[]) | `List<String> semesterActions` | ✅ |
| feedback (RecommendationFeedback) | `RecommendationFeedbackDto feedback`（无值省略） | ✅ |

### 2.2 接口路径 / 结构差异（3 接口 → 5 接口）

| 线上定义 | 当前后端 | 动作 |
|---------|---------|------|
| `POST /api/v1/students/me/recommendations/runs` | `POST /api/v1/recommendations/run` | 改路径 + 新增请求体 |
| `GET /api/v1/students/me/recommendations/latest` | `GET /api/v1/recommendations/latest` | 改路径 + 改响应 |
| `GET /api/v1/students/me/recommendations` | 无 | 新增（批次历史） |
| `GET /api/v1/recommendation-runs/{runId}` | 无 | 新增（批次详情） |
| `POST /api/v1/recommendation-results/{resultId}/feedback` | `POST /api/v1/recommendations/{id}/feedback` | 改路径 + 改枚举 |

**线上 `RecommendationRun`**：`runId(string) / profileVersion(int) / ruleVersion(string) / generatedAt(date-time) / status(RUNNING|SUCCESS|DEGRADED|FAILED) / results[]`

**线上 `RecommendationResult` 与本地差异**：

| 线上字段 | 当前后端字段 | 差异 |
|---------|------------|------|
| `directionId` string（方向编码） | `directionId` Long | 改 string 方向编码 |
| `rank` int | `rank` int | 一致 |
| `score` number 0-100 | `score` double 0-1 | 改百分制 |
| `confidence` string HIGH/MEDIUM/LOW | `confidence` double softmax 概率 | 改枚举 |
| `reasons` string[] | `reason` 单个 string | 拆数组 |
| `strengths` string[] | 无 | 新增 |
| `gaps` string[] | 无（仅落库） | 新增 |
| `semesterActions` string[] | 无 | 新增 |
| `feedback` object | 无 | 新增（可空） |

### 2.3 反馈枚举差异

- 线上：`HELPFUL / NEUTRAL / MISMATCH / NOT_INTERESTED`
- 当前后端：`LIKE / DISLIKE / INVALID`（仅落库，无校验）

> ⚠️ 小提醒：线上历史接口（`GET /recommendations`）响应 schema 引用单个 `RecommendationRun`，而本地返回数组。若契约测试严格比对需确认。

---

## 三、画像模块（Profile）

### 3.1 字段核对（✅ 一致，1 处风险）

| 线上接口 | 本地实现 | 响应结构 |
|---------|---------|---------|
| `GET /students/me/profile/latest` | ✅ `getLatestProfile` | `ProfileSnapshotDto` ✅ |
| `GET /students/me/profile/versions` | ✅ `getVersions` | `ProfileSnapshotDto`（线上 schema 为单个，返回最近一条）✅ |
| `POST /students/me/profile/refresh` | ✅ `refreshProfile` | `ProfileSnapshotDto` ✅ |
| `GET /profile-snapshots/{snapshotId}` | ✅ `getSnapshot` | `ProfileSnapshotDto` ✅ |
| `POST /profile-snapshots/{snapshotId}/feedback` | ✅ `addFeedback` | `ProfileFeedbackDto` ✅ |

| 线上字段 | 本地 `ProfileSnapshotDto` | 一致 |
|---------|---------------------------|------|
| id (string "PS-1002") | `String id`（`"PS-"+id`） | ✅ |
| version (int) | `int version`（Demo：复用快照 id） | ✅ |
| generatedAt (date-time) | `String generatedAt` | ✅ |
| sourceVersion (string) | `String sourceVersion` | ✅ |
| completeness (int) | `int completeness` | ✅ |
| dimensions[] (DimensionValue) | `List<DimensionValueDto>`（key/name/score，key 映射 tendency/practice） | ✅ |
| summary (string) | `String summary` | ✅ |
| strengths (string[]) | `List<String> strengths` | ✅ |
| explore (string[]) | `List<String> explore` | ✅ |
| feedback (ProfileFeedback) | `ProfileFeedbackDto feedback` | ⚠️ |

> ⚠️ **风险点**：本地 `ProfileSnapshotDto.feedback` 恒为 `null`，且该 record **未加 `@JsonInclude(NON_NULL)`**，会序列化为 `"feedback": null`——与推荐模块已修复的行为不一致。若 Apifox 契约测试不允许对象字段为 null，`/profile/latest` 会报与推荐模块之前相同的错。

### 3.2 路径 / 结构差异

- 路径：线上 `GET /api/v1/students/me/profile/latest` ｜ 当前 `GET /api/v1/profiles/latest`
- **线上 `ProfileSnapshot`**：`id(string) / version(int) / generatedAt / sourceVersion(string) / completeness(int) / dimensions[](DimensionValue{key,name,score}) / summary / strengths[] / explore[] / feedback`
- **当前后端**：`dimensions(object 键值) / summary / version(string) / completeness(number) / studentId / experiences[]`
- 关键差异：dimensions object→array、version string→int、completeness number→int、新增 id/generatedAt/strengths/explore/feedback、移除 studentId/experiences。

---

## 四、计划模块（Planning）— ❌ 多接口字段与线上差异大

### 4.1 字段核对

| 线上接口 | 本地实现 | 请求体 | 响应结构 |
|---------|---------|--------|---------|
| `POST /students/me/plans/draft` | ✅ `generateDraft` | `directionId/useAi/requestId` ✅ | `PlanDraftDto`（goalSummary/semesterGoals/monthlyTasks/notes）✅ |
| `GET /students/me/plans/latest` | ⚠️ `getLatestPlan` | - | ❌ 线上 `Plan`（id/version/status/source/goalSummary/semesterGoals/monthlyTasks/notes/confirmedAt/updatedAt）≠ 本地 `PlanDto`（id/goalId/semester/source/status） |
| `PATCH /plans/{planId}` | ⚠️ `editPlan` | 线上 `PlanUpdate`（goalSummary/semesterGoals/monthlyTasks/notes） | ❌ 同上 PlanDto |
| `POST /plans/{planId}/confirm` | ⚠️ `confirmPlan` | 线上 `PlanConfirmRequest{confirm}` | ❌ 同上 PlanDto |
| `GET /plan-versions` | ⚠️ `getPlanVersions` | - | ❌ 线上 Plan 列表，本地 PlanDto 简化 |
| `GET /students/me/goals` | ❌ `getGoals` | - | ❌ 线上 `Goal{primary{directionId,name,chosenAt}, backup{...}, version, updatedAt}` ≠ 本地 `GoalDto{id,directionId,title,goalType,status}` |
| `POST /students/me/goals` | ❌ `setGoal` | 线上 `GoalRequest{primaryDirectionId, backupDirectionId, changeReason}` ≠ 本地 `{directionId,title,goalType}` | ❌ GoalDto |
| `GET /goal-versions` | ❌ `getGoalVersions` | - | ❌ 线上 `GoalVersion{version, primaryDirectionId, backupDirectionId, changeReason, changedAt, changedBy}` ≠ 本地 GoalDto |
| `GET /tasks` | ❌ `getTasks` | - | ❌ 线上 `Task{id,month,title,type,estHours,status,deadline,abilityTags,note,checkedInAt,checkin}` ≠ 本地 `TaskDto{id,title,status,month}` |
| `POST /tasks` | ❌ `addTask` | 线上 Task 结构 | ❌ TaskDto |
| `PATCH /tasks/{taskId}` | ❌ `updateTask` | 线上 Task 结构 | ❌ TaskDto |
| `DELETE /tasks/{taskId}` | ⚠️ `deleteTask` | - | 本地返回 `{}`（线上可能要求特定结构） |
| `POST /tasks/{taskId}/checkin` | ❌ `checkinTask` | 线上 `TaskCheckin` | ❌ TaskDto |
| `GET /students/me/reminders` | ❌ `getReminders` | - | ❌ 线上 `Reminder{id,type,title,content,read,createdAt}` ≠ 本地 `List<Map{taskId,message}>` |

### 4.2 关键字段差异明细

**Plan**（线上 vs 本地 `PlanDto`）：

| 线上 | 本地 | 说明 |
|------|------|------|
| `id` (string) | `Long id` | 类型不一致 |
| `version` (string "P-v2") | 无 | 缺字段 |
| `goalSummary` (string) | 无 | 缺字段 |
| `semesterGoals` (array) | 无 | 缺字段 |
| `monthlyTasks` (array) | 无 | 缺字段 |
| `notes` (array) | 无 | 缺字段 |
| `confirmedAt` / `updatedAt` | 无 | 缺字段 |
| - | `goalId` / `semester` | 线上无此字段 |

**Goal**（线上 vs 本地 `GoalDto`）：

| 线上 | 本地 | 说明 |
|------|------|------|
| `primary{directionId,name,chosenAt}` | `directionId(Long)` | 结构完全不同（线上是主/备选嵌套对象） |
| `backup{...}` | 无 | 缺字段 |
| `version` / `updatedAt` | 无 | 缺字段 |
| - | `id` / `title` / `goalType` / `status` | 线上无此字段 |

**Task**（线上 vs 本地 `TaskDto`）：

| 线上 | 本地 | 说明 |
|------|------|------|
| `id` (string) | `Long id` | 类型不一致 |
| `type` (LEARNING/PRACTICE/CAREER/REVIEW) | 无 | 缺字段 |
| `estHours` (number) | 无 | 缺字段 |
| `deadline` / `abilityTags` / `note` / `checkedInAt` / `checkin` | 无 | 缺字段 |
| `status` 枚举 PENDING/DOING/DONE/DELAYED/ABANDONED | `String status` | 枚举未约束 |
| - | `month` | 线上有 month 但本地字段顺序/语义需确认 |

**Reminder**（线上 vs 本地）：

| 线上 | 本地 | 说明 |
|------|------|------|
| `id` (string) | `taskId` (Long) | 结构完全不同 |
| `type` (TASK_DEADLINE/...) | 无 | 缺字段 |
| `title` / `content` / `read` / `createdAt` | `message` | 字段不同 |

### 4.3 路径 / 结构差异

- 路径：线上 `POST /api/v1/students/me/plans/draft`（请求体 `PlanDraftRequest{directionId, useAi, requestId}`）｜ 当前 `POST /api/v1/planning/plans/generate`（无请求体）
- **线上 `PlanDraft`**：`goalSummary(string) / semesterGoals[](SemesterGoal{title, abilityTag}) / monthlyTasks[](MonthlyTask{month, title, taskType, estimatedHours}) / notes[]`
- **当前后端**：`studentId / direction / goal{id,title,goalType} / semester / status / tasks[]{month,title,status}`

### 4.4 改造建议

计划模块当前为「可用简化版」实现，若需通过 Apifox 契约测试，建议按本节逐项补齐：

- 按线上 Schema 重构 `PlanDto/GoalDto/TaskDto`（含 `version/goalSummary/semesterGoals/monthlyTasks/notes` 等缺失字段、`id` 改 String、状态枚举对齐）。
- `goals` 接口改为线上 `Goal{primary/backup}` 嵌套结构。
- `reminders` 接口返回线上 `Reminder` 结构。
- `POST /goals` 请求体改为 `primaryDirectionId/backupDirectionId/changeReason`。

---

## 五、数据库字段影响（不动 schema，仅语义调整）

- `recommendation_result.score` 由 0-1 语义改为 0-100 语义（列 DECIMAL(6,4) 可容纳，不改列）。
- `recommendation_result.explanation_json` 由存单个 reason 字符串改为存结构化 JSON（reasons/strengths/gaps/semesterActions）。
- `recommendation_run.status` 落库值由 `DONE` 改为线上枚举 `SUCCESS`。
- `profileVersion` 暂无独立版本号列，复用 `profile_snapshot.id` 表示画像版本号（Demo 精简点）。
- 方向对外 ID 使用 `career_direction.direction_code`（如 `DIR001`），内部落库仍用 `id`（Long）。

---

## 六、改造文件清单

| 文件 | 动作 |
|------|------|
| `RecommendationDto.java` | 重构为 `RecommendationResultDto` + `RecommendationRunDto` |
| `RecommendationEngine.java` | score 0-100；新增 strengths/gaps/semesterActions/reasons 结构化方法 |
| `RecommendationService.java` | 新响应组装、confidence 枚举、latest/history/detail 方法 |
| `RecommendationDao.java` | 新增批次/结果查询方法 |
| `RecommendationController.java` | 5 个线上路径 |
| `StudentProfileController.java` | 路径改 `/students/me/profile/latest` |
| `StudentProfileService.java` | ProfileSnapshot 结构对齐 |
| `PlanningController.java` | 路径改 `/students/me/plans/draft` + 请求体 |
| `PlanningService.java` | PlanDraft 结构对齐 |
