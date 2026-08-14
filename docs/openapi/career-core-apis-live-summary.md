# 生涯规划系统 · 全量 API 接口清单（Apifox 线上）

> 数据来源：Apifox 项目「生涯规划系统」(ID 8662286) 主分支 ｜ 接口总数：**122** ｜ 目录：16 个

## 一、统计概览

| HTTP 方法 | 数量 |
|-----------|------|
| GET | 54 |
| POST | 48 |
| PUT | 2 |
| PATCH | 13 |
| DELETE | 5 |

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
| 1 | `POST` | `/api/v1/assessment-sessions` | 创建测评会话 |
| 2 | `GET` | `/api/v1/assessment-sessions` | 我的测评会话 |
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
| 1 | `GET` | `/api/v1/profile-snapshots/{snapshotId}` | 画像快照详情 |
| 2 | `POST` | `/api/v1/profile-snapshots/{snapshotId}/feedback` | 画像反馈 |
| 3 | `GET` | `/api/v1/students/me/profile/latest` | 最新画像 |
| 4 | `POST` | `/api/v1/students/me/profile/refresh` | 重新生成画像 |
| 5 | `GET` | `/api/v1/students/me/profile/versions` | 画像版本列表 |

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
| 1 | `POST` | `/api/v1/recommendation-results/{resultId}/feedback` | 推荐反馈 |
| 2 | `GET` | `/api/v1/recommendation-runs/{runId}` | 推荐批次详情 |
| 3 | `GET` | `/api/v1/students/me/recommendations` | 推荐批次历史 |
| 4 | `GET` | `/api/v1/students/me/recommendations/latest` | 最新推荐结果 |
| 5 | `POST` | `/api/v1/students/me/recommendations/runs` | 创建推荐批次 |

## 二.7 目标计划（14 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/goal-versions` | 目标版本历史 |
| 2 | `GET` | `/api/v1/plan-versions` | 计划版本历史 |
| 3 | `PATCH` | `/api/v1/plans/{planId}` | 编辑计划 |
| 4 | `POST` | `/api/v1/plans/{planId}/confirm` | 确认计划 |
| 5 | `GET` | `/api/v1/students/me/goals` | 我的目标 |
| 6 | `POST` | `/api/v1/students/me/goals` | 设置 / 变更目标 |
| 7 | `POST` | `/api/v1/students/me/plans/draft` | 生成计划草案 |
| 8 | `GET` | `/api/v1/students/me/plans/latest` | 最新计划 |
| 9 | `GET` | `/api/v1/students/me/reminders` | 站内提醒 |
| 10 | `GET` | `/api/v1/tasks` | 任务列表 |
| 11 | `POST` | `/api/v1/tasks` | 新增任务 |
| 12 | `PATCH` | `/api/v1/tasks/{taskId}` | 更新任务 |
| 13 | `DELETE` | `/api/v1/tasks/{taskId}` | 删除任务 |
| 14 | `POST` | `/api/v1/tasks/{taskId}/checkin` | 任务打卡 |

## 二.8 阶段复盘（8 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/reviews` | 复盘列表 |
| 2 | `POST` | `/api/v1/reviews/drafts` | 创建复盘草稿 |
| 3 | `GET` | `/api/v1/reviews/{reviewId}` | 复盘详情 |
| 4 | `POST` | `/api/v1/reviews/{reviewId}/adopt-advice` | 采纳调整建议 |
| 5 | `POST` | `/api/v1/reviews/{reviewId}/ai-summary` | 生成 AI 阶段总结 |
| 6 | `PUT` | `/api/v1/reviews/{reviewId}/draft` | 保存复盘草稿 |
| 7 | `POST` | `/api/v1/reviews/{reviewId}/guidance-request` | 申请辅导员指导 |
| 8 | `POST` | `/api/v1/reviews/{reviewId}/submit` | 提交复盘 |

## 二.9 AI 智能服务（5 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/ai/chat` | 生涯咨询问答 |
| 2 | `POST` | `/api/v1/ai/pdf/parse` | 解析培养方案 PDF |
| 3 | `POST` | `/api/v1/ai/plan/generate` | 生成学期计划草案 |
| 4 | `POST` | `/api/v1/ai/recommendation/explain` | 生成推荐解释 |
| 5 | `POST` | `/api/v1/ai/review/summarize` | 生成阶段总结 |

## 二.10 AI 生涯咨询（2 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/ai/chat/history` | 会话历史 |
| 2 | `POST` | `/api/v1/ai/chat/{messageId}/feedback` | 回答反馈 |

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

## 二.13 管理端·配置（21 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `GET` | `/api/v1/admin/abilities` | 能力标签列表 |
| 2 | `POST` | `/api/v1/admin/abilities` | 新增能力标签 |
| 3 | `PATCH` | `/api/v1/admin/abilities/{tagId}` | 更新能力标签 |
| 4 | `GET` | `/api/v1/admin/directions` | 方向库管理 |
| 5 | `POST` | `/api/v1/admin/directions` | 新增方向 |
| 6 | `PATCH` | `/api/v1/admin/directions/{directionId}` | 更新方向 |
| 7 | `PATCH` | `/api/v1/admin/directions/{directionId}/status` | 启停方向 |
| 8 | `GET` | `/api/v1/admin/models` | 模型与提示词配置 |
| 9 | `POST` | `/api/v1/admin/models/prompts` | 新建提示词版本 |
| 10 | `POST` | `/api/v1/admin/models/prompts/{promptVersionId}/publish` | 发布提示词 |
| 11 | `PATCH` | `/api/v1/admin/models/{key}` | 更新模型配置 |
| 12 | `GET` | `/api/v1/admin/questionnaires` | 问卷管理列表 |
| 13 | `POST` | `/api/v1/admin/questionnaires` | 新建问卷 |
| 14 | `GET` | `/api/v1/admin/questionnaires/{questionnaireId}/preview` | 问卷预览 |
| 15 | `PATCH` | `/api/v1/admin/questionnaires/{questionnaireId}/status` | 发布 / 停用问卷 |
| 16 | `POST` | `/api/v1/admin/questionnaires/{questionnaireId}/versions` | 新建问卷版本 |
| 17 | `GET` | `/api/v1/admin/templates` | 任务模板列表 |
| 18 | `POST` | `/api/v1/admin/templates` | 新增任务模板 |
| 19 | `PATCH` | `/api/v1/admin/templates/{templateId}` | 更新任务模板 |
| 20 | `GET` | `/api/v1/admin/weights` | 推荐权重配置 |
| 21 | `POST` | `/api/v1/admin/weights` | 更新推荐权重 |

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

## 二.15 管理端·日志与导出（5 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | `POST` | `/api/v1/admin/exports` | 创建导出任务 |
| 2 | `GET` | `/api/v1/admin/exports` | 导出任务列表 |
| 3 | `GET` | `/api/v1/admin/exports/{jobId}/download` | 下载导出文件 |
| 4 | `GET` | `/api/v1/admin/logs/ai` | AI 调用日志 |
| 5 | `GET` | `/api/v1/admin/logs/operations` | 操作审计日志 |
