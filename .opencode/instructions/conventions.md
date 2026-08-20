# 开发约定

以下约定在每次回答中必须遵守：

- **接口与鉴权**：统一前缀 `/api/v1`；Demo 无登录态，`studentId` 为可选参数、缺省取 1001；涉及接口改动前先对照 `docs/接口设计.md` 与 `docs/openapi/career-core-apis.yaml`。
- **连续斜杠 `//` 处理**：Apifox 契约测试在路径变量为空时会请求带连续斜杠的 URL（如 `POST /api/v1/profile-snapshots//feedback`），Spring PathPattern 无法匹配空路径段（返回 404「接口不存在」）。全局已用 `career-core` 的 `com.career.core.common.PathNormalizeFilter`（OncePerRequestFilter + HttpServletRequestWrapper）把请求路径中的连续斜杠折叠为单斜杠；涉及「路径参数可为空」的接口改动时，需同时为该路径补充兜底路由（如 `{"/profile-snapshots/feedback", "/profile-snapshots/{snapshotId:.*}/feedback"}`），并让空 id 落到「最新一条」逻辑，保证 200 + 完整响应结构。
- **分层结构**：后端在 `career-core` 的 `modules/<模块>` 下，每个模块 = Controller + Service + Dao(JdbcTemplate)，包名 `com.career.core.modules.*`；公共类在 `com.career.core.common`。
- **数据库约定**：表/字段 snake_case、主键 bigint 自增、通用列 `created_at/updated_at`；**优先沿用现有表结构，不轻易新增表/字段**；MySQL 保留字（如 `rank`）需反引号。
- **注释约定**：所有 Demo 简化/增强/兼容逻辑处必须用注释标明「Demo 精简点 / 后续迭代替换位置」，便于后续识别替换。
- **语言与编码**：代码与数据统一 UTF-8；与用户使用中文交流。