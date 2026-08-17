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
| JDK | Microsoft OpenJDK **25.0.2+10**（LTS，64 位） | `D:\devtools\jdk25\jdk-25.0.2` |
| Maven | **3.9.16** | `D:\devtools\maven\apache-maven-3.9.16` |
| Node.js | **v24.18.0** | 系统 PATH |
| npm | **11.16.0** | 系统 PATH |
| Python | **3.14.6**（uv 管理：`C:\Users\uio8k\AppData\Roaming\uv\python\cpython-3.14-...`） | 系统 PATH |
| MySQL | **26.7.0** Community Server（GPL） | `D:\devtools\mysql\mysql-26.7.0-winx64` |

## 4. 环境变量

- `JAVA_HOME=D:\devtools\jdk25\jdk-25.0.2`（项目目标运行 JDK，**pom 要求 Java 25**；若 User 级变量仍指向旧 JDK，请手动更新）
- `MAVEN_HOME=D:\devtools\maven\apache-maven-3.9.16`（已写入 User 级环境变量）
- ⚠️ **新开的终端会话可能读不到 User 级 JAVA_HOME，且默认 `java` 可能仍是 JDK 21**。运行 Maven/Java 前需手动设置，或用 JDK 25 绝对路径直接调用：
  ```powershell
  # 方式一：修正当前会话的 JAVA_HOME / PATH
  $env:JAVA_HOME='D:\devtools\jdk25\jdk-25.0.2'
  $env:Path='D:\devtools\jdk25\jdk-25.0.2\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path

  # 方式二：直接用 JDK 25 绝对路径启动（不依赖 PATH）
  & "D:\devtools\jdk25\jdk-25.0.2\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"
  ```
- ⚠️ **JDK 版本不一致会启动失败**：用 JDK 21 跑 JDK 25 编译的 jar 会报 `UnsupportedClassVersionError: ... class file version 69.0`（69.0=Java 25，21 只识别到 65.0）。务必用 JDK 25 运行/打包。

## 5. 数据库（MySQL 26.7.0）

- **端口**：3306（监听 127.0.0.1）
- **库名**：`career_core`（utf8mb4）
- **应用账号**：默认 `career` / 密码经环境变量注入（`$env:DB_USERNAME` / `$env:DB_PASSWORD`，缺省 `career123` 供本地直启；`application.yml` 已改为 `${DB_PASSWORD:career123}`，不硬编码入库）
- **root**：空密码（仅本机）
- **配置**：`D:\devtools\mysql\my.ini`（basedir/datadir 见该文件，`datadir=D:\devtools\mysql\data`，`mysqlx=0`）
- **手动启动**（非 Windows 服务）：
  ```powershell
  & D:\devtools\mysql\mysql-26.7.0-winx64\bin\mysqld.exe --defaults-file=D:\devtools\mysql\my.ini --console
  ```
- **客户端**：mysql 命令需加 `--default-character-set=utf8mb4`，否则中文可能乱码
- **建表/种子脚本**：`career-planning\career-core\src\main\resources\db\migration\schema.sql`、`data.sql`（应用启动时由 `spring.sql.init` 幂等执行）

## 6. 后端（career-core）运行

- **技术栈**：Spring Boot **3.5.16**（pom 管理）+ Java 25 + MySQL + JdbcTemplate（无 MyBatis）
- **服务端口**：8080
- **前置依赖**：MySQL 必须先启动（3306 有监听），否则后端启动后查库失败。
- **构建与启动**（务必用 JDK 25；jar 路径用绝对路径，避免工具丢 `cd`）：
  ```powershell
  # 1) 构建（在 career-core 目录，先修正 JAVA_HOME）
  $env:JAVA_HOME='D:\devtools\jdk25\jdk-25.0.2'
  $env:Path='D:\devtools\jdk25\jdk-25.0.2\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
  mvn -DskipTests package

  # 2) 启动（JDK 25 绝对路径 + jar 绝对路径）
  & "D:\devtools\jdk25\jdk-25.0.2\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"
  ```
- **接口**（已对齐 Apifox 线上定义，成功响应为裸业务对象，非 `{code,message,data}` 包装）：
  - 画像：`GET /api/v1/students/me/profile/latest`、`GET /api/v1/students/me/profile/versions`
  - 推荐：`POST /api/v1/students/me/recommendations/runs`、`GET /api/v1/students/me/recommendations/latest`、`GET /api/v1/students/me/recommendations`、`GET /api/v1/recommendation-runs/{runId}`、`POST /api/v1/recommendation-results/{resultId}/feedback`
  - 计划：`POST /api/v1/students/me/plans/draft`
  - `studentId` 均为可选参数，缺省 1001；错误响应由全局异常处理统一 `{code,message,data}`。

## 7. 本机常用注意事项（避坑）

- **PowerShell 编码**：PowerShell 管道会把 curl 输出的 UTF-8 按 GBK 解码，导致中文乱码/伪空格。对比接口结果请用 `curl.exe -o 文件` 存原始字节 + `Get-FileHash` 比对，或用 `Get-Content -Encoding utf8` 读取；查库用 `mysql ... -e "SELECT HEX(...)"`。
- **JSON 请求体**：PowerShell 中 `curl -d "{\"...\"}"` 的 `\"` 不会转义，会原样发送反斜杠导致服务端 JSON 解析失败（500）。请用 `--data-binary @文件`。
- **接口返回字段一般不允许为 null**：成功响应的业务对象字段尽量不返回 `null`，否则可能触发 Apifox 契约校验失败（已踩坑：画像模块 `ProfileSnapshotDto.feedback` 恒为 `null` 且未加过滤，被序列化为 `"feedback": null` 报契约错误）。实现约定：
  - 给 DTO/record 加 `@JsonInclude(JsonInclude.Include.NON_NULL)` 过滤空值（与推荐模块同一套修复保持一致）；
  - 空态统一为默认值/空串/空数组，而非 `null`；
  - 涉及返回结构改动时对照 `docs/本地实现vs线上核对报告.md` 与 `docs/openapi/career-core-apis.yaml`。
- **分支名带括号**：PowerShell 会把 `git ... origin/career-core(back-end)` 里的 `(back-end)` 解析成命令调用，报错 `back-end\` 不是命令。引用含括号的分支名/引用必须加引号，如 `$br = 'origin/career-core(back-end)'` 或 `git log 'origin/career-core(back-end)'`。
- **终端工具会简化命令（丢掉 `cd`）**：后台长命令（如 `java -jar`、`python -m uvicorn`）可能被工具剥离开头的 `cd <目录>`，导致相对路径（如 `target\xxx.jar`）解析失败，报 `Unable to access jarfile`。启动服务请用绝对路径（`java -jar D:\...\target\xxx.jar`），或用 `--app-dir`（uvicorn）等不依赖当前目录的方式。
- **保留字**：MySQL 8+ 中 `rank` 为保留字，SQL 需写成 `` `rank` ``。
- **下载源**：archive.apache.org 很慢，Maven 用 dlcdn.apache.org；MySQL 版本须到 dev.mysql.com 下载页查当前版本（2026-08 为 26.7.0）。
- **Apifox**：MCP 服务只读（无法直接写线上文档）；接口定义以 `docs/openapi/career-core-apis.yaml` 供导入。
- **Apifox CLI 与开发环境排坑**：涉及 Apifox CLI 拉取/整理接口（中文乱码、uv 托管解释器、`.ps1`/`.cmd` 调用、PowerShell 转义等）时，先读 `docs/Apifox-CLI与开发环境排坑记录.md`。要点速记：
  - CLI 真实路径用 `C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd`（Python 跨进程调用必须用 `.cmd`，`.ps1` 无法被 subprocess 执行）。
  - CLI 输出落盘用 `cmd /c "... > file"` 保留原始 UTF-8 字节，避免 PowerShell 按 GBK 解码致中文乱码；Python 侧 `subprocess.run(...).stdout.decode("utf-8-sig")`。
  - Python 解释器由 uv 托管（PEP 668），**不要 `pip install`**；脚本优先纯标准库（json/subprocess），运行用 `& "C:/Users/uio8k/.local/bin/python.exe" script.py`。
  - 生成含反引号的 Markdown 文档用 Python f-string，不要在 PowerShell 字符串插值里混用 `` `$ `` 转义。
  - 全量接口清单：`docs/openapi/career-core-apis-live-summary.md`；可复跑脚本：`docs/scripts/organize_apifox_apis.py`（项目 ID 8662286，主分支 127 个接口）。
  - **`test-case run` 不替换 path 变量（2026-08-15 坑）**：CLI 2.2.9 直接跑单接口测试用例（`apifox test-case run <id> -e <envId>`）时，URL 中 `{taskId}` 不会被替换成实际值（生成的 collection 里 `url.variable` 为空、URL 仍为字面 `{taskId}`），且 `--env-var` / `--global-var` / `--variables` 注入均不生效 → 请求打到字面路径返回 400。**可靠跑法**：先 `apifox test-case run <id> -e <envId> -r json` 生成报告 → 从报告 JSON 提取 `collection` 段另存 → 用 Python 把 URL path 中的 `{taskId}` 替换为实际值（`url.variable` 置空）→ 再 `apifox run <collection.json> -r cli,json` 执行（真实 URL 200 OK）。参考脚本 `docs/scripts/fix_collection_task.py`；示例用例「更新任务-正向 (PATCH /tasks/{taskId})」id=404389855，本地环境 47907998。
- **无 winget / Docker**：安装软件一律手动下载 ZIP/安装包解压配置。

## 8. 开发约定（每次回答都要遵守）

- **接口与鉴权**：统一前缀 `/api/v1`；Demo 无登录态，`studentId` 为可选参数、缺省取 1001；涉及接口改动前先对照 `docs/接口设计.md` 与 `docs/openapi/career-core-apis.yaml`。
- **连续斜杠 `//` 处理**：Apifox 契约测试在路径变量为空时会请求带连续斜杠的 URL（如 `POST /api/v1/profile-snapshots//feedback`），Spring PathPattern 无法匹配空路径段（返回 404「接口不存在」）。全局已用 `career-core` 的 `com.career.core.common.PathNormalizeFilter`（OncePerRequestFilter + HttpServletRequestWrapper）把请求路径中的连续斜杠折叠为单斜杠；涉及「路径参数可为空」的接口改动时，需同时为该路径补充兜底路由（如 `{"/profile-snapshots/feedback", "/profile-snapshots/{snapshotId:.*}/feedback"}`），并让空 id 落到「最新一条」逻辑，保证 200 + 完整响应结构。
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