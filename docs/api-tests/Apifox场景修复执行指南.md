# Apifox「基础 CRUD」场景修复执行指南

> 对应报告：`基础CRUD2.html`（2026-09-02 18:24:54）
> 剩余失败：2 个断言（批量建立关系 + 新增能力标签）
> 后端修复已部署验证通过，以下为 Apifox 云端场景的同步操作

---

## 修复 1：步骤 7 后置脚本 —— 补充变量提取

**问题**：步骤 8/9 需要 `{{relationAdvisorId}}` 和 `{{relationStudentId}}`，但当前后置脚本未提取这两个变量。

**操作**：
1. 打开 Apifox → 自动化测试 → 测试场景 → 「基础 CRUD」
2. 点击 **步骤 7（辅导员学生关系）** → 右侧「后置操作」标签
3. 将「后置脚本」**整段替换**为以下代码：

```javascript
const json = pm.response.json();
pm.test("看关系列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
pm.test("关系非空", () => pm.expect((json.data && json.data.list || []).length).to.be.above(0));
if (json.data && json.data.list && json.data.list.length > 0) {
    const rel = json.data.list[0];
    pm.variables.set("relationId", rel.id);
    pm.variables.set("relationAdvisorId", rel.advisorId);
    pm.variables.set("relationStudentId", rel.studentId);
    console.log("✅ 选用关系ID:", rel.id, "辅导员:", rel.advisorId, "学生:", rel.studentId);
} else { console.log("❌ 关系列表为空"); }
```

> 关键变化：新增 `pm.variables.set("relationAdvisorId", ...)` 和 `pm.variables.set("relationStudentId", ...)` 两行。

---

## 修复 2：调换步骤 8/9 顺序 —— 先解除再重建

**问题**：当前顺序是「先批量建 → 再解除」。DataInitializer 已预建 S1005↔S1003 关系，批量建时后端跳过已存在关系返回 0 条。必须先解除再重建，走 restore 分支返回 ≥1。

**操作**：

### 2a. 将步骤 8（批量建立关系）和步骤 9（解除关系）互换位置

1. 在场景步骤列表中，按住 **步骤 8（批量建立关系）** 的拖拽手柄
2. **向下拖动到步骤 9 之后**，使其变为步骤 9
3. 原 **步骤 9（解除关系）** 自动上移为步骤 8

> 拖拽后顺序应为：... → 步骤 7（关系列表）→ 步骤 8（解除关系）→ 步骤 9（批量建立关系）→ ...

### 2b. 修改新的步骤 8（解除关系）的 URL

1. 点击 **步骤 8（解除关系）**
2. 确认 URL 为：`DELETE /api/v1/admin/relations/{{relationId}}`
3. 如果 URL 里是硬编码的 `AR-002`，改为 `{{relationId}}`

### 2c. 修改新的步骤 9（批量建立关系）的 Body

1. 点击 **步骤 9（批量建立关系）**
2. 将 Body 原内容：
   ```json
   { "advisorId": "S1005", "studentIds": ["S1003"] }
   ```
   **替换为**：
   ```json
   { "advisorId": "{{relationAdvisorId}}", "studentIds": ["{{relationStudentId}}"] }
   ```

### 2d. 确认步骤 9 的后置脚本不变

步骤 9 的后置脚本应为：

```javascript
const json = pm.response.json();
pm.test("批量建关系成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
pm.test("返回关系数>=1", () => pm.expect((json.data || []).length).to.be.above(0));
```

---

## 修复 3：步骤 11 请求体 —— 补充必填 id 字段

**问题**：`POST /api/v1/admin/abilities` 的 `id`（标签编码，主键）为必填，当前 Body 缺少该字段导致 VALIDATION_ERROR。

**操作**：
1. 点击 **步骤 11（新增能力标签）**
2. 将 Body 原内容：
   ```json
   { "name": "测试能力_自动", "category": "测试", "status": "ACTIVE" }
   ```
   **替换为**：
   ```json
   { "id": "test_ability_{{$timestamp}}", "name": "测试能力_自动", "category": "测试", "status": "ACTIVE" }
   ```

> `{{$timestamp}}` 是 Apifox 内置动态变量，每次运行生成不同的时间戳，避免主键冲突。

### 3b. 修改步骤 11 的后置脚本

将后置脚本替换为：

```javascript
const json = pm.response.json();
pm.test("新增能力标签成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
if (json.data && json.data.id) {
    pm.variables.set("newTagId", json.data.id);
    console.log("✅ 新能力标签ID:", json.data.id);
}
```

---

## 操作完成后验证

1. 确认场景步骤顺序（关键）：

```
步骤  1  登录
步骤  2  用户列表
步骤  3  更新用户
步骤  4  白名单列表
步骤  5  新增白名单
步骤  6  删除白名单
步骤  7  辅导员学生关系（列表）
步骤  8  解除关系（DELETE）          ← 原步骤 9，已上移
步骤  9  批量建立关系（POST）        ← 原步骤 8，已下移，Body 用变量
步骤 10  能力标签列表
步骤 11  新增能力标签               ← Body 已补 id 字段
步骤 12  更新能力标签
...后续步骤不变
```

2. 场景右上角点 **「运行」▶**
3. 预期结果：**12 个接口请求全部通过，17 个断言全部通过**
4. 如果仍有失败，检查：
   - 步骤 7 后置脚本里是否用了 `pm.variables.set`（不是 `pm.environment.set`）
   - 步骤 9 Body 里是否是 `{{relationAdvisorId}}` / `{{relationStudentId}}`（不是硬编码值）
   - 步骤 11 Body 里是否有 `"id": "test_ability_{{$timestamp}}"`

---

## 附：改动速查表

| 步骤 | 改动项 | 原值 | 新值 |
|------|--------|------|------|
| 7 | 后置脚本 | 仅提取 `relationId` | 新增提取 `relationAdvisorId`、`relationStudentId` |
| 8/9 | 顺序 | 先批量建后解除 | **先解除后批量建** |
| 9 | Body.advisorId | `"S1005"`（硬编码） | `"{{relationAdvisorId}}"`（变量） |
| 9 | Body.studentIds | `["S1003"]`（硬编码） | `["{{relationStudentId}}"]`（变量） |
| 11 | Body | 缺 `id` 字段 | 新增 `"id": "test_ability_{{$timestamp}}"` |

---

## 修复 4：步骤 8/9 幂等键 —— 用前置脚本生成真实 UUID

**问题**：`{{$uuid}}` 在 Apifox Runner 中可能不被解析（存为字面量 `{{$uuid}}`），导致幂等服务命中缓存返回旧响应。而直接去掉 `Idempotency-Key` 又会触发 Service 层强制校验 `VALIDATION_ERROR: "缺少 Idempotency-Key 请求头"`。**步骤 8（DELETE）和步骤 9（POST）都需要加前置脚本**，否则 DELETE 会命中缓存不执行实际删除。

**操作**：

### 4a. 步骤 8（解除关系）添加前置脚本

1. 点击 **步骤 8（解除关系）**
2. 点击右侧「前置操作」标签（或「Pre-request Script」）
3. **添加以下前置脚本**：

```javascript
const uniqueKey = "del-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
```

### 4b. 步骤 9（批量建立关系）添加前置脚本

1. 点击 **步骤 9（批量建立关系）**
2. 点击右侧「前置操作」标签（或「Pre-request Script」）
3. **添加以下前置脚本**：

```javascript
const uniqueKey = "rel-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
```

> 两个步骤用不同前缀（`del-` vs `rel-`），保证即使时间戳撞上也不会冲突。

### 更新后验证

操作完成后重新运行场景，预期：
- 步骤 8 返回 `code=OK`（实际执行了软删除）
- 步骤 9 返回 `code=OK` 且 `data` 数组长度 ≥1（实际执行了恢复）
- 全部 12 个接口请求通过，17 个断言全部通过

---

## 修复 5：步骤 13 方向列表 status 值 —— ACTIVE → PUBLISHED

**问题**：步骤 13 用 `status=ACTIVE` 查询方向列表，但后端方向模块的 status 枚举为 `DRAFT/PUBLISHED/DISABLED`（非 ACTIVE）。传 `ACTIVE` 会触发 `VALIDATION_ERROR: "status 仅支持 PUBLISHED/DISABLED/DRAFT"`。

**操作**：

1. 点击 **步骤 13（方向列表）**
2. 将 URL 从 `GET /api/v1/admin/directions?status=ACTIVE&page=1&size=20`
   **改为** `GET /api/v1/admin/directions?status=PUBLISHED&page=1&size=20`

> 方向状态机：DRAFT（草稿）→ PUBLISHED（已发布）→ DISABLED（已停用）。新建方向默认 DRAFT，步骤 16 再切到 DISABLED。

---

## 操作完成后验证

1. 确认场景步骤顺序（关键）：

```
步骤  1  登录
步骤  2  用户列表
步骤  3  更新用户
步骤  4  白名单列表
步骤  5  新增白名单
步骤  6  删除白名单
步骤  7  辅导员学生关系（列表）
步骤  8  解除关系（DELETE）          ← 有前置脚本生成幂等键
步骤  9  批量建立关系（POST）        ← 有前置脚本生成幂等键
步骤 10  能力标签列表
步骤 11  新增能力标签               ← Body 已补 id 字段
步骤 12  更新能力标签
步骤 13  方向列表                   ← status=PUBLISHED
步骤 14  新增方向                   ← path=employment + target六维
...后续步骤不变
步骤 31  新增任务模板               ← Body 已补 id 字段
...培养方案模块依赖种子数据（已在 admin-curriculum.sql 中添加）
```

2. **重启后端**（让种子数据生效）：修改了 `admin-curriculum.sql`，需要重启 career-core 才能执行新的 INSERT
3. 场景右上角点 **「运行」▶**
4. 预期结果：**47 个接口请求全部通过，58 个断言全部通过**
5. 如果仍有失败，检查：
   - 步骤 8 和步骤 9 是否都加了前置脚本（生成唯一幂等键）
   - 步骤 7 后置脚本里是否用了 `pm.variables.set`（不是 `pm.environment.set`）
   - 步骤 9 Body 里是否是 `{{relationAdvisorId}}` / `{{relationStudentId}}`（不是硬编码值）
   - 步骤 11 Body 里是否有 `"id": "test_ability_{{$timestamp}}"`
   - 步骤 13 URL 里 status 是否为 `PUBLISHED`（不是 `ACTIVE`）
   - 步骤 14 Body 里 `path` 是否为 `employment`（不是 `数据`），且包含 `target` 六维对象
   - 步骤 15 和 16 是否都加了前置脚本（生成唯一幂等键）
   - 步骤 30 URL 里 `directionId` 是否为 `{{directionId}}`（不是 `data_analysis`）
   - 步骤 31 Body 里 `directionId` 是否为 `{{directionId}}`（不是 `data_analysis`），且有 `"id": "tpl_test_auto"` 字段
   - 后端是否已重启（种子数据需要重启才能加载）

---

## 附：改动速查表

| 步骤 | 改动项 | 原值 | 新值 |
|------|--------|------|------|
| 7 | 后置脚本 | 仅提取 `relationId` | 新增提取 `relationAdvisorId`、`relationStudentId` |
| 8/9 | 顺序 | 先批量建后解除 | **先解除后批量建** |
| 8 | 前置脚本 | 无 | 新增：生成唯一幂等键 `del-{时间戳}-{随机}` |
| 9 | Body.advisorId | `"S1005"`（硬编码） | `"{{relationAdvisorId}}"`（变量） |
| 9 | Body.studentIds | `["S1003"]`（硬编码） | `["{{relationStudentId}}"]`（变量） |
| 9 | 前置脚本 | 无 | 新增：生成唯一幂等键 `rel-{时间戳}-{随机}` |
| 11 | Body | 缺 `id` 字段 | 新增 `"id": "test_ability_{{$timestamp}}"` |
| 13 | URL query | `status=ACTIVE` | `status=PUBLISHED` |
| 14 | Body.path | `"数据"`（非法） | `"employment"`（合法枚举） |
| 14 | Body.target | 缺失（必填） | 新增六维对象 `{interest,values,ability,academic,tendency,practice}` |
| 15 | 前置脚本 | 无 | 新增：生成唯一幂等键 `upd-dir-{时间戳}-{随机}` |
| 16 | 前置脚本 | 无 | 新增：生成唯一幂等键 `sts-dir-{时间戳}-{随机}` |
| 30 | URL query | `directionId=data_analysis` | `directionId={{directionId}}` |
| 31 | Body.directionId | `"data_analysis"`（不存在） | `"{{directionId}}"`（步骤14创建的方向） |
| 31 | Body | 缺 `id` 字段 | 新增 `"id": "tpl_test_auto"` |
| DB | admin-curriculum.sql | 无种子数据 | 新增 CJ-001 + IT-001/002/003 种子数据 |
