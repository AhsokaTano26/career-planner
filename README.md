# 生涯规划系统

大学生生涯规划系统（学生工作台）。前端 Vue 3 + 后端 Spring Boot + AI 服务 FastAPI，数据存储 MySQL。

## 架构

| 服务 | 技术栈 | 端口 | 说明 |
|------|--------|------|------|
| career-core | Spring Boot 3.3.4 + Java 17 + MyBatis + JWT | 8080 | 后端主服务（认证、画像、推荐、计划、辅导员、管理后台） |
| fronted | Vue 3 + Vite + TypeScript | 5173 (dev) | 前端（已构建产物打入后端 static） |
| career-ai | FastAPI + litellm | 8000 | AI 网关与智能服务（推荐解释、生涯规划、聊天） |
| MySQL | MySQL 8 | 3306 | 数据库 `career_core`（utf8mb4） |

## 本地开发启动

> Linux 容器环境：先 `source ~/devtools/env.sh` 加载 JDK17/Maven/DB 环境变量。

**后端**（career-core 下）：

```bash
./mvnw clean package -DskipTests      # JDK 17
java -Dfile.encoding=UTF-8 -jar target/career-core-0.0.1-SNAPSHOT.jar
```

**前端**（fronted 下，Node >= 20）：

```bash
npm install
npm run dev       # 开发
npm run build     # 构建产物 dist 打入后端 static
npm run test      # vitest
```

**AI 服务**（career-ai 下）：

```bash
source career-ai/.env                  # 配置 GATEWAY_API_KEY 等
python -m uvicorn api.main:app --host 127.0.0.1 --port 8000
```

## Docker 部署

- 镜像：`tano26/career-planner`（Docker Hub），CI 由 `.github/workflows/docker-publish.yml` 驱动（push main 自动构建推送）
- 编排：根目录 `docker-compose.yml`（mysql:8.4 + app），环境变量由 `.env` 注入
- 默认账号：管理员 `admin / Admin@2026`；学生 `2026011301 / 202601`

## 接口与认证

- 统一前缀 `/api/v1`，JWT 认证（登录 `POST /api/v1/auth/login`）
- 成功响应包装 `{code, message, data, traceId, timestamp}` 且 `code=OK`
