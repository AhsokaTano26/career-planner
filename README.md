# career-planner

计算机与软件大类新生生涯规划系统（MVP V1.0 Demo）。

## 文档入口

- **📖 使用手册**：`docs/使用手册.md`（环境准备 / 启动 / 验证 / 接口使用 / 部署 / 排坑）
- 接口设计：`docs/接口设计.md`
- 具体实现细节：`docs/具体实现细节_MVP_V1.0.md`
- OpenAPI 定义：`docs/openapi/career-core-apis.yaml`

## 组件

| 组件 | 技术栈 | 端口 | 状态 |
|------|--------|------|------|
| `career-core` | Spring Boot 3.5 + Java 25 + MySQL | 8080 | ✅ 已实现 |
| `career-ai` | FastAPI | 8000 | ✅ 推荐解释已实现 |
| `frontend` | Vue 3 | - | ⚠️ 空骨架 |
| `deploy` | Docker Compose + Nginx | 80 | ⚠️ 最小骨架 |