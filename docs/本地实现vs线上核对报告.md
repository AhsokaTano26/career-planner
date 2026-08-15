# 本地实现 vs Apifox 线上文档 · 接口核对报告

> 核对时间：2026-08-15 ｜ 数据源：Apifox 项目 8662286 主分支最新导出（`docs/openapi/career-core-apis-live.yaml`）
> 核对对象：`career-core` 已实现的 3 个模块（画像 / 推荐 / 计划）
> 结论总览：推荐模块字段一致 ✅；画像模块字段一致但 1 处 null 序列化风险 ⚠️；计划模块多接口字段与线上差异较大 ❌

---

## 一、推荐模块（Recommendation）— ✅ 字段一致

| 线上接口 | 本地实现 | 请求体 | 响应结构 |
|---------|---------|--------|---------|
| `POST /students/me/recommendations/runs` | ✅ `RecommendationController.createRun` | `pathFilter/requestId` ✅ | `RecommendationRunDto`（runId/profileVersion/ruleVersion/generatedAt/status/results）✅ |
| `GET /students/me/recommendations/latest` | ✅ `getLatest` | - | `RecommendationRunDto` ✅ |
| `GET /students/me/recommendations`（历史） | ⚠️ `getHistory` | `page/size/sort` | 线上 schema 为**单个 RecommendationRun**，本地返回 **List**，结构不匹配 |
| `GET /recommendation-runs/{runId}` | ✅ `getRunDetail` | - | `RecommendationRunDto` ✅ |
| `POST /recommendation-results/{resultId}/feedback` | ✅ `feedback` | `feedbackType/comment` ✅ | `RecommendationFeedbackDto`（HELPFUL/NEUTRAL/MISMATCH/NOT_INTERESTED）✅ |

**RecommendationResult 字段核对**：

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

> ⚠️ 小提醒：线上历史接口（`GET /recommendations`）响应 schema 引用单个 `RecommendationRun`，而本地返回数组。若契约测试严格比对需确认。

---

## 二、画像模块（Profile）— ✅ 字段一致（1 处风险）

| 线上接口 | 本地实现 | 响应结构 |
|---------|---------|---------|
| `GET /students/me/profile/latest` | ✅ `getLatestProfile` | `ProfileSnapshotDto` ✅ |
| `GET /students/me/profile/versions` | ✅ `getVersions` | `ProfileSnapshotDto`（线上 schema 为单个，返回最近一条）✅ |
| `POST /students/me/profile/refresh` | ✅ `refreshProfile` | `ProfileSnapshotDto` ✅ |
| `GET /profile-snapshots/{snapshotId}` | ✅ `getSnapshot` | `ProfileSnapshotDto` ✅ |
| `POST /profile-snapshots/{snapshotId}/feedback` | ✅ `addFeedback` | `ProfileFeedbackDto` ✅ |

**ProfileSnapshot 字段核对**：

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

---

## 三、计划模块（Planning）— ❌ 多接口字段与线上差异大

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

### 计划模块关键字段差异明细

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

---

## 四、核对结论与建议

1. **推荐模块**：✅ 字段与线上一致，可放心对接（唯一小项：历史接口返回 List vs 线上单对象，需确认契约期望）。
2. **画像模块**：✅ 字段一致，但需给 `ProfileSnapshotDto` 补 `@JsonInclude(NON_NULL)`，避免 `feedback: null` 触发契约报错（与推荐模块保持同一套修复）。
3. **计划模块**：❌ **差异最大**。`Plan/Goal/Task/Reminder` 四类响应结构、`GoalRequest` 请求体均与线上不一致：
   - 需按线上 Schema 重构 `PlanDto/GoalDto/TaskDto`（含 `version/goalSummary/semesterGoals/monthlyTasks/notes` 等缺失字段、`id` 改 String、状态枚举对齐）。
   - `goals` 接口改为线上 `Goal{primary/backup}` 嵌套结构。
   - `reminders` 接口返回线上 `Reminder` 结构。
   - `POST /goals` 请求体改为 `primaryDirectionId/backupDirectionId/changeReason`。

> 说明：计划模块当前为「可用简化版」实现，若需通过 Apifox 契约测试，建议按本报告第三节逐项补齐。是否要我按线上 Schema 重构计划模块的 DTO 与 Service？
