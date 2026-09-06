# 生涯规划系统 · 全量 API 接口清单（Apifox 线上 + 本地补全）

> 数据来源：Apifox 项目「生涯规划系统」(ID 8662286) 主分支 + 本地手工补全 ｜ 接口总数：**135** ｜ 目录：20 个

## 一、统计概览

| HTTP 方法 | 数量 |
|-----------|------|
| GET | 61 |
| POST | 54 |
| PUT | 5 |
| PATCH | 11 |
| DELETE | 4 |

## 二.1 认证与账号（9 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/auth/login` | 登录 |
| 2 | `POST` | `/api/v1/auth/logout` | 退出登录 |
| 3 | `GET` | `/api/v1/auth/me` | 当前用户信息 |
| 4 | `PATCH` | `/api/v1/auth/me/password` | 修改密码 |
| 5 | `POST` | `/api/v1/auth/password/reset` | 管理员重置密码 |
| 6 | `POST` | `/api/v1/auth/privacy-consent` | 同意隐私授权 |
| 7 | `GET` | `/api/v1/auth/privacy-consent/status` | 查询授权状态 |
| 8 | `POST` | `/api/v1/auth/refresh` | 刷新访问令牌 |
| 9 | `POST` | `/api/v1/auth/register` | 学号注册 |

## 二.2 学生档案（8 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/students/me` | 获取我的档案 |
| 2 | `PATCH` | `/api/v1/students/me` | 分步保存学生资料 |
| 3 | `GET` | `/api/v1/students/me/completeness` | 资料完整度明细 |
| 4 | `POST` | `/api/v1/students/me/deletion-request` | 申请删除本人信息 |
| 5 | `GET` | `/api/v1/students/me/experiences` | 经历列表 |
| 6 | `POST` | `/api/v1/students/me/experiences` | 新增经历 |
| 7 | `PATCH` | `/api/v1/students/me/experiences/{experienceId}` | 修改经历 |
| 8 | `DELETE` | `/api/v1/students/me/experiences/{experienceId}` | 删除经历 |

## 二.3 测评（9 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/assessment-sessions` | 我的测评会话 |
| 2 | `POST` | `/api/v1/assessment-sessions` | 创建测评会话 |
| 3 | `GET` | `/api/v1/assessment-sessions/{sessionId}` | 会话详情 |
| 4 | `PUT` | `/api/v1/assessment-sessions/{sessionId}/answers` | 保存 / 自动保存答案 |
| 5 | `GET` | `/api/v1/assessment-sessions/{sessionId}/scores` | 得分明细 |
| 6 | `POST` | `/api/v1/assessment-sessions/{sessionId}/submit` | 提交并计分 |
| 7 | `GET` | `/api/v1/questionnaires` | 问卷列表 |
| 8 | `GET` | `/api/v1/questionnaires/{questionnaireId}` | 问卷详情 |
| 9 | `GET` | `/api/v1/questionnaires/{questionnaireId}/versions` | 问卷版本历史 |

## 二.4 学生画像（5 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/profile-snapshots/{snapshotId}` | 查指定快照 |
| 2 | `POST` | `/api/v1/profile-snapshots/{snapshotId}/feedback` | 提交反馈 |
| 3 | `GET` | `/api/v1/students/me/profile/latest` | 查最新画像 |
| 4 | `POST` | `/api/v1/students/me/profile/refresh` | 刷新画像 |
| 5 | `GET` | `/api/v1/students/me/profile/versions` | 列历史版本 |

## 二.5 路径与方向（7 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/directions` | 方向列表 |
| 2 | `GET` | `/api/v1/directions/compare` | 方向对比 |
| 3 | `GET` | `/api/v1/directions/{directionId}` | 方向详情 |
| 4 | `GET` | `/api/v1/paths` | 发展路径列表 |
| 5 | `GET` | `/api/v1/students/me/favorites` | 我的收藏 |
| 6 | `POST` | `/api/v1/students/me/favorites/{directionId}` | 收藏方向 |
| 7 | `DELETE` | `/api/v1/students/me/favorites/{directionId}` | 取消收藏 |

## 二.6 方向推荐（5 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/recommendation-results/{resultId}/feedback` | 提交反馈 |
| 2 | `GET` | `/api/v1/recommendation-runs/{runId}` | 批次详情 |
| 3 | `GET` | `/api/v1/students/me/recommendations` | 列推荐批次 |
| 4 | `GET` | `/api/v1/students/me/recommendations/latest` | 查最新推荐 |
| 5 | `POST` | `/api/v1/students/me/recommendations/runs` | 生成推荐 |

## 二.7 目标计划（16 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/students/me/goals` | 设目标 |
| 2 | `GET` | `/api/v1/students/me/goals/versions` | listGoalVersions |
| 3 | `PUT` | `/api/v1/students/me/plans` | updatePlan |
| 4 | `POST` | `/api/v1/students/me/plans/confirm` | 确认计划 |
| 5 | `POST` | `/api/v1/students/me/plans/draft` | 生成计划草案 |
| 6 | `GET` | `/api/v1/students/me/plans/latest` | 查最新计划 |
| 7 | `GET` | `/api/v1/students/me/plans/{planId}` | 查计划详情 |
| 8 | `GET` | `/api/v1/students/me/reminders` | 列提醒（全部） |
| 9 | `POST` | `/api/v1/students/me/reminders/generate` | 触发生成提醒 |
| 10 | `GET` | `/api/v1/students/me/reminders/unread-count` | 看未读数 |
| 11 | `POST` | `/api/v1/students/me/reminders/{reminderId}/read` | 标记已读 |
| 12 | `GET` | `/api/v1/students/me/tasks` | 列任务 |
| 13 | `POST` | `/api/v1/students/me/tasks` | 手动创建任务 |
| 14 | `GET` | `/api/v1/students/me/tasks/{taskId}` | 获取任务 |
| 15 | `PUT` | `/api/v1/students/me/tasks/{taskId}` | updateTask |
| 16 | `POST` | `/api/v1/students/me/tasks/{taskId}/checkin` | checkinTask |

## 二.8 阶段复盘（8 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/reviews` | 列复盘 |
| 2 | `POST` | `/api/v1/reviews/drafts` | 建复盘草案 |
| 3 | `GET` | `/api/v1/reviews/{reviewId}` | 查复盘详情 |
| 4 | `POST` | `/api/v1/reviews/{reviewId}/adopt-advice` | 采纳建议 |
| 5 | `POST` | `/api/v1/reviews/{reviewId}/ai-summary` | AI 总结 |
| 6 | `PUT` | `/api/v1/reviews/{reviewId}/draft` | 更新草案 |
| 7 | `POST` | `/api/v1/reviews/{reviewId}/guidance-request` | 申请辅导员指导 |
| 8 | `POST` | `/api/v1/reviews/{reviewId}/submit` | 提交复盘 |

## 二.9 AI 智能服务（4 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/ai/pdf/parse` | pdfParse |
| 2 | `POST` | `/api/v1/ai/plan/generate` | planGenerate |
| 3 | `POST` | `/api/v1/ai/recommendation/explain` | recommendExplain |
| 4 | `POST` | `/api/v1/ai/review/summarize` | reviewSummarize |

## 二.10 AI 生涯咨询（4 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/ai/chat` | AI 问答 |
| 2 | `POST` | `/api/v1/ai/chat/feedback` | 兜底反馈 |
| 3 | `GET` | `/api/v1/ai/chat/history` | 查会话历史 |
| 4 | `POST` | `/api/v1/ai/chat/{messageId}/feedback` | 按消息 ID 反馈 |

## 二.11 辅导员端（7 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/advisor/attention` | 需关注学生 |
| 2 | `GET` | `/api/v1/advisor/statistics` | 群体统计 |
| 3 | `GET` | `/api/v1/advisor/students` | 所带学生列表 |
| 4 | `GET` | `/api/v1/advisor/students/{studentId}` | 学生详情总览 |
| 5 | `POST` | `/api/v1/advisor/students/{studentId}/advice` | 提出建议任务 / 建议重新测评 |
| 6 | `GET` | `/api/v1/advisor/students/{studentId}/guidance` | 指导记录 |
| 7 | `POST` | `/api/v1/advisor/students/{studentId}/guidance` | 填写指导意见 |

## 二.12 管理端·用户与白名单（9 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/relations` | 辅导员学生关系 |
| 2 | `POST` | `/api/v1/admin/relations` | 批量建立关系 |
| 3 | `DELETE` | `/api/v1/admin/relations/{relationId}` | 解除关系 |
| 4 | `GET` | `/api/v1/admin/users` | 用户列表 |
| 5 | `PATCH` | `/api/v1/admin/users/{userId}` | 更新用户 |
| 6 | `GET` | `/api/v1/admin/whitelist` | 白名单列表 |
| 7 | `POST` | `/api/v1/admin/whitelist` | 新增白名单 |
| 8 | `POST` | `/api/v1/admin/whitelist/import` | 批量导入白名单（CSV） |
| 9 | `DELETE` | `/api/v1/admin/whitelist/{whitelistId}` | 删除白名单 |

## 二.13 管理端·配置（16 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/abilities` | 能力标签列表 |
| 2 | `POST` | `/api/v1/admin/abilities` | 新增能力标签 |
| 3 | `PATCH` | `/api/v1/admin/abilities/{tagId}` | 更新能力标签 |
| 4 | `GET` | `/api/v1/admin/directions` | 方向库管理 |
| 5 | `POST` | `/api/v1/admin/directions` | 新增方向 |
| 6 | `PATCH` | `/api/v1/admin/directions/{directionId}` | 更新方向 |
| 7 | `PATCH` | `/api/v1/admin/directions/{directionId}/status` | 启停方向 |
| 8 | `GET` | `/api/v1/admin/questionnaires` | 问卷管理列表 |
| 9 | `POST` | `/api/v1/admin/questionnaires` | 新建问卷 |
| 10 | `PATCH` | `/api/v1/admin/questionnaires/{questionnaireId}/status` | 发布 / 停用问卷 |
| 11 | `POST` | `/api/v1/admin/questionnaires/{questionnaireId}/versions` | 新建问卷版本 |
| 12 | `GET` | `/api/v1/admin/templates` | 任务模板列表 |
| 13 | `POST` | `/api/v1/admin/templates` | 新增任务模板 |
| 14 | `PATCH` | `/api/v1/admin/templates/{templateId}` | 更新任务模板 |
| 15 | `GET` | `/api/v1/admin/weights` | 推荐权重配置 |
| 16 | `POST` | `/api/v1/admin/weights` | 更新推荐权重 |

## 二.14 管理端·培养方案（8 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/admin/curricula/import` | 上传培养方案 PDF |
| 2 | `GET` | `/api/v1/admin/curricula/items` | 待审核课程列表 |
| 3 | `POST` | `/api/v1/admin/curricula/items/batch` | 批量校核 / 合并 / 删除 |
| 4 | `PATCH` | `/api/v1/admin/curricula/items/{itemId}` | 校核课程 |
| 5 | `GET` | `/api/v1/admin/curricula/jobs` | 导入任务列表 |
| 6 | `GET` | `/api/v1/admin/curricula/jobs/{jobId}` | 导入任务详情 |
| 7 | `POST` | `/api/v1/admin/curricula/publish` | 发布培养方案版本 |
| 8 | `GET` | `/api/v1/admin/curricula/versions` | 方案版本列表 |

## 二.15 管理端·日志与导出（6 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/exports` | 导出任务列表 |
| 2 | `POST` | `/api/v1/admin/exports` | 创建导出任务 |
| 3 | `GET` | `/api/v1/admin/exports/{jobId}` | 查询导出任务详情 |
| 4 | `GET` | `/api/v1/admin/exports/{jobId}/download` | 下载导出文件 |
| 5 | `GET` | `/api/v1/admin/logs/ai` | AI 调用日志 |
| 6 | `GET` | `/api/v1/admin/logs/operations` | 操作审计日志 |

## 二.16 admin-model-config-controller（2 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/model-configs` | 模型配置列表 |
| 2 | `PUT` | `/api/v1/admin/model-configs/{configKey}` | 更新模型配置 |

## 二.17 direction-controller（2 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/students/me/directions` | listDirections |
| 2 | `GET` | `/api/v1/students/me/directions/{directionId}` | getDirection |

## 二.18 admin-questionnaire-controller（4 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `PATCH` | `/api/v1/admin/questionnaires/{questionnaireId}` | 更新问卷 |
| 2 | `GET` | `/api/v1/admin/questionnaires/{questionnaireId}/versions` | 问卷版本列表 |
| 3 | `GET` | `/api/v1/admin/questionnaires/{questionnaireId}/versions/{versionId}` | 问卷版本详情 |
| 4 | `POST` | `/api/v1/admin/questionnaires/{questionnaireId}/versions/{versionId}/publish` | 发布问卷版本 |

## 二.19 admin-prompt-controller（4 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/prompts` | 提示词版本列表 |
| 2 | `POST` | `/api/v1/admin/prompts` | 新建提示词版本 |
| 3 | `GET` | `/api/v1/admin/prompts/scenes` | 提示词场景列表 |
| 4 | `POST` | `/api/v1/admin/prompts/{promptId}/publish` | 发布提示词 |

## 二.20 AI-Gateway（2 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/gateway/chat/completions` | Chat Completions |
| 2 | `POST` | `/api/v1/gateway/generate` | Gateway Generate |
