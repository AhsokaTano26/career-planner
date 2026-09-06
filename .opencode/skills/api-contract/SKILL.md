---
name: api-contract
description: Use when adding or changing any /api/v1 interface, DTO response shape, or before syncing docs to Apifox.
---

# API Contract Skill

## 改接口前必对照（按序）

1. `docs/接口设计.md`（业务口径）
2. `docs/openapi/career-core-apis.yaml`（契约源，Apifox 导入用它）
3. `docs/本地实现vs线上核对报告.md`（返回结构改动时必对）

## 契约红线

- 前缀 `/api/v1`；除登录外全接口 `Authorization: Bearer <token>`；登录 `POST /api/v1/auth/login`。
- 成功响应包装 `{code,message,data,traceId,timestamp}`，`code=OK`。
- 业务对象字段禁裸 `null`（Apifox 校验失败，已踩坑：`ProfileSnapshotDto.feedback`）：DTO/record 加 `@JsonInclude(JsonInclude.Include.NON_NULL)`，空态用 `""`/`[]`/默认值代替 `null`。
- 枚举/状态变更、字段增删必须同步改 yaml + 核对报告，不得只改 Java。

## 同步 Apifox

Apifox MCP 只读，线上文档写入口是 yaml 导入；CLI 批量整理用 `docs/scripts/organize_apifox_apis.py`（项目 8662286），`test-case run` 的 path 变量坑见 `apifox-cli` Skill。
