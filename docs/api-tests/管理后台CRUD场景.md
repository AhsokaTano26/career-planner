# 管理后台 CRUD 场景（47 步）

> 自动化测试 · 测试场景 · 生涯规划-全量回归
> 角色：ADMIN（admin / Admin@2026）
> 创建时间：2026-09-01
> 适用版本：career-core 当前 main 分支

## 0. 准备：环境变量

| 变量名 | 本地初始值 | 来源 |
|---|---|---|
| `adminToken` | 空 | 步骤 1 后置脚本写入（用 `pm.environment.set`） |
| `userId` | 空 | 步骤 2 提取 |
| `whitelistId` | 空 | 步骤 5 创建 |
| `relationId` | 空 | 步骤 7 提取（`pm.variables.set`） |
| `relationAdvisorId` | 空 | 步骤 7 提取（`pm.variables.set`） |
| `relationStudentId` | 空 | 步骤 7 提取（`pm.variables.set`） |
| `newTagId` | 空 | 步骤 11 创建（`pm.variables.set`） |
| `tagId` | 空 | 步骤 10 提取 |
| `directionId` | 空 | 步骤 14 创建 |
| `promptId` | 空 | 步骤 20 创建 |
| `questionnaireId` | 空 | 步骤 24 创建 |
| `versionId` | 空 | 步骤 28 创建 |
| `templateId` | 空 | 步骤 31 创建 |
| `jobId` | 空 | 步骤 35 提取 |
| `itemId` | 空 | 步骤 37 提取 |
| `exportJobId` | 空 | 步骤 44 创建 |

> ⚠️ 不要复用 `bearerToken` / `studentToken` / `advisorToken`，**新建 `adminToken`** 避免变量名错位坑。

---

## A. 删除/清理旧场景（如有）

1. 自动化测试 → 测试场景 → 找到「管理后台 CRUD」（旧版本如有）→ 删除
2. 新建测试场景 → 命名「**管理后台 CRUD**」

---

## B. 47 步逐一配置

> 每一步固定配置：
> - **Auth 标签** → BearerAuth → Token = `{{adminToken}}`
> - 步骤右侧「···」→「同步设置」→「**手动同步**」
> - 步骤 1 同步设置同样切「手动同步」

---

### 步骤 1 · 管理员登录 · Auth: BearerAuth(`{{adminToken}}`)
- 导入接口：搜「登录」→ `POST /api/v1/auth/login`
- Body：
  ```json
  { "account": "admin", "password": "Admin@2026" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("登录成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && json.data.accessToken) {
      pm.environment.set("adminToken", json.data.accessToken);
      console.log("✅ adminToken 已写入,长度:", json.data.accessToken.length);
      console.log("✅ 角色:", json.data.user ? json.data.user.role : "?");
  } else { console.log("❌ 未拿到 accessToken"); }
  ```

---

### 步骤 2 · 用户列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/users?role=STUDENT&page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看用户列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("至少返回一个 STUDENT", () => {
      pm.expect(json.data && Array.isArray(json.data.list)).to.eql(true);
      pm.expect(json.data.list.length).to.be.above(0);
  });
  const student = (json.data && json.data.list) ? json.data.list.find(u => u.role === "STUDENT") : null;
  if (student) {
      pm.environment.set("userId", student.id);
      console.log("✅ 选用用户:", student.id, student.username, "状态:", student.status);
  } else { console.log("❌ 没找到 STUDENT"); }
  ```

---

### 步骤 3 · 更新用户（保持 ACTIVE）· Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/users/{{userId}}`
- 路径参数：`userId` = `{{userId}}`
- Body：
  ```json
  { "status": "ACTIVE" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新用户成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 4 · 白名单列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/whitelist?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看白名单成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("白名单非空", () => pm.expect((json.data && json.data.list || []).length).to.be.above(0));
  console.log("✅ 白名单数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 5 · 新增白名单 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/whitelist`
- Body：
  ```json
  { "studentNo": "2026011999", "className": "计科2602", "verifyCode": "Test2026" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新增白名单成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && json.data.id) {
      pm.environment.set("whitelistId", json.data.id);
      console.log("✅ 新白名单ID:", json.data.id, "学号:", json.data.studentNo);
  } else { console.log("❌ 未拿到新白名单ID"); }
  ```

---

### 步骤 6 · 删除白名单 · Auth: BearerAuth(`{{adminToken}}`)
- `DELETE /api/v1/admin/whitelist/{{whitelistId}}`
- 路径参数：`whitelistId` = `{{whitelistId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("删除白名单成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 7 · 关系列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/relations?page=1&size=20`
- 后置脚本：
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

---

### 步骤 8 · 解除关系 · Auth: BearerAuth(`{{adminToken}}`)
> ⚠️ 顺序调整为「先解除再批量重建」：DataInitializer 启动时已预建 S1005↔S1003/S1004
> 关系，而批量建关系的后端会**跳过已存在关系**（返回 0 条）。必须先软删一条，
> 步骤 9 再重建才会走"恢复旧关系"分支返回 ≥1。用 `pm.variables` 传递跨步变量
> （`pm.environment` 跨步骤读取不稳定，见排坑记录）。
> ⚠️ 幂等键：DELETE 同样需要唯一幂等键（Service 层强制校验非空），否则命中缓存
> 不执行实际删除。
- `DELETE /api/v1/admin/relations/{{relationId}}`
- 路径参数：`relationId` = `{{relationId}}`
- 前置脚本（Pre-request Script）：
  ```javascript
  const uniqueKey = "del-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
  pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("解除关系成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 9 · 批量建立关系 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/relations`
- > ⚠️ 幂等键必须传且每次唯一：`IdempotencyService` 要求 `Idempotency-Key` 非空
  > （`required=false` 只是 Controller 层，Service 层强制校验）。`{{$uuid}}` 在 Apifox
  > Runner 中可能不解析（存为字面量导致幂等缓存），改用前置脚本生成真正 UUID。
- 前置脚本（Pre-request Script）：
  ```javascript
  // 生成唯一幂等键，避免 {{$uuid}} 未解析导致幂等缓存
  const uniqueKey = "rel-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
  pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
  ```
- Body：
  ```json
  { "advisorId": "{{relationAdvisorId}}", "studentIds": ["{{relationStudentId}}"] }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("批量建关系成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("返回关系数>=1", () => pm.expect((json.data || []).length).to.be.above(0));
  ```

---

### 步骤 10 · 能力标签列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/abilities?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看能力标签成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("至少有一条能力", () => pm.expect((json.data && json.data.list || []).length).to.be.above(0));
  if (json.data && json.data.list && json.data.list.length > 0) {
      pm.environment.set("tagId", json.data.list[0].id);
      console.log("✅ 选用能力标签ID:", json.data.list[0].id, "名称:", json.data.list[0].name);
  }
  ```

---

### 步骤 11 · 新增能力标签 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/abilities`
- > ⚠️ `id`（标签编码,主键）为必填,后端校验缺失会返回 VALIDATION_ERROR;
  > 用时间戳保证每次跑不与其他运行冲突(重跑/并行都不撞主键)。
- Body：
  ```json
  { "id": "test_ability_{{$timestamp}}", "name": "测试能力_自动", "category": "测试", "status": "ACTIVE" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新增能力标签成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && json.data.id) {
      pm.variables.set("newTagId", json.data.id);
      console.log("✅ 新能力标签ID:", json.data.id);
  }
  ```

---

### 步骤 12 · 更新能力标签 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/abilities/{{tagId}}`
- 路径参数：`tagId` = `{{tagId}}`
- Body：
  ```json
  { "status": "ACTIVE" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新能力标签成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 13 · 方向列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/directions?status=PUBLISHED&page=1&size=20`
- > ⚠️ 方向 status 枚举为 DRAFT/PUBLISHED/DISABLED（非 ACTIVE），
  > 传 ACTIVE 会触发 VALIDATION_ERROR。
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看方向列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 方向数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 14 · 新增方向 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/directions`
- > ⚠️ `path` 必须是 `graduate/employment/overseas` 枚举值；`target` 六维（interest/values/ability/academic/tendency/practice）必填且 0-100。
- Body：
  ```json
  {
    "id": "dir_test_auto",
    "name": "测试方向_自动",
    "path": "employment",
    "intro": "自动化测试用方向,可随时停用",
    "target": {
      "interest": 60, "values": 70, "ability": 65,
      "academic": 55, "tendency": 75, "practice": 50
    }
  }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新增方向成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && (json.data.id || json.data.code)) {
      pm.environment.set("directionId", json.data.id || json.data.code);
      console.log("✅ 新方向ID:", json.data.id || json.data.code);
  } else { console.log("❌ 未拿到方向ID,resp:", JSON.stringify(json.data).substring(0,150)); }
  ```

---

### 步骤 15 · 更新方向 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/directions/{{directionId}}`
- 路径参数：`directionId` = `{{directionId}}`
- > ⚠️ 幂等键必须传且每次唯一（Service 层强制校验），否则命中缓存返回 INTERNAL_ERROR。
- 前置脚本（Pre-request Script）：
  ```javascript
  const uniqueKey = "upd-dir-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
  pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
  ```
- Body：
  ```json
  { "name": "测试方向_已更新", "intro": "已被自动化测试更新过" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新方向成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 16 · 启停方向 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/directions/{{directionId}}/status`
- 路径参数：`directionId` = `{{directionId}}`
- > ⚠️ 幂等键必须传且每次唯一。
- 前置脚本（Pre-request Script）：
  ```javascript
  const uniqueKey = "sts-dir-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
  pm.request.headers.upsert({ key: "Idempotency-Key", value: uniqueKey });
  ```
- Body：
  ```json
  { "status": "DISABLED" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("启停方向成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 17 · 模型配置列表（正确路径）· Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/model-configs`
- > ⚠️ **必须用 Apifox 里这个正确的路径**（OAS 写的 `/admin/models` 是错的，404）
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看模型配置成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("配置非空", () => pm.expect((json.data || []).length).to.be.above(0));
  console.log("✅ 模型配置项:", (json.data || []).map(c => c.configKey).join(", "));
  ```

---

### 步骤 18 · 更新模型配置（PUT 不是 PATCH）· Auth: BearerAuth(`{{adminToken}}`)
- `PUT /api/v1/admin/model-configs/llm.provider`
- 路径参数：`configKey` = `llm.provider`（字面量）
- Body：
  ```json
  { "configValue": "deepseek" }
  ```
- > ⚠️ 用 Apifox 里的 PUT，不要用 OAS 里的 PATCH `/admin/models/{key}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新模型配置成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 19 · 提示词版本列表（正确路径）· Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/prompts?scene=recommendation_explain`
- > ⚠️ 路径用 `/admin/prompts`，不是 OAS 写的 `/admin/models/prompts`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看提示词成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 提示词版本数:", (json.data || []).length);
  ```

---

### 步骤 20 · 新建提示词版本 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/prompts`
- > ⚠️ `version` 必须唯一（同场景下不允许重复），用 `{{$timestamp}}` 保证每次运行生成新版本。
- Body：
  ```json
  {
    "scene": "recommendation_explain",
    "version": "P_AUTO_{{$timestamp}}",
    "content": "自动化测试新建的提示词版本,可随时发布。"
  }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新建提示词成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && json.data.id) {
      pm.environment.set("promptId", json.data.id);
      console.log("✅ 新提示词ID:", json.data.id, "版本:", json.data.version);
  } else { console.log("❌ 未拿到 promptId,resp:", JSON.stringify(json.data).substring(0,150)); }
  ```

---

### 步骤 21 · 发布提示词 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/prompts/{{promptId}}/publish`
- 路径参数：`promptId` = `{{promptId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("发布提示词成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 22 · 提示词场景列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/prompts/scenes`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看提示词场景成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 场景列表:", JSON.stringify(json.data));
  ```

---

### 步骤 23 · 问卷列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/questionnaires?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看问卷列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 问卷数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 24 · 新建问卷 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/questionnaires`
- Body：
  ```json
  {
    "type": "holland",
    "name": "测试问卷_自动",
    "typeName": "霍兰德测试",
    "minutes": 5,
    "tip": "自动化测试创建"
  }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新建问卷成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && (json.data.id || json.data.code)) {
      pm.environment.set("questionnaireId", json.data.id || json.data.code);
      console.log("✅ 新问卷ID:", json.data.id || json.data.code);
  } else { console.log("❌ 未拿到 questionnaireId"); }
  ```

---

### 步骤 25 · 更新问卷 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/questionnaires/{{questionnaireId}}`
- 路径参数：`questionnaireId` = `{{questionnaireId}}`
- Body：
  ```json
  { "name": "测试问卷_已更新" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新问卷成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 26 · 问卷版本列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/questionnaires/{{questionnaireId}}/versions`
- 路径参数：`questionnaireId` = `{{questionnaireId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看问卷版本成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("至少 1 个版本", () => pm.expect((json.data || []).length).to.be.above(0));
  if (json.data && json.data.length > 0) {
      pm.environment.set("versionId", json.data[0].id || json.data[0].version);
      console.log("✅ 选用版本ID:", json.data[0].id || json.data[0].version);
  }
  ```

---

### 步骤 27 · 问卷版本详情 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/questionnaires/{{questionnaireId}}/versions/{{versionId}}`
- 路径参数：`questionnaireId` = `{{questionnaireId}}`, `versionId` = `{{versionId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("查版本详情成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 28 · 新建问卷版本 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/questionnaires/{{questionnaireId}}/versions`
- 路径参数：`questionnaireId` = `{{questionnaireId}}`
- Body：
  ```json
  { "version": "V2_AUTO", "changeNote": "自动化测试新建版本 V2" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新建问卷版本成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && (json.data.id || json.data.version)) {
      pm.environment.set("versionId", json.data.id || json.data.version);
      console.log("✅ 新版本ID:", json.data.id || json.data.version);
  } else { console.log("❌ 未拿到 versionId"); }
  ```

---

### 步骤 29 · 发布问卷版本 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/questionnaires/{{questionnaireId}}/versions/{{versionId}}/publish`
- 路径参数：`questionnaireId` = `{{questionnaireId}}`, `versionId` = `{{versionId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("发布版本成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 30 · 任务模板列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/templates?directionId={{directionId}}&page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看模板列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 模板数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 31 · 新增任务模板 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/templates`
- > ⚠️ `id`、`name`、`directionId` 三个字段必填；`directionId` 对应的方向必须存在。使用步骤14创建的方向。
- Body：
  ```json
  {
    "id": "tpl_test_auto",
    "directionId": "{{directionId}}",
    "name": "测试模板_自动",
    "goalSummary": "自动化测试用模板,验证 CRUD 通畅"
  }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("新增模板成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && (json.data.id || json.data.code)) {
      pm.environment.set("templateId", json.data.id || json.data.code);
      console.log("✅ 新模板ID:", json.data.id || json.data.code);
  } else { console.log("❌ 未拿到 templateId"); }
  ```

---

### 步骤 32 · 更新任务模板 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/templates/{{templateId}}`
- 路径参数：`templateId` = `{{templateId}}`
- Body：
  ```json
  { "name": "测试模板_已更新" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新模板成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 33 · 权重配置 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/weights`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看权重成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 当前权重版本:", json.data && json.data.version);
  ```

---

### 步骤 34 · 更新权重 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/weights`
- > ⚠️ `version` 有唯一索引，重复会 STATE_CONFLICT。用 `{{$timestamp}}` 保证每次运行生成新版本。
- Body：
  ```json
  {
    "version": "W_AUTO_{{$timestamp}}",
    "weights": {
      "interest": 0.30, "values": 0.20, "ability": 0.20,
      "academic": 0.10, "tendency": 0.10, "practice": 0.10
    },
    "minConfidence": 0.6,
    "topN": 5
  }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("更新权重成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 35 · 培养方案导入任务列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/curricula/jobs?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看任务列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("至少 1 个任务", () => pm.expect((json.data && json.data.list || []).length).to.be.above(0));
  if (json.data && json.data.list && json.data.list.length > 0) {
      pm.environment.set("jobId", json.data.list[0].id);
      console.log("✅ 选用 jobId:", json.data.list[0].id, "状态:", json.data.list[0].status);
  } else { console.log("❌ 任务列表为空"); }
  ```

---

### 步骤 36 · 导入任务详情 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/curricula/jobs/{{jobId}}`
- 路径参数：`jobId` = `{{jobId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("查任务详情成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 37 · 待审核课程列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/curricula/items?jobId={{jobId}}&page=1&size=20`
- Query 参数：`jobId` = `{{jobId}}`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看课程列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("至少 1 门课程", () => pm.expect((json.data && json.data.list || []).length).to.be.above(0));
  if (json.data && json.data.list && json.data.list.length > 0) {
      pm.environment.set("itemId", json.data.list[0].id);
      console.log("✅ 选用 itemId:", json.data.list[0].id, "课程:", json.data.list[0].courseName);
  } else { console.log("❌ 课程列表为空"); }
  ```

---

### 步骤 38 · 校核单条课程 · Auth: BearerAuth(`{{adminToken}}`)
- `PATCH /api/v1/admin/curricula/items/{{itemId}}`
- 路径参数：`itemId` = `{{itemId}}`
- Body：
  ```json
  { "status": "APPROVED", "credits": 3.0 }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("校核课程成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  ```

---

### 步骤 39 · 批量校核 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/curricula/items/batch`
- Body：
  ```json
  { "actions": [ { "itemId": "{{itemId}}", "action": "APPROVE" } ] }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("批量校核成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  pm.test("返回>=1 条", () => pm.expect((json.data || []).length).to.be.above(0));
  ```

---

### 步骤 40 · 培养方案版本列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/curricula/versions?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看方案版本成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 方案版本数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 41 · 发布培养方案 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/curricula/publish`
- Body：
  ```json
  { "jobId": "{{jobId}}", "name": "测试方案_自动", "major": "计算机类" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("发布方案成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 新发布方案ID:", json.data && json.data.id);
  ```

---

### 步骤 42 · AI 调用日志 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/logs/ai?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看 AI 日志成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ AI 调用日志数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 43 · 操作审计日志 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/logs/operations?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看审计日志成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 审计日志数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 44 · 创建导出任务 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/admin/exports`
- Body：
  ```json
  { "type": "OPERATION_LOG", "scope": "自动化测试导出-最近一周" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("创建导出任务成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  if (json.data && json.data.id) {
      pm.environment.set("exportJobId", json.data.id);
      console.log("✅ 导出任务ID:", json.data.id, "状态:", json.data.status);
  } else { console.log("❌ 未拿到 exportJobId"); }
  ```

---

### 步骤 45 · 导出任务列表 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/exports?page=1&size=20`
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("看导出列表成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("✅ 导出任务数:", (json.data && json.data.total) || 0);
  ```

---

### 步骤 46 · 下载导出文件 · Auth: BearerAuth(`{{adminToken}}`)
- `GET /api/v1/admin/exports/{{exportJobId}}/download`
- 路径参数：`jobId` = `{{exportJobId}}`
- 后置脚本：
  ```javascript
  pm.test("下载返回 200", () => pm.response.to.have.status(200));
  pm.test("返回二进制流", () => pm.expect(pm.response.headers.get("Content-Type")).to.match(/octet-stream|excel|csv/));
  ```

---

### 步骤 47 · 管理员重置密码 · Auth: BearerAuth(`{{adminToken}}`)
- `POST /api/v1/auth/password/reset`
- Body：
  ```json
  { "studentNo": "2026011399", "newPassword": "Reset2026!", "reason": "自动化测试重置" }
  ```
- 后置脚本：
  ```javascript
  const json = pm.response.json();
  pm.test("重置密码成功 code=OK", () => pm.expect(json.code).to.eql("OK"));
  console.log("⚠️ 注意:S1004(2026011399)密码已重置为 Reset2026!,下次学生登录流程可能需要更新");
  ```

---

## C. 跑前自检清单

- [ ] 环境变量新增 `adminToken`（空值）
- [ ] 步骤 1 脚本写入 `adminToken`（不是 bearerToken）
- [ ] **所有 47 步** Auth = BearerAuth(`{{adminToken}}`)
- [ ] 所有「···」→「同步设置」→「手动同步」
- [ ] 跳过 OAS 错的 5 个接口（已不包含在 47 步里）
- [ ] 跳过了 2 个 multipart 上传（whitelist/import、curricula/import）

## D. 运行 + 加进总套件

1. 场景页右上方「运行」▶
2. 全 47 步应绿（code=OK）
3. 加进「**生涯规划-全量回归**」套件

## E. 跑完后的已知副作用（可恢复）

| 步骤 | 副作用 | 恢复 |
|---|---|---|
| 5 | 新增白名单 `2026011999` | 步骤 6 已删 ✓ |
| 8+9 | 关系 S1005↔学生 先软删后恢复 | 步骤 9 恢复 ✓（走 restore 分支,关系 ID 不变） |
| 11 | 新增能力标签 `test_ability_<时间戳>` | 留着无害 |
| 14 | 新增方向 `dir_test_auto` | 步骤 16 已 DISABLED |
| 20 | 新建提示词版本 `P_AUTO_TEST` | 步骤 21 已发布（如果想删需手工） |
| 24 | 新建问卷 `测试问卷_自动` | 步骤 25 更新过名字 |
| 28 | 新建问卷版本 `V2_AUTO` | 留着无害 |
| 31 | 新建任务模板 `测试模板_自动` | 步骤 32 更新过名字 |
| 41 | 发布新方案 `测试方案_自动` | 留着无害 |
| 44 | 新建导出任务 | 步骤 46 已下载 |
| **47** | **S1004(2026011399)密码改为 `Reset2026!`** | ⚠️ **需手工改回** `testverify01`，否则学生注册流程下次会失败 |

## F. 已知 OAS 差异（不影响本场景，后续清理）

跳过 5 个错的 OAS 路径：
1. `GET /api/v1/admin/models`
2. `PATCH /api/v1/admin/models/{key}`
3. `POST /api/v1/admin/models/prompts`
4. `POST /api/v1/admin/models/prompts/{promptVersionId}/publish`
5. `GET /api/v1/admin/questionnaires/{questionnaireId}/preview`

→ Apifox 里这 5 个接口**存在但 404**，别在本场景用。后续用 springdoc 重新生成 OAS 修复。

---

## G. 全部完成情况

跑通后，覆盖接口数：**47（管理后台）+ 1（密码重置）= 48 个**
+ 之前 12 个场景的 ~80 个
= **项目 ~128/130 接口全覆盖** 🎯
