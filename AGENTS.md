# AGENTS.md — career-planner（精简版：只留构建命令、架构一行、非 obvious 约定）

> 详细环境与排坑见 `.opencode/instructions/*.md`（按需 `read`，不要全量背诵）。
> 可用 Skill（按需 `skill` 按名加载）：`backend-verify`、`db-migration`、`api-contract`、`smoke-run`、`apifox-cli`。
> 常用命令：`/smoke`、`/contract-check`、`/verify-frontend`。

## 构建 / 运行（一行版）

- 后端：`source ~/devtools/env.sh` → `career-core/` 下 `./mvnw clean package -DskipTests`（JDK17）→ `setsid` 起 jar（构建与启动分两次调用）；详见 `backend-verify` Skill。
- 冒烟：`python3 tests/smoke_api.py`（10 项）+ `python3 tests/ai_gateway_smoke.py`（20 项）；详见 `smoke-run` Skill。
- 前端：`fronted/`（注意拼写）`npm run build` + `npm run test`，要求 Node>=20。

## 架构（一行）

Spring Boot 3.3.4（`career-core`, :8080, `/api/v1`+JWT）+ Vue3（`fronted/`）+ FastAPI（`career-ai/`, :8000）+ MySQL8（:3306, `career_core`）。

## 非 obvious 约定（必守）

1. DTO 禁裸 `null`（加 `@JsonInclude(NON_NULL)`，空态用 `""`/`[]`），否则 Apifox 契约失败。
2. SQL 禁 MariaDB 方言（`IF NOT EXISTS` 列操作），列补齐走 `DatabaseSchemaMigration`；`rank` 加反引号。
3. 真密钥只放 gitignore 文件（`career-ai/.env`、`application-local.yml`），禁 `pip install`（career-ai 用 `~/devtools/python` 3.12）。
