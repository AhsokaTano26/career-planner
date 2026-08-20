# 项目环境信息

> 本文件记录本机/本项目的运行环境信息，供 AI 协作时参考，避免重复探测。
> **注意：本地开发环境依赖已与 Docker 部署环境对齐（见第 3/6 节对照表）。安装或更改任何依赖后必须同步更新本文件。**

## 1. 项目位置与结构

- **项目代码根目录**：`D:\Zht20241287\career-planner`（Git 仓库根目录）
  - `fronted/` —— Vue 3 + Vite + TypeScript 前端（已实现，实际代码所在）
  - `frontend/` —— 旧空骨架目录（占位，非实际前端）
  - `career-core/` —— Spring Boot 后端（已实现：认证、画像、推荐、计划、辅导员、管理后台）
  - `career-ai/` —— FastAPI 智能服务（独立服务，未打包进线上镜像）
  - `deploy/` —— 旧版 docker-compose 骨架、nginx/ 配置、scripts/
  - `data-seed/` —— 空
  - `docs/` —— 项目文档（`接口设计.md`、`openapi/career-core-apis.yaml`、`Apifox-CLI与开发环境排坑记录.md` 等）
  - `tests/` —— `smoke_api.py`（跨服务冒烟）、`feedback.json` 等
  - `.github/workflows/docker-publish.yml` —— GitHub Action：main 分支推送后自动构建 Docker 镜像并推送到 Docker Hub
  - `.opencode/` —— opencode 项目配置（本文件所在目录 + 指令 + 技能）
- 本机无 winget、无 Docker；安装软件一律手动下载 ZIP/安装包解压配置

## 2. 操作系统与硬件

- **系统**：Microsoft Windows 10 企业版（Enterprise），版本号 **10.0.19045**（Build 19045），**64 位**（x86_64 / amd64）
- **CPU**：16 逻辑处理器
- **内存**：约 16 GB（17082138624 字节）
- **终端**：PowerShell（pwsh）

## 3. 开发工具链（本机 vs Docker 对齐表）

**对齐原则：本地配置（pom.xml / package.json 等）以 Docker 环境为准，保证 GitHub Action 构建与本地一致。**

| 工具 | 本机（实际安装） | Docker（Dockerfile / 镜像） | 说明 |
|------|-----------------|----------------------------|------|
| JDK（编译/运行） | 安装 **17.0.20**（Temurin，与 Docker 同源）、21.0.12、25.0.2 | `maven:3.9.9-eclipse-temurin-17` + `eclipse-temurin:17-jre` = **Java 17** | **完全对齐：本机默认用 JDK 17 构建/运行（已验证构建产物 major 61）** |
| Maven | **3.9.16** | Maven Wrapper（`maven-wrapper.properties` 指向 3.9.16） | 一致，构建统一用 `mvnw` |
| Spring Boot | pom 管理 **3.3.4** | 同左（Docker 读取仓库内 pom） | 一致 |
| Node.js | **v24.18.0** | `node:20-alpine` = **Node 20** | 兼容（package.json 已声明 `"engines": { "node": ">=20.0.0" }`） |
| npm | **11.16.0** | npm 10.x（随 Node 20） | 兼容 |
| Python | **3.14.6**（uv 托管） | 线上镜像不含 career-ai（无对应 Dockerfile） | 本地开发用；不影响线上 |
| MySQL | **26.7.0** Community Server | `mysql:8.4`（根 docker-compose.yml） | 大版本不同，SQL 脚本需兼容 8.4；端口均 3306 |

**JDK 路径**：
- JDK 17（默认，与 Docker 对齐）：`D:\devtools\jdk17\jdk-17.0.20+8`
- JDK 21：`D:\devtools\jdk21\jdk-21.0.12+8`
- JDK 25：`D:\devtools\jdk25\jdk-25.0.2`
- Maven：`D:\devtools\maven\apache-maven-3.9.16`
- MySQL：`D:\devtools\mysql\mysql-26.7.0-winx64`（`my.ini` 配置、`datadir=D:\devtools\mysql\data`）

## 4. 环境变量

- `JAVA_HOME`：**当前指向 JDK 25（User 级）**，但 pom 要求 Java 17。构建/运行前建议显式覆盖为 JDK 17（与 Docker 完全对齐）：
  ```powershell
  $env:JAVA_HOME='D:\devtools\jdk17\jdk-17.0.20+8'
  $env:Path='D:\devtools\jdk17\jdk-17.0.20+8\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
  ```
- `MAVEN_HOME=D:\devtools\maven\apache-maven-3.9.16`
- `DB_USERNAME` / `DB_PASSWORD`：数据库账号密码（缺省 `career` / `career123`，见 `application-local.yml`）
- `JWT_SECRET`：JWT 签名密钥（`application-local.yml` 已设缺省值；Docker 部署时由 `.env` 注入，须 ≥32 字节）
- ⚠️ **新开的终端会话可能读不到 User 级 JAVA_HOME，且默认 `java` 可能是 JDK 21**。运行 Maven/Java 前需显式设置或走绝对路径。
- ⚠️ **JDK 版本不一致会启动失败**：class file version 61.0 = Java 17、65.0 = Java 21、69.0 = Java 25。本地 jar 以 Java 17 编译（major 61），**用 JDK 17/21/25 均可运行**（17 为 Docker 运行时）。

## 5. 数据库（MySQL）

- **端口**：3306（监听 127.0.0.1）
- **库名**：`career_core`（utf8mb4）
- **应用账号**：`career` / 密码经环境变量注入（`$env:DB_PASSWORD`，缺省 `career123`）；`application-local.yml` 使用 `${DB_PASSWORD:career123}`，不硬编码入库
- **root**：空密码（仅本机）
- **手动启动**（非 Windows 服务）：
  ```powershell
  & D:\devtools\mysql\mysql-26.7.0-winx64\bin\mysqld.exe --defaults-file=D:\devtools\mysql\my.ini --console
  ```
- **客户端**：mysql 命令需加 `--default-character-set=utf8mb4`，否则中文可能乱码
- **建表/种子脚本**：`career-core/src/main/resources/db/` 下 `schema.sql`、`advisor.sql`、`admin-config.sql`、`admin-curriculum.sql`、`admin-log.sql`、`data.sql`（应用启动时由 `spring.sql.init` 幂等执行；`application.yml` 已配置 schema/data locations）
- **Docker 侧**：`mysql:8.4` 镜像，数据卷 `mysql-data`，健康检查依赖 `MYSQL_ROOT_PASSWORD`

## 6. Docker 部署环境（线上）

- **镜像仓库**：`tano26/career-planner`（Docker Hub）
- **线上域名**：`https://career-planner.tano.asia`（学生工作台 / 生涯规划系统前端）
- **CI/CD**：`.github/workflows/docker-publish.yml` → push 到 `main` 分支自动触发（也可在 GitHub Actions 页面手动 Run workflow）→ 构建镜像 → 推送 Docker Hub → 服务器拉取运行
- **Dockerfile 构建链路**：
  1. `node:20-alpine` 构建前端（`npm ci` + `npm run build` → dist 拷入 Spring Boot static）
  2. `maven:3.9.9-eclipse-temurin-17` 编译后端（`./mvnw package -DskipTests`）
  3. `eclipse-temurin:17-jre-jammy` 运行 jar（端口 8080，非 root 用户）
- **docker-compose（根目录）**：`mysql:8.4` + `app`（镜像 `tano26/career-planner:latest`），环境变量由 `.env` 注入（`MYSQL_*`、`JWT_SECRET`、`DOCKER_IMAGE`）
- **默认账号**：管理员 `admin` / `Admin@2026`；学生 `2026011301` / `202601`

## 7. 后端（career-core）运行

- **技术栈**：Spring Boot **3.3.4**（pom 管理）+ **Java 17**（pom 要求，与 Docker 一致）+ MySQL + **MyBatis** + Spring Security（JWT）+ Lombok + Validation
- **包结构**：`com.rickgao.careercore`（`modules/<模块>` 下 controller/dto/entity/mapper/service/vo；Mapper XML 在 `resources/mapper/`）
- **服务端口**：8080
- **前置依赖**：MySQL 必须先启动（3306 有监听），否则后端启动后查库失败
- **构建与启动**（jar 路径用绝对路径，避免工具丢 `cd`；与 Docker 一致用 mvnw）：
  ```powershell
  # 1) 构建（在 career-core 目录，先修正 JAVA_HOME 为 JDK 17）
  $env:JAVA_HOME='D:\devtools\jdk17\jdk-17.0.20+8'
  $env:Path='D:\devtools\jdk17\jdk-17.0.20+8\bin;D:\devtools\maven\apache-maven-3.9.16\bin;'+$env:Path
  .\mvnw.cmd clean package -DskipTests

  # 2) 启动（JDK 17 绝对路径 + jar 绝对路径）
  $env:DB_PASSWORD='career123'
  $env:JWT_SECRET='career-planner-demo-secret-key-0123456789abcdef0123456789abcdef'
  & "D:\devtools\jdk17\jdk-17.0.20+8\bin\java.exe" -jar "D:\Zht20241287\career-planner\career-core\target\career-core-0.0.1-SNAPSHOT.jar"
  ```
- **接口**：统一前缀 `/api/v1`，JWT 认证；`POST /api/v1/auth/login` 获取 token，其余接口带 `Authorization: Bearer <token>`。响应统一 `{code, message, data, traceId, timestamp}` 包装（`code=OK` 成功）。
- **`application-local.yml`**：本地配置（已 gitignore），含数据库连接 + JWT secret；`application.yml` 默认 `local` profile 加载它。Docker 部署时通过环境变量 `SPRING_DATASOURCE_*`、`JWT_SECRET` 注入。

## 8. 本机常用注意事项（避坑）

- **PowerShell 编码**：PowerShell 管道会把 curl 输出的 UTF-8 按 GBK 解码，导致中文乱码/伪空格。对比接口结果请用 `curl.exe -o 文件` 存原始字节 + `Get-FileHash` 比对，或用 `Get-Content -Encoding utf8` 读取；查库用 `mysql ... -e "SELECT HEX(...)"`。
- **JSON 请求体**：PowerShell 中 `curl -d "{\"...\"}"` 的 `\"` 不会转义，会原样发送反斜杠导致服务端 JSON 解析失败（500）。请用 `--data-binary @文件`。
- **接口返回字段一般不允许为 null**：成功响应的业务对象字段尽量不返回 `null`，否则可能触发 Apifox 契约校验失败。实现约定：
  - 给 DTO/record 加 `@JsonInclude(JsonInclude.Include.NON_NULL)` 过滤空值；
  - 空态统一为默认值/空串/空数组，而非 `null`；
  - 涉及返回结构改动时对照 `docs/本地实现vs线上核对报告.md` 与 `docs/openapi/career-core-apis.yaml`。
- **分支名带括号**：PowerShell 会把 `git ... origin/career-core(back-end)` 里的 `(back-end)` 解析成命令调用，报错 `back-end\` 不是命令。引用含括号的分支名/引用必须加引号，如 `$br = 'origin/career-core(back-end)'` 或 `git log 'origin/career-core(back-end)'`。
- **终端工具会简化命令（丢掉 `cd`）**：后台长命令（如 `java -jar`、`python -m uvicorn`）可能被工具剥离开头的 `cd <目录>`，导致相对路径（如 `target\xxx.jar`）解析失败，报 `Unable to access jarfile`。启动服务请用绝对路径，或用 `--app-dir`（uvicorn）等不依赖当前目录的方式。
- **Maven 构建**：本地与 Docker 统一用 `mvnw`（wrapper 3.9.16）；若本地用 `mvn` 也需确保 Maven 3.9.x。改 pom 后先 `clean` 再 `package`，避免 target 残留旧版本 class（如 major 69）导致 `Unsupported class file major version` 报错。
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

## 9. 开发约定（每次回答都要遵守）

- **接口与鉴权**：统一前缀 `/api/v1`；JWT 认证（Spring Security + JJWT），登录接口 `POST /api/v1/auth/login`；涉及接口改动前先对照 `docs/接口设计.md` 与 `docs/openapi/career-core-apis.yaml`。
- **分层结构**：后端在 `career-core` 的 `com.rickgao.careercore.modules.<模块>` 下，每个模块 = controller + dto + entity + mapper + service(+impl) + vo；Mapper 接口 + XML 在 `resources/mapper/<模块>/`；公共类在 `com.rickgao.careercore.common`、`config`、`security`。
- **数据库约定**：表/字段 snake_case、主键 varchar(32)（IdGenerator 生成，非自增 bigint）、通用列 `created_at/updated_at`；**优先沿用现有表结构，不轻易新增表/字段**；MySQL 保留字（如 `rank`）需反引号。
- **注释约定**：所有 Demo 简化/增强/兼容逻辑处必须用注释标明「Demo 精简点 / 后续迭代替换位置」，便于后续识别替换。
- **语言与编码**：代码与数据统一 UTF-8；与用户使用中文交流。
- **版本对齐**：修改任何依赖（pom.xml / package.json / requirements.txt）前先对照第 3 节「本机 vs Docker 对齐表」，确保本地与 Docker 一致；修改后**必须更新本文件**。

## 10. 标准工作流（改后端的固定闭环）

1. **动手前先探测状态**：确认 8080（career-core）与 3306（MySQL）是否在跑、是否需要重启。
2. **修改 → 构建 → 重启**：改完执行 `.\mvnw.cmd clean package -DskipTests`（在 `career-core` 下，JAVA_HOME 指向 JDK 17），停旧 java 进程后用 JDK 17 绝对路径启动 jar。
3. **验证**：用 `python tests\smoke_api.py` 或 curl 冒烟，报告每个接口的 HTTP 状态与结果；涉及表结构时先看 `db/schema.sql`（启动幂等执行）。
4. **异常定位**：先看应用日志（后台终端输出），再回查代码；PowerShell 中文乱码时用 `curl.exe -o 文件` 存原始字节 + `Get-Content -Encoding utf8` / `Get-FileHash` 比对。

## 11. 安全与边界

- `.vscode/mcp.json` 内含 Apifox 访问令牌，**不得写入会同步的文件或外泄**；新令牌用系统环境变量管理。
- 不要把数据库密码、API 密钥硬编码进代码或文档；`deploy/env.example`、`.env.example` 仅作示例。
- Docker Hub 凭据通过 GitHub Secrets（`DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`）注入，不得写入仓库。
- 本机无 winget / Docker，安装软件一律手动下载配置；勿尝试 `winget install` / `docker pull`。

## 12. 环境依赖有任何的安装和更改都请记得更新该文档

## 13.有任何问题请问我