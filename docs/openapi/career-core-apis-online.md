---
title: 生涯规划系统
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers: []
includes: []
search: true
code_clipboard: true
highlight_theme: darkula
headingLevel: 2
generator: "@tarslib/widdershins v4.0.30"

---

# 生涯规划系统

推荐 Recommendations 与计划 Planning 模块的接口定义。
请求参数：Demo 无登录态，统一增加可选 studentId（缺省 1001）。
返回结果：线上结构 + 增强字段（name/type/reason 等）。

Base URLs:

# Authentication

- HTTP Authentication, scheme: bearer<br/>登录/刷新后获得的短期访问令牌，格式：`Authorization: Bearer <accessToken>`

* API Key (InternalToken)
    - Parameter Name: **X-Internal-Token**, in: header. career-core 调用 career-ai 时携带的内网令牌

# 认证与账号

<a id="opIdauthRegister"></a>

## POST 学号注册

POST /api/v1/auth/register

学生凭白名单学号与校验码注册。校验码不匹配或学号不在白名单则返回 400。
注册成功后自动登录，返回令牌；`firstLogin=true` 表示首次登录，需进入隐私授权流程。

> Body Parameters

```json
{
    "studentNo": "2026011309",
    "name": "张同学",
    "className": "计科2601",
    "verifyCode": "202609"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[RegisterRequest](#schemaregisterrequest)| yes | 注册请求|none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "rt_9f8e7d6c5b4a...",
        "expiresIn": 7200,
        "tokenType": "Bearer",
        "firstLogin": true,
        "user": {
            "id": "S1009",
            "username": "2026011309",
            "name": "张同学",
            "role": "STUDENT",
            "consentAgreed": false
        }
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TokenResponse](#schematokenresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|

<a id="opIdauthLogin"></a>

## POST 登录

POST /api/v1/auth/login

学生用学号登录，教职工用工号登录。连续失败达到阈值将临时锁定账号。

> Body Parameters

```json
{
    "account": "2026011301",
    "password": "123456"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[LoginRequest](#schemaloginrequest)| yes | 登录请求|none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "rt_9f8e7d6c5b4a...",
        "expiresIn": 7200,
        "tokenType": "Bearer",
        "firstLogin": false,
        "user": {
            "id": "S1001",
            "username": "2026011301",
            "name": "李明",
            "role": "STUDENT",
            "studentNo": "2026011301",
            "grade": "2026级",
            "majorCategory": "计算机类",
            "className": "计科2601",
            "consentAgreed": true
        }
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TokenResponse](#schematokenresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdauthRefresh"></a>

## POST 刷新访问令牌

POST /api/v1/auth/refresh

用 refreshToken 换取新的 accessToken（旧的 refreshToken 随即作废）。

> Body Parameters

```json
{
    "refreshToken": "rt_9f8e7d6c5b4a..."
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[RefreshRequest](#schemarefreshrequest)| yes | 刷新令牌请求|none|

> Response Examples

> 200 Response

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "rt_9f8e7d6c5b4a...",
  "expiresIn": 7200,
  "tokenType": "Bearer",
  "firstLogin": false,
  "user": {
    "id": "S1001",
    "username": "2026011301",
    "name": "李明",
    "role": "STUDENT",
    "studentNo": "2026011301",
    "grade": "2026级",
    "majorCategory": "计算机类",
    "className": "计科2601",
    "consentAgreed": true
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TokenResponse](#schematokenresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdauthLogout"></a>

## POST 退出登录

POST /api/v1/auth/logout

使当前访问令牌失效（服务端加入黑名单）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdauthMe"></a>

## GET 当前用户信息

GET /api/v1/auth/me

返回当前登录用户的基本信息、角色与授权状态，用于前端初始化。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "S1001",
  "username": "2026011301",
  "name": "李明",
  "role": "STUDENT",
  "studentNo": "2026011301",
  "grade": "2026级",
  "majorCategory": "计算机类",
  "className": "计科2601",
  "consentAgreed": true
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CurrentUser](#schemacurrentuser)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdauthChangePassword"></a>

## PATCH 修改密码

PATCH /api/v1/auth/me/password

需提供原密码；新密码强度校验通过后更新，同时使其他端会话失效。

> Body Parameters

```json
{
    "oldPassword": "123456",
    "newPassword": "NewPass2026!"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PasswordChangeRequest](#schemapasswordchangerequest)| yes | 修改密码请求|none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdauthAdminResetPassword"></a>

## POST 管理员重置密码

POST /api/v1/auth/password/reset

仅管理员可调用。将学生密码重置为临时密码，操作写入审计日志。

> Body Parameters

```json
{
    "studentNo": "2026011399",
    "newPassword": "Temp@2026",
    "reason": "学生忘记密码"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PasswordResetRequest](#schemapasswordresetrequest)| yes | 管理员重置密码请求|none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdauthConsent"></a>

## POST 同意隐私授权

POST /api/v1/auth/privacy-consent

首次登录后必须调用。version 须与当前发布版本一致，否则拒绝；记录同意时间与 IP。
未同意前不得进入测评与规划流程（前端由路由守卫控制，后端同样校验）。

> Body Parameters

```json
{
    "version": "v1.0"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ConsentRequest](#schemaconsentrequest)| yes | 隐私授权请求|none|

> Response Examples

> 200 Response

```json
{
  "agreed": false,
  "version": "string",
  "agreedAt": "2026-08-25T09:00:00+08:00",
  "currentVersion": "v1.0",
  "currentVersionPublishedAt": "2026-08-01T00:00:00+08:00",
  "content": "隐私告知与 AI 使用说明…"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ConsentStatus](#schemaconsentstatus)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdauthConsentStatus"></a>

## GET 查询授权状态

GET /api/v1/auth/privacy-consent/status

返回当前学生是否已同意、同意版本、当前发布版本及文本摘要。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "agreed": false,
  "version": "string",
  "agreedAt": "2026-08-25T09:00:00+08:00",
  "currentVersion": "v1.0",
  "currentVersionPublishedAt": "2026-08-01T00:00:00+08:00",
  "content": "隐私告知与 AI 使用说明…"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ConsentStatus](#schemaconsentstatus)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

# 学生档案

<a id="opIdstudentGetProfile"></a>

## GET 获取我的档案

GET /api/v1/students/me

返回当前学生完整档案（基本信息、学业、兴趣、能力、价值观、经历、意向与约束）及资料完整度。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "userId": "S1001",
  "name": "李明",
  "className": "计科2601",
  "grade": "2026级",
  "majorCategory": "计算机类",
  "basic": {
    "gender": "男",
    "hometown": "重庆",
    "birthday": "2008-05-14",
    "phone": "138****6721"
  },
  "academic": {
    "math": 4,
    "english": 3,
    "programming": 2,
    "note": "高中数学较好，英语一般，编程刚起步"
  },
  "interestPrefs": [
    "编程"
  ],
  "abilitySelf": {
    "programming": 2,
    "math": 4,
    "english": 3,
    "communication": 4,
    "organization": 3
  },
  "values": [
    "成长"
  ],
  "experiences": {
    "id": "EXP-001",
    "type": "竞赛",
    "title": "数学建模校赛 · 二等奖",
    "startDate": "2026-05",
    "endDate": "2026-06",
    "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
    "attachmentUrl": "string"
  },
  "developmentIntention": "employment",
  "constraints": [
    "愿意在课余投入学习"
  ],
  "completeness": 92,
  "updatedAt": "2026-09-30T09:12:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[StudentProfile](#schemastudentprofile)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdstudentUpdateProfile"></a>

## PATCH 分步保存学生资料

PATCH /api/v1/students/me

按需提交字段，未提交的字段不覆盖。用于多步骤表单的自动保存，调用方自行合并本地草稿。

> Body Parameters

```json
{
    "academic": {
        "math": 4,
        "english": 3,
        "programming": 2
    },
    "interestPrefs": [
        "编程",
        "数学建模"
    ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[StudentProfileUpdate](#schemastudentprofileupdate)| yes | 分步保存学生资料请求（按需提交字段，空字段不覆盖）|none|

> Response Examples

> 200 Response

```json
{
  "userId": "S1001",
  "name": "李明",
  "className": "计科2601",
  "grade": "2026级",
  "majorCategory": "计算机类",
  "basic": {
    "gender": "男",
    "hometown": "重庆",
    "birthday": "2008-05-14",
    "phone": "138****6721"
  },
  "academic": {
    "math": 4,
    "english": 3,
    "programming": 2,
    "note": "高中数学较好，英语一般，编程刚起步"
  },
  "interestPrefs": [
    "编程"
  ],
  "abilitySelf": {
    "programming": 2,
    "math": 4,
    "english": 3,
    "communication": 4,
    "organization": 3
  },
  "values": [
    "成长"
  ],
  "experiences": {
    "id": "EXP-001",
    "type": "竞赛",
    "title": "数学建模校赛 · 二等奖",
    "startDate": "2026-05",
    "endDate": "2026-06",
    "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
    "attachmentUrl": "string"
  },
  "developmentIntention": "employment",
  "constraints": [
    "愿意在课余投入学习"
  ],
  "completeness": 92,
  "updatedAt": "2026-09-30T09:12:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[StudentProfile](#schemastudentprofile)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdstudentCompleteness"></a>

## GET 资料完整度明细

GET /api/v1/students/me/completeness

返回完整度总分、必填字段、缺失字段清单及各维度是否已填，供首页进度展示。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "score": 92,
  "total": 16,
  "filled": 15,
  "missing": [
    {
      "key": "academic.note",
      "name": "学业备注"
    }
  ],
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "filled": true,
      "required": true
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CompletenessDetail](#schemacompletenessdetail)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdstudentListExperiences"></a>

## GET 经历列表

GET /api/v1/students/me/experiences

列出学生已填写的经历条目（竞赛 / 项目 / 学生工作 / 志愿服务）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "EXP-001",
  "type": "竞赛",
  "title": "数学建模校赛 · 二等奖",
  "startDate": "2026-05",
  "endDate": "2026-06",
  "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
  "attachmentUrl": "string"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Experience](#schemaexperience)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdstudentCreateExperience"></a>

## POST 新增经历

POST /api/v1/students/me/experiences

新增一条经历。附件可选，multipart 上传后返回临时文件 ID 填入 attachment。

> Body Parameters

```json
{
    "type": "项目",
    "title": "暑期自学 · 小计算器程序",
    "startDate": "2026-07",
    "endDate": "2026-08",
    "description": "用 Python 完成命令行计算器。"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ExperienceRequest](#schemaexperiencerequest)| yes | 新增/修改经历请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "EXP-001",
  "type": "竞赛",
  "title": "数学建模校赛 · 二等奖",
  "startDate": "2026-05",
  "endDate": "2026-06",
  "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
  "attachmentUrl": "string"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Experience](#schemaexperience)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdstudentUpdateExperience"></a>

## PATCH 修改经历

PATCH /api/v1/students/me/experiences/{experienceId}

修改经历条目内容。

> Body Parameters

```json
{
  "type": "项目",
  "title": "暑期自学 · 小计算器程序",
  "startDate": "2026-07",
  "endDate": "2026-08",
  "description": "用 Python 完成命令行计算器。",
  "attachment": "string"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|experienceId|path|string| yes ||经历 ID|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ExperienceRequest](#schemaexperiencerequest)| yes | 新增/修改经历请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "EXP-001",
  "type": "竞赛",
  "title": "数学建模校赛 · 二等奖",
  "startDate": "2026-05",
  "endDate": "2026-06",
  "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
  "attachmentUrl": "string"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Experience](#schemaexperience)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdstudentDeleteExperience"></a>

## DELETE 删除经历

DELETE /api/v1/students/me/experiences/{experienceId}

删除经历条目（软删除）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|experienceId|path|string| yes ||经历 ID|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdstudentRequestDeletion"></a>

## POST 申请删除本人信息

POST /api/v1/students/me/deletion-request

提交删除申请，进入管理员处理流程并记录日志。系统不自动删除。

> Body Parameters

```json
{
    "reason": "本人不再使用该系统"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[DeletionRequest](#schemadeletionrequest)| yes | 申请删除本人信息|none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

# 测评

<a id="opIdassessmentListQuestionnaires"></a>

## GET 问卷列表

GET /api/v1/questionnaires

返回当前发布状态的四类测评（霍兰德兴趣 / 职业价值观 / 能力自评 / 专业认知与发展倾向）概要。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "icon": "🧩",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "minutes": 4,
  "tip": "通过你对学习与活动的偏好，刻画兴趣类型与倾向强度。",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Questionnaire](#schemaquestionnaire)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdassessmentGetQuestionnaire"></a>

## GET 问卷详情

GET /api/v1/questionnaires/{questionnaireId}

返回某问卷的当前发布版本及全部题目与选项。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|path|string| yes ||问卷 ID（类型编码）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "questionnaire": {
    "id": "holland",
    "type": "holland",
    "typeName": "霍兰德兴趣简版",
    "icon": "🧩",
    "version": "v2",
    "status": "DRAFT",
    "questionCount": 6,
    "minutes": 4,
    "tip": "通过你对学习与活动的偏好，刻画兴趣类型与倾向强度。",
    "publishedAt": "2026-09-01T00:00:00+08:00"
  },
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[QuestionnaireDetail](#schemaquestionnairedetail)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdassessmentQuestionnaireVersions"></a>

## GET 问卷版本历史

GET /api/v1/questionnaires/{questionnaireId}/versions

查看问卷的历史版本，用于结果回溯（不展示已停用版本的题目）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "QV-1002",
  "version": "v2",
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00",
  "publishedBy": "系统管理员",
  "questionCount": 6,
  "changeNote": "调整第 3 题选项措辞"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[QuestionnaireVersion](#schemaquestionnaireversion)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdassessmentCreateSession"></a>

## POST 创建测评会话

POST /api/v1/assessment-sessions

开始一次测评。传 resumeSessionId 可续答未完成的会话（不新建）。

> Body Parameters

```json
{
    "questionnaireId": "holland"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[CreateSessionRequest](#schemacreatesessionrequest)| yes | 创建测评会话|none|

> Response Examples

> 200 Response

```json
{
  "id": "AS-20260901-001",
  "questionnaireId": "holland",
  "questionnaireName": "霍兰德兴趣简版",
  "questionnaireVersion": "v2",
  "status": "IN_PROGRESS",
  "totalQuestions": 6,
  "answeredQuestions": 4,
  "startedAt": "2026-09-01T09:00:00+08:00",
  "updatedAt": "2026-09-01T09:05:00+08:00",
  "finishedAt": "2019-08-24T14:15:22Z"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AssessmentSession](#schemaassessmentsession)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdassessmentListSessions"></a>

## GET 我的测评会话

GET /api/v1/assessment-sessions

列出当前学生的测评会话与进度（用于续答入口）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "AS-20260901-001",
  "questionnaireId": "holland",
  "questionnaireName": "霍兰德兴趣简版",
  "questionnaireVersion": "v2",
  "status": "IN_PROGRESS",
  "totalQuestions": 6,
  "answeredQuestions": 4,
  "startedAt": "2026-09-01T09:00:00+08:00",
  "updatedAt": "2026-09-01T09:05:00+08:00",
  "finishedAt": "2019-08-24T14:15:22Z"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AssessmentSession](#schemaassessmentsession)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdassessmentGetSession"></a>

## GET 会话详情

GET /api/v1/assessment-sessions/{sessionId}

返回会话进度与已保存答案，用于断点续填。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|sessionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "AS-20260901-001",
  "questionnaireId": "holland",
  "questionnaireName": "霍兰德兴趣简版",
  "questionnaireVersion": "v2",
  "status": "IN_PROGRESS",
  "totalQuestions": 6,
  "answeredQuestions": 4,
  "startedAt": "2026-09-01T09:00:00+08:00",
  "updatedAt": "2026-09-01T09:05:00+08:00",
  "finishedAt": "2019-08-24T14:15:22Z"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AssessmentSession](#schemaassessmentsession)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdassessmentSaveAnswers"></a>

## PUT 保存 / 自动保存答案

PUT /api/v1/assessment-sessions/{sessionId}/answers

分页/分步保存答案。`requestId` 保证幂等，防止重复点击。`finished=true` 表示最后一页，服务端将执行计分并标记完成。

> Body Parameters

```json
{
    "requestId": "req-20260901-001",
    "answers": [
        {
            "questionId": "q-holland-1",
            "optionIndex": 0
        },
        {
            "questionId": "q-ability-1",
            "ratingValue": 4
        }
    ],
    "finished": false
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|sessionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[SaveAnswersRequest](#schemasaveanswersrequest)| yes | 保存/自动保存答案|none|

> Response Examples

> 200 Response

```json
{
  "id": "AS-20260901-001",
  "questionnaireId": "holland",
  "questionnaireName": "霍兰德兴趣简版",
  "questionnaireVersion": "v2",
  "status": "IN_PROGRESS",
  "totalQuestions": 6,
  "answeredQuestions": 4,
  "startedAt": "2026-09-01T09:00:00+08:00",
  "updatedAt": "2026-09-01T09:05:00+08:00",
  "finishedAt": "2019-08-24T14:15:22Z"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AssessmentSession](#schemaassessmentsession)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdassessmentSubmit"></a>

## POST 提交并计分

POST /api/v1/assessment-sessions/{sessionId}/submit

提交整份问卷。计分由后端确定性规则完成（不依赖大模型），相同答案得到相同分数。

> Body Parameters

```json
{
    "requestId": "req-20260901-002",
    "answers": [
        {
            "questionId": "q-holland-1",
            "optionIndex": 0
        }
    ],
    "finished": true
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|sessionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[SaveAnswersRequest](#schemasaveanswersrequest)| yes | 保存/自动保存答案|none|

> Response Examples

> 200 Response

```json
{
  "sessionId": "AS-20260901-001",
  "status": "COMPLETED",
  "dimensionScores": [
    {
      "dimensionCode": "interest",
      "dimensionName": "兴趣",
      "score": 78
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ScoreResult](#schemascoreresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdassessmentGetScores"></a>

## GET 得分明细

GET /api/v1/assessment-sessions/{sessionId}/scores

返回已提交会话的各维度得分，用于画像计算回看。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|sessionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "sessionId": "AS-20260901-001",
  "status": "COMPLETED",
  "dimensionScores": [
    {
      "dimensionCode": "interest",
      "dimensionName": "兴趣",
      "score": 78
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ScoreResult](#schemascoreresult)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# 学生画像

<a id="opIdprofileLatest"></a>

## GET 最新画像

GET /api/v1/students/me/profile/latest

返回当前学生最新画像快照（六维得分、摘要、优势、待探索问题、完整度）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PS-1002",
  "version": 2,
  "generatedAt": "2026-09-01T09:13:00+08:00",
  "sourceVersion": "Q v2 + 自主填报 v3",
  "completeness": 92,
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "score": 78
    }
  ],
  "summary": "你的兴趣集中在技术问题求解与动手实践…",
  "strengths": [
    "数学基础较好，是算法与数据方向的加分项"
  ],
  "explore": [
    "编程实践有待积累，建议从完成小项目开始"
  ],
  "feedback": {
    "feedbackType": "MATCH",
    "comment": "学习能力描述与我实际情况基本一致"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ProfileSnapshot](#schemaprofilesnapshot)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdprofileVersions"></a>

## GET 画像版本列表

GET /api/v1/students/me/profile/versions

列出历史画像快照（每次重新计算生成新版本）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PS-1002",
  "version": 2,
  "generatedAt": "2026-09-01T09:13:00+08:00",
  "sourceVersion": "Q v2 + 自主填报 v3",
  "completeness": 92,
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "score": 78
    }
  ],
  "summary": "你的兴趣集中在技术问题求解与动手实践…",
  "strengths": [
    "数学基础较好，是算法与数据方向的加分项"
  ],
  "explore": [
    "编程实践有待积累，建议从完成小项目开始"
  ],
  "feedback": {
    "feedbackType": "MATCH",
    "comment": "学习能力描述与我实际情况基本一致"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ProfileSnapshot](#schemaprofilesnapshot)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdprofileGetSnapshot"></a>

## GET 画像快照详情

GET /api/v1/profile-snapshots/{snapshotId}

返回指定版本画像，用于历史回看与推荐结果溯源。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|snapshotId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PS-1002",
  "version": 2,
  "generatedAt": "2026-09-01T09:13:00+08:00",
  "sourceVersion": "Q v2 + 自主填报 v3",
  "completeness": 92,
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "score": 78
    }
  ],
  "summary": "你的兴趣集中在技术问题求解与动手实践…",
  "strengths": [
    "数学基础较好，是算法与数据方向的加分项"
  ],
  "explore": [
    "编程实践有待积累，建议从完成小项目开始"
  ],
  "feedback": {
    "feedbackType": "MATCH",
    "comment": "学习能力描述与我实际情况基本一致"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ProfileSnapshot](#schemaprofilesnapshot)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdprofileRefresh"></a>

## POST 重新生成画像

POST /api/v1/students/me/profile/refresh

基于最新测评得分与资料重新计算画像，生成新版本快照。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PS-1002",
  "version": 2,
  "generatedAt": "2026-09-01T09:13:00+08:00",
  "sourceVersion": "Q v2 + 自主填报 v3",
  "completeness": 92,
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "score": 78
    }
  ],
  "summary": "你的兴趣集中在技术问题求解与动手实践…",
  "strengths": [
    "数学基础较好，是算法与数据方向的加分项"
  ],
  "explore": [
    "编程实践有待积累，建议从完成小项目开始"
  ],
  "feedback": {
    "feedbackType": "MATCH",
    "comment": "学习能力描述与我实际情况基本一致"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ProfileSnapshot](#schemaprofilesnapshot)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdprofileFeedback"></a>

## POST 画像反馈

POST /api/v1/profile-snapshots/{snapshotId}/feedback

学生对画像描述反馈“符合 / 部分符合 / 不符合”。反馈仅用于后续优化，不自动改变计分。

> Body Parameters

```json
{
    "feedbackType": "PARTIAL",
    "comment": "学习能力描述与我实际情况基本一致"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|snapshotId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ProfileFeedbackRequest](#schemaprofilefeedbackrequest)| yes | 画像反馈请求|none|

> Response Examples

> 200 Response

```json
{
  "feedbackType": "MATCH",
  "comment": "学习能力描述与我实际情况基本一致"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ProfileFeedback](#schemaprofilefeedback)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# 路径与方向

<a id="opIdcareerPaths"></a>

## GET 发展路径列表

GET /api/v1/paths

返回国内升学 / 就业 / 出国留学三条一级路径。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "graduate",
  "name": "国内升学",
  "shortName": "升学",
  "description": "考研 / 保研 / 攻读计算机相关研究生"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CareerPath](#schemacareerpath)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdcareerDirections"></a>

## GET 方向列表

GET /api/v1/directions

返回已发布方向。可按路径、关键字筛选；学生视角附带是否已收藏。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|path|query|string| no ||路径过滤|
|keyword|query|string| no ||名称关键字|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维。",
  "status": "PUBLISHED",
  "updated": "2026-07-31"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[DirectionBrief](#schemadirectionbrief)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdcareerDirectionDetail"></a>

## GET 方向详情

GET /api/v1/directions/{directionId}

返回方向完整内容：简介、六维目标值、学习内容、能力要求、推荐课程、活动、发展路径、常见误区。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
  "target": {
    "interest": 70,
    "values": 70,
    "ability": 70,
    "academic": 70,
    "tendency": 70,
    "practice": 70
  },
  "minAbility": 65,
  "minAcademic": 50,
  "learning": [
    "Java / Go / Python 服务端语言"
  ],
  "abilities": [
    "编程实现"
  ],
  "courses": [
    "《Java 程序设计》《数据库原理》"
  ],
  "activities": [
    "完成 1—2 个后端小项目并部署上线"
  ],
  "pathDesc": [
    "大一大二：打好编程与数据结构基础"
  ],
  "misconceptions": [
    "后端只会 CRUD 就够了"
  ],
  "favorited": true,
  "updated": "2026-07-31",
  "status": "PUBLISHED"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Direction](#schemadirection)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdcareerCompareDirections"></a>

## GET 方向对比

GET /api/v1/directions/compare

对比两个方向的六维目标值、能力要求与核心内容（至少两个方向）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|ids|query|string| yes ||方向编码列表，逗号分隔，2 个|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "directions": [
    {
      "id": "employment_backend",
      "name": "后端开发工程师",
      "path": "graduate",
      "icon": "🖥️",
      "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
      "target": {
        "interest": 70,
        "values": 70,
        "ability": 70,
        "academic": 70,
        "tendency": 70,
        "practice": 70
      },
      "minAbility": 65,
      "minAcademic": 50,
      "learning": [
        "Java / Go / Python 服务端语言"
      ],
      "abilities": [
        "编程实现"
      ],
      "courses": [
        "《Java 程序设计》《数据库原理》"
      ],
      "activities": [
        "完成 1—2 个后端小项目并部署上线"
      ],
      "pathDesc": [
        "大一大二：打好编程与数据结构基础"
      ],
      "misconceptions": [
        "后端只会 CRUD 就够了"
      ],
      "favorited": true,
      "updated": "2026-07-31",
      "status": "PUBLISHED"
    }
  ],
  "matrix": [
    {
      "dimension": "兴趣",
      "values": {
        "1": 80,
        "2": 60
      }
    }
  ],
  "abilityCompare": [
    {
      "ability": "编程实现",
      "levels": {
        "1": "要求高",
        "2": "要求中"
      }
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[DirectionCompare](#schemadirectioncompare)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdcareerFavorites"></a>

## GET 我的收藏

GET /api/v1/students/me/favorites

返回当前学生收藏的方向列表。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维。",
  "status": "PUBLISHED",
  "updated": "2026-07-31"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[DirectionBrief](#schemadirectionbrief)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdcareerFavorite"></a>

## POST 收藏方向

POST /api/v1/students/me/favorites/{directionId}

收藏指定方向（重复收藏幂等返回成功）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维。",
  "status": "PUBLISHED",
  "updated": "2026-07-31"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[DirectionBrief](#schemadirectionbrief)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdcareerUnfavorite"></a>

## DELETE 取消收藏

DELETE /api/v1/students/me/favorites/{directionId}

取消收藏指定方向（未收藏时幂等返回成功）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

# 方向推荐

<a id="opIdrecommendationCreateRun"></a>

## POST 创建推荐批次

POST /api/v1/students/me/recommendations/runs

触发三段式推荐（规则过滤 → 结构化评分 → AI 解释）。`requestId` 幂等。
AI 不可用时自动降级：仍返回排序结果，仅解释使用规则模板。

> Body Parameters

```json
{
    "pathFilter": "employment",
    "requestId": "req-rec-001"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[CreateRecommendationRequest](#schemacreaterecommendationrequest)| yes | 创建推荐批次|none|

> Response Examples

> 200 Response

```json
{
  "runId": "190001",
  "profileVersion": 2,
  "ruleVersion": "R1.0",
  "generatedAt": "2026-09-01T09:13:30+08:00",
  "status": "RUNNING",
  "results": [
    {
      "directionId": "employment_backend",
      "rank": 1,
      "score": 82.4,
      "confidence": "HIGH",
      "reasons": [
        "偏好结构化问题求解（兴趣维度）"
      ],
      "strengths": [
        "数学与逻辑基础较好"
      ],
      "gaps": [
        "缺少系统编程实践"
      ],
      "semesterActions": [
        "完成《程序设计基础》课程"
      ],
      "feedback": {
        "feedbackType": "HELPFUL",
        "comment": "与我预期的方向基本一致"
      }
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|推荐批次（可能为 RUNNING，需轮询详情）|[RecommendationRun](#schemarecommendationrun)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdrecommendationLatest"></a>

## GET 最新推荐结果

GET /api/v1/students/me/recommendations/latest

返回最新一次推荐批次（含方向、分数、排序、可信程度、理由、优势、差距与行动建议）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "runId": "190001",
        "profileVersion": 2,
        "ruleVersion": "R1.0",
        "generatedAt": "2026-09-01T09:13:30+08:00",
        "status": "SUCCESS",
        "results": [
            {
                "directionId": "employment_backend",
                "rank": 1,
                "score": 82.4,
                "confidence": "MEDIUM",
                "reasons": [
                    "偏好结构化问题求解（兴趣维度）",
                    "重视技术成长与挑战（价值观维度）"
                ],
                "strengths": [
                    "数学与逻辑基础较好",
                    "沟通协作能力突出"
                ],
                "gaps": [
                    "缺少系统编程实践，需补足编程基础"
                ],
                "semesterActions": [
                    "完成《程序设计基础》课程",
                    "完成 1 个可运行的小项目"
                ],
                "feedback": null
            },
            {
                "directionId": "data_analysis",
                "rank": 2,
                "score": 76.8,
                "confidence": "MEDIUM",
                "reasons": [
                    "对数据分析与呈现感兴趣",
                    "数学基础较好，符合数据方向要求"
                ],
                "strengths": [
                    "数学基础较好"
                ],
                "gaps": [
                    "数据分析工具链尚未入门"
                ],
                "semesterActions": [
                    "学习 SQL 与 Python 数据分析基础"
                ],
                "feedback": null
            },
            {
                "directionId": "graduate_ai",
                "rank": 3,
                "score": 70.2,
                "confidence": "MEDIUM",
                "reasons": [
                    "对智能技术与问题求解感兴趣"
                ],
                "strengths": [
                    "数学基础较好"
                ],
                "gaps": [
                    "编程实践与英语阅读需要加强"
                ],
                "semesterActions": [
                    "完成 Python 入门"
                ],
                "feedback": null
            }
        ]
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[RecommendationRun](#schemarecommendationrun)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdrecommendationHistory"></a>

## GET 推荐批次历史

GET /api/v1/students/me/recommendations

列出历史推荐批次，可关联当时的画像版本与规则版本。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "runId": "190001",
  "profileVersion": 2,
  "ruleVersion": "R1.0",
  "generatedAt": "2026-09-01T09:13:30+08:00",
  "status": "RUNNING",
  "results": [
    {
      "directionId": "employment_backend",
      "rank": 1,
      "score": 82.4,
      "confidence": "HIGH",
      "reasons": [
        "偏好结构化问题求解（兴趣维度）"
      ],
      "strengths": [
        "数学与逻辑基础较好"
      ],
      "gaps": [
        "缺少系统编程实践"
      ],
      "semesterActions": [
        "完成《程序设计基础》课程"
      ],
      "feedback": {
        "feedbackType": "HELPFUL",
        "comment": "与我预期的方向基本一致"
      }
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[RecommendationRun](#schemarecommendationrun)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdrecommendationRunDetail"></a>

## GET 推荐批次详情

GET /api/v1/recommendation-runs/{runId}

按批次查看推荐结果；批次创建后为 RUNNING 时轮询此接口。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|runId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "runId": "190001",
  "profileVersion": 2,
  "ruleVersion": "R1.0",
  "generatedAt": "2026-09-01T09:13:30+08:00",
  "status": "RUNNING",
  "results": [
    {
      "directionId": "employment_backend",
      "rank": 1,
      "score": 82.4,
      "confidence": "HIGH",
      "reasons": [
        "偏好结构化问题求解（兴趣维度）"
      ],
      "strengths": [
        "数学与逻辑基础较好"
      ],
      "gaps": [
        "缺少系统编程实践"
      ],
      "semesterActions": [
        "完成《程序设计基础》课程"
      ],
      "feedback": {
        "feedbackType": "HELPFUL",
        "comment": "与我预期的方向基本一致"
      }
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[RecommendationRun](#schemarecommendationrun)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdrecommendationFeedback"></a>

## POST 推荐反馈

POST /api/v1/recommendation-results/{resultId}/feedback

学生对单个方向的推荐结果反馈：有帮助 / 一般 / 不符合 / 不感兴趣。反馈不立即覆盖推荐结果，仅用于优化。

> Body Parameters

```json
{
    "feedbackType": "HELPFUL",
    "comment": "与我预期的方向基本一致"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|resultId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[RecommendationFeedbackRequest](#schemarecommendationfeedbackrequest)| yes | 推荐反馈请求|none|

> Response Examples

> 200 Response

```json
{
  "feedbackType": "HELPFUL",
  "comment": "与我预期的方向基本一致"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[RecommendationFeedback](#schemarecommendationfeedback)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# 目标计划

<a id="opIdplanningGetGoals"></a>

## GET 我的目标

GET /api/v1/students/me/goals

返回当前主目标与备选目标及目标版本。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "primary": {
    "directionId": "employment_backend",
    "name": "后端开发工程师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "backup": {
    "directionId": "data_analysis",
    "name": "数据分析师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "version": "G-v3",
  "updatedAt": "2026-09-02T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Goal](#schemagoal)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdplanningSetGoal"></a>

## POST 设置 / 变更目标

POST /api/v1/students/me/goals

设置主目标（必填）与备选目标（可选）。变更时须传 changeReason，保存新版本。

> Body Parameters

```json
{
    "primaryDirectionId": "employment_backend",
    "backupDirectionId": "data_analysis",
    "changeReason": "综合课程安排重新评估"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[GoalRequest](#schemagoalrequest)| yes | 设置/变更目标|none|

> Response Examples

> 200 Response

```json
{
  "primary": {
    "directionId": "employment_backend",
    "name": "后端开发工程师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "backup": {
    "directionId": "data_analysis",
    "name": "数据分析师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "version": "G-v3",
  "updatedAt": "2026-09-02T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Goal](#schemagoal)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdplanningGoalVersions"></a>

## GET 目标版本历史

GET /api/v1/goal-versions

返回目标变更历史（含变更原因与变更人）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "version": "G-v2",
  "primaryDirectionId": "employment_backend",
  "backupDirectionId": "data_analysis",
  "changeReason": "初期更偏好前端，结合测评后调整",
  "changedAt": "2026-09-05T14:00:00+08:00",
  "changedBy": "STUDENT"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[GoalVersion](#schemagoalversion)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdplanningGenerateDraft"></a>

## POST 生成计划草案

POST /api/v1/students/me/plans/draft

基于目标方向 + 任务模板生成学期计划草案。`useAi=true` 时先调用 AI 服务，失败则回退模板。
草案未经确认不会写入正式计划。

> Body Parameters

```json
{
    "directionId": "employment_backend",
    "useAi": true,
    "requestId": "req-plan-001"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PlanDraftRequest](#schemaplandraftrequest)| yes | 生成计划草案请求|none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "goalSummary": "本学期完成后端技术基础入门：掌握 Java 语法与数据结构基础，完成 1 个可运行的小项目。",
        "semesterGoals": [
            {
                "title": "掌握 Java 基础与面向对象编程",
                "abilityTag": "programming_basic"
            },
            {
                "title": "完成数据结构与算法入门",
                "abilityTag": "algorithm_basic"
            },
            {
                "title": "完成 1 个控制台 / Web 后端小项目",
                "abilityTag": "project_basic"
            }
        ],
        "monthlyTasks": [
            {
                "month": "2026-09",
                "title": "完成 Java 语法与面向对象章节学习",
                "taskType": "LEARNING",
                "estimatedHours": 12
            },
            {
                "month": "2026-10",
                "title": "搭建开发环境，完成一个通讯录小项目",
                "taskType": "PRACTICE",
                "estimatedHours": 12
            },
            {
                "month": "2026-11",
                "title": "学习 SQL 基础并完成 20 道练习题",
                "taskType": "LEARNING",
                "estimatedHours": 8
            }
        ],
        "notes": [
            "任务可随课程安排与兴趣变化调整。"
        ]
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[PlanDraft](#schemaplandraft)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdplanningLatestPlan"></a>

## GET 最新计划

GET /api/v1/students/me/plans/latest

返回当前学生最新的正式计划（目标摘要、学期目标、月度任务、状态）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Plan](#schemaplan)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdplanningUpdatePlan"></a>

## PATCH 编辑计划

PATCH /api/v1/plans/{planId}

学生编辑计划的学期目标与月度任务，保存为新版本（不覆盖历史版本）。

> Body Parameters

```json
{
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|planId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PlanUpdate](#schemaplanupdate)| yes | 编辑计划请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Plan](#schemaplan)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdplanningConfirmPlan"></a>

## POST 确认计划

POST /api/v1/plans/{planId}/confirm

学生确认计划草案，正式生效并关联当前目标版本。确认后任务进入任务看板。

> Body Parameters

```json
{
    "confirm": true
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|planId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PlanConfirmRequest](#schemaplanconfirmrequest)| yes | 确认计划|none|

> Response Examples

> 200 Response

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Plan](#schemaplan)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdplanningPlanVersions"></a>

## GET 计划版本历史

GET /api/v1/plan-versions

返回计划版本历史（草稿、确认、编辑记录）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Plan](#schemaplan)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdplanningListTasks"></a>

## GET 任务列表

GET /api/v1/tasks

按月份 / 状态筛选当前学生的计划任务，用于任务看板。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|month|query|string| no ||月份（YYYY-MM）|
|status|query|string| no ||状态（可逗号分隔多个）|
|keyword|query|string| no ||标题关键字|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "list": [
            {
                "id": "T1",
                "month": "2026-09",
                "title": "完成 Java 语法与面向对象章节学习",
                "type": "LEARNING",
                "estHours": 12,
                "status": "DONE",
                "checkedInAt": "2026-09-20T10:00:00+08:00"
            },
            {
                "id": "T2",
                "month": "2026-09",
                "title": "LeetCode 简单题 15 道",
                "type": "LEARNING",
                "estHours": 8,
                "status": "DOING",
                "note": "已做 9 道"
            },
            {
                "id": "T6",
                "month": "2026-11",
                "title": "参加蓝桥杯报名并完成首轮模拟",
                "type": "PRACTICE",
                "estHours": 6,
                "status": "DELAYED",
                "reason": "与课程时间冲突"
            }
        ],
        "page": 1,
        "size": 20,
        "total": 6,
        "totalPages": 1
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Task](#schematask)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdplanningCreateTask"></a>

## POST 新增任务

POST /api/v1/tasks

学生手动新增计划任务。

> Body Parameters

```json
{
    "month": "2026-10",
    "title": "完成《数据结构》栈与队列的学习与练习",
    "type": "LEARNING",
    "estHours": 10
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[TaskRequest](#schemataskrequest)| yes | 新增任务请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "T1",
  "month": "2026-09",
  "title": "完成 Java 语法与面向对象章节学习",
  "type": "LEARNING",
  "estHours": 12,
  "status": "PENDING",
  "deadline": "2026-09-30",
  "abilityTags": [
    "programming_basic"
  ],
  "note": "已做 9 道，双指针方法还不熟。",
  "checkedInAt": "2019-08-24T14:15:22Z",
  "checkin": {
    "id": "TC-001",
    "taskId": "T1",
    "doneDesc": "已完成，掌握了类与对象、集合基础。",
    "gains": "理解了面向对象三大特性。",
    "difficulties": "泛型部分较抽象。",
    "proofUrl": "string",
    "checkedInAt": "2026-09-20T10:00:00+08:00"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Task](#schematask)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdplanningUpdateTask"></a>

## PATCH 更新任务

PATCH /api/v1/tasks/{taskId}

更新任务标题 / 月份 / 预计时长 / 状态。延期或放弃时 reason 必填。

> Body Parameters

```json
{
    "status": "DELAYED",
    "reason": "与课程时间冲突",
    "note": "推迟到 11 月中旬"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|taskId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[TaskStatusUpdate](#schemataskstatusupdate)| yes | 更新任务状态|none|

> Response Examples

> 200 Response

```json
{
  "id": "T1",
  "month": "2026-09",
  "title": "完成 Java 语法与面向对象章节学习",
  "type": "LEARNING",
  "estHours": 12,
  "status": "PENDING",
  "deadline": "2026-09-30",
  "abilityTags": [
    "programming_basic"
  ],
  "note": "已做 9 道，双指针方法还不熟。",
  "checkedInAt": "2019-08-24T14:15:22Z",
  "checkin": {
    "id": "TC-001",
    "taskId": "T1",
    "doneDesc": "已完成，掌握了类与对象、集合基础。",
    "gains": "理解了面向对象三大特性。",
    "difficulties": "泛型部分较抽象。",
    "proofUrl": "string",
    "checkedInAt": "2026-09-20T10:00:00+08:00"
  }
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Task](#schematask)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdplanningDeleteTask"></a>

## DELETE 删除任务

DELETE /api/v1/tasks/{taskId}

删除任务（软删除，保留审计）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|taskId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdplanningCheckin"></a>

## POST 任务打卡

POST /api/v1/tasks/{taskId}/checkin

任务完成后打卡：填写完成说明、收获、困难和可选证明材料。打卡后任务状态自动置为 DONE。

> Body Parameters

```json
{
    "doneDesc": "已完成，掌握了类与对象、集合基础。",
    "gains": "理解了面向对象三大特性。",
    "difficulties": "泛型部分较抽象。"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|taskId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[TaskCheckinRequest](#schemataskcheckinrequest)| yes | 任务打卡请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "TC-001",
  "taskId": "T1",
  "doneDesc": "已完成，掌握了类与对象、集合基础。",
  "gains": "理解了面向对象三大特性。",
  "difficulties": "泛型部分较抽象。",
  "proofUrl": "string",
  "checkedInAt": "2026-09-20T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TaskCheckin](#schemataskcheckin)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdplanningReminders"></a>

## GET 站内提醒

GET /api/v1/students/me/reminders

站内提醒列表（任务到期、复盘提醒、辅导员回复等）。首版不建设短信 / 微信提醒。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "RM-001",
  "type": "TASK_DEADLINE",
  "title": "本月还有 3 项任务待完成",
  "content": "距离月末还有 7 天，请及时更新任务状态并完成复盘。",
  "read": false,
  "createdAt": "2026-10-24T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Reminder](#schemareminder)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

# 阶段复盘

<a id="opIdreviewList"></a>

## GET 复盘列表

GET /api/v1/reviews

列出当前学生的阶段复盘记录。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdreviewCreateDraft"></a>

## POST 创建复盘草稿

POST /api/v1/reviews/drafts

按月 / 阶段创建复盘草稿，支持中途保存。

> Body Parameters

```json
{
    "cycle": "2026-09",
    "content": {
        "done": "完成 Java 语法与通讯录项目",
        "undone": "",
        "interest": "",
        "ability": "",
        "next": ""
    }
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ReviewDraftRequest](#schemareviewdraftrequest)| yes | 保存复盘草稿|none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdreviewDetail"></a>

## GET 复盘详情

GET /api/v1/reviews/{reviewId}

返回复盘内容、AI 总结、辅导员回复与状态。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdreviewSaveDraft"></a>

## PUT 保存复盘草稿

PUT /api/v1/reviews/{reviewId}/draft

保存草稿内容（不提交）。

> Body Parameters

```json
{
  "cycle": "2026-09",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  }
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ReviewDraftRequest](#schemareviewdraftrequest)| yes | 保存复盘草稿|none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdreviewSubmit"></a>

## POST 提交复盘

POST /api/v1/reviews/{reviewId}/submit

提交复盘，进入已完成状态；提交后触发 AI 总结生成（失败可稍后单独调用）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdreviewAiSummary"></a>

## POST 生成 AI 阶段总结

POST /api/v1/reviews/{reviewId}/ai-summary

调用智能服务生成阶段总结与下一阶段调整建议（同步 ≤20s，超时返回结构化模板）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "id": "R1",
        "cycle": "2026-09",
        "status": "SUBMITTED",
        "content": {
            "done": "完成 Java 语法与通讯录项目，通过四六级报名",
            "undone": "LeetCode 练习因时间不足未完成一半",
            "interest": "后端开发的兴趣比预期更强",
            "ability": "编程能力明显提升",
            "next": "10 月聚焦数据结构与 SQL"
        },
        "aiSummary": "9 月你的编程基础快速提升，兴趣与‘后端开发’高度一致；主要瓶颈是并行任务过多。",
        "aiSuggest": [
            "将任务从 6 条收敛到 3 条主线",
            "每周固定 2 次刷题时间块"
        ],
        "advisorRequested": true,
        "advisorReply": "建议 10 月聚焦数据结构主线。",
        "submittedAt": "2026-10-02T09:00:00+08:00"
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdreviewRequestGuidance"></a>

## POST 申请辅导员指导

POST /api/v1/reviews/{reviewId}/guidance-request

提交后辅导员端出现待处理标记。

> Body Parameters

```json
{
    "message": "想咨询备选目标是否需要调整"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[GuidanceRequestPayload](#schemaguidancerequestpayload)| yes | 申请辅导员指导|none|

> Response Examples

> 200 Response

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Review](#schemareview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdreviewAdoptAdvice"></a>

## POST 采纳调整建议

POST /api/v1/reviews/{reviewId}/adopt-advice

学生确认采纳 AI 调整建议，生成新的计划版本。未确认前 AI 建议不写入正式计划。

> Body Parameters

```json
{
    "adopt": true
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|reviewId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AdoptAdviceRequest](#schemaadoptadvicerequest)| yes | 采纳调整建议|none|

> Response Examples

> 200 Response

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[Plan](#schemaplan)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

# AI 智能服务

<a id="opIdaiChat"></a>

## POST 生涯咨询问答

POST /api/v1/ai/chat

回答专业认知、路径比较、学习安排和计划优化问题。优先使用经审核的方向库与规则说明。涉及重大决策 / 心理健康 / 法律 / 医疗时返回 needsHumanSupport=true。对单次输入长度、调用频率与会话保留范围做限制。首版普通响应，可后置流式。

> Body Parameters

```json
{
    "studentRef": "student_ref_8f3a",
    "sessionId": "CHAT-001",
    "question": "后端开发和数据分析师怎么选？",
    "context": {
        "directionId": "employment_backend",
        "goalSummary": "本学期入门后端基础"
    }
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ChatRequest](#schemachatrequest)| yes | 生涯咨询请求|none|

> Response Examples

> 200 Response

```json
{
  "answer": "可以从兴趣与技术栈偏好判断：…",
  "references": [
    "《Java 程序设计》"
  ],
  "needsHumanSupport": false,
  "supportReason": "涉及心理健康话题",
  "disclaimer": "智能生成，供探索参考"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ChatResponse](#schemachatresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdaiExplain"></a>

## POST 生成推荐解释

POST /api/v1/ai/recommendation/explain

将结构化评分结果转写为通俗解释。输入已脱敏（仅维度得分与方向编码）。方向 ID 必须存在于本次候选列表；禁止模型新增方向。同步调用，≤20 秒，超时返回规则模板解释。

> Body Parameters

```json
{
    "studentRef": "student_ref_8f3a",
    "ruleVersion": "R1.0",
    "profileVersion": 2,
    "profile": {
        "interest": 0.78,
        "values": 0.7,
        "ability": 0.62,
        "academic": 0.58,
        "tendency": 0.8,
        "practice": 0.45
    },
    "results": [
        {
            "directionId": "employment_backend",
            "score": 82.4,
            "rank": 1
        },
        {
            "directionId": "data_analysis",
            "score": 76.8,
            "rank": 2
        }
    ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ExplainRequest](#schemaexplainrequest)| yes | 推荐解释请求|none|

> Response Examples

> 200 Response

```json
{
  "runId": "190001",
  "explanations": [
    {
      "directionId": "employment_backend",
      "summary": "你的兴趣偏向结构化问题求解，数学基础较好…",
      "confidenceText": "数据基本完整，方向间差异不大，可信程度中等",
      "disclaimer": "智能生成，供探索参考"
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ExplainResult](#schemaexplainresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdaiPlanGenerate"></a>

## POST 生成学期计划草案

POST /api/v1/ai/plan/generate

依据目标方向、培养方案与任务模板生成学期目标与月度任务草案。任务数量设上限，避免生成几十项不可执行任务。同步或短任务轮询。

> Body Parameters

```json
{
    "studentRef": "student_ref_8f3a",
    "directionId": "employment_backend",
    "semester": "2026-1",
    "template": {
        "goalSummary": "本学期完成后端技术基础入门",
        "semesterGoals": [
            {
                "title": "掌握 Java 基础",
                "abilityTag": "programming_basic"
            }
        ],
        "monthlyTasks": [
            {
                "month": "2026-09",
                "title": "完成 Java 语法学习",
                "taskType": "LEARNING",
                "estimatedHours": 12
            }
        ],
        "notes": []
    }
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PlanGenerateRequest](#schemaplangeneraterequest)| yes | 计划生成请求|none|

> Response Examples

> 200 Response

```json
{
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[PlanGenerateResult](#schemaplangenerateresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdaiReviewSummarize"></a>

## POST 生成阶段总结

POST /api/v1/ai/review/summarize

基于复盘内容与任务完成情况生成阶段总结与下一阶段调整建议。同步调用，≤20 秒。

> Body Parameters

```json
{
    "studentRef": "student_ref_8f3a",
    "cycle": "2026-09",
    "reviewContent": {
        "done": "完成 Java 语法与通讯录项目",
        "undone": "LeetCode 练习未完成",
        "interest": "后端兴趣更强",
        "ability": "编程能力提升",
        "next": "聚焦数据结构"
    },
    "taskSummary": "完成 4/6 项任务"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ReviewSummarizeRequest](#schemareviewsummarizerequest)| yes | 复盘总结请求|none|

> Response Examples

> 200 Response

```json
{
  "summary": "9 月你的编程基础快速提升…",
  "suggestions": [
    "将任务从 6 条收敛到 3 条主线"
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ReviewSummarizeResult](#schemareviewsummarizeresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

<a id="opIdaiPdfParse"></a>

## POST 解析培养方案 PDF

POST /api/v1/ai/pdf/parse

从 PDF 中提取课程表，映射课程代码、名称、学期、学分、学时、类别、模块与先修课程。异步执行；扫描型 PDF 标记为需人工处理，不以 OCR 作为上线强依赖。返回解析状态供轮询。

> Body Parameters

```json
{
    "jobId": "CJ-001",
    "fileUrl": "http://storage.internal/uploads/cj-001.pdf",
    "filename": "软件工程培养方案2026.pdf"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PdfParseRequest](#schemapdfparserequest)| yes | PDF 解析请求|none|

> Response Examples

> 200 Response

```json
{
  "jobId": "CJ-001",
  "status": "PARSING",
  "itemCount": 172,
  "confidence": 86
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[PdfParseResult](#schemapdfparseresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|503|[Service Unavailable](https://tools.ietf.org/html/rfc7231#section-6.6.4)|智能服务超时 / 不可用，已降级或可重试（503）|None|

# AI 生涯咨询

<a id="opIdaiChatHistory"></a>

## GET 会话历史

GET /api/v1/ai/chat/history

返回当前学生 AI 咨询会话历史（默认保留近期记录）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "answer": "可以从兴趣与技术栈偏好判断：…",
  "references": [
    "《Java 程序设计》"
  ],
  "needsHumanSupport": false,
  "supportReason": "涉及心理健康话题",
  "disclaimer": "智能生成，供探索参考"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ChatResponse](#schemachatresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|

<a id="opIdaiChatFeedback"></a>

## POST 回答反馈

POST /api/v1/ai/chat/{messageId}/feedback

对某条回答反馈有帮助 / 无帮助，记录模型、提示词版本与时间。

> Body Parameters

```json
{
    "feedbackType": "HELPFUL"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|messageId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[RecommendationFeedbackRequest](#schemarecommendationfeedbackrequest)| yes | 推荐反馈请求|none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# 辅导员端

<a id="opIdadvisorListStudents"></a>

## GET 所带学生列表

GET /api/v1/advisor/students

返回辅导员所带学生列表与组合筛选（路径 / 方向 / 目标状态 / 复盘状态 / 申请指导 / 关键字）。
仅返回辅导员学生关系内的学生；访问本身不记日志，查看详情才记。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|path|query|string| no ||none|
|directionId|query|string| no ||none|
|goalStatus|query|string| no ||none|
|reviewStatus|query|string| no ||none|
|guidanceRequested|query|boolean| no ||none|
|keyword|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|goalStatus|HAS_GOAL|
|goalStatus|NO_GOAL|
|reviewStatus|LONG_NO_REVIEW|
|reviewStatus|REVIEWED_THIS_MONTH|

> Response Examples

> 200 Response

```json
{
    "code": "OK",
    "message": "success",
    "data": {
        "list": [
            {
                "id": "S1001",
                "name": "李明",
                "className": "计科2601",
                "completeness": 92,
                "assessed": true,
                "path": "employment",
                "direction": "后端开发工程师",
                "primaryGoal": "后端开发",
                "planRate": 71,
                "lastReview": "2026-10-02",
                "askGuidance": false,
                "status": "good"
            },
            {
                "id": "S1002",
                "name": "张雨",
                "className": "计科2602",
                "completeness": 78,
                "assessed": true,
                "path": "graduate",
                "direction": "计算机技术考研",
                "primaryGoal": "考研上岸",
                "planRate": 64,
                "lastReview": "2026-09-28",
                "askGuidance": true,
                "status": "review"
            }
        ],
        "page": 1,
        "size": 20,
        "total": 30,
        "totalPages": 2
    },
    "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
    "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdvisorStudent](#schemaadvisorstudent)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadvisorAttention"></a>

## GET 需关注学生

GET /api/v1/advisor/attention

主动申请指导、长期未复盘、多次调整目标的学生（组合原因）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "student": {
    "id": "S1001",
    "name": "李明",
    "className": "计科2601",
    "completeness": 92,
    "assessed": true,
    "path": "employment",
    "direction": "后端开发工程师",
    "primaryGoal": "后端开发",
    "planRate": 71,
    "lastReview": "2026-10-02",
    "askGuidance": false,
    "status": "good"
  },
  "reasons": [
    "已申请辅导员指导"
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AttentionStudent](#schemaattentionstudent)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadvisorStudentDetail"></a>

## GET 学生详情总览

GET /api/v1/advisor/students/{studentId}

查看学生画像 / 推荐 / 目标 / 计划 / 任务 / 复盘 / 指导记录。访问写入审计日志。
校验当前辅导员与该学生存在管理关系，否则 403。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|studentId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "profile": {
    "userId": "S1001",
    "name": "李明",
    "className": "计科2601",
    "grade": "2026级",
    "majorCategory": "计算机类",
    "basic": {
      "gender": "男",
      "hometown": "重庆",
      "birthday": "2008-05-14",
      "phone": "138****6721"
    },
    "academic": {
      "math": 4,
      "english": 3,
      "programming": 2,
      "note": "高中数学较好，英语一般，编程刚起步"
    },
    "interestPrefs": [
      "编程"
    ],
    "abilitySelf": {
      "programming": 2,
      "math": 4,
      "english": 3,
      "communication": 4,
      "organization": 3
    },
    "values": [
      "成长"
    ],
    "experiences": {
      "id": "EXP-001",
      "type": "竞赛",
      "title": "数学建模校赛 · 二等奖",
      "startDate": "2026-05",
      "endDate": "2026-06",
      "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
      "attachmentUrl": "string"
    },
    "developmentIntention": "employment",
    "constraints": [
      "愿意在课余投入学习"
    ],
    "completeness": 92,
    "updatedAt": "2026-09-30T09:12:00+08:00"
  },
  "portrait": {
    "id": "PS-1002",
    "version": 2,
    "generatedAt": "2026-09-01T09:13:00+08:00",
    "sourceVersion": "Q v2 + 自主填报 v3",
    "completeness": 92,
    "dimensions": [
      {
        "key": "interest",
        "name": "兴趣",
        "score": 78
      }
    ],
    "summary": "你的兴趣集中在技术问题求解与动手实践…",
    "strengths": [
      "数学基础较好，是算法与数据方向的加分项"
    ],
    "explore": [
      "编程实践有待积累，建议从完成小项目开始"
    ],
    "feedback": {
      "feedbackType": "MATCH",
      "comment": "学习能力描述与我实际情况基本一致"
    }
  },
  "recommendation": {
    "runId": "190001",
    "profileVersion": 2,
    "ruleVersion": "R1.0",
    "generatedAt": "2026-09-01T09:13:30+08:00",
    "status": "RUNNING",
    "results": [
      {
        "directionId": "employment_backend",
        "rank": 1,
        "score": 82.4,
        "confidence": "HIGH",
        "reasons": [
          "偏好结构化问题求解（兴趣维度）"
        ],
        "strengths": [
          "数学与逻辑基础较好"
        ],
        "gaps": [
          "缺少系统编程实践"
        ],
        "semesterActions": [
          "完成《程序设计基础》课程"
        ],
        "feedback": {
          "feedbackType": "[",
          "comment": "与我预期的方向基本一致"
        }
      }
    ]
  },
  "goal": {
    "primary": {
      "directionId": "employment_backend",
      "name": "后端开发工程师",
      "chosenAt": "2026-09-02T10:00:00+08:00"
    },
    "backup": {
      "directionId": "data_analysis",
      "name": "数据分析师",
      "chosenAt": "2026-09-02T10:00:00+08:00"
    },
    "version": "G-v3",
    "updatedAt": "2026-09-02T10:00:00+08:00"
  },
  "plan": {
    "id": "PLAN-1002",
    "version": "P-v2",
    "status": "DRAFT",
    "source": "AI",
    "goalSummary": "本学期完成后端技术基础入门…",
    "semesterGoals": [
      {
        "title": "掌握 Java 基础与面向对象编程",
        "abilityTag": "programming_basic"
      }
    ],
    "monthlyTasks": [
      {
        "month": "2026-09",
        "title": "完成 Java 语法与面向对象章节学习",
        "taskType": "LEARNING",
        "estimatedHours": 12
      }
    ],
    "notes": [
      "任务可随课程安排调整"
    ],
    "confirmedAt": "2026-09-02T10:05:00+08:00",
    "updatedAt": "2026-09-02T10:05:00+08:00"
  },
  "tasks": [
    {
      "id": "T1",
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "type": "LEARNING",
      "estHours": 12,
      "status": "PENDING",
      "deadline": "2026-09-30",
      "abilityTags": [
        "programming_basic"
      ],
      "note": "已做 9 道，双指针方法还不熟。",
      "checkedInAt": "2019-08-24T14:15:22Z",
      "checkin": {
        "id": "TC-001",
        "taskId": "T1",
        "doneDesc": "已完成，掌握了类与对象、集合基础。",
        "gains": "理解了面向对象三大特性。",
        "difficulties": "泛型部分较抽象。",
        "proofUrl": "string",
        "checkedInAt": "2026-09-20T10:00:00+08:00"
      }
    }
  ],
  "reviews": [
    {
      "id": "R1",
      "cycle": "2026-09",
      "status": "DRAFT",
      "content": {
        "done": "完成 Java 语法与通讯录项目，通过四六级报名",
        "undone": "LeetCode 练习因时间不足未完成一半",
        "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
        "ability": "编程能力明显提升，能独立写 300 行左右的程序",
        "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
      },
      "aiSummary": "9 月你的编程基础快速提升…",
      "aiSuggest": [
        "将任务从 6 条收敛到 3 条主线"
      ],
      "advisorRequested": true,
      "advisorReply": "string",
      "submittedAt": "2026-10-02T09:00:00+08:00"
    }
  ],
  "guidance": [
    {
      "id": "GC-001",
      "studentId": "S1001",
      "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
      "adviceType": "COMMENT",
      "suggestedTask": "每周完成 3 道 LeetCode 简单题",
      "retestReason": "string",
      "createdAt": "2026-10-03T10:00:00+08:00"
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[StudentDetailView](#schemastudentdetailview)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadvisorListGuidance"></a>

## GET 指导记录

GET /api/v1/advisor/students/{studentId}/guidance

返回某学生的历史指导意见。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|studentId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "GC-001",
  "studentId": "S1001",
  "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
  "adviceType": "COMMENT",
  "suggestedTask": "每周完成 3 道 LeetCode 简单题",
  "retestReason": "string",
  "createdAt": "2026-10-03T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[GuidanceComment](#schemaguidancecomment)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadvisorWriteGuidance"></a>

## POST 填写指导意见

POST /api/v1/advisor/students/{studentId}/guidance

填写指导意见，或给出建议任务 / 建议重新测评。只写入指导记录，不覆盖学生原始数据。

> Body Parameters

```json
{
    "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
    "adviceType": "COMMENT"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|studentId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[GuidanceCommentRequest](#schemaguidancecommentrequest)| yes | 填写指导意见|none|

> Response Examples

> 200 Response

```json
{
  "id": "GC-001",
  "studentId": "S1001",
  "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
  "adviceType": "COMMENT",
  "suggestedTask": "每周完成 3 道 LeetCode 简单题",
  "retestReason": "string",
  "createdAt": "2026-10-03T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[GuidanceComment](#schemaguidancecomment)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadvisorSuggest"></a>

## POST 提出建议任务 / 建议重新测评

POST /api/v1/advisor/students/{studentId}/advice

向学生推送建议（建议任务或建议重新测评）。学生确认后才会写入正式计划 / 生成新的测评会话。

> Body Parameters

```json
{
    "content": "建议补充一次霍兰德复测，确认兴趣是否变化。",
    "adviceType": "SUGGEST_RETEST",
    "retestReason": "方向兴趣变化较大"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|studentId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[GuidanceCommentRequest](#schemaguidancecommentrequest)| yes | 填写指导意见|none|

> Response Examples

> 200 Response

```json
{
  "id": "GC-001",
  "studentId": "S1001",
  "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
  "adviceType": "COMMENT",
  "suggestedTask": "每周完成 3 道 LeetCode 简单题",
  "retestReason": "string",
  "createdAt": "2026-10-03T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[GuidanceComment](#schemaguidancecomment)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadvisorStatistics"></a>

## GET 群体统计

GET /api/v1/advisor/statistics

路径分布、测评完成率、计划制定率、任务完成率等基础统计。不自动认定风险学生。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "totalStudents": 30,
  "assessedCount": 24,
  "planMadeCount": 20,
  "reviewedCount": 14,
  "pathDistribution": [
    {
      "path": "graduate",
      "count": 10
    }
  ],
  "taskCompletionRate": 68
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdvisorStatistics](#schemaadvisorstatistics)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

# 管理端·用户与白名单

<a id="opIdadminListUsers"></a>

## GET 用户列表

GET /api/v1/admin/users

按关键字 / 角色 / 状态分页查询用户。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|role|query|string| no ||none|
|status|query|string| no ||none|
|keyword|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|role|STUDENT|
|role|ADVISOR|
|role|ADMIN|
|status|ACTIVE|
|status|LOCKED|
|status|DISABLED|

> Response Examples

> 200 Response

```json
{
  "id": "S1001",
  "username": "2026011301",
  "name": "李明",
  "role": "STUDENT",
  "className": "计科2601",
  "status": "ACTIVE",
  "lastLoginAt": "2026-10-01T09:00:00+08:00",
  "createdAt": "2026-08-25T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminUser](#schemaadminuser)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateUser"></a>

## PATCH 更新用户

PATCH /api/v1/admin/users/{userId}

修改用户状态（锁定 / 停用）或班级。高风险操作写入审计日志。

> Body Parameters

```json
{
    "status": "LOCKED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|userId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AdminUserUpdate](#schemaadminuserupdate)| yes | 更新用户|none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminListWhitelist"></a>

## GET 白名单列表

GET /api/v1/admin/whitelist

分页查询可注册学号白名单。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|used|query|boolean| no ||none|
|keyword|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "WL-001",
  "studentNo": "2026011309",
  "className": "计科2601",
  "verifyCode": "202609",
  "used": false,
  "createdAt": "2026-08-20T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[WhitelistEntry](#schemawhitelistentry)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateWhitelist"></a>

## POST 新增白名单

POST /api/v1/admin/whitelist

单条新增白名单学号。

> Body Parameters

```json
{
    "studentNo": "2026011309",
    "className": "计科2601",
    "verifyCode": "202609"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[WhitelistCreate](#schemawhitelistcreate)| yes | 新增白名单|none|

> Response Examples

> 200 Response

```json
{
  "id": "WL-001",
  "studentNo": "2026011309",
  "className": "计科2601",
  "verifyCode": "202609",
  "used": false,
  "createdAt": "2026-08-20T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[WhitelistEntry](#schemawhitelistentry)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminImportWhitelist"></a>

## POST 批量导入白名单（CSV）

POST /api/v1/admin/whitelist/import

上传 CSV（学号, 班级, 校验码）。分批处理并返回成功 / 失败 / 错误明细；重复学号拒绝。批量操作需要二次确认。

> Body Parameters

```yaml
studentNo: "2026011309"
className: ""
verifyCode: ""

```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|object| yes ||none|
|» studentNo|body|string| yes ||学号|
|» className|body|string| no ||班级（可选）|
|» verifyCode|body|string| yes ||校验码（可由系统生成）|

> Response Examples

> 200 Response

```json
{
  "successCount": 42,
  "failCount": 3,
  "failures": [
    {
      "row": 7,
      "studentNo": "2026011399",
      "reason": "重复学号"
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[WhitelistImportResult](#schemawhitelistimportresult)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminDeleteWhitelist"></a>

## DELETE 删除白名单

DELETE /api/v1/admin/whitelist/{whitelistId}

删除白名单条目（已注册使用的条目禁止删除）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|whitelistId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdadminListRelations"></a>

## GET 辅导员学生关系

GET /api/v1/admin/relations

分页查询辅导员与学生的管理关系。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|advisorId|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "REL-001",
  "advisorId": "A2001",
  "advisorName": "王老师",
  "studentId": "S1001",
  "studentName": "李明",
  "createdAt": "2026-08-25T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdvisorRelation](#schemaadvisorrelation)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateRelations"></a>

## POST 批量建立关系

POST /api/v1/admin/relations

为一个辅导员批量绑定学生。批量操作需二次确认。

> Body Parameters

```json
{
    "advisorId": "A2001",
    "studentIds": [
        "S1001",
        "S1002",
        "S1003"
    ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[RelationRequest](#schemarelationrequest)| yes | 批量建立关系|none|

> Response Examples

> 200 Response

```json
{
  "id": "REL-001",
  "advisorId": "A2001",
  "advisorName": "王老师",
  "studentId": "S1001",
  "studentName": "李明",
  "createdAt": "2026-08-25T09:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdvisorRelation](#schemaadvisorrelation)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminDeleteRelation"></a>

## DELETE 解除关系

DELETE /api/v1/admin/relations/{relationId}

解除辅导员学生关系（解除后辅导员不可再访问该学生）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|relationId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# 管理端·配置

<a id="opIdadminListQuestionnaires"></a>

## GET 问卷管理列表

GET /api/v1/admin/questionnaires

列出所有问卷及版本状态。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "updatedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminQuestionnaire](#schemaadminquestionnaire)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateQuestionnaire"></a>

## POST 新建问卷

POST /api/v1/admin/questionnaires

创建新问卷（含题目与选项），生成 DRAFT 版本。修改已发布版本须新建版本。

> Body Parameters

```json
{
  "type": "holland",
  "name": "霍兰德兴趣简版 v2",
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AdminQuestionnaireCreate](#schemaadminquestionnairecreate)| yes | 新建问卷|none|

> Response Examples

> 200 Response

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "updatedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminQuestionnaire](#schemaadminquestionnaire)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateQuestionnaireVersion"></a>

## POST 新建问卷版本

POST /api/v1/admin/questionnaires/{questionnaireId}/versions

在现有问卷基础上创建新版本（草稿），用于发布前修改。

> Body Parameters

```json
{
  "type": "holland",
  "name": "霍兰德兴趣简版 v2",
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AdminQuestionnaireCreate](#schemaadminquestionnairecreate)| yes | 新建问卷|none|

> Response Examples

> 200 Response

```json
{
  "id": "QV-1002",
  "version": "v2",
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00",
  "publishedBy": "系统管理员",
  "questionCount": 6,
  "changeNote": "调整第 3 题选项措辞"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[QuestionnaireVersion](#schemaquestionnaireversion)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminPreviewQuestionnaire"></a>

## GET 问卷预览

GET /api/v1/admin/questionnaires/{questionnaireId}/preview

预览指定问卷内容（含 DRAFT 版本）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|path|string| yes ||none|
|version|query|string| no ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "questionnaire": {
    "id": "holland",
    "type": "holland",
    "typeName": "霍兰德兴趣简版",
    "icon": "🧩",
    "version": "v2",
    "status": "DRAFT",
    "questionCount": 6,
    "minutes": 4,
    "tip": "通过你对学习与活动的偏好，刻画兴趣类型与倾向强度。",
    "publishedAt": "2026-09-01T00:00:00+08:00"
  },
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[QuestionnaireDetail](#schemaquestionnairedetail)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminSetQuestionnaireStatus"></a>

## PATCH 发布 / 停用问卷

PATCH /api/v1/admin/questionnaires/{questionnaireId}/status

发布 DRAFT 版本上线，或停用现有版本。发布后不可直接修改已使用版本。

> Body Parameters

```json
{
    "status": "PUBLISHED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[QuestionnaireStatusUpdate](#schemaquestionnairestatusupdate)| yes | 发布/停用问卷|none|

> Response Examples

> 200 Response

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "updatedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminQuestionnaire](#schemaadminquestionnaire)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdadminListDirections"></a>

## GET 方向库管理

GET /api/v1/admin/directions

分页查询方向（含草稿 / 停用）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|path|query|string| no ||none|
|status|query|string| no ||none|
|keyword|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|status|PUBLISHED|
|status|DISABLED|
|status|DRAFT|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "status": "PUBLISHED",
  "sortOrder": 1,
  "applicableMajors": [
    "计算机类"
  ],
  "updatedAt": "2026-07-31T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminDirection](#schemaadmindirection)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateDirection"></a>

## POST 新增方向

POST /api/v1/admin/directions

新增方向（需补全六维目标值、能力要求、内容），初始为 DRAFT。

> Body Parameters

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
  "target": {
    "interest": 70,
    "values": 70,
    "ability": 70,
    "academic": 70,
    "tendency": 70,
    "practice": 70
  },
  "minAbility": 65,
  "minAcademic": 50,
  "learning": [
    "Java / Go / Python 服务端语言"
  ],
  "abilities": [
    "编程实现"
  ],
  "courses": [
    "《Java 程序设计》《数据库原理》"
  ],
  "activities": [
    "完成 1—2 个后端小项目并部署上线"
  ],
  "pathDesc": [
    "大一大二：打好编程与数据结构基础"
  ],
  "misconceptions": [
    "后端只会 CRUD 就够了"
  ],
  "favorited": true,
  "updated": "2026-07-31",
  "status": "PUBLISHED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[Direction](#schemadirection)| yes | 方向详情|none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "status": "PUBLISHED",
  "sortOrder": 1,
  "applicableMajors": [
    "计算机类"
  ],
  "updatedAt": "2026-07-31T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminDirection](#schemaadmindirection)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateDirection"></a>

## PATCH 更新方向

PATCH /api/v1/admin/directions/{directionId}

修改方向内容（生成新版本）。

> Body Parameters

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
  "target": {
    "interest": 70,
    "values": 70,
    "ability": 70,
    "academic": 70,
    "tendency": 70,
    "practice": 70
  },
  "minAbility": 65,
  "minAcademic": 50,
  "learning": [
    "Java / Go / Python 服务端语言"
  ],
  "abilities": [
    "编程实现"
  ],
  "courses": [
    "《Java 程序设计》《数据库原理》"
  ],
  "activities": [
    "完成 1—2 个后端小项目并部署上线"
  ],
  "pathDesc": [
    "大一大二：打好编程与数据结构基础"
  ],
  "misconceptions": [
    "后端只会 CRUD 就够了"
  ],
  "favorited": true,
  "updated": "2026-07-31",
  "status": "PUBLISHED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[Direction](#schemadirection)| yes | 方向详情|none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "status": "PUBLISHED",
  "sortOrder": 1,
  "applicableMajors": [
    "计算机类"
  ],
  "updatedAt": "2026-07-31T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminDirection](#schemaadmindirection)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminSetDirectionStatus"></a>

## PATCH 启停方向

PATCH /api/v1/admin/directions/{directionId}/status

发布 / 停用方向。停用方向不参与新推荐，已选目标不受影响。

> Body Parameters

```json
{
    "status": "DISABLED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[DirectionStatusUpdate](#schemadirectionstatusupdate)| yes | 方向启停|none|

> Response Examples

> 200 Response

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "status": "PUBLISHED",
  "sortOrder": 1,
  "applicableMajors": [
    "计算机类"
  ],
  "updatedAt": "2026-07-31T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AdminDirection](#schemaadmindirection)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdadminListAbilities"></a>

## GET 能力标签列表

GET /api/v1/admin/abilities

分页查询能力标签。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|category|query|string| no ||none|
|keyword|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AbilityTag](#schemaabilitytag)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateAbility"></a>

## POST 新增能力标签

POST /api/v1/admin/abilities

新增能力标签，供方向、课程与任务关联。

> Body Parameters

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AbilityTag](#schemaabilitytag)| yes | 能力标签|none|

> Response Examples

> 200 Response

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AbilityTag](#schemaabilitytag)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateAbility"></a>

## PATCH 更新能力标签

PATCH /api/v1/admin/abilities/{tagId}

修改标签名称 / 分类 / 状态。

> Body Parameters

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|tagId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[AbilityTag](#schemaabilitytag)| yes | 能力标签|none|

> Response Examples

> 200 Response

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AbilityTag](#schemaabilitytag)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminListTemplates"></a>

## GET 任务模板列表

GET /api/v1/admin/templates

按方向查询任务模板（计划生成的回退来源）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|directionId|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TaskTemplate](#schematasktemplate)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminCreateTemplate"></a>

## POST 新增任务模板

POST /api/v1/admin/templates

为方向创建任务模板（目标摘要 + 学期目标 + 月度任务）。

> Body Parameters

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[TaskTemplate](#schematasktemplate)| yes | 任务模板|none|

> Response Examples

> 200 Response

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TaskTemplate](#schematasktemplate)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateTemplate"></a>

## PATCH 更新任务模板

PATCH /api/v1/admin/templates/{templateId}

修改任务模板。

> Body Parameters

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|templateId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[TaskTemplate](#schematasktemplate)| yes | 任务模板|none|

> Response Examples

> 200 Response

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[TaskTemplate](#schematasktemplate)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminGetModels"></a>

## GET 模型与提示词配置

GET /api/v1/admin/models

查询模型供应商配置、脱敏规则与提示词版本列表。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "key": "llm.provider",
  "value": "openai",
  "updatedAt": "2026-09-01T00:00:00+08:00",
  "updatedBy": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ModelConfig](#schemamodelconfig)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateModel"></a>

## PATCH 更新模型配置

PATCH /api/v1/admin/models/{key}

更新模型 / 供应商 / 频率限制等配置（密钥仅回显掩码）。

> Body Parameters

```json
{
  "key": "llm.provider",
  "value": "openai",
  "updatedAt": "2026-09-01T00:00:00+08:00",
  "updatedBy": "系统管理员"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|key|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ModelConfig](#schemamodelconfig)| yes | 模型配置项|none|

> Response Examples

> 200 Response

```json
{
  "key": "llm.provider",
  "value": "openai",
  "updatedAt": "2026-09-01T00:00:00+08:00",
  "updatedBy": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ModelConfig](#schemamodelconfig)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdadminCreatePrompt"></a>

## POST 新建提示词版本

POST /api/v1/admin/models/prompts

创建提示词新版本。生产环境仅允许发布状态提示词；修改须创建新版本。

> Body Parameters

```json
{
  "version": "v1.3",
  "scene": "recommendation_explain",
  "status": "DRAFT",
  "content": "string",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[PromptVersion](#schemapromptversion)| yes | 提示词版本|none|

> Response Examples

> 200 Response

```json
{
  "version": "v1.3",
  "scene": "recommendation_explain",
  "status": "DRAFT",
  "content": "string",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[PromptVersion](#schemapromptversion)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminPublishPrompt"></a>

## POST 发布提示词

POST /api/v1/admin/models/prompts/{promptVersionId}/publish

将 DRAFT 提示词发布为生产可用版本。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|promptVersionId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "version": "v1.3",
  "scene": "recommendation_explain",
  "status": "DRAFT",
  "content": "string",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[PromptVersion](#schemapromptversion)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdadminGetWeights"></a>

## GET 推荐权重配置

GET /api/v1/admin/weights

查询当前生效的推荐规则版本（六维权重与阈值）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "version": "R1.0",
  "weights": {
    "interest": 0.2,
    "values": 0.2,
    "ability": 0.2,
    "academic": 0.2,
    "tendency": 0.2,
    "practice": 0.2
  },
  "minConfidence": 0,
  "topN": 5,
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[WeightConfig](#schemaweightconfig)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdadminUpdateWeights"></a>

## POST 更新推荐权重

POST /api/v1/admin/weights

创建新的权重版本（DRAFT），经教师确认后发布。权重禁止写死在前端或提示词。

> Body Parameters

```json
{
  "version": "R1.0",
  "weights": {
    "interest": 0.2,
    "values": 0.2,
    "ability": 0.2,
    "academic": 0.2,
    "tendency": 0.2,
    "practice": 0.2
  },
  "minConfidence": 0,
  "topN": 5,
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[WeightConfig](#schemaweightconfig)| yes | 推荐权重配置|none|

> Response Examples

> 200 Response

```json
{
  "version": "R1.0",
  "weights": {
    "interest": 0.2,
    "values": 0.2,
    "ability": 0.2,
    "academic": 0.2,
    "tendency": 0.2,
    "practice": 0.2
  },
  "minConfidence": 0,
  "topN": 5,
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[WeightConfig](#schemaweightconfig)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

# 管理端·培养方案

<a id="opIdcurriculumImport"></a>

## POST 上传培养方案 PDF

POST /api/v1/admin/curricula/import

管理员上传培养方案 PDF，创建解析任务。Spring Boot 保存文件元数据后交由 AI 服务异步解析；解析完成进入待审核区，返回 202。

> Body Parameters

```yaml
id: CJ-001
filename: 软件工程培养方案2026.pdf
status: ""
totalItems: 180
parsedItems: 172
confidence: 86
createdAt: 2026-09-10T10:00:00+08:00

```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|object| yes ||none|
|» id|body|string| yes ||导入任务 ID|
|» filename|body|string| yes ||文件名|
|» status|body|string| yes ||状态|
|» totalItems|body|integer| no ||识别到的课程总数|
|» parsedItems|body|integer| no ||已解析条目数|
|» confidence|body|number| no ||整体解析置信度（0–100）|
|» createdAt|body|string(date-time)| no ||创建时间|

#### Enum

|Name|Value|
|---|---|
|» status|UPLOADED|
|» status|PARSING|
|» status|REVIEW_REQUIRED|
|» status|PUBLISHED|
|» status|FAILED|

> Response Examples

> 202 Response

```json
{
  "id": "CJ-001",
  "filename": "软件工程培养方案2026.pdf",
  "status": "UPLOADED",
  "totalItems": 180,
  "parsedItems": 172,
  "confidence": 86,
  "createdAt": "2026-09-10T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|202|[Accepted](https://tools.ietf.org/html/rfc7231#section-6.3.3)|已创建解析任务，需轮询任务详情|[CurriculumImportJob](#schemacurriculumimportjob)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdcurriculumListJobs"></a>

## GET 导入任务列表

GET /api/v1/admin/curricula/jobs

分页查询培养方案导入任务及解析状态。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "CJ-001",
  "filename": "软件工程培养方案2026.pdf",
  "status": "UPLOADED",
  "totalItems": 180,
  "parsedItems": 172,
  "confidence": 86,
  "createdAt": "2026-09-10T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CurriculumImportJob](#schemacurriculumimportjob)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdcurriculumJobDetail"></a>

## GET 导入任务详情

GET /api/v1/admin/curricula/jobs/{jobId}

查询单个导入任务状态（UPLOADED / PARSING / REVIEW_REQUIRED / PUBLISHED / FAILED）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|jobId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "CJ-001",
  "filename": "软件工程培养方案2026.pdf",
  "status": "UPLOADED",
  "totalItems": 180,
  "parsedItems": 172,
  "confidence": 86,
  "createdAt": "2026-09-10T10:00:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CurriculumImportJob](#schemacurriculumimportjob)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

<a id="opIdcurriculumListItems"></a>

## GET 待审核课程列表

GET /api/v1/admin/curricula/items

按任务查询解析出的课程条目（含置信度、原文片段、审核状态）。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|jobId|query|string| yes ||none|
|status|query|string| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|status|PENDING|
|status|APPROVED|
|status|REJECTED|
|status|MERGED|

> Response Examples

> 200 Response

```json
{
  "id": "IT-001",
  "jobId": "CJ-001",
  "courseCode": "CS101",
  "courseName": "程序设计基础",
  "semester": "2026-2027-1",
  "credits": 4,
  "hours": 64,
  "category": "专业基础",
  "module": "必修",
  "prerequisites": [
    "CS100"
  ],
  "abilityTags": [
    "programming_basic"
  ],
  "confidence": 92,
  "pageRef": "第 12 页",
  "status": "PENDING"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ImportItem](#schemaimportitem)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdcurriculumReviewItem"></a>

## PATCH 校核课程

PATCH /api/v1/admin/curricula/items/{itemId}

人工校核单条解析结果：修正字段、补充能力标签、审核通过或驳回。

> Body Parameters

```json
{
    "courseName": "程序设计基础",
    "semester": "2026-2027-1",
    "abilityTags": [
        "programming_basic"
    ],
    "status": "APPROVED"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|itemId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ImportItemUpdate](#schemaimportitemupdate)| yes | 校核课程请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "IT-001",
  "jobId": "CJ-001",
  "courseCode": "CS101",
  "courseName": "程序设计基础",
  "semester": "2026-2027-1",
  "credits": 4,
  "hours": 64,
  "category": "专业基础",
  "module": "必修",
  "prerequisites": [
    "CS100"
  ],
  "abilityTags": [
    "programming_basic"
  ],
  "confidence": 92,
  "pageRef": "第 12 页",
  "status": "PENDING"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ImportItem](#schemaimportitem)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

<a id="opIdcurriculumBatchReview"></a>

## POST 批量校核 / 合并 / 删除

POST /api/v1/admin/curricula/items/batch

批量执行课程条目的通过、驳回与合并操作。

> Body Parameters

```json
{
    "actions": [
        {
            "itemId": "IT-001",
            "action": "APPROVE",
            "abilityTags": [
                "programming_basic"
            ]
        },
        {
            "itemId": "IT-002",
            "action": "MERGE",
            "targetItemId": "IT-003"
        }
    ]
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[BatchReviewRequest](#schemabatchreviewrequest)| yes | 批量校核请求|none|

> Response Examples

> 200 Response

```json
{
  "id": "IT-001",
  "jobId": "CJ-001",
  "courseCode": "CS101",
  "courseName": "程序设计基础",
  "semester": "2026-2027-1",
  "credits": 4,
  "hours": 64,
  "category": "专业基础",
  "module": "必修",
  "prerequisites": [
    "CS100"
  ],
  "abilityTags": [
    "programming_basic"
  ],
  "confidence": 92,
  "pageRef": "第 12 页",
  "status": "PENDING"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ImportItem](#schemaimportitem)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdcurriculumVersions"></a>

## GET 方案版本列表

GET /api/v1/admin/curricula/versions

查询培养方案版本（DRAFT / PUBLISHED）与课程数。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "CV-001",
  "name": "软件工程培养方案 2026 版",
  "major": "软件工程",
  "courseCount": 180,
  "status": "DRAFT",
  "publishedAt": "2026-09-15T00:00:00+08:00",
  "publishedBy": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CurriculumVersion](#schemacurriculumversion)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdcurriculumPublish"></a>

## POST 发布培养方案版本

POST /api/v1/admin/curricula/publish

将审核完成的课程数据发布为正式方案版本，参与计划生成。未审核数据不得参与推荐。

> Body Parameters

```json
{
  "id": "CV-001",
  "name": "软件工程培养方案 2026 版",
  "major": "软件工程",
  "courseCount": 180,
  "status": "DRAFT",
  "publishedAt": "2026-09-15T00:00:00+08:00",
  "publishedBy": "系统管理员"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[CurriculumVersion](#schemacurriculumversion)| yes | 培养方案版本|none|

> Response Examples

> 200 Response

```json
{
  "id": "CV-001",
  "name": "软件工程培养方案 2026 版",
  "major": "软件工程",
  "courseCount": 180,
  "status": "DRAFT",
  "publishedAt": "2026-09-15T00:00:00+08:00",
  "publishedBy": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[CurriculumVersion](#schemacurriculumversion)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|409|[Conflict](https://tools.ietf.org/html/rfc7231#section-6.5.8)|当前业务状态不允许该操作（409）|None|

# 管理端·日志与导出

<a id="opIdauditOperationLogs"></a>

## GET 操作审计日志

GET /api/v1/admin/logs/operations

查询登录、查看详情、导出、配置发布、权限变更等操作日志。普通管理员不可随意删除。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|action|query|string| no ||none|
|operator|query|string| no ||none|
|from|query|string(date-time)| no ||none|
|to|query|string(date-time)| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "LOG-001",
  "time": "2026-09-30T11:03:00+08:00",
  "operator": "系统管理员",
  "action": "配置发布",
  "target": "questionnaire/holland",
  "detail": "发布问卷 v2",
  "level": "info",
  "ip": "172.16.1.10"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[OperationLog](#schemaoperationlog)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdauditAiLogs"></a>

## GET AI 调用日志

GET /api/v1/admin/logs/ai

查询 AI 调用日志（模型、提示词版本、耗时、状态、token 估算、脱敏请求哈希）。不保存完整敏感提示词。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|scene|query|string| no ||none|
|status|query|string| no ||none|
|from|query|string(date-time)| no ||none|
|to|query|string(date-time)| no ||none|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

#### Enum

|Name|Value|
|---|---|
|status|SUCCESS|
|status|FAILED|
|status|TIMEOUT|
|status|DEGRADED|

> Response Examples

> 200 Response

```json
{
  "id": "AI-001",
  "time": "2026-09-30T11:03:00+08:00",
  "userRef": "student_ref_8f3a",
  "scene": "recommendation_explain",
  "modelName": "gpt-4o-mini",
  "promptVersion": "v1.2",
  "durationMs": 890,
  "status": "SUCCESS",
  "tokenEstimate": 1200,
  "requestHash": "a1b2c3..."
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[AiCallLog](#schemaaicalllog)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdauditCreateExport"></a>

## POST 创建导出任务

POST /api/v1/admin/exports

创建数据导出任务（学生数据 / 白名单 / 日志等）。导出范围、操作者、时间写入审计日志。

> Body Parameters

```json
{
    "type": "STUDENT_DATA",
    "scope": "计科2601 全部学生画像与计划"
}
```

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|
|body|body|[ExportRequest](#schemaexportrequest)| yes | 创建导出任务|none|

> Response Examples

> 200 Response

```json
{
  "id": "EX-001",
  "type": "STUDENT_DATA",
  "scope": "计科2601 全部学生画像与计划",
  "status": "PENDING",
  "downloadUrl": "string",
  "createdAt": "2026-09-30T11:03:00+08:00",
  "operator": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ExportJob](#schemaexportjob)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|请求参数校验失败（400）|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdauditListExports"></a>

## GET 导出任务列表

GET /api/v1/admin/exports

查询导出任务及完成状态。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|page|query|integer| no ||页码，从 1 开始|
|size|query|integer| no ||每页条数（最大 100）|
|sort|query|string| no ||排序，如 `-createdAt`（desc）或 `createdAt`（asc）|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "id": "EX-001",
  "type": "STUDENT_DATA",
  "scope": "计科2601 全部学生画像与计划",
  "status": "PENDING",
  "downloadUrl": "string",
  "createdAt": "2026-09-30T11:03:00+08:00",
  "operator": "系统管理员"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ExportJob](#schemaexportjob)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|

<a id="opIdauditDownloadExport"></a>

## GET 下载导出文件

GET /api/v1/admin/exports/{jobId}/download

下载已完成的导出文件。访问记入审计日志。

### Params

|Name|Location|Type|Required|Title|Description|
|---|---|---|---|---|---|
|jobId|path|string| yes ||none|
|X-Request-Id|header|string| no ||none|
|Idempotency-Key|header|string| no ||none|

> Response Examples

> 200 Response

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}
```

### Responses

|HTTP Status Code |Meaning|Description|Data schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|操作成功|[ApiResponse](#schemaapiresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|未登录或令牌失效（401）|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|无权访问目标资源 / 角色不足（403）|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|资源不存在（404）|None|

# Data Schema

<h2 id="tocS_DIM_NAMES">DIM_NAMES</h2>

<a id="schemadim_names"></a>
<a id="schema_DIM_NAMES"></a>
<a id="tocSdim_names"></a>
<a id="tocsdim_names"></a>

```json
null

```

### Attribute

*None*

<h2 id="tocS_ApiResponse">ApiResponse</h2>

<a id="schemaapiresponse"></a>
<a id="schema_ApiResponse"></a>
<a id="tocSapiresponse"></a>
<a id="tocsapiresponse"></a>

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}

```

统一响应

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|code|string|true|none||业务码，OK 表示成功|
|message|string|true|none||提示信息|
|data|object¦null|true|none||业务数据，随接口不同而变化（见各接口的 data 类型）|
|traceId|string|true|none||链路追踪 ID，用于排障|
|timestamp|string(date-time)|true|none||服务器时间（ISO 8601）|

<h2 id="tocS_ErrorDetail">ErrorDetail</h2>

<a id="schemaerrordetail"></a>
<a id="schema_ErrorDetail"></a>
<a id="tocSerrordetail"></a>
<a id="tocserrordetail"></a>

```json
{
  "field": "studentNo",
  "message": "学号已注册"
}

```

错误详情

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|field|string|false|none||出错字段（可选）|
|message|string|true|none||该字段的具体错误|

<h2 id="tocS_ErrorResponse">ErrorResponse</h2>

<a id="schemaerrorresponse"></a>
<a id="schema_ErrorResponse"></a>
<a id="tocSerrorresponse"></a>
<a id="tocserrorresponse"></a>

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "data": [
    {
      "field": "studentNo",
      "message": "学号已注册"
    }
  ],
  "traceId": "01J5X3K9Q2ZQ2Y3V9A1M0N8B7C",
  "timestamp": "2026-08-04T10:30:00+08:00"
}

```

错误响应

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|code|string|true|none||业务错误码|
|message|string|true|none||人类可读的错误信息|
|data|[[ErrorDetail](#schemaerrordetail)]|true|none||字段级错误明细，可为空数组|
|traceId|string|true|none||链路追踪 ID|
|timestamp|string(date-time)|true|none||服务器时间|

<h2 id="tocS_Page">Page</h2>

<a id="schemapage"></a>
<a id="schema_Page"></a>
<a id="tocSpage"></a>
<a id="tocspage"></a>

```json
{
  "list": [
    {}
  ],
  "page": 1,
  "size": 20,
  "total": 128,
  "totalPages": 7
}

```

分页结构

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|list|[object]|true|none||当前页数据（元素类型见各接口说明）|
|page|integer|true|none||页码，从 1 开始|
|size|integer|true|none||每页条数|
|total|integer|true|none||符合条件的总条数|
|totalPages|integer|true|none||总页数|

<h2 id="tocS_RegisterRequest">RegisterRequest</h2>

<a id="schemaregisterrequest"></a>
<a id="schema_RegisterRequest"></a>
<a id="tocSregisterrequest"></a>
<a id="tocsregisterrequest"></a>

```json
{
  "studentNo": "2026011309",
  "name": "张同学",
  "className": "计科2601",
  "verifyCode": "202609"
}

```

注册请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentNo|string|true|none||白名单学号|
|name|string|true|none||姓名|
|className|string|false|none||班级（可选，白名单无则忽略）|
|verifyCode|string|true|none||初始校验码，取自辅导员下发的白名单|

<h2 id="tocS_LoginRequest">LoginRequest</h2>

<a id="schemaloginrequest"></a>
<a id="schema_LoginRequest"></a>
<a id="tocSloginrequest"></a>
<a id="tocsloginrequest"></a>

```json
{
  "account": "2026011301",
  "password": "123456",
  "role": "STUDENT"
}

```

登录请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|account|string|true|none||登录账号：学生用学号，教职工用工号|
|password|string|true|none||密码|
|role|string|false|none||角色（可选，通常由后端根据账号判断）|

#### Enum

|Name|Value|
|---|---|
|role|STUDENT|
|role|ADVISOR|
|role|ADMIN|

<h2 id="tocS_TokenResponse">TokenResponse</h2>

<a id="schematokenresponse"></a>
<a id="schema_TokenResponse"></a>
<a id="tocStokenresponse"></a>
<a id="tocstokenresponse"></a>

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "rt_9f8e7d6c5b4a...",
  "expiresIn": 7200,
  "tokenType": "Bearer",
  "firstLogin": false,
  "user": {
    "id": "S1001",
    "username": "2026011301",
    "name": "李明",
    "role": "STUDENT",
    "studentNo": "2026011301",
    "grade": "2026级",
    "majorCategory": "计算机类",
    "className": "计科2601",
    "consentAgreed": true
  }
}

```

登录响应

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|accessToken|string|true|none||短期访问令牌（JWT，有效期约 2 小时）|
|refreshToken|string|true|none||刷新令牌（有效期约 7 天）|
|expiresIn|integer|true|none||访问令牌有效秒数|
|tokenType|string|true|none||令牌类型|
|firstLogin|boolean|false|none||是否首次登录（需完成隐私授权）|
|user|[CurrentUser](#schemacurrentuser)|false|none||none|

<h2 id="tocS_RefreshRequest">RefreshRequest</h2>

<a id="schemarefreshrequest"></a>
<a id="schema_RefreshRequest"></a>
<a id="tocSrefreshrequest"></a>
<a id="tocsrefreshrequest"></a>

```json
{
  "refreshToken": "rt_9f8e7d6c5b4a..."
}

```

刷新令牌请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|refreshToken|string|true|none||刷新令牌|

<h2 id="tocS_CurrentUser">CurrentUser</h2>

<a id="schemacurrentuser"></a>
<a id="schema_CurrentUser"></a>
<a id="tocScurrentuser"></a>
<a id="tocscurrentuser"></a>

```json
{
  "id": "S1001",
  "username": "2026011301",
  "name": "李明",
  "role": "STUDENT",
  "studentNo": "2026011301",
  "grade": "2026级",
  "majorCategory": "计算机类",
  "className": "计科2601",
  "consentAgreed": true
}

```

当前用户

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||用户 ID|
|username|string|true|none||登录名（学号或工号）|
|name|string|true|none||姓名|
|role|string|true|none||角色|
|studentNo|string|false|none||学号（学生）|
|grade|string|false|none||年级（学生）|
|majorCategory|string|false|none||专业大类（学生）|
|className|string|false|none||班级（学生）|
|consentAgreed|boolean|false|none||是否已同意隐私授权|

#### Enum

|Name|Value|
|---|---|
|role|STUDENT|
|role|ADVISOR|
|role|ADMIN|

<h2 id="tocS_PasswordChangeRequest">PasswordChangeRequest</h2>

<a id="schemapasswordchangerequest"></a>
<a id="schema_PasswordChangeRequest"></a>
<a id="tocSpasswordchangerequest"></a>
<a id="tocspasswordchangerequest"></a>

```json
{
  "oldPassword": "123456",
  "newPassword": "NewPass2026!"
}

```

修改密码请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|oldPassword|string|true|none||原密码|
|newPassword|string|true|none||新密码（6–128 位）|

<h2 id="tocS_PasswordResetRequest">PasswordResetRequest</h2>

<a id="schemapasswordresetrequest"></a>
<a id="schema_PasswordResetRequest"></a>
<a id="tocSpasswordresetrequest"></a>
<a id="tocspasswordresetrequest"></a>

```json
{
  "studentNo": "2026011399",
  "newPassword": "Temp@2026",
  "reason": "学生忘记密码"
}

```

管理员重置密码请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentNo|string|true|none||待重置学生的学号|
|newPassword|string|true|none||新初始密码（管理员设置）|
|reason|string|false|none||重置原因（写入审计日志）|

<h2 id="tocS_ConsentRequest">ConsentRequest</h2>

<a id="schemaconsentrequest"></a>
<a id="schema_ConsentRequest"></a>
<a id="tocSconsentrequest"></a>
<a id="tocsconsentrequest"></a>

```json
{
  "version": "v1.0"
}

```

隐私授权请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|version|string|true|none||隐私授权文本版本号，须与当前发布版本一致|

<h2 id="tocS_ConsentStatus">ConsentStatus</h2>

<a id="schemaconsentstatus"></a>
<a id="schema_ConsentStatus"></a>
<a id="tocSconsentstatus"></a>
<a id="tocsconsentstatus"></a>

```json
{
  "agreed": false,
  "version": "string",
  "agreedAt": "2026-08-25T09:00:00+08:00",
  "currentVersion": "v1.0",
  "currentVersionPublishedAt": "2026-08-01T00:00:00+08:00",
  "content": "隐私告知与 AI 使用说明…"
}

```

授权状态

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|agreed|boolean|true|none||是否已同意|
|version|string|false|none||已同意的版本（未同意则为空）|
|agreedAt|string(date-time)|false|none||同意时间|
|currentVersion|string|true|none||当前发布版本|
|currentVersionPublishedAt|string(date-time)|false|none||当前版本发布时间|
|content|string|false|none||当前版本文本摘要（完整文本由管理端维护）|

<h2 id="tocS_BasicInfo">BasicInfo</h2>

<a id="schemabasicinfo"></a>
<a id="schema_BasicInfo"></a>
<a id="tocSbasicinfo"></a>
<a id="tocsbasicinfo"></a>

```json
{
  "gender": "男",
  "hometown": "重庆",
  "birthday": "2008-05-14",
  "phone": "138****6721"
}

```

基本信息

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|gender|string|false|none||性别|
|hometown|string|false|none||籍贯（选填）|
|birthday|string(date)|false|none||出生日期|
|phone|string|false|none||手机号（脱敏展示，修改需二次校验）|

<h2 id="tocS_AcademicInfo">AcademicInfo</h2>

<a id="schemaacademicinfo"></a>
<a id="schema_AcademicInfo"></a>
<a id="tocSacademicinfo"></a>
<a id="tocsacademicinfo"></a>

```json
{
  "math": 4,
  "english": 3,
  "programming": 2,
  "note": "高中数学较好，英语一般，编程刚起步"
}

```

学业基础

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|math|number|false|none||数学基础自评（1–5）|
|english|number|false|none||英语基础自评（1–5）|
|programming|number|false|none||编程基础自评（1–5）|
|note|string|false|none||学业备注（选填）|

<h2 id="tocS_AbilitySelf">AbilitySelf</h2>

<a id="schemaabilityself"></a>
<a id="schema_AbilitySelf"></a>
<a id="tocSabilityself"></a>
<a id="tocsabilityself"></a>

```json
{
  "programming": 2,
  "math": 4,
  "english": 3,
  "communication": 4,
  "organization": 3
}

```

能力自评

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|programming|number|false|none||编程能力（1–5）|
|math|number|false|none||数学能力（1–5）|
|english|number|false|none||英语能力（1–5）|
|communication|number|false|none||沟通表达（1–5）|
|organization|number|false|none||组织执行（1–5）|

<h2 id="tocS_Experience">Experience</h2>

<a id="schemaexperience"></a>
<a id="schema_Experience"></a>
<a id="tocSexperience"></a>
<a id="tocsexperience"></a>

```json
{
  "id": "EXP-001",
  "type": "竞赛",
  "title": "数学建模校赛 · 二等奖",
  "startDate": "2026-05",
  "endDate": "2026-06",
  "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
  "attachmentUrl": "string"
}

```

经历条目

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|false|none||经历 ID|
|type|string|true|none||经历类别：竞赛 / 项目 / 学生工作 / 志愿服务|
|title|string|true|none||经历名称|
|startDate|string|true|none||开始时间（YYYY-MM）|
|endDate|string|false|none||结束时间（选填）|
|description|string|true|none||经历描述|
|attachmentUrl|string|false|none||可选附件（文件服务器 URL）|

<h2 id="tocS_ExperienceRequest">ExperienceRequest</h2>

<a id="schemaexperiencerequest"></a>
<a id="schema_ExperienceRequest"></a>
<a id="tocSexperiencerequest"></a>
<a id="tocsexperiencerequest"></a>

```json
{
  "type": "项目",
  "title": "暑期自学 · 小计算器程序",
  "startDate": "2026-07",
  "endDate": "2026-08",
  "description": "用 Python 完成命令行计算器。",
  "attachment": "string"
}

```

新增/修改经历请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|type|string|true|none||经历类别|
|title|string|true|none||经历名称|
|startDate|string|true|none||开始时间（YYYY-MM）|
|endDate|string|false|none||结束时间（选填）|
|description|string|true|none||经历描述|
|attachment|string|false|none||可选附件（multipart 上传的临时文件 ID）|

<h2 id="tocS_StudentProfile">StudentProfile</h2>

<a id="schemastudentprofile"></a>
<a id="schema_StudentProfile"></a>
<a id="tocSstudentprofile"></a>
<a id="tocsstudentprofile"></a>

```json
{
  "userId": "S1001",
  "name": "李明",
  "className": "计科2601",
  "grade": "2026级",
  "majorCategory": "计算机类",
  "basic": {
    "gender": "男",
    "hometown": "重庆",
    "birthday": "2008-05-14",
    "phone": "138****6721"
  },
  "academic": {
    "math": 4,
    "english": 3,
    "programming": 2,
    "note": "高中数学较好，英语一般，编程刚起步"
  },
  "interestPrefs": [
    "编程"
  ],
  "abilitySelf": {
    "programming": 2,
    "math": 4,
    "english": 3,
    "communication": 4,
    "organization": 3
  },
  "values": [
    "成长"
  ],
  "experiences": {
    "id": "EXP-001",
    "type": "竞赛",
    "title": "数学建模校赛 · 二等奖",
    "startDate": "2026-05",
    "endDate": "2026-06",
    "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
    "attachmentUrl": "string"
  },
  "developmentIntention": "employment",
  "constraints": [
    "愿意在课余投入学习"
  ],
  "completeness": 92,
  "updatedAt": "2026-09-30T09:12:00+08:00"
}

```

学生档案

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|userId|string|true|none||用户 ID|
|name|string|true|none||姓名|
|className|string|true|none||班级|
|grade|string|false|none||年级|
|majorCategory|string|false|none||专业大类|
|basic|[BasicInfo](#schemabasicinfo)|false|none||none|
|academic|[AcademicInfo](#schemaacademicinfo)|false|none||none|
|interestPrefs|[string]|false|none||兴趣偏好标签|
|abilitySelf|[AbilitySelf](#schemaabilityself)|false|none||none|
|values|[string]|false|none||职业价值观标签|
|experiences|[Experience](#schemaexperience)|false|none||none|
|developmentIntention|string|false|none||发展意向：graduate / employment / overseas / undecided|
|constraints|[string]|false|none||现实约束（选填）|
|completeness|integer|false|none||资料完整度（0–100）|
|updatedAt|string(date-time)|false|none||最近更新|

<h2 id="tocS_StudentProfileUpdate">StudentProfileUpdate</h2>

<a id="schemastudentprofileupdate"></a>
<a id="schema_StudentProfileUpdate"></a>
<a id="tocSstudentprofileupdate"></a>
<a id="tocsstudentprofileupdate"></a>

```json
{
  "basic": {
    "gender": "男",
    "hometown": "重庆",
    "birthday": "2008-05-14",
    "phone": "138****6721"
  },
  "academic": {
    "math": 4,
    "english": 3,
    "programming": 2,
    "note": "高中数学较好，英语一般，编程刚起步"
  },
  "interestPrefs": [
    "编程"
  ],
  "abilitySelf": {
    "programming": 2,
    "math": 4,
    "english": 3,
    "communication": 4,
    "organization": 3
  },
  "values": [
    "成长"
  ],
  "developmentIntention": "graduate",
  "constraints": [
    "愿意在课余投入学习"
  ]
}

```

分步保存学生资料请求（按需提交字段，空字段不覆盖）

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|basic|[BasicInfo](#schemabasicinfo)|false|none||none|
|academic|[AcademicInfo](#schemaacademicinfo)|false|none||none|
|interestPrefs|[string]|false|none||none|
|abilitySelf|[AbilitySelf](#schemaabilityself)|false|none||none|
|values|[string]|false|none||none|
|developmentIntention|string|false|none||none|
|constraints|[string]|false|none||none|

#### Enum

|Name|Value|
|---|---|
|developmentIntention|graduate|
|developmentIntention|employment|
|developmentIntention|overseas|
|developmentIntention|undecided|

<h2 id="tocS_CompletenessDetail">CompletenessDetail</h2>

<a id="schemacompletenessdetail"></a>
<a id="schema_CompletenessDetail"></a>
<a id="tocScompletenessdetail"></a>
<a id="tocscompletenessdetail"></a>

```json
{
  "score": 92,
  "total": 16,
  "filled": 15,
  "missing": [
    {
      "key": "academic.note",
      "name": "学业备注"
    }
  ],
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "filled": true,
      "required": true
    }
  ]
}

```

完整度明细

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|score|integer|true|none||综合完整度|
|total|integer|true|none||必填字段总数|
|filled|integer|true|none||已填字段数|
|missing|[object]|false|none||缺失字段清单|
|» key|string|false|none||none|
|» name|string|false|none||none|
|dimensions|[object]|false|none||按维度拆分|
|» key|string|false|none||none|
|» name|string|false|none||none|
|» filled|boolean|false|none||该维度是否已填|
|» required|boolean|false|none||是否必填|

#### Enum

|Name|Value|
|---|---|
|key|interest|
|key|values|
|key|ability|
|key|academic|
|key|tendency|
|key|practice|

<h2 id="tocS_DeletionRequest">DeletionRequest</h2>

<a id="schemadeletionrequest"></a>
<a id="schema_DeletionRequest"></a>
<a id="tocSdeletionrequest"></a>
<a id="tocsdeletionrequest"></a>

```json
{
  "reason": "本人不再使用该系统"
}

```

申请删除本人信息

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|reason|string|false|none||申请删除原因（选填）|

<h2 id="tocS_Questionnaire">Questionnaire</h2>

<a id="schemaquestionnaire"></a>
<a id="schema_Questionnaire"></a>
<a id="tocSquestionnaire"></a>
<a id="tocsquestionnaire"></a>

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "icon": "🧩",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "minutes": 4,
  "tip": "通过你对学习与活动的偏好，刻画兴趣类型与倾向强度。",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}

```

问卷概要

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||问卷 ID（类型编码）|
|type|string|true|none||问卷类型编码|
|typeName|string|true|none||问卷类型名称|
|icon|string|false|none||图标 emoji|
|version|string|true|none||当前版本号|
|status|string|true|none||状态|
|questionCount|integer|true|none||题目数量|
|minutes|integer|false|none||预计耗时（分钟）|
|tip|string|false|none||问卷引导语|
|publishedAt|string(date-time)|false|none||发布时间|

#### Enum

|Name|Value|
|---|---|
|typeName|霍兰德兴趣简版|
|typeName|职业价值观|
|typeName|能力自评|
|typeName|专业认知与发展倾向|
|status|DRAFT|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_QuestionOption">QuestionOption</h2>

<a id="schemaquestionoption"></a>
<a id="schema_QuestionOption"></a>
<a id="tocSquestionoption"></a>
<a id="tocsquestionoption"></a>

```json
{
  "text": "调试程序直到它运行起来",
  "scores": {
    "interest": 5,
    "values": 5,
    "ability": 5,
    "academic": 5,
    "tendency": 5,
    "practice": 5
  }
}

```

问卷选项

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|text|string|true|none||选项文案|
|scores|object|false|none||none|
|» interest|number|false|none||得分映射|
|» values|number|false|none||得分映射|
|» ability|number|false|none||得分映射|
|» academic|number|false|none||得分映射|
|» tendency|number|false|none||得分映射|
|» practice|number|false|none||得分映射|

<h2 id="tocS_Question">Question</h2>

<a id="schemaquestion"></a>
<a id="schema_Question"></a>
<a id="tocSquestion"></a>
<a id="tocsquestion"></a>

```json
{
  "id": "q-holland-1",
  "text": "以下学习活动，你最愿意投入时间的是？",
  "type": "CHOICE",
  "dim": "programming",
  "labels": [
    "一般"
  ],
  "options": [
    {
      "text": "调试程序直到它运行起来",
      "scores": {
        "interest": 5,
        "values": 5,
        "ability": 5,
        "academic": 5,
        "tendency": 5,
        "practice": 5
      }
    }
  ]
}

```

题目

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||题目 ID|
|text|string|true|none||题干|
|type|string|true|none||题型|
|dim|string|false|none||RATING 题型对应的能力维度|
|labels|[string]|false|none||RATING 题型的等级标签|
|options|[[QuestionOption](#schemaquestionoption)]|false|none||选项列表（CHOICE 题）|

#### Enum

|Name|Value|
|---|---|
|type|CHOICE|
|type|RATING|

<h2 id="tocS_QuestionnaireDetail">QuestionnaireDetail</h2>

<a id="schemaquestionnairedetail"></a>
<a id="schema_QuestionnaireDetail"></a>
<a id="tocSquestionnairedetail"></a>
<a id="tocsquestionnairedetail"></a>

```json
{
  "questionnaire": {
    "id": "holland",
    "type": "holland",
    "typeName": "霍兰德兴趣简版",
    "icon": "🧩",
    "version": "v2",
    "status": "DRAFT",
    "questionCount": 6,
    "minutes": 4,
    "tip": "通过你对学习与活动的偏好，刻画兴趣类型与倾向强度。",
    "publishedAt": "2026-09-01T00:00:00+08:00"
  },
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}

```

问卷详情

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|questionnaire|[Questionnaire](#schemaquestionnaire)|true|none||none|
|questions|[[Question](#schemaquestion)]|true|none||none|

<h2 id="tocS_QuestionnaireVersion">QuestionnaireVersion</h2>

<a id="schemaquestionnaireversion"></a>
<a id="schema_QuestionnaireVersion"></a>
<a id="tocSquestionnaireversion"></a>
<a id="tocsquestionnaireversion"></a>

```json
{
  "id": "QV-1002",
  "version": "v2",
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00",
  "publishedBy": "系统管理员",
  "questionCount": 6,
  "changeNote": "调整第 3 题选项措辞"
}

```

问卷版本

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||版本记录 ID|
|version|string|true|none||版本号|
|status|string|true|none||版本状态|
|publishedAt|string(date-time)|false|none||发布时间|
|publishedBy|string|false|none||发布人|
|questionCount|integer|false|none||题目数量|
|changeNote|string|false|none||变更说明|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_AssessmentSession">AssessmentSession</h2>

<a id="schemaassessmentsession"></a>
<a id="schema_AssessmentSession"></a>
<a id="tocSassessmentsession"></a>
<a id="tocsassessmentsession"></a>

```json
{
  "id": "AS-20260901-001",
  "questionnaireId": "holland",
  "questionnaireName": "霍兰德兴趣简版",
  "questionnaireVersion": "v2",
  "status": "IN_PROGRESS",
  "totalQuestions": 6,
  "answeredQuestions": 4,
  "startedAt": "2026-09-01T09:00:00+08:00",
  "updatedAt": "2026-09-01T09:05:00+08:00",
  "finishedAt": "2019-08-24T14:15:22Z"
}

```

测评会话

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||测评会话 ID|
|questionnaireId|string|true|none||问卷 ID|
|questionnaireName|string|false|none||问卷名称|
|questionnaireVersion|string|false|none||问卷版本|
|status|string|true|none||会话状态|
|totalQuestions|integer|true|none||总题数|
|answeredQuestions|integer|false|none||已答题数|
|startedAt|string(date-time)|false|none||开始时间|
|updatedAt|string(date-time)|false|none||最近自动保存时间|
|finishedAt|string(date-time)|false|none||完成时间|

#### Enum

|Name|Value|
|---|---|
|status|IN_PROGRESS|
|status|COMPLETED|

<h2 id="tocS_CreateSessionRequest">CreateSessionRequest</h2>

<a id="schemacreatesessionrequest"></a>
<a id="schema_CreateSessionRequest"></a>
<a id="tocScreatesessionrequest"></a>
<a id="tocscreatesessionrequest"></a>

```json
{
  "questionnaireId": "holland",
  "resumeSessionId": "string"
}

```

创建测评会话

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|questionnaireId|string|true|none||问卷 ID|
|resumeSessionId|string|false|none||续答的会话 ID（可选，续填不新建）|

<h2 id="tocS_AnswerItem">AnswerItem</h2>

<a id="schemaansweritem"></a>
<a id="schema_AnswerItem"></a>
<a id="tocSansweritem"></a>
<a id="tocsansweritem"></a>

```json
{
  "questionId": "q-holland-1",
  "optionIndex": 0,
  "ratingValue": 4
}

```

单题答案

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|questionId|string|false|none||题目 ID|
|optionIndex|integer|false|none||CHOICE 题：所选选项下标|
|ratingValue|integer|false|none||RATING 题：1–5 评分|

<h2 id="tocS_SaveAnswersRequest">SaveAnswersRequest</h2>

<a id="schemasaveanswersrequest"></a>
<a id="schema_SaveAnswersRequest"></a>
<a id="tocSsaveanswersrequest"></a>
<a id="tocssaveanswersrequest"></a>

```json
{
  "requestId": "req-20260901-001",
  "answers": [
    {
      "questionId": "q-holland-1",
      "optionIndex": 0,
      "ratingValue": 4
    }
  ],
  "finished": false
}

```

保存/自动保存答案

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|requestId|string|false|none||业务请求 ID（幂等，防止重复点击）|
|answers|[[AnswerItem](#schemaansweritem)]|true|none||none|
|finished|boolean|false|none||是否为最后一页/最后一题（true 则按提交处理）|

<h2 id="tocS_ScoreResult">ScoreResult</h2>

<a id="schemascoreresult"></a>
<a id="schema_ScoreResult"></a>
<a id="tocSscoreresult"></a>
<a id="tocsscoreresult"></a>

```json
{
  "sessionId": "AS-20260901-001",
  "status": "COMPLETED",
  "dimensionScores": [
    {
      "dimensionCode": "interest",
      "dimensionName": "兴趣",
      "score": 78
    }
  ]
}

```

计分结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|sessionId|string|true|none||测评会话 ID|
|status|string|true|none||会话状态|
|dimensionScores|[object]|true|none||各维度得分|
|» dimensionCode|string|false|none||维度编码|
|» dimensionName|string|false|none||维度名称|
|» score|number|false|none||该维度得分（0–100）|

#### Enum

|Name|Value|
|---|---|
|status|COMPLETED|
|dimensionCode|interest|
|dimensionCode|values|
|dimensionCode|ability|
|dimensionCode|academic|
|dimensionCode|tendency|
|dimensionCode|practice|

<h2 id="tocS_DimensionValue">DimensionValue</h2>

<a id="schemadimensionvalue"></a>
<a id="schema_DimensionValue"></a>
<a id="tocSdimensionvalue"></a>
<a id="tocsdimensionvalue"></a>

```json
{
  "key": "interest",
  "name": "兴趣",
  "score": 78
}

```

维度得分

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|key|string|true|none||维度编码|
|name|string|true|none||维度名称|
|score|number|true|none||得分（0–100）|

#### Enum

|Name|Value|
|---|---|
|key|interest|
|key|values|
|key|ability|
|key|academic|
|key|tendency|
|key|practice|

<h2 id="tocS_ProfileSnapshot">ProfileSnapshot</h2>

<a id="schemaprofilesnapshot"></a>
<a id="schema_ProfileSnapshot"></a>
<a id="tocSprofilesnapshot"></a>
<a id="tocsprofilesnapshot"></a>

```json
{
  "id": "PS-1002",
  "version": 2,
  "generatedAt": "2026-09-01T09:13:00+08:00",
  "sourceVersion": "Q v2 + 自主填报 v3",
  "completeness": 92,
  "dimensions": [
    {
      "key": "interest",
      "name": "兴趣",
      "score": 78
    }
  ],
  "summary": "你的兴趣集中在技术问题求解与动手实践…",
  "strengths": [
    "数学基础较好，是算法与数据方向的加分项"
  ],
  "explore": [
    "编程实践有待积累，建议从完成小项目开始"
  ],
  "feedback": {
    "feedbackType": "MATCH",
    "comment": "学习能力描述与我实际情况基本一致"
  }
}

```

画像快照

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||画像快照 ID|
|version|integer|true|none||画像版本号|
|generatedAt|string(date-time)|true|none||生成时间|
|sourceVersion|string|false|none||数据来源版本（问卷版本 + 自主填报版本）|
|completeness|integer|false|none||数据完整度|
|dimensions|[[DimensionValue](#schemadimensionvalue)]|true|none||六维得分|
|summary|string|false|none||画像摘要|
|strengths|[string]|false|none||当前优势|
|explore|[string]|false|none||待探索问题|
|feedback|[ProfileFeedback](#schemaprofilefeedback)|false|none||none|

<h2 id="tocS_ProfileFeedback">ProfileFeedback</h2>

<a id="schemaprofilefeedback"></a>
<a id="schema_ProfileFeedback"></a>
<a id="tocSprofilefeedback"></a>
<a id="tocsprofilefeedback"></a>

```json
{
  "feedbackType": "MATCH",
  "comment": "学习能力描述与我实际情况基本一致"
}

```

画像反馈

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|feedbackType|string|true|none||反馈：符合 / 部分符合 / 不符合|
|comment|string|false|none||补充说明（选填）|

#### Enum

|Name|Value|
|---|---|
|feedbackType|MATCH|
|feedbackType|PARTIAL|
|feedbackType|MISMATCH|

<h2 id="tocS_ProfileFeedbackRequest">ProfileFeedbackRequest</h2>

<a id="schemaprofilefeedbackrequest"></a>
<a id="schema_ProfileFeedbackRequest"></a>
<a id="tocSprofilefeedbackrequest"></a>
<a id="tocsprofilefeedbackrequest"></a>

```json
{
  "feedbackType": "MATCH",
  "comment": "string"
}

```

画像反馈请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|feedbackType|string|true|none||none|
|comment|string|false|none||补充说明（选填）|

#### Enum

|Name|Value|
|---|---|
|feedbackType|MATCH|
|feedbackType|PARTIAL|
|feedbackType|MISMATCH|

<h2 id="tocS_CareerPath">CareerPath</h2>

<a id="schemacareerpath"></a>
<a id="schema_CareerPath"></a>
<a id="tocScareerpath"></a>
<a id="tocscareerpath"></a>

```json
{
  "id": "graduate",
  "name": "国内升学",
  "shortName": "升学",
  "description": "考研 / 保研 / 攻读计算机相关研究生"
}

```

发展路径

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||路径编码|
|name|string|true|none||路径名称|
|shortName|string|false|none||短名称|
|description|string|false|none||路径描述|

#### Enum

|Name|Value|
|---|---|
|id|graduate|
|id|employment|
|id|overseas|

<h2 id="tocS_DirectionBrief">DirectionBrief</h2>

<a id="schemadirectionbrief"></a>
<a id="schema_DirectionBrief"></a>
<a id="tocSdirectionbrief"></a>
<a id="tocsdirectionbrief"></a>

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维。",
  "status": "PUBLISHED",
  "updated": "2026-07-31"
}

```

方向概要

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||方向编码|
|name|string|true|none||方向名称|
|path|string|true|none||所属路径|
|icon|string|false|none||图标|
|intro|string|false|none||一句话简介|
|status|string|true|none||状态|
|updated|string(date)|false|none||最近更新时间|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_Direction">Direction</h2>

<a id="schemadirection"></a>
<a id="schema_Direction"></a>
<a id="tocSdirection"></a>
<a id="tocsdirection"></a>

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "icon": "🖥️",
  "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
  "target": {
    "interest": 70,
    "values": 70,
    "ability": 70,
    "academic": 70,
    "tendency": 70,
    "practice": 70
  },
  "minAbility": 65,
  "minAcademic": 50,
  "learning": [
    "Java / Go / Python 服务端语言"
  ],
  "abilities": [
    "编程实现"
  ],
  "courses": [
    "《Java 程序设计》《数据库原理》"
  ],
  "activities": [
    "完成 1—2 个后端小项目并部署上线"
  ],
  "pathDesc": [
    "大一大二：打好编程与数据结构基础"
  ],
  "misconceptions": [
    "后端只会 CRUD 就够了"
  ],
  "favorited": true,
  "updated": "2026-07-31",
  "status": "PUBLISHED"
}

```

方向详情

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||方向编码|
|name|string|true|none||方向名称|
|path|string|true|none||所属路径|
|icon|string|false|none||图标|
|intro|string|true|none||方向简介|
|target|object|true|none||none|
|» interest|number|false|none||兴趣目标值（0–100）|
|» values|number|false|none||职业价值观目标值（0–100）|
|» ability|number|false|none||能力基础目标值（0–100）|
|» academic|number|false|none||学业基础目标值（0–100）|
|» tendency|number|false|none||发展倾向目标值（0–100）|
|» practice|number|false|none||实践经历目标值（0–100）|
|minAbility|number|false|none||最低能力要求|
|minAcademic|number|false|none||最低学业要求|
|learning|[string]|false|none||学习内容|
|abilities|[string]|false|none||能力要求标签|
|courses|[string]|false|none||推荐课程|
|activities|[string]|false|none||实践活动建议|
|pathDesc|[string]|false|none||发展路径描述|
|misconceptions|[string]|false|none||常见误区|
|favorited|boolean|false|none||当前学生是否已收藏|
|updated|string(date)|false|none||更新时间|
|status|string|false|none||状态|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_DirectionCompare">DirectionCompare</h2>

<a id="schemadirectioncompare"></a>
<a id="schema_DirectionCompare"></a>
<a id="tocSdirectioncompare"></a>
<a id="tocsdirectioncompare"></a>

```json
{
  "directions": [
    {
      "id": "employment_backend",
      "name": "后端开发工程师",
      "path": "graduate",
      "icon": "🖥️",
      "intro": "面向软件系统的服务端设计、开发与运维，是软件行业需求量大、成长路径清晰的就业方向之一。",
      "target": {
        "interest": 70,
        "values": 70,
        "ability": 70,
        "academic": 70,
        "tendency": 70,
        "practice": 70
      },
      "minAbility": 65,
      "minAcademic": 50,
      "learning": [
        "Java / Go / Python 服务端语言"
      ],
      "abilities": [
        "编程实现"
      ],
      "courses": [
        "《Java 程序设计》《数据库原理》"
      ],
      "activities": [
        "完成 1—2 个后端小项目并部署上线"
      ],
      "pathDesc": [
        "大一大二：打好编程与数据结构基础"
      ],
      "misconceptions": [
        "后端只会 CRUD 就够了"
      ],
      "favorited": true,
      "updated": "2026-07-31",
      "status": "PUBLISHED"
    }
  ],
  "matrix": [
    {
      "dimension": "兴趣",
      "values": {
        "1": 80,
        "2": 60
      }
    }
  ],
  "abilityCompare": [
    {
      "ability": "编程实现",
      "levels": {
        "1": "要求高",
        "2": "要求中"
      }
    }
  ]
}

```

方向对比

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|directions|[[Direction](#schemadirection)]|true|none||被比较的方向（2 个）|
|matrix|[object]|false|none||六维目标值对比矩阵|
|» dimension|string|false|none||维度名|
|» values|object|false|none||none|
|»» 1|number|false|none||none|
|»» 2|number|false|none||none|
|abilityCompare|[object]|false|none||能力要求对比|
|» ability|string|false|none||none|
|» levels|object|false|none||none|
|»» 1|string|false|none||none|
|»» 2|string|false|none||none|

<h2 id="tocS_RecommendationResult">RecommendationResult</h2>

<a id="schemarecommendationresult"></a>
<a id="schema_RecommendationResult"></a>
<a id="tocSrecommendationresult"></a>
<a id="tocsrecommendationresult"></a>

```json
{
  "directionId": "employment_backend",
  "rank": 1,
  "score": 82.4,
  "confidence": "HIGH",
  "reasons": [
    "偏好结构化问题求解（兴趣维度）"
  ],
  "strengths": [
    "数学与逻辑基础较好"
  ],
  "gaps": [
    "缺少系统编程实践"
  ],
  "semesterActions": [
    "完成《程序设计基础》课程"
  ],
  "feedback": {
    "feedbackType": "HELPFUL",
    "comment": "与我预期的方向基本一致"
  }
}

```

推荐结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|directionId|string|true|none||方向编码|
|rank|integer|true|none||推荐排序（1 起）|
|score|number|true|none||匹配得分（0–100）|
|confidence|string|true|none||可信程度|
|reasons|[string]|false|none||推荐理由|
|strengths|[string]|false|none||匹配优势|
|gaps|[string]|false|none||主要差距|
|semesterActions|[string]|false|none||本学期探索建议|
|feedback|[RecommendationFeedback](#schemarecommendationfeedback)|false|none||none|

#### Enum

|Name|Value|
|---|---|
|confidence|HIGH|
|confidence|MEDIUM|
|confidence|LOW|

<h2 id="tocS_RecommendationRun">RecommendationRun</h2>

<a id="schemarecommendationrun"></a>
<a id="schema_RecommendationRun"></a>
<a id="tocSrecommendationrun"></a>
<a id="tocsrecommendationrun"></a>

```json
{
  "runId": "190001",
  "profileVersion": 2,
  "ruleVersion": "R1.0",
  "generatedAt": "2026-09-01T09:13:30+08:00",
  "status": "RUNNING",
  "results": [
    {
      "directionId": "employment_backend",
      "rank": 1,
      "score": 82.4,
      "confidence": "HIGH",
      "reasons": [
        "偏好结构化问题求解（兴趣维度）"
      ],
      "strengths": [
        "数学与逻辑基础较好"
      ],
      "gaps": [
        "缺少系统编程实践"
      ],
      "semesterActions": [
        "完成《程序设计基础》课程"
      ],
      "feedback": {
        "feedbackType": "HELPFUL",
        "comment": "与我预期的方向基本一致"
      }
    }
  ]
}

```

推荐批次

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|runId|string|true|none||推荐批次 ID|
|profileVersion|integer|true|none||画像版本|
|ruleVersion|string|true|none||规则版本|
|generatedAt|string(date-time)|false|none||生成时间|
|status|string|true|none||状态|
|results|[[RecommendationResult](#schemarecommendationresult)]|false|none||推荐结果（3–5 个）|

#### Enum

|Name|Value|
|---|---|
|status|RUNNING|
|status|SUCCESS|
|status|DEGRADED|
|status|FAILED|

<h2 id="tocS_CreateRecommendationRequest">CreateRecommendationRequest</h2>

<a id="schemacreaterecommendationrequest"></a>
<a id="schema_CreateRecommendationRequest"></a>
<a id="tocScreaterecommendationrequest"></a>
<a id="tocscreaterecommendationrequest"></a>

```json
{
  "pathFilter": "graduate",
  "requestId": "req-rec-001"
}

```

创建推荐批次

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|pathFilter|string|false|none||路径过滤（可选）|
|requestId|string|false|none||业务请求 ID（幂等）|

#### Enum

|Name|Value|
|---|---|
|pathFilter|graduate|
|pathFilter|employment|
|pathFilter|overseas|

<h2 id="tocS_RecommendationFeedback">RecommendationFeedback</h2>

<a id="schemarecommendationfeedback"></a>
<a id="schema_RecommendationFeedback"></a>
<a id="tocSrecommendationfeedback"></a>
<a id="tocsrecommendationfeedback"></a>

```json
{
  "feedbackType": "HELPFUL",
  "comment": "与我预期的方向基本一致"
}

```

推荐反馈

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|feedbackType|string|true|none||反馈类型|
|comment|string|false|none||补充说明（选填）|

#### Enum

|Name|Value|
|---|---|
|feedbackType|HELPFUL|
|feedbackType|NEUTRAL|
|feedbackType|MISMATCH|
|feedbackType|NOT_INTERESTED|

<h2 id="tocS_RecommendationFeedbackRequest">RecommendationFeedbackRequest</h2>

<a id="schemarecommendationfeedbackrequest"></a>
<a id="schema_RecommendationFeedbackRequest"></a>
<a id="tocSrecommendationfeedbackrequest"></a>
<a id="tocsrecommendationfeedbackrequest"></a>

```json
{
  "feedbackType": "HELPFUL",
  "comment": "string"
}

```

推荐反馈请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|feedbackType|string|true|none||none|
|comment|string|false|none||补充说明（选填）|

#### Enum

|Name|Value|
|---|---|
|feedbackType|HELPFUL|
|feedbackType|NEUTRAL|
|feedbackType|MISMATCH|
|feedbackType|NOT_INTERESTED|

<h2 id="tocS_Goal">Goal</h2>

<a id="schemagoal"></a>
<a id="schema_Goal"></a>
<a id="tocSgoal"></a>
<a id="tocsgoal"></a>

```json
{
  "primary": {
    "directionId": "employment_backend",
    "name": "后端开发工程师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "backup": {
    "directionId": "data_analysis",
    "name": "数据分析师",
    "chosenAt": "2026-09-02T10:00:00+08:00"
  },
  "version": "G-v3",
  "updatedAt": "2026-09-02T10:00:00+08:00"
}

```

学生目标

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|primary|object|true|none||none|
|» directionId|string|true|none||主目标方向编码|
|» name|string|true|none||方向名称|
|» chosenAt|string(date-time)|false|none||选择时间|
|backup|object|false|none||none|
|» directionId|string|true|none||备选目标方向编码|
|» name|string|true|none||方向名称|
|» chosenAt|string(date-time)|false|none||选择时间|
|version|string|false|none||目标版本号|
|updatedAt|string(date-time)|false|none||最近变更时间|

<h2 id="tocS_GoalRequest">GoalRequest</h2>

<a id="schemagoalrequest"></a>
<a id="schema_GoalRequest"></a>
<a id="tocSgoalrequest"></a>
<a id="tocsgoalrequest"></a>

```json
{
  "primaryDirectionId": "employment_backend",
  "backupDirectionId": "data_analysis",
  "changeReason": "综合课程安排重新评估"
}

```

设置/变更目标

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|primaryDirectionId|string|true|none||主目标方向编码|
|backupDirectionId|string|false|none||备选目标方向编码（可选）|
|changeReason|string|false|none||变更原因（变更时必填，写入版本）|

<h2 id="tocS_GoalVersion">GoalVersion</h2>

<a id="schemagoalversion"></a>
<a id="schema_GoalVersion"></a>
<a id="tocSgoalversion"></a>
<a id="tocsgoalversion"></a>

```json
{
  "version": "G-v2",
  "primaryDirectionId": "employment_backend",
  "backupDirectionId": "data_analysis",
  "changeReason": "初期更偏好前端，结合测评后调整",
  "changedAt": "2026-09-05T14:00:00+08:00",
  "changedBy": "STUDENT"
}

```

目标版本

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|version|string|true|none||版本号|
|primaryDirectionId|string|true|none||主目标方向|
|backupDirectionId|string|false|none||备选目标方向|
|changeReason|string|false|none||变更原因|
|changedAt|string(date-time)|true|none||变更时间|
|changedBy|string|false|none||变更人角色|

#### Enum

|Name|Value|
|---|---|
|changedBy|STUDENT|
|changedBy|ADVISOR|
|changedBy|AI|

<h2 id="tocS_SemesterGoal">SemesterGoal</h2>

<a id="schemasemestergoal"></a>
<a id="schema_SemesterGoal"></a>
<a id="tocSsemestergoal"></a>
<a id="tocssemestergoal"></a>

```json
{
  "title": "掌握 Java 基础与面向对象编程",
  "abilityTag": "programming_basic"
}

```

学期目标

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|title|string|true|none||学期目标标题|
|abilityTag|string|false|none||关联能力标签|

<h2 id="tocS_MonthlyTask">MonthlyTask</h2>

<a id="schemamonthlytask"></a>
<a id="schema_MonthlyTask"></a>
<a id="tocSmonthlytask"></a>
<a id="tocsmonthlytask"></a>

```json
{
  "month": "2026-09",
  "title": "完成 Java 语法与面向对象章节学习",
  "taskType": "LEARNING",
  "estimatedHours": 12
}

```

月度任务模板项

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|month|string|true|none||月份（YYYY-MM）|
|title|string|true|none||任务标题|
|taskType|string|false|none||任务类型|
|estimatedHours|number|false|none||预计投入（小时）|

#### Enum

|Name|Value|
|---|---|
|taskType|LEARNING|
|taskType|PRACTICE|
|taskType|CAREER|
|taskType|REVIEW|

<h2 id="tocS_PlanDraft">PlanDraft</h2>

<a id="schemaplandraft"></a>
<a id="schema_PlanDraft"></a>
<a id="tocSplandraft"></a>
<a id="tocsplandraft"></a>

```json
{
  "goalSummary": "本学期完成后端技术基础入门：掌握 Java 语法与数据结构基础，完成 1 个可运行的小项目。",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排与兴趣变化调整。"
  ]
}

```

计划草案

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|goalSummary|string|true|none||目标摘要|
|semesterGoals|[[SemesterGoal](#schemasemestergoal)]|true|none||none|
|monthlyTasks|[[MonthlyTask](#schemamonthlytask)]|true|none||none|
|notes|[string]|false|none||none|

<h2 id="tocS_Plan">Plan</h2>

<a id="schemaplan"></a>
<a id="schema_Plan"></a>
<a id="tocSplan"></a>
<a id="tocsplan"></a>

```json
{
  "id": "PLAN-1002",
  "version": "P-v2",
  "status": "DRAFT",
  "source": "AI",
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ],
  "confirmedAt": "2026-09-02T10:05:00+08:00",
  "updatedAt": "2026-09-02T10:05:00+08:00"
}

```

学期计划

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||计划 ID|
|version|string|true|none||计划版本号|
|status|string|true|none||状态|
|source|string|false|none||生成来源|
|goalSummary|string|true|none||目标摘要|
|semesterGoals|[[SemesterGoal](#schemasemestergoal)]|false|none||none|
|monthlyTasks|[[MonthlyTask](#schemamonthlytask)]|false|none||none|
|notes|[string]|false|none||none|
|confirmedAt|string(date-time)|false|none||确认时间|
|updatedAt|string(date-time)|false|none||最近更新|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|CONFIRMED|
|source|AI|
|source|TEMPLATE|
|source|MANUAL|

<h2 id="tocS_PlanDraftRequest">PlanDraftRequest</h2>

<a id="schemaplandraftrequest"></a>
<a id="schema_PlanDraftRequest"></a>
<a id="tocSplandraftrequest"></a>
<a id="tocsplandraftrequest"></a>

```json
{
  "directionId": "employment_backend",
  "useAi": true,
  "requestId": "req-plan-001"
}

```

生成计划草案请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|directionId|string|false|none||方向编码（基于主目标生成时可省略）|
|useAi|boolean|false|none||是否调用 AI 生成（false 则使用任务模板）|
|requestId|string|false|none||业务请求 ID（幂等）|

<h2 id="tocS_PlanUpdate">PlanUpdate</h2>

<a id="schemaplanupdate"></a>
<a id="schema_PlanUpdate"></a>
<a id="tocSplanupdate"></a>
<a id="tocsplanupdate"></a>

```json
{
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ]
}

```

编辑计划请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|goalSummary|string|false|none||目标摘要|
|semesterGoals|[[SemesterGoal](#schemasemestergoal)]|false|none||none|
|monthlyTasks|[[MonthlyTask](#schemamonthlytask)]|false|none||none|
|notes|[string]|false|none||none|

<h2 id="tocS_PlanConfirmRequest">PlanConfirmRequest</h2>

<a id="schemaplanconfirmrequest"></a>
<a id="schema_PlanConfirmRequest"></a>
<a id="tocSplanconfirmrequest"></a>
<a id="tocsplanconfirmrequest"></a>

```json
{
  "confirm": true
}

```

确认计划

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|confirm|boolean|true|none||true 确认计划并写入正式版本|

<h2 id="tocS_Task">Task</h2>

<a id="schematask"></a>
<a id="schema_Task"></a>
<a id="tocStask"></a>
<a id="tocstask"></a>

```json
{
  "id": "T1",
  "month": "2026-09",
  "title": "完成 Java 语法与面向对象章节学习",
  "type": "LEARNING",
  "estHours": 12,
  "status": "PENDING",
  "deadline": "2026-09-30",
  "abilityTags": [
    "programming_basic"
  ],
  "note": "已做 9 道，双指针方法还不熟。",
  "checkedInAt": "2019-08-24T14:15:22Z",
  "checkin": {
    "id": "TC-001",
    "taskId": "T1",
    "doneDesc": "已完成，掌握了类与对象、集合基础。",
    "gains": "理解了面向对象三大特性。",
    "difficulties": "泛型部分较抽象。",
    "proofUrl": "string",
    "checkedInAt": "2026-09-20T10:00:00+08:00"
  }
}

```

计划任务

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||任务 ID|
|month|string|true|none||月份（YYYY-MM）|
|title|string|true|none||任务标题|
|type|string|true|none||任务类型|
|estHours|number|false|none||预计投入（小时）|
|status|string|true|none||任务状态|
|deadline|string(date)|false|none||建议完成时间（选填）|
|abilityTags|[string]|false|none||关联能力标签|
|note|string|false|none||备注（选填）|
|checkedInAt|string(date-time)|false|none||打卡时间|
|checkin|[TaskCheckin](#schemataskcheckin)|false|none||none|

#### Enum

|Name|Value|
|---|---|
|type|LEARNING|
|type|PRACTICE|
|type|CAREER|
|type|REVIEW|
|status|PENDING|
|status|DOING|
|status|DONE|
|status|DELAYED|
|status|ABANDONED|

<h2 id="tocS_TaskRequest">TaskRequest</h2>

<a id="schemataskrequest"></a>
<a id="schema_TaskRequest"></a>
<a id="tocStaskrequest"></a>
<a id="tocstaskrequest"></a>

```json
{
  "month": "2026-10",
  "title": "完成《数据结构》栈与队列的学习与练习",
  "type": "LEARNING",
  "estHours": 10,
  "deadline": "2019-08-24",
  "abilityTags": [
    "algorithm_basic"
  ]
}

```

新增任务请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|month|string|true|none||月份（YYYY-MM）|
|title|string|true|none||任务标题|
|type|string|true|none||任务类型|
|estHours|number|false|none||预计投入（小时）|
|deadline|string(date)|false|none||建议完成时间（选填）|
|abilityTags|[string]|false|none||none|

#### Enum

|Name|Value|
|---|---|
|type|LEARNING|
|type|PRACTICE|
|type|CAREER|
|type|REVIEW|

<h2 id="tocS_TaskStatusUpdate">TaskStatusUpdate</h2>

<a id="schemataskstatusupdate"></a>
<a id="schema_TaskStatusUpdate"></a>
<a id="tocStaskstatusupdate"></a>
<a id="tocstaskstatusupdate"></a>

```json
{
  "status": "PENDING",
  "reason": "与课程时间冲突",
  "note": "推迟到 11 月中旬"
}

```

更新任务状态

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|status|string|true|none||目标任务状态|
|reason|string|false|none||延期/放弃原因（状态为 DELAYED / ABANDONED 时必填）|
|note|string|false|none||备注（选填）|

#### Enum

|Name|Value|
|---|---|
|status|PENDING|
|status|DOING|
|status|DONE|
|status|DELAYED|
|status|ABANDONED|

<h2 id="tocS_TaskCheckin">TaskCheckin</h2>

<a id="schemataskcheckin"></a>
<a id="schema_TaskCheckin"></a>
<a id="tocStaskcheckin"></a>
<a id="tocstaskcheckin"></a>

```json
{
  "id": "TC-001",
  "taskId": "T1",
  "doneDesc": "已完成，掌握了类与对象、集合基础。",
  "gains": "理解了面向对象三大特性。",
  "difficulties": "泛型部分较抽象。",
  "proofUrl": "string",
  "checkedInAt": "2026-09-20T10:00:00+08:00"
}

```

任务打卡

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||打卡记录 ID|
|taskId|string|true|none||任务 ID|
|doneDesc|string|true|none||完成说明|
|gains|string|false|none||收获（选填）|
|difficulties|string|false|none||遇到的困难（选填）|
|proofUrl|string|false|none||证明材料 URL（选填）|
|checkedInAt|string(date-time)|false|none||打卡时间|

<h2 id="tocS_TaskCheckinRequest">TaskCheckinRequest</h2>

<a id="schemataskcheckinrequest"></a>
<a id="schema_TaskCheckinRequest"></a>
<a id="tocStaskcheckinrequest"></a>
<a id="tocstaskcheckinrequest"></a>

```json
{
  "doneDesc": "已完成，掌握了类与对象、集合基础。",
  "gains": "string",
  "difficulties": "string",
  "proofUrl": "string"
}

```

任务打卡请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|doneDesc|string|true|none||完成说明|
|gains|string|false|none||收获（选填）|
|difficulties|string|false|none||遇到的困难（选填）|
|proofUrl|string|false|none||证明材料 URL（选填）|

<h2 id="tocS_Reminder">Reminder</h2>

<a id="schemareminder"></a>
<a id="schema_Reminder"></a>
<a id="tocSreminder"></a>
<a id="tocsreminder"></a>

```json
{
  "id": "RM-001",
  "type": "TASK_DEADLINE",
  "title": "本月还有 3 项任务待完成",
  "content": "距离月末还有 7 天，请及时更新任务状态并完成复盘。",
  "read": false,
  "createdAt": "2026-10-24T09:00:00+08:00"
}

```

站内提醒

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||提醒 ID|
|type|string|true|none||提醒类型|
|title|string|true|none||标题|
|content|string|true|none||内容|
|read|boolean|false|none||是否已读|
|createdAt|string(date-time)|false|none||创建时间|

#### Enum

|Name|Value|
|---|---|
|type|TASK_DEADLINE|
|type|REVIEW_REMIND|
|type|ADVISOR_REPLY|
|type|PLAN_UPDATE|

<h2 id="tocS_ReviewContent">ReviewContent</h2>

<a id="schemareviewcontent"></a>
<a id="schema_ReviewContent"></a>
<a id="tocSreviewcontent"></a>
<a id="tocsreviewcontent"></a>

```json
{
  "done": "完成 Java 语法与通讯录项目，通过四六级报名",
  "undone": "LeetCode 练习因时间不足未完成一半",
  "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
  "ability": "编程能力明显提升，能独立写 300 行左右的程序",
  "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
}

```

复盘内容

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|done|string|false|none||本阶段完成情况|
|undone|string|false|none||未完成情况及原因|
|interest|string|false|none||方向兴趣变化|
|ability|string|false|none||能力提升|
|next|string|false|none||下一步安排|

<h2 id="tocS_Review">Review</h2>

<a id="schemareview"></a>
<a id="schema_Review"></a>
<a id="tocSreview"></a>
<a id="tocsreview"></a>

```json
{
  "id": "R1",
  "cycle": "2026-09",
  "status": "DRAFT",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "aiSummary": "9 月你的编程基础快速提升…",
  "aiSuggest": [
    "将任务从 6 条收敛到 3 条主线"
  ],
  "advisorRequested": true,
  "advisorReply": "string",
  "submittedAt": "2026-10-02T09:00:00+08:00"
}

```

阶段复盘

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||复盘 ID|
|cycle|string|true|none||复盘周期（YYYY-MM）|
|status|string|true|none||状态|
|content|[ReviewContent](#schemareviewcontent)|true|none||none|
|aiSummary|string|false|none||AI 阶段总结（生成后存在）|
|aiSuggest|[string]|false|none||AI 调整建议|
|advisorRequested|boolean|false|none||是否已申请辅导员指导|
|advisorReply|string|false|none||辅导员回复（选填）|
|submittedAt|string(date-time)|false|none||提交时间|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|SUBMITTED|

<h2 id="tocS_ReviewDraftRequest">ReviewDraftRequest</h2>

<a id="schemareviewdraftrequest"></a>
<a id="schema_ReviewDraftRequest"></a>
<a id="tocSreviewdraftrequest"></a>
<a id="tocsreviewdraftrequest"></a>

```json
{
  "cycle": "2026-09",
  "content": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  }
}

```

保存复盘草稿

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|cycle|string|true|none||复盘周期（YYYY-MM）|
|content|[ReviewContent](#schemareviewcontent)|true|none||none|

<h2 id="tocS_GuidanceRequestPayload">GuidanceRequestPayload</h2>

<a id="schemaguidancerequestpayload"></a>
<a id="schema_GuidanceRequestPayload"></a>
<a id="tocSguidancerequestpayload"></a>
<a id="tocsguidancerequestpayload"></a>

```json
{
  "message": "想咨询备选目标是否需要调整"
}

```

申请辅导员指导

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|message|string|false|none||向辅导员说明的问题（选填）|

<h2 id="tocS_AdoptAdviceRequest">AdoptAdviceRequest</h2>

<a id="schemaadoptadvicerequest"></a>
<a id="schema_AdoptAdviceRequest"></a>
<a id="tocSadoptadvicerequest"></a>
<a id="tocsadoptadvicerequest"></a>

```json
{
  "adopt": true,
  "planId": "string"
}

```

采纳调整建议

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|adopt|boolean|true|none||true 采纳调整建议并写入正式计划|
|planId|string|false|none||目标计划 ID（不传则基于当前计划生成新版本）|

<h2 id="tocS_AdvisorStudent">AdvisorStudent</h2>

<a id="schemaadvisorstudent"></a>
<a id="schema_AdvisorStudent"></a>
<a id="tocSadvisorstudent"></a>
<a id="tocsadvisorstudent"></a>

```json
{
  "id": "S1001",
  "name": "李明",
  "className": "计科2601",
  "completeness": 92,
  "assessed": true,
  "path": "employment",
  "direction": "后端开发工程师",
  "primaryGoal": "后端开发",
  "planRate": 71,
  "lastReview": "2026-10-02",
  "askGuidance": false,
  "status": "good"
}

```

辅导员视图·学生概要

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||学生 ID|
|name|string|true|none||姓名|
|className|string|true|none||班级|
|completeness|integer|false|none||资料完整度|
|assessed|boolean|false|none||测评完成|
|path|string|false|none||当前路径|
|direction|string|false|none||当前方向|
|primaryGoal|string|false|none||主目标|
|planRate|number|false|none||计划完成率（%）|
|lastReview|string(date)|false|none||最近复盘时间|
|askGuidance|boolean|false|none||是否待处理指导申请|
|status|string|false|none||概览标记|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|status|good|
|status|todo|
|status|late|
|status|review|

<h2 id="tocS_AdvisorStudentFilter">AdvisorStudentFilter</h2>

<a id="schemaadvisorstudentfilter"></a>
<a id="schema_AdvisorStudentFilter"></a>
<a id="tocSadvisorstudentfilter"></a>
<a id="tocsadvisorstudentfilter"></a>

```json
{
  "path": "graduate",
  "directionId": "string",
  "goalStatus": "HAS_GOAL",
  "reviewStatus": "LONG_NO_REVIEW",
  "guidanceRequested": true,
  "keyword": "string"
}

```

学生筛选条件

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|path|string|false|none||按路径筛选|
|directionId|string|false|none||按方向筛选|
|goalStatus|string|false|none||目标状态|
|reviewStatus|string|false|none||复盘状态|
|guidanceRequested|boolean|false|none||仅看已申请指导|
|keyword|string|false|none||姓名/学号关键字|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|goalStatus|HAS_GOAL|
|goalStatus|NO_GOAL|
|reviewStatus|LONG_NO_REVIEW|
|reviewStatus|REVIEWED_THIS_MONTH|

<h2 id="tocS_GuidanceComment">GuidanceComment</h2>

<a id="schemaguidancecomment"></a>
<a id="schema_GuidanceComment"></a>
<a id="tocSguidancecomment"></a>
<a id="tocsguidancecomment"></a>

```json
{
  "id": "GC-001",
  "studentId": "S1001",
  "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
  "adviceType": "COMMENT",
  "suggestedTask": "每周完成 3 道 LeetCode 简单题",
  "retestReason": "string",
  "createdAt": "2026-10-03T10:00:00+08:00"
}

```

指导意见

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||意见 ID|
|studentId|string|true|none||学生 ID|
|content|string|true|none||指导意见正文|
|adviceType|string|true|none||建议类型|
|suggestedTask|string|false|none||建议任务（SUGGEST_TASK 时）|
|retestReason|string|false|none||建议重新测评原因（SUGGEST_RETEST 时）|
|createdAt|string(date-time)|true|none||填写时间|

#### Enum

|Name|Value|
|---|---|
|adviceType|COMMENT|
|adviceType|SUGGEST_TASK|
|adviceType|SUGGEST_RETEST|

<h2 id="tocS_GuidanceCommentRequest">GuidanceCommentRequest</h2>

<a id="schemaguidancecommentrequest"></a>
<a id="schema_GuidanceCommentRequest"></a>
<a id="tocSguidancecommentrequest"></a>
<a id="tocsguidancecommentrequest"></a>

```json
{
  "content": "建议 10 月聚焦数据结构主线。",
  "adviceType": "COMMENT",
  "suggestedTask": "string",
  "retestReason": "string"
}

```

填写指导意见

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|content|string|true|none||指导意见正文|
|adviceType|string|true|none||none|
|suggestedTask|string|false|none||建议任务（选填）|
|retestReason|string|false|none||建议重新测评原因（选填）|

#### Enum

|Name|Value|
|---|---|
|adviceType|COMMENT|
|adviceType|SUGGEST_TASK|
|adviceType|SUGGEST_RETEST|

<h2 id="tocS_StudentDetailView">StudentDetailView</h2>

<a id="schemastudentdetailview"></a>
<a id="schema_StudentDetailView"></a>
<a id="tocSstudentdetailview"></a>
<a id="tocsstudentdetailview"></a>

```json
{
  "profile": {
    "userId": "S1001",
    "name": "李明",
    "className": "计科2601",
    "grade": "2026级",
    "majorCategory": "计算机类",
    "basic": {
      "gender": "男",
      "hometown": "重庆",
      "birthday": "2008-05-14",
      "phone": "138****6721"
    },
    "academic": {
      "math": 4,
      "english": 3,
      "programming": 2,
      "note": "高中数学较好，英语一般，编程刚起步"
    },
    "interestPrefs": [
      "编程"
    ],
    "abilitySelf": {
      "programming": 2,
      "math": 4,
      "english": 3,
      "communication": 4,
      "organization": 3
    },
    "values": [
      "成长"
    ],
    "experiences": {
      "id": "EXP-001",
      "type": "竞赛",
      "title": "数学建模校赛 · 二等奖",
      "startDate": "2026-05",
      "endDate": "2026-06",
      "description": "三人组队，负责建模与论文撰写，完成‘校园快递点选址’问题。",
      "attachmentUrl": "string"
    },
    "developmentIntention": "employment",
    "constraints": [
      "愿意在课余投入学习"
    ],
    "completeness": 92,
    "updatedAt": "2026-09-30T09:12:00+08:00"
  },
  "portrait": {
    "id": "PS-1002",
    "version": 2,
    "generatedAt": "2026-09-01T09:13:00+08:00",
    "sourceVersion": "Q v2 + 自主填报 v3",
    "completeness": 92,
    "dimensions": [
      {
        "key": "interest",
        "name": "兴趣",
        "score": 78
      }
    ],
    "summary": "你的兴趣集中在技术问题求解与动手实践…",
    "strengths": [
      "数学基础较好，是算法与数据方向的加分项"
    ],
    "explore": [
      "编程实践有待积累，建议从完成小项目开始"
    ],
    "feedback": {
      "feedbackType": "MATCH",
      "comment": "学习能力描述与我实际情况基本一致"
    }
  },
  "recommendation": {
    "runId": "190001",
    "profileVersion": 2,
    "ruleVersion": "R1.0",
    "generatedAt": "2026-09-01T09:13:30+08:00",
    "status": "RUNNING",
    "results": [
      {
        "directionId": "employment_backend",
        "rank": 1,
        "score": 82.4,
        "confidence": "HIGH",
        "reasons": [
          "偏好结构化问题求解（兴趣维度）"
        ],
        "strengths": [
          "数学与逻辑基础较好"
        ],
        "gaps": [
          "缺少系统编程实践"
        ],
        "semesterActions": [
          "完成《程序设计基础》课程"
        ],
        "feedback": {
          "feedbackType": "[",
          "comment": "与我预期的方向基本一致"
        }
      }
    ]
  },
  "goal": {
    "primary": {
      "directionId": "employment_backend",
      "name": "后端开发工程师",
      "chosenAt": "2026-09-02T10:00:00+08:00"
    },
    "backup": {
      "directionId": "data_analysis",
      "name": "数据分析师",
      "chosenAt": "2026-09-02T10:00:00+08:00"
    },
    "version": "G-v3",
    "updatedAt": "2026-09-02T10:00:00+08:00"
  },
  "plan": {
    "id": "PLAN-1002",
    "version": "P-v2",
    "status": "DRAFT",
    "source": "AI",
    "goalSummary": "本学期完成后端技术基础入门…",
    "semesterGoals": [
      {
        "title": "掌握 Java 基础与面向对象编程",
        "abilityTag": "programming_basic"
      }
    ],
    "monthlyTasks": [
      {
        "month": "2026-09",
        "title": "完成 Java 语法与面向对象章节学习",
        "taskType": "LEARNING",
        "estimatedHours": 12
      }
    ],
    "notes": [
      "任务可随课程安排调整"
    ],
    "confirmedAt": "2026-09-02T10:05:00+08:00",
    "updatedAt": "2026-09-02T10:05:00+08:00"
  },
  "tasks": [
    {
      "id": "T1",
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "type": "LEARNING",
      "estHours": 12,
      "status": "PENDING",
      "deadline": "2026-09-30",
      "abilityTags": [
        "programming_basic"
      ],
      "note": "已做 9 道，双指针方法还不熟。",
      "checkedInAt": "2019-08-24T14:15:22Z",
      "checkin": {
        "id": "TC-001",
        "taskId": "T1",
        "doneDesc": "已完成，掌握了类与对象、集合基础。",
        "gains": "理解了面向对象三大特性。",
        "difficulties": "泛型部分较抽象。",
        "proofUrl": "string",
        "checkedInAt": "2026-09-20T10:00:00+08:00"
      }
    }
  ],
  "reviews": [
    {
      "id": "R1",
      "cycle": "2026-09",
      "status": "DRAFT",
      "content": {
        "done": "完成 Java 语法与通讯录项目，通过四六级报名",
        "undone": "LeetCode 练习因时间不足未完成一半",
        "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
        "ability": "编程能力明显提升，能独立写 300 行左右的程序",
        "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
      },
      "aiSummary": "9 月你的编程基础快速提升…",
      "aiSuggest": [
        "将任务从 6 条收敛到 3 条主线"
      ],
      "advisorRequested": true,
      "advisorReply": "string",
      "submittedAt": "2026-10-02T09:00:00+08:00"
    }
  ],
  "guidance": [
    {
      "id": "GC-001",
      "studentId": "S1001",
      "content": "建议 10 月聚焦数据结构主线，减少并行任务。",
      "adviceType": "COMMENT",
      "suggestedTask": "每周完成 3 道 LeetCode 简单题",
      "retestReason": "string",
      "createdAt": "2026-10-03T10:00:00+08:00"
    }
  ]
}

```

学生详情总览

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|profile|[StudentProfile](#schemastudentprofile)|false|none||none|
|portrait|[ProfileSnapshot](#schemaprofilesnapshot)|false|none||none|
|recommendation|[RecommendationRun](#schemarecommendationrun)|false|none||none|
|goal|[Goal](#schemagoal)|false|none||none|
|plan|[Plan](#schemaplan)|false|none||none|
|tasks|[[Task](#schematask)]|false|none||none|
|reviews|[[Review](#schemareview)]|false|none||none|
|guidance|[[GuidanceComment](#schemaguidancecomment)]|false|none||none|

<h2 id="tocS_AttentionStudent">AttentionStudent</h2>

<a id="schemaattentionstudent"></a>
<a id="schema_AttentionStudent"></a>
<a id="tocSattentionstudent"></a>
<a id="tocsattentionstudent"></a>

```json
{
  "student": {
    "id": "S1001",
    "name": "李明",
    "className": "计科2601",
    "completeness": 92,
    "assessed": true,
    "path": "employment",
    "direction": "后端开发工程师",
    "primaryGoal": "后端开发",
    "planRate": 71,
    "lastReview": "2026-10-02",
    "askGuidance": false,
    "status": "good"
  },
  "reasons": [
    "已申请辅导员指导"
  ]
}

```

需关注学生

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|student|[AdvisorStudent](#schemaadvisorstudent)|true|none||none|
|reasons|[string]|true|none||需要关注的原因|

<h2 id="tocS_AdvisorStatistics">AdvisorStatistics</h2>

<a id="schemaadvisorstatistics"></a>
<a id="schema_AdvisorStatistics"></a>
<a id="tocSadvisorstatistics"></a>
<a id="tocsadvisorstatistics"></a>

```json
{
  "totalStudents": 30,
  "assessedCount": 24,
  "planMadeCount": 20,
  "reviewedCount": 14,
  "pathDistribution": [
    {
      "path": "graduate",
      "count": 10
    }
  ],
  "taskCompletionRate": 68
}

```

群体统计

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|totalStudents|integer|true|none||所带学生总数|
|assessedCount|integer|false|none||已完成测评人数|
|planMadeCount|integer|false|none||已制定计划人数|
|reviewedCount|integer|false|none||本月已复盘人数|
|pathDistribution|[object]|false|none||路径分布|
|» path|string|false|none||none|
|» count|integer|false|none||none|
|taskCompletionRate|number|false|none||平均任务完成率（%）|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|path|undecided|

<h2 id="tocS_AdminUser">AdminUser</h2>

<a id="schemaadminuser"></a>
<a id="schema_AdminUser"></a>
<a id="tocSadminuser"></a>
<a id="tocsadminuser"></a>

```json
{
  "id": "S1001",
  "username": "2026011301",
  "name": "李明",
  "role": "STUDENT",
  "className": "计科2601",
  "status": "ACTIVE",
  "lastLoginAt": "2026-10-01T09:00:00+08:00",
  "createdAt": "2026-08-25T09:00:00+08:00"
}

```

管理端·用户

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||用户 ID|
|username|string|true|none||登录名|
|name|string|true|none||姓名|
|role|string|true|none||角色|
|className|string|false|none||班级|
|status|string|true|none||账号状态|
|lastLoginAt|string(date-time)|false|none||最近登录|
|createdAt|string(date-time)|false|none||创建时间|

#### Enum

|Name|Value|
|---|---|
|role|STUDENT|
|role|ADVISOR|
|role|ADMIN|
|status|ACTIVE|
|status|LOCKED|
|status|DISABLED|

<h2 id="tocS_AdminUserUpdate">AdminUserUpdate</h2>

<a id="schemaadminuserupdate"></a>
<a id="schema_AdminUserUpdate"></a>
<a id="tocSadminuserupdate"></a>
<a id="tocsadminuserupdate"></a>

```json
{
  "status": "ACTIVE",
  "className": "string"
}

```

更新用户

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|status|string|false|none||none|
|className|string|false|none||班级（选填）|

#### Enum

|Name|Value|
|---|---|
|status|ACTIVE|
|status|LOCKED|
|status|DISABLED|

<h2 id="tocS_WhitelistEntry">WhitelistEntry</h2>

<a id="schemawhitelistentry"></a>
<a id="schema_WhitelistEntry"></a>
<a id="tocSwhitelistentry"></a>
<a id="tocswhitelistentry"></a>

```json
{
  "id": "WL-001",
  "studentNo": "2026011309",
  "className": "计科2601",
  "verifyCode": "202609",
  "used": false,
  "createdAt": "2026-08-20T10:00:00+08:00"
}

```

白名单条目

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||条目 ID|
|studentNo|string|true|none||学号|
|className|string|false|none||班级（可选）|
|verifyCode|string|true|none||校验码|
|used|boolean|false|none||是否已被注册使用|
|createdAt|string(date-time)|false|none||导入时间|

<h2 id="tocS_WhitelistCreate">WhitelistCreate</h2>

<a id="schemawhitelistcreate"></a>
<a id="schema_WhitelistCreate"></a>
<a id="tocSwhitelistcreate"></a>
<a id="tocswhitelistcreate"></a>

```json
{
  "studentNo": "2026011309",
  "className": "string",
  "verifyCode": "string"
}

```

新增白名单

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentNo|string|true|none||学号|
|className|string|false|none||班级（可选）|
|verifyCode|string|true|none||校验码（可由系统生成）|

<h2 id="tocS_WhitelistImportResult">WhitelistImportResult</h2>

<a id="schemawhitelistimportresult"></a>
<a id="schema_WhitelistImportResult"></a>
<a id="tocSwhitelistimportresult"></a>
<a id="tocswhitelistimportresult"></a>

```json
{
  "successCount": 42,
  "failCount": 3,
  "failures": [
    {
      "row": 7,
      "studentNo": "2026011399",
      "reason": "重复学号"
    }
  ]
}

```

白名单批量导入结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|successCount|integer|true|none||成功条数|
|failCount|integer|true|none||失败条数|
|failures|[object]|false|none||失败明细|
|» row|integer|false|none||CSV 行号|
|» studentNo|string|false|none||学号|
|» reason|string|false|none||失败原因|

<h2 id="tocS_AdvisorRelation">AdvisorRelation</h2>

<a id="schemaadvisorrelation"></a>
<a id="schema_AdvisorRelation"></a>
<a id="tocSadvisorrelation"></a>
<a id="tocsadvisorrelation"></a>

```json
{
  "id": "REL-001",
  "advisorId": "A2001",
  "advisorName": "王老师",
  "studentId": "S1001",
  "studentName": "李明",
  "createdAt": "2026-08-25T09:00:00+08:00"
}

```

辅导员学生关系

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||关系 ID|
|advisorId|string|true|none||辅导员用户 ID|
|advisorName|string|false|none||辅导员姓名|
|studentId|string|true|none||学生 ID|
|studentName|string|false|none||学生姓名|
|createdAt|string(date-time)|false|none||建立时间|

<h2 id="tocS_RelationRequest">RelationRequest</h2>

<a id="schemarelationrequest"></a>
<a id="schema_RelationRequest"></a>
<a id="tocSrelationrequest"></a>
<a id="tocsrelationrequest"></a>

```json
{
  "advisorId": "A2001",
  "studentIds": [
    "S1001"
  ]
}

```

批量建立关系

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|advisorId|string|true|none||辅导员用户 ID|
|studentIds|[string]|true|none||学生 ID 列表|

<h2 id="tocS_AdminQuestionnaire">AdminQuestionnaire</h2>

<a id="schemaadminquestionnaire"></a>
<a id="schema_AdminQuestionnaire"></a>
<a id="tocSadminquestionnaire"></a>
<a id="tocsadminquestionnaire"></a>

```json
{
  "id": "holland",
  "type": "holland",
  "typeName": "霍兰德兴趣简版",
  "version": "v2",
  "status": "DRAFT",
  "questionCount": 6,
  "updatedAt": "2026-09-01T00:00:00+08:00"
}

```

管理端·问卷

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||问卷 ID|
|type|string|true|none||类型编码|
|typeName|string|false|none||类型名称|
|version|string|true|none||当前版本|
|status|string|true|none||状态|
|questionCount|integer|false|none||题目数|
|updatedAt|string(date-time)|false|none||最近更新|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_AdminQuestionnaireCreate">AdminQuestionnaireCreate</h2>

<a id="schemaadminquestionnairecreate"></a>
<a id="schema_AdminQuestionnaireCreate"></a>
<a id="tocSadminquestionnairecreate"></a>
<a id="tocsadminquestionnairecreate"></a>

```json
{
  "type": "holland",
  "name": "霍兰德兴趣简版 v2",
  "questions": [
    {
      "id": "q-holland-1",
      "text": "以下学习活动，你最愿意投入时间的是？",
      "type": "CHOICE",
      "dim": "programming",
      "labels": [
        "一般"
      ],
      "options": [
        {
          "text": "调试程序直到它运行起来",
          "scores": {
            "interest": null,
            "values": null,
            "ability": null,
            "academic": null,
            "tendency": null,
            "practice": null
          }
        }
      ]
    }
  ]
}

```

新建问卷

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|type|string|true|none||类型编码（四选一）|
|name|string|true|none||问卷名称|
|questions|[[Question](#schemaquestion)]|true|none||none|

#### Enum

|Name|Value|
|---|---|
|type|holland|
|type|values|
|type|ability|
|type|tendency|

<h2 id="tocS_QuestionnaireStatusUpdate">QuestionnaireStatusUpdate</h2>

<a id="schemaquestionnairestatusupdate"></a>
<a id="schema_QuestionnaireStatusUpdate"></a>
<a id="tocSquestionnairestatusupdate"></a>
<a id="tocsquestionnairestatusupdate"></a>

```json
{
  "status": "PUBLISHED"
}

```

发布/停用问卷

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|status|string|true|none||none|

#### Enum

|Name|Value|
|---|---|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_AdminDirection">AdminDirection</h2>

<a id="schemaadmindirection"></a>
<a id="schema_AdminDirection"></a>
<a id="tocSadmindirection"></a>
<a id="tocsadmindirection"></a>

```json
{
  "id": "employment_backend",
  "name": "后端开发工程师",
  "path": "graduate",
  "status": "PUBLISHED",
  "sortOrder": 1,
  "applicableMajors": [
    "计算机类"
  ],
  "updatedAt": "2026-07-31T00:00:00+08:00"
}

```

管理端·方向

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||方向编码|
|name|string|true|none||方向名称|
|path|string|true|none||所属路径|
|status|string|true|none||状态|
|sortOrder|integer|false|none||展示顺序|
|applicableMajors|[string]|false|none||适用专业|
|updatedAt|string(date-time)|false|none||最近更新|

#### Enum

|Name|Value|
|---|---|
|path|graduate|
|path|employment|
|path|overseas|
|status|PUBLISHED|
|status|DISABLED|
|status|DRAFT|

<h2 id="tocS_DirectionStatusUpdate">DirectionStatusUpdate</h2>

<a id="schemadirectionstatusupdate"></a>
<a id="schema_DirectionStatusUpdate"></a>
<a id="tocSdirectionstatusupdate"></a>
<a id="tocsdirectionstatusupdate"></a>

```json
{
  "status": "PUBLISHED"
}

```

方向启停

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|status|string|true|none||none|

#### Enum

|Name|Value|
|---|---|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_AbilityTag">AbilityTag</h2>

<a id="schemaabilitytag"></a>
<a id="schema_AbilityTag"></a>
<a id="tocSabilitytag"></a>
<a id="tocsabilitytag"></a>

```json
{
  "id": "programming_basic",
  "name": "编程基础",
  "category": "能力",
  "status": "ACTIVE"
}

```

能力标签

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||标签编码|
|name|string|true|none||标签名称|
|category|string|false|none||分类|
|status|string|false|none||none|

#### Enum

|Name|Value|
|---|---|
|status|ACTIVE|
|status|DISABLED|

<h2 id="tocS_TaskTemplate">TaskTemplate</h2>

<a id="schematasktemplate"></a>
<a id="schema_TaskTemplate"></a>
<a id="tocStasktemplate"></a>
<a id="tocstasktemplate"></a>

```json
{
  "id": "TPL-backend",
  "directionId": "employment_backend",
  "name": "后端开发方向任务模板",
  "goalSummary": "string",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "status": "ACTIVE"
}

```

任务模板

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||模板 ID|
|directionId|string|true|none||方向编码|
|name|string|true|none||模板名称|
|goalSummary|string|false|none||目标摘要模板|
|semesterGoals|[[SemesterGoal](#schemasemestergoal)]|false|none||none|
|monthlyTasks|[[MonthlyTask](#schemamonthlytask)]|false|none||none|
|status|string|true|none||状态|

#### Enum

|Name|Value|
|---|---|
|status|ACTIVE|
|status|DISABLED|

<h2 id="tocS_ModelConfig">ModelConfig</h2>

<a id="schemamodelconfig"></a>
<a id="schema_ModelConfig"></a>
<a id="tocSmodelconfig"></a>
<a id="tocsmodelconfig"></a>

```json
{
  "key": "llm.provider",
  "value": "openai",
  "updatedAt": "2026-09-01T00:00:00+08:00",
  "updatedBy": "系统管理员"
}

```

模型配置项

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|key|string|true|none||配置项键|
|value|string|true|none||配置值|
|updatedAt|string(date-time)|false|none||最近更新|
|updatedBy|string|false|none||更新人|

<h2 id="tocS_PromptVersion">PromptVersion</h2>

<a id="schemapromptversion"></a>
<a id="schema_PromptVersion"></a>
<a id="tocSpromptversion"></a>
<a id="tocspromptversion"></a>

```json
{
  "version": "v1.3",
  "scene": "recommendation_explain",
  "status": "DRAFT",
  "content": "string",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}

```

提示词版本

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|version|string|true|none||版本号|
|scene|string|true|none||场景|
|status|string|true|none||状态|
|content|string|false|none||提示词内容|
|publishedAt|string(date-time)|false|none||发布时间|

#### Enum

|Name|Value|
|---|---|
|scene|recommendation_explain|
|scene|plan_generate|
|scene|review_summarize|
|scene|career_chat|
|status|DRAFT|
|status|PUBLISHED|
|status|DISABLED|

<h2 id="tocS_WeightConfig">WeightConfig</h2>

<a id="schemaweightconfig"></a>
<a id="schema_WeightConfig"></a>
<a id="tocSweightconfig"></a>
<a id="tocsweightconfig"></a>

```json
{
  "version": "R1.0",
  "weights": {
    "interest": 0.2,
    "values": 0.2,
    "ability": 0.2,
    "academic": 0.2,
    "tendency": 0.2,
    "practice": 0.2
  },
  "minConfidence": 0,
  "topN": 5,
  "status": "DRAFT",
  "publishedAt": "2026-09-01T00:00:00+08:00"
}

```

推荐权重配置

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|version|string|true|none||规则版本号|
|weights|object|true|none||none|
|» interest|number|false|none||兴趣权重|
|» values|number|false|none||职业价值观权重|
|» ability|number|false|none||能力基础权重|
|» academic|number|false|none||学业基础权重|
|» tendency|number|false|none||发展倾向权重|
|» practice|number|false|none||实践经历权重|
|minConfidence|number|false|none||推荐的最低可信阈值|
|topN|integer|false|none||返回推荐数量上限|
|status|string|true|none||状态|
|publishedAt|string(date-time)|false|none||发布时间|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|PUBLISHED|

<h2 id="tocS_CurriculumImportJob">CurriculumImportJob</h2>

<a id="schemacurriculumimportjob"></a>
<a id="schema_CurriculumImportJob"></a>
<a id="tocScurriculumimportjob"></a>
<a id="tocscurriculumimportjob"></a>

```json
{
  "id": "CJ-001",
  "filename": "软件工程培养方案2026.pdf",
  "status": "UPLOADED",
  "totalItems": 180,
  "parsedItems": 172,
  "confidence": 86,
  "createdAt": "2026-09-10T10:00:00+08:00"
}

```

培养方案导入任务

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||导入任务 ID|
|filename|string|true|none||文件名|
|status|string|true|none||状态|
|totalItems|integer|false|none||识别到的课程总数|
|parsedItems|integer|false|none||已解析条目数|
|confidence|number|false|none||整体解析置信度（0–100）|
|createdAt|string(date-time)|false|none||创建时间|

#### Enum

|Name|Value|
|---|---|
|status|UPLOADED|
|status|PARSING|
|status|REVIEW_REQUIRED|
|status|PUBLISHED|
|status|FAILED|

<h2 id="tocS_ImportItem">ImportItem</h2>

<a id="schemaimportitem"></a>
<a id="schema_ImportItem"></a>
<a id="tocSimportitem"></a>
<a id="tocsimportitem"></a>

```json
{
  "id": "IT-001",
  "jobId": "CJ-001",
  "courseCode": "CS101",
  "courseName": "程序设计基础",
  "semester": "2026-2027-1",
  "credits": 4,
  "hours": 64,
  "category": "专业基础",
  "module": "必修",
  "prerequisites": [
    "CS100"
  ],
  "abilityTags": [
    "programming_basic"
  ],
  "confidence": 92,
  "pageRef": "第 12 页",
  "status": "PENDING"
}

```

待审核课程

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||条目 ID|
|jobId|string|true|none||导入任务 ID|
|courseCode|string|true|none||课程代码|
|courseName|string|true|none||课程名称|
|semester|string|false|none||开课学期|
|credits|number|false|none||学分|
|hours|number|false|none||学时|
|category|string|false|none||课程类别|
|module|string|false|none||课程模块|
|prerequisites|[string]|false|none||先修课程代码|
|abilityTags|[string]|false|none||课程能力标签（需人工补充）|
|confidence|number|false|none||单条解析置信度（0–100）|
|pageRef|string|false|none||来源页码/原文片段|
|status|string|true|none||审核状态|

#### Enum

|Name|Value|
|---|---|
|status|PENDING|
|status|APPROVED|
|status|REJECTED|
|status|MERGED|

<h2 id="tocS_ImportItemUpdate">ImportItemUpdate</h2>

<a id="schemaimportitemupdate"></a>
<a id="schema_ImportItemUpdate"></a>
<a id="tocSimportitemupdate"></a>
<a id="tocsimportitemupdate"></a>

```json
{
  "courseCode": "string",
  "courseName": "string",
  "semester": "string",
  "credits": 0,
  "hours": 0,
  "category": "string",
  "module": "string",
  "prerequisites": [
    "CS100"
  ],
  "abilityTags": [
    "programming_basic"
  ],
  "status": "APPROVED"
}

```

校核课程请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|courseCode|string|false|none||课程代码（可修正）|
|courseName|string|false|none||课程名称（可修正）|
|semester|string|false|none||开课学期（可修正）|
|credits|number|false|none||学分（可修正）|
|hours|number|false|none||学时（可修正）|
|category|string|false|none||课程类别|
|module|string|false|none||课程模块|
|prerequisites|[string]|false|none||none|
|abilityTags|[string]|false|none||none|
|status|string|false|none||审核操作|

#### Enum

|Name|Value|
|---|---|
|status|APPROVED|
|status|REJECTED|

<h2 id="tocS_BatchReviewAction">BatchReviewAction</h2>

<a id="schemabatchreviewaction"></a>
<a id="schema_BatchReviewAction"></a>
<a id="tocSbatchreviewaction"></a>
<a id="tocsbatchreviewaction"></a>

```json
{
  "itemId": "IT-001",
  "action": "APPROVE",
  "targetItemId": "IT-002",
  "abilityTags": [
    "programming_basic"
  ]
}

```

批量校核动作

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|itemId|string|true|none||条目 ID|
|action|string|true|none||操作|
|targetItemId|string|false|none||合并目标条目 ID（MERGE 时）|
|abilityTags|[string]|false|none||none|

#### Enum

|Name|Value|
|---|---|
|action|APPROVE|
|action|REJECT|
|action|MERGE|

<h2 id="tocS_BatchReviewRequest">BatchReviewRequest</h2>

<a id="schemabatchreviewrequest"></a>
<a id="schema_BatchReviewRequest"></a>
<a id="tocSbatchreviewrequest"></a>
<a id="tocsbatchreviewrequest"></a>

```json
{
  "actions": [
    {
      "itemId": "IT-001",
      "action": "APPROVE",
      "targetItemId": "IT-002",
      "abilityTags": [
        "programming_basic"
      ]
    }
  ]
}

```

批量校核请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|actions|[[BatchReviewAction](#schemabatchreviewaction)]|true|none||none|

<h2 id="tocS_CurriculumVersion">CurriculumVersion</h2>

<a id="schemacurriculumversion"></a>
<a id="schema_CurriculumVersion"></a>
<a id="tocScurriculumversion"></a>
<a id="tocscurriculumversion"></a>

```json
{
  "id": "CV-001",
  "name": "软件工程培养方案 2026 版",
  "major": "软件工程",
  "courseCount": 180,
  "status": "DRAFT",
  "publishedAt": "2026-09-15T00:00:00+08:00",
  "publishedBy": "系统管理员"
}

```

培养方案版本

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||方案版本 ID|
|name|string|true|none||方案名称|
|major|string|true|none||适用专业|
|courseCount|integer|true|none||课程数|
|status|string|true|none||状态|
|publishedAt|string(date-time)|false|none||发布时间|
|publishedBy|string|false|none||发布人|

#### Enum

|Name|Value|
|---|---|
|status|DRAFT|
|status|PUBLISHED|

<h2 id="tocS_OperationLog">OperationLog</h2>

<a id="schemaoperationlog"></a>
<a id="schema_OperationLog"></a>
<a id="tocSoperationlog"></a>
<a id="tocsoperationlog"></a>

```json
{
  "id": "LOG-001",
  "time": "2026-09-30T11:03:00+08:00",
  "operator": "系统管理员",
  "action": "配置发布",
  "target": "questionnaire/holland",
  "detail": "发布问卷 v2",
  "level": "info",
  "ip": "172.16.1.10"
}

```

操作日志

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||日志 ID|
|time|string(date-time)|true|none||操作时间|
|operator|string|true|none||操作人|
|action|string|true|none||操作类型|
|target|string|false|none||操作对象|
|detail|string|false|none||详情|
|level|string|false|none||级别|
|ip|string|false|none||来源 IP|

#### Enum

|Name|Value|
|---|---|
|level|info|
|level|warn|
|level|error|

<h2 id="tocS_AiCallLog">AiCallLog</h2>

<a id="schemaaicalllog"></a>
<a id="schema_AiCallLog"></a>
<a id="tocSaicalllog"></a>
<a id="tocsaicalllog"></a>

```json
{
  "id": "AI-001",
  "time": "2026-09-30T11:03:00+08:00",
  "userRef": "student_ref_8f3a",
  "scene": "recommendation_explain",
  "modelName": "gpt-4o-mini",
  "promptVersion": "v1.2",
  "durationMs": 890,
  "status": "SUCCESS",
  "tokenEstimate": 1200,
  "requestHash": "a1b2c3..."
}

```

AI 调用日志

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||日志 ID|
|time|string(date-time)|true|none||调用时间|
|userRef|string|false|none||脱敏用户引用|
|scene|string|true|none||场景|
|modelName|string|false|none||模型名|
|promptVersion|string|false|none||提示词版本|
|durationMs|integer|false|none||耗时（毫秒）|
|status|string|true|none||状态|
|tokenEstimate|integer|false|none||Token 估算|
|requestHash|string|false|none||脱敏请求哈希|

#### Enum

|Name|Value|
|---|---|
|scene|recommendation_explain|
|scene|plan_generate|
|scene|review_summarize|
|scene|career_chat|
|scene|pdf_parse|
|status|SUCCESS|
|status|FAILED|
|status|TIMEOUT|
|status|DEGRADED|

<h2 id="tocS_ExportJob">ExportJob</h2>

<a id="schemaexportjob"></a>
<a id="schema_ExportJob"></a>
<a id="tocSexportjob"></a>
<a id="tocsexportjob"></a>

```json
{
  "id": "EX-001",
  "type": "STUDENT_DATA",
  "scope": "计科2601 全部学生画像与计划",
  "status": "PENDING",
  "downloadUrl": "string",
  "createdAt": "2026-09-30T11:03:00+08:00",
  "operator": "系统管理员"
}

```

导出任务

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|id|string|true|none||导出任务 ID|
|type|string|true|none||导出类型|
|scope|string|false|none||导出范围描述|
|status|string|true|none||状态|
|downloadUrl|string|false|none||下载地址（完成后）|
|createdAt|string(date-time)|false|none||创建时间|
|operator|string|true|none||操作人|

#### Enum

|Name|Value|
|---|---|
|type|STUDENT_DATA|
|type|WHITELIST|
|type|OPERATION_LOG|
|type|AI_LOG|
|type|DIRECTION_LIB|
|status|PENDING|
|status|DONE|
|status|FAILED|

<h2 id="tocS_ExportRequest">ExportRequest</h2>

<a id="schemaexportrequest"></a>
<a id="schema_ExportRequest"></a>
<a id="tocSexportrequest"></a>
<a id="tocsexportrequest"></a>

```json
{
  "type": "STUDENT_DATA",
  "scope": "计科2601 全部学生",
  "filters": {}
}

```

创建导出任务

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|type|string|true|none||导出类型|
|scope|string|false|none||导出范围|
|filters|object|false|none||none|

#### Enum

|Name|Value|
|---|---|
|type|STUDENT_DATA|
|type|WHITELIST|
|type|OPERATION_LOG|
|type|AI_LOG|
|type|DIRECTION_LIB|

<h2 id="tocS_ExplainRequest">ExplainRequest</h2>

<a id="schemaexplainrequest"></a>
<a id="schema_ExplainRequest"></a>
<a id="tocSexplainrequest"></a>
<a id="tocsexplainrequest"></a>

```json
{
  "studentRef": "student_ref_8f3a",
  "ruleVersion": "R1.0",
  "profileVersion": 2,
  "profile": {
    "interest": 0.78,
    "values": 0.78,
    "ability": 0.78,
    "academic": 0.78,
    "tendency": 0.78,
    "practice": 0.78
  },
  "results": [
    {
      "directionId": "employment_backend",
      "score": 82.4,
      "rank": 1
    }
  ]
}

```

推荐解释请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentRef|string|true|none||脱敏学生引用（禁止直接标识）|
|ruleVersion|string|true|none||规则版本|
|profileVersion|integer|true|none||画像版本|
|profile|object|false|none||none|
|» interest|number|false|none||兴趣标准化得分|
|» values|number|false|none||职业价值观标准化得分|
|» ability|number|false|none||能力基础标准化得分|
|» academic|number|false|none||学业基础标准化得分|
|» tendency|number|false|none||发展倾向标准化得分|
|» practice|number|false|none||实践经历标准化得分|
|results|[object]|true|none||候选方向与得分|
|» directionId|string|false|none||方向编码|
|» score|number|false|none||匹配得分|
|» rank|integer|false|none||none|

<h2 id="tocS_ExplainResult">ExplainResult</h2>

<a id="schemaexplainresult"></a>
<a id="schema_ExplainResult"></a>
<a id="tocSexplainresult"></a>
<a id="tocsexplainresult"></a>

```json
{
  "runId": "190001",
  "explanations": [
    {
      "directionId": "employment_backend",
      "summary": "你的兴趣偏向结构化问题求解，数学基础较好…",
      "confidenceText": "数据基本完整，方向间差异不大，可信程度中等",
      "disclaimer": "智能生成，供探索参考"
    }
  ]
}

```

推荐解释结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|runId|string|true|none||推荐批次 ID|
|explanations|[object]|true|none||各方向解释|
|» directionId|string|false|none||方向编码|
|» summary|string|false|none||通俗解释|
|» confidenceText|string|false|none||可信程度文字|
|» disclaimer|string|false|none||免责声明|

<h2 id="tocS_PlanGenerateRequest">PlanGenerateRequest</h2>

<a id="schemaplangeneraterequest"></a>
<a id="schema_PlanGenerateRequest"></a>
<a id="tocSplangeneraterequest"></a>
<a id="tocsplangeneraterequest"></a>

```json
{
  "studentRef": "student_ref_8f3a",
  "directionId": "employment_backend",
  "semester": "2026-1",
  "goalSummary": "string",
  "template": {
    "goalSummary": "本学期完成后端技术基础入门：掌握 Java 语法与数据结构基础，完成 1 个可运行的小项目。",
    "semesterGoals": [
      {
        "title": "掌握 Java 基础与面向对象编程",
        "abilityTag": "programming_basic"
      }
    ],
    "monthlyTasks": [
      {
        "month": "2026-09",
        "title": "完成 Java 语法与面向对象章节学习",
        "taskType": "LEARNING",
        "estimatedHours": 12
      }
    ],
    "notes": [
      "任务可随课程安排与兴趣变化调整。"
    ]
  }
}

```

计划生成请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentRef|string|true|none||脱敏学生引用|
|directionId|string|true|none||方向编码|
|semester|string|true|none||学期（YYYY-S）|
|goalSummary|string|false|none||目标摘要（可选）|
|template|[PlanDraft](#schemaplandraft)|false|none||none|

<h2 id="tocS_PlanGenerateResult">PlanGenerateResult</h2>

<a id="schemaplangenerateresult"></a>
<a id="schema_PlanGenerateResult"></a>
<a id="tocSplangenerateresult"></a>
<a id="tocsplangenerateresult"></a>

```json
{
  "goalSummary": "本学期完成后端技术基础入门…",
  "semesterGoals": [
    {
      "title": "掌握 Java 基础与面向对象编程",
      "abilityTag": "programming_basic"
    }
  ],
  "monthlyTasks": [
    {
      "month": "2026-09",
      "title": "完成 Java 语法与面向对象章节学习",
      "taskType": "LEARNING",
      "estimatedHours": 12
    }
  ],
  "notes": [
    "任务可随课程安排调整"
  ]
}

```

计划生成结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|goalSummary|string|true|none||目标摘要|
|semesterGoals|[[SemesterGoal](#schemasemestergoal)]|true|none||none|
|monthlyTasks|[[MonthlyTask](#schemamonthlytask)]|true|none||none|
|notes|[string]|false|none||none|

<h2 id="tocS_ReviewSummarizeRequest">ReviewSummarizeRequest</h2>

<a id="schemareviewsummarizerequest"></a>
<a id="schema_ReviewSummarizeRequest"></a>
<a id="tocSreviewsummarizerequest"></a>
<a id="tocsreviewsummarizerequest"></a>

```json
{
  "studentRef": "student_ref_8f3a",
  "cycle": "2026-09",
  "reviewContent": {
    "done": "完成 Java 语法与通讯录项目，通过四六级报名",
    "undone": "LeetCode 练习因时间不足未完成一半",
    "interest": "后端开发的兴趣比预期更强，尤其喜欢调试解决问题的过程",
    "ability": "编程能力明显提升，能独立写 300 行左右的程序",
    "next": "10 月聚焦数据结构与 SQL，减少并行任务数量"
  },
  "taskSummary": "完成 4/6 项任务"
}

```

复盘总结请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentRef|string|true|none||脱敏学生引用|
|cycle|string|true|none||复盘周期|
|reviewContent|[ReviewContent](#schemareviewcontent)|true|none||none|
|taskSummary|string|false|none||任务完成情况摘要|

<h2 id="tocS_ReviewSummarizeResult">ReviewSummarizeResult</h2>

<a id="schemareviewsummarizeresult"></a>
<a id="schema_ReviewSummarizeResult"></a>
<a id="tocSreviewsummarizeresult"></a>
<a id="tocsreviewsummarizeresult"></a>

```json
{
  "summary": "9 月你的编程基础快速提升…",
  "suggestions": [
    "将任务从 6 条收敛到 3 条主线"
  ]
}

```

复盘总结结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|summary|string|true|none||阶段总结|
|suggestions|[string]|true|none||下一阶段调整建议|

<h2 id="tocS_ChatRequest">ChatRequest</h2>

<a id="schemachatrequest"></a>
<a id="schema_ChatRequest"></a>
<a id="tocSchatrequest"></a>
<a id="tocschatrequest"></a>

```json
{
  "studentRef": "student_ref_8f3a",
  "sessionId": "CHAT-001",
  "question": "后端开发和数据分析师怎么选？",
  "context": {
    "directionId": "employment_backend",
    "goalSummary": "本学期入门后端基础"
  }
}

```

生涯咨询请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|studentRef|string|true|none||脱敏学生引用|
|sessionId|string|true|none||会话 ID|
|question|string|true|none||学生提问|
|context|object|false|none||none|
|» directionId|string|false|none||当前关注方向|
|» goalSummary|string|false|none||当前目标摘要|

<h2 id="tocS_ChatResponse">ChatResponse</h2>

<a id="schemachatresponse"></a>
<a id="schema_ChatResponse"></a>
<a id="tocSchatresponse"></a>
<a id="tocschatresponse"></a>

```json
{
  "answer": "可以从兴趣与技术栈偏好判断：…",
  "references": [
    "《Java 程序设计》"
  ],
  "needsHumanSupport": false,
  "supportReason": "涉及心理健康话题",
  "disclaimer": "智能生成，供探索参考"
}

```

生涯咨询响应

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|answer|string|true|none||回答内容|
|references|[string]|false|none||引用来源（可选）|
|needsHumanSupport|boolean|false|none||是否需转人工/专业机构|
|supportReason|string|false|none||转人工原因（needsHumanSupport=true 时）|
|disclaimer|string|true|none||免责声明|

<h2 id="tocS_PdfParseRequest">PdfParseRequest</h2>

<a id="schemapdfparserequest"></a>
<a id="schema_PdfParseRequest"></a>
<a id="tocSpdfparserequest"></a>
<a id="tocspdfparserequest"></a>

```json
{
  "jobId": "CJ-001",
  "fileUrl": "http://storage.internal/uploads/cj-001.pdf",
  "filename": "软件工程培养方案2026.pdf"
}

```

PDF 解析请求

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|jobId|string|true|none||导入任务 ID|
|fileUrl|string|true|none||文件内网地址（由 Spring Boot 上传后提供）|
|filename|string|true|none||文件名|

<h2 id="tocS_PdfParseResult">PdfParseResult</h2>

<a id="schemapdfparseresult"></a>
<a id="schema_PdfParseResult"></a>
<a id="tocSpdfparseresult"></a>
<a id="tocspdfparseresult"></a>

```json
{
  "jobId": "CJ-001",
  "status": "PARSING",
  "itemCount": 172,
  "confidence": 86
}

```

PDF 解析结果

### Attribute

|Name|Type|Required|Restrictions|Title|Description|
|---|---|---|---|---|---|
|jobId|string|true|none||导入任务 ID|
|status|string|true|none||none|
|itemCount|integer|false|none||识别课程条数|
|confidence|number|false|none||整体置信度（0–100）|

#### Enum

|Name|Value|
|---|---|
|status|PARSING|
|status|REVIEW_REQUIRED|
|status|FAILED|

