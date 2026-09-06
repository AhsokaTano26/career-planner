---
name: backend-verify
description: Use when modifying career-core Spring Boot backend code, building with mvnw, restarting the jar, or debugging startup failures and API errors.
---

# Backend Verify Skill (career-core)

## 分层与约定（改代码前必查）

- 模块路径：`career-core/src/main/java/com/rickgao/careercore/modules/<模块>/`，每模块 = controller + dto + entity + mapper + service(+impl) + vo；Mapper XML 在 `src/main/resources/mapper/<模块>/`；公共类在 `common`/`config`/`security`。
- 接口统一前缀 `/api/v1`，JWT（`POST /api/v1/auth/login` 取 token）；响应统一 `{code,message,data,traceId,timestamp}`，`code=OK` 成功。
- 主键 `varchar(32)`（IdGenerator 生成，禁自增 bigint）；表/字段 snake_case；通用列 `created_at/updated_at`；优先沿用现有表，不轻易新增表/字段。
- MySQL 保留字（`rank` 等）SQL 必须反引号：`` `rank` ``。
- 返回字段禁裸 `null`（Apifox 契约会失败）：DTO/record 加 `@JsonInclude(JsonInclude.Include.NON_NULL)`，空态用 `""`/`[]`/默认值。所有 Demo 简化处加注释「Demo 精简点 / 后续迭代替换位置」。
- 依赖对齐：改 `pom.xml` 前对照 `.opencode/instructions/project-environment.md` 第 3 节本机 vs Docker 对齐表（JDK17 + Spring Boot 3.3.4 + mvnw 3.9.16）；改后必须同步更新该文档。

## 构建与重启闭环（Linux 容器）

1. 先探测：`ss -lnt | grep -E '8080|3306'` 确认 core/MySQL 状态；MySQL 须先行（`~/mysql-run/mysqld.sock`）。
2. 环境：`source ~/devtools/env.sh`（JAVA_HOME=JDK17，含 DB_PASSWORD/JWT_SECRET/AI_GATEWAY_API_KEY）。
3. 构建（`career-core/` 下，checkout 后先 `chmod +x mvnw`）：`./mvnw clean package -DskipTests`。
4. 重启必须与构建分两次调用（bash 工具超时会连带杀掉 `&` 后台，勿写同一条命令）：先停旧 java 进程，再 `setsid <JDK17绝对路径>/bin/java -jar <jar绝对路径> >> /tmp/core.log 2>&1 < /dev/null & disown`。core 调网关要求启动环境带 `AI_GATEWAY_API_KEY`（与 career-ai 的 `GATEWAY_API_KEY` 同值），为空则 `LlmGateway` 直接抛错。
5. JDK 版本坑：jar 以 Java17 编译（major 61），运行可用 17/21/25，但构建必须 JDK17；`Unsupported class file major version` 即 target 混入旧 class，先 `clean`。

## 异常定位

先看应用日志（后台终端输出），再回查代码；表结构问题先看 `src/main/resources/db/schema.sql`（启动幂等执行）。
