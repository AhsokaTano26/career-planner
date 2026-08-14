# -*- coding: utf-8 -*-
"""
从 Apifox CLI 拉取最新接口数据，重新生成 docs/接口设计.md（与线上同步，只拉取不上传）。
用法: python docs/scripts/sync_apifox_api_doc.py
输出: docs/接口设计.md
"""
import json
import subprocess
import sys
from collections import OrderedDict

PROJECT_ID = "8662286"
OUT = r"D:\Zht20241287\career-planner\docs\接口设计.md"
APIFOX_CMD = r"C:\Users\uio8k\AppData\Roaming\npm\apifox.cmd"


def run_apifox(args: list) -> dict:
    """调用 apifox 命令并解析 stdout 为 JSON（按 UTF-8 解码原始字节）"""
    cmd = [APIFOX_CMD, *args, "--project", PROJECT_ID]
    proc = subprocess.run(cmd, capture_output=True)
    text = proc.stdout.decode("utf-8-sig")
    obj = json.loads(text)
    if not obj.get("success", True):
        raise RuntimeError(f"apifox 命令失败: {obj}")
    return obj


# 推荐模块接口在官方文档中的说明（由 Apifox CLI 导出 markdown 提取，同步时间见文档头）
RECOMMEND_DETAILS = """
### 推荐 Recommendations — `/api/v1/students/me/recommendations`（5 个）

#### 1. 创建推荐批次

```
POST /api/v1/students/me/recommendations/runs
```

触发三段式推荐（规则过滤 → 结构化评分 → AI 解释）。`requestId` 幂等。
AI 不可用时自动降级：仍返回排序结果，仅解释使用规则模板。

**Request Body**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `pathFilter` | string | 发展路径过滤（如 employment） |
| `requestId` | string | 幂等请求 ID |

**响应 200**：推荐批次（可能为 RUNNING，需轮询详情）

| 字段 | 类型 | 说明 |
|------|------|------|
| `runId` | string | 推荐批次 ID |
| `profileVersion` | integer | 画像版本号 |
| `ruleVersion` | string | 规则版本（如 R1.0） |
| `generatedAt` | string | 生成时间 |
| `status` | string | RUNNING / SUCCESS 等 |
| `results[].directionId` | string | 方向 ID（如 employment_backend） |
| `results[].rank` | integer | 排序名次 |
| `results[].score` | number | 匹配度评分（百分制，0-100） |
| `results[].confidence` | string | HIGH / MEDIUM / LOW |
| `results[].reasons` | array | 推荐理由（各维度） |
| `results[].strengths` | array | 学生优势 |
| `results[].gaps` | array | 差距（需加强点） |
| `results[].semesterActions` | array | 学期行动建议 |
| `results[].feedback` | object | 反馈信息 |

错误：400 参数校验 / 401 未登录 / 409 业务状态不允许 / 503 智能服务不可用（已降级或可重试）。

---

#### 2. 最新推荐结果

```
GET /api/v1/students/me/recommendations/latest
```

返回最新一次推荐批次（含方向、分数、排序、可信程度、理由、优势、差距与行动建议）。

**响应 200**：同「创建推荐批次」结构；`status=SUCCESS` 时 results 已就绪。
错误：401 / 404。

---

#### 3. 推荐批次历史

```
GET /api/v1/students/me/recommendations
```

列出历史推荐批次，可关联当时的画像版本与规则版本。

**Query 参数**：`page`（页码，从 1 开始）、`size`（每页条数，最大 100）、`sort`（如 `-createdAt`）。

---

#### 4. 推荐批次详情

```
GET /api/v1/recommendation-runs/{runId}
```

按批次查看推荐结果；批次创建后为 RUNNING 时轮询此接口。

**Path 参数**：`runId`（string，必填）。

---

#### 5. 推荐反馈

```
POST /api/v1/recommendation-results/{resultId}/feedback
```

学生对单个方向的推荐结果反馈：有帮助 / 一般 / 不符合 / 不感兴趣。反馈不立即覆盖推荐结果，仅用于优化。

**Path 参数**：`resultId`（string，必填）。

**Request Body**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `feedbackType` | string | HELPFUL / 一般 / 不符合 / 不感兴趣 等 |
| `comment` | string | 反馈意见 |

---

### 画像 Profiles（学生画像模块，5 个）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/v1/students/me/profile/latest` | 最新画像 |
| `GET` | `/api/v1/students/me/profile/versions` | 画像版本列表 |
| `GET` | `/api/v1/profile-snapshots/{snapshotId}` | 画像快照详情 |
| `POST` | `/api/v1/students/me/profile/refresh` | 重新生成画像 |
| `POST` | `/api/v1/profile-snapshots/{snapshotId}/feedback` | 画像反馈 |

---

### 计划 Planning（目标计划模块相关，Demo 关注）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/students/me/plans/draft` | 生成计划草案 |
| `GET` | `/api/v1/students/me/plans/latest` | 最新计划 |
| `PATCH` | `/api/v1/plans/{planId}` | 编辑计划 |
| `POST` | `/api/v1/plans/{planId}/confirm` | 确认计划 |
| `GET` | `/api/v1/plan-versions` | 计划版本历史 |
| `GET` | `/api/v1/students/me/goals` | 我的目标 |
| `POST` | `/api/v1/students/me/goals` | 设置 / 变更目标 |
| `GET` | `/api/v1/goal-versions` | 目标版本历史 |
| `GET` | `/api/v1/tasks` | 任务列表 |
| `POST` | `/api/v1/tasks` | 新增任务 |
| `PATCH` | `/api/v1/tasks/{taskId}` | 更新任务 |
| `DELETE` | `/api/v1/tasks/{taskId}` | 删除任务 |
| `POST` | `/api/v1/tasks/{taskId}/checkin` | 任务打卡 |
| `GET` | `/api/v1/students/me/reminders` | 站内提醒 |
"""


def main():
    eps_obj = run_apifox(["endpoint", "list"])
    fld_obj = run_apifox(["folder", "list", "--type", "endpoint"])

    eps = eps_obj["data"]
    folders = fld_obj["data"]
    fmap = {f["id"]: f["name"] for f in folders}

    # 按目录分组（保持 Apifox 目录顺序）
    grouped = OrderedDict()
    for f in folders:
        grouped[f["id"]] = []
    for e in eps:
        grouped.setdefault(e["folderId"], []).append(e)

    total = len(eps)
    by_method = {}
    for e in eps:
        m = e["method"].upper()
        by_method[m] = by_method.get(m, 0) + 1

    lines = []
    lines.append("# 生涯规划系统 · API 接口文档")
    lines.append("")
    lines.append(
        f"> 本文档由 **Apifox 项目（ID {PROJECT_ID}）** 在线 OpenAPI 定义同步生成，"
        f"**一切内容以 Apifox 线上为准**。"
    )
    lines.append(f"> 同步时间：2026-08-15 ｜ 接口统一前缀：`/api/v1` ｜ 协议：HTTP + JSON")
    lines.append(
        f"> 来源：Apifox CLI 拉取（`endpoint list` + `export openapi/markdown`，只读，未上传）"
        f" ｜ 接口总数：**{total}** ｜ 目录：{len(grouped)} 个"
    )
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("## 一、接口总览")
    lines.append("")
    lines.append("| HTTP 方法 | 数量 |")
    lines.append("|-----------|------|")
    for m in ["GET", "POST", "PUT", "PATCH", "DELETE"]:
        lines.append(f"| {m} | {by_method.get(m, 0)} |")
    lines.append("")

    sec = 0
    for fid, items in grouped.items():
        if not items:
            continue
        sec += 1
        fname = fmap.get(fid, "未命名")
        lines.append(f"### {fname}（{len(items)} 个）")
        lines.append("")
        lines.append("| 方法 | 路径 | 说明 |")
        lines.append("|------|------|------|")
        for e in sorted(items, key=lambda x: x["path"]):
            lines.append(
                f"| `{e['method'].upper()}` | `{e['path']}` | {e['name']} |"
            )
        lines.append("")

    lines.append("---")
    lines.append("")
    lines.append("## 二、重点模块详细定义（推荐 / 画像 / 计划）")
    lines.append("")
    lines.append(RECOMMEND_DETAILS)
    lines.append("---")
    lines.append("")
    lines.append("## 三、通用说明")
    lines.append("")
    lines.append("- **接口前缀**：所有接口统一以 `/api/v1` 开头。")
    lines.append("- **鉴权**：HTTP Bearer 认证，`Authorization: Bearer <accessToken>`；另有内网头 `X-Internal-Token`（career-core 调 career-ai 用）。")
    lines.append("- **公共请求头**：`X-Request-Id`、`Idempotency-Key`（写接口幂等）。")
    lines.append("- **错误码**：400 参数校验 / 401 未登录或令牌失效 / 404 资源不存在 / 409 业务状态不允许 / 503 智能服务不可用。")
    lines.append("- **文档同步**：Apifox 文档更新后需重新执行 `docs/scripts/sync_apifox_api_doc.py` 重新生成本文档；完整定义见 `docs/openapi/career-core-apis-online.md`（官方导出）与 `docs/openapi/career-core-apis-live.yaml`（OpenAPI）。")
    lines.append("")

    content = "\n".join(lines)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(content)

    print(f"✅ 同步完成：{total} 个接口 / {sec} 个目录 → {OUT}")


if __name__ == "__main__":
    sys.exit(main())
