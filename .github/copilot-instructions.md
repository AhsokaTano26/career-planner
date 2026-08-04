# 项目环境信息（Copilot Instructions）

> 本文件记录本机/本项目的运行环境信息，供 AI 协作时参考，避免重复探测。

## 1. 项目位置与结构

- **工作区根目录**：`D:\Zht20241287\生涯规划系统开发`（当前 VS Code 打开的工作区）
- **项目代码根目录**：`D:\Zht20241287\生涯规划系统开发\career-planning`
  - `docs/` —— 项目文档（`接口设计.md`、设计/需求 docx、`openapi/career-core-apis.yaml`）
  - `frontend/` —— Vue 3 前端（空骨架，未实现）
  - `career-core/` —— Spring Boot 后端（已实现：画像、推荐、计划生成）
  - `career-ai/` —— FastAPI 智能服务（占位骨架，未实现）
  - `deploy/` —— docker-compose.yml、env.example、nginx/、scripts/
  - `data-seed/` —— 空
  - `tests/` —— `smoke_api.py`（跨服务冒烟）、`feedback.json`
- 无 Git 仓库（`.git` 不存在）；本机无 winget、无 Docker

## 2. 操作系统与硬件

- **系统**：Microsoft Windows 10 企业版（Enterprise），版本号 **10.0.19045**（Build 19045），**64 位**（x86_64 / amd64）
- **CPU**：16 逻辑处理器
- **内存**：约 16 GB（17082138624 字节）
- **终端**：PowerShell（pwsh）

## 3. 开发工具链（版本与路径）

| 工具 | 版本 | 安装路径 |
|------|------|----------|
| JDK | Temurin OpenJDK **21.0.12+8**（LTS，64 位） | `D:\devtools\jdk21\jdk-21.0.12+8` |
| Maven | **3.9.16** | `D:\devtools\maven\apache-maven-3.9.16` |
| Node.js | **v24.18.0** | 系统 PATH |
| npm | **11.16.0** | 系统 PATH |
| Python | **3.14.6**（uv 管理：`C:\Users\uio8k\AppData\Roaming\uv\python\cpython-3.14-...`） | 系统 PATH |
| MySQL | **26.7.0** Community Server（GPL） | `D:\devtools\mysql\mysql-26.7.0-winx64` |

## 4. 环境变量

- `JAVA_HOME=D:\devtools\jdk21\jdk-21.0.12+8`（已写入 User 级环境变量）
- `MAVEN_HOME=D:\devtools\maven\apache-maven-3.9.16`（已写入 User 级环境变量）
- ⚠️ **新开的终端会话可能读不到 User 级 JAVA_HOME**，运行 Maven/Java 前需手动设置：
  ```powershell
  $env:JAVA_HOME='D:\devtools\jdk21\jdk-21.0.12+8'
  $env:Path='D:\devtools\jdk21\jdk-21.0.12+8\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
  ```

## 5. 数据库（MySQL 26.7.0）

- **端口**：3306（监听 127.0.0.1）
- **库名**：`career_core`（utf8mb4）
- **应用账号**：`career` / `career123`（拥有 career_core 全部权限）
- **root**：空密码（仅本机）
- **配置**：`D:\devtools\mysql\my.ini`（basedir/datadir 见该文件，`datadir=D:\devtools\mysql\data`，`mysqlx=0`）
- **手动启动**（非 Windows 服务）：
  ```powershell
  & D:\devtools\mysql\mysql-26.7.0-winx64\bin\mysqld.exe --defaults-file=D:\devtools\mysql\my.ini --console
  ```
- **客户端**：mysql 命令需加 `--default-character-set=utf8mb4`，否则中文可能乱码
- **建表/种子脚本**：`career-planning\career-core\src\main\resources\db\migration\schema.sql`、`data.sql`（应用启动时由 `spring.sql.init` 幂等执行）

## 6. 后端（career-core）运行

- **技术栈**：Spring Boot **3.5.16**（pom 管理）+ Java 21 + MySQL + JdbcTemplate（无 MyBatis）
- **服务端口**：8080
- **构建与启动**：
  ```powershell
  cd D:\Zht20241287\生涯规划系统开发\career-planning\career-core
  mvn -DskipTests package
  java -jar target\career-core-0.0.1-SNAPSHOT.jar
  ```
- **接口**：`GET /api/v1/profiles/latest`、`POST /api/v1/recommendations/run`、`GET /api/v1/recommendations/latest`、`POST /api/v1/recommendations/{id}/feedback`、`POST /api/v1/planning/plans/generate`（`studentId` 均为可选参数，缺省 1001；响应统一 `{code,message,data}`）

## 7. 本机常用注意事项（避坑）

- **PowerShell 编码**：PowerShell 管道会把 curl 输出的 UTF-8 按 GBK 解码，导致中文乱码/伪空格。对比接口结果请用 `curl.exe -o 文件` 存原始字节 + `Get-FileHash` 比对，或用 `Get-Content -Encoding utf8` 读取；查库用 `mysql ... -e "SELECT HEX(...)"`。
- **JSON 请求体**：PowerShell 中 `curl -d "{\"...\"}"` 的 `\"` 不会转义，会原样发送反斜杠导致服务端 JSON 解析失败（500）。请用 `--data-binary @文件`。
- **保留字**：MySQL 8+ 中 `rank` 为保留字，SQL 需写成 `` `rank` ``。
- **下载源**：archive.apache.org 很慢，Maven 用 dlcdn.apache.org；MySQL 版本须到 dev.mysql.com 下载页查当前版本（2026-08 为 26.7.0）。
- **Apifox**：MCP 服务只读（无法直接写线上文档）；接口定义以 `docs/openapi/career-core-apis.yaml` 供导入。
- **无 winget / Docker**：安装软件一律手动下载 ZIP/安装包解压配置。

## 8. 开发约定（每次回答都要遵守）

- **统一响应**：所有接口返回 `{code,message,data}`；`code=0` 成功；**错误响应 `data` 必须为 `{}`（非 null）**，不可为 null。
- **接口与鉴权**：统一前缀 `/api/v1`；Demo 无登录态，`studentId` 为可选参数、缺省取 1001；涉及接口改动前先对照 `docs/接口设计.md` 与 `docs/openapi/career-core-apis.yaml`。
- **分层结构**：后端在 `career-core` 的 `modules/<模块>` 下，每个模块 = Controller + Service + Dao(JdbcTemplate)，包名 `com.career.core.modules.*`；公共类在 `com.career.core.common`。
- **数据库约定**：表/字段 snake_case、主键 bigint 自增、通用列 `created_at/updated_at`；**优先沿用现有表结构，不轻易新增表/字段**；MySQL 保留字（如 `rank`）需反引号。
- **注释约定**：所有 Demo 简化/增强/兼容逻辑处必须用注释标明「Demo 精简点 / 后续迭代替换位置」，便于后续识别替换。
- **语言与编码**：代码与数据统一 UTF-8；与用户使用中文交流。

## 9. 标准工作流（改后端的固定闭环）

1. **动手前先探测状态**：确认 8080（career-core）与 3306（MySQL）是否在跑、是否需要重启。
2. **修改 → 构建 → 重启**：改完执行 `mvn -DskipTests package`（在 `career-planning\career-core` 下），停旧 java 进程后用 `java -jar target\career-core-0.0.1-SNAPSHOT.jar` 后台启动。
3. **验证**：用 `python tests\smoke_api.py` 或 curl 冒烟，报告每个接口的 HTTP 状态与结果；涉及表结构时先看 `db/migration/schema.sql`（启动幂等执行）。
4. **异常定位**：先看应用日志（后台终端输出），再回查代码；PowerShell 中文乱码时用 `curl.exe -o 文件` 存原始字节 + `Get-Content -Encoding utf8` / `Get-FileHash` 比对。

## 10. 安全与边界

- `.vscode/mcp.json` 内含 Apifox 访问令牌，**不得写入会同步的文件或外泄**；新令牌用系统环境变量管理。
- 不要把数据库密码、API 密钥硬编码进代码或文档；`deploy/env.example` 仅作示例。
- 本机无 winget / Docker，安装软件一律手动下载配置；勿尝试 `winget install` / `docker pull`。

## 有任何问题请问我