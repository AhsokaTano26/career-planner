---
name: db-migration
description: Use when changing MySQL tables, editing db SQL scripts, fixing schema startup errors, or seeding directions and questionnaires.
---

# DB Migration Skill (career_core, MySQL 8.0)

## 铁律

- 禁 MariaDB 方言：`ADD COLUMN IF NOT EXISTS` / `ADD INDEX IF NOT EXISTS` 在标准 MySQL 8 启动即报语法错。列补齐一律走 `DatabaseSchemaMigration` 的 `information_schema` 检测模式（见 `config/DatabaseSchemaMigration.java`），SQL 文件只放 `CREATE TABLE IF NOT EXISTS`。
- 表文件：`career-core/src/main/resources/db/` 下 `schema.sql`、`core-domains.sql`、`advisor*.sql`、`admin-*.sql`、`data.sql`、`seed-directions.sql`、`seed-questionnaires.sql`；`application.yml` 的 `spring.sql.init` 幂等执行，`data-locations` 已接入 seed（空库启动即有 4 方向/2 问卷/9 题）。
- 约定：snake_case、主键 `varchar(32)`、通用列 `created_at/updated_at`；保留字 `` `rank` `` 反引号。

## 本容器操作

- 一键载入：`source ~/devtools/env.sh`（含 `LD_LIBRARY_PATH` + `DB_PASSWORD`）。MySQL 用户态运行：端口 3306，socket `~/mysql-run/mysqld.sock`，库 `career_core`，账号 `career`/`$DB_PASSWORD`（缺省 `career123`）。
- 查库客户端加 `--default-character-set=utf8mb4` 防中文乱码；只读巡检优先用 `mysql-ro` MCP（账号 `career_ro`，仅 `SELECT/SHOW`），不要用 `career` 账号做 AI 直查。
- 改 SQL 后验证：重启 core 看启动日志无 `You have an error in your SQL syntax`，再 `SELECT COUNT(*)` 抽查种子（directions=4）。
