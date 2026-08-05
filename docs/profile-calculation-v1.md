# 学生六维画像计算规则 V1

## 1. 适用范围

本规则实现需求 `FR-I01` 至 `FR-I04`，由 `career-core` 执行并保存正式结果。`career-ai` 可以在后续生成更自然的文字解释，但不得决定或修改任何画像分数。

规则版本固定为 `PROFILE_RULE_V1`。相同输入必须得到相同输出。

## 2. 六维稳定编码

| 稳定编码 | 中文名称 | 第一版 Demo 兼容键 | 主要数据来源 |
| --- | --- | --- | --- |
| `INTEREST` | 兴趣 | `interest` | 霍兰德兴趣简版、兴趣偏好 |
| `WORK_VALUES` | 职业价值观 | `values` | 职业价值观量表 |
| `ACADEMIC_FOUNDATION` | 学业基础 | `academic` | 学业基础资料、专业认知测评 |
| `ABILITY` | 能力 | `ability` | 能力自评、能力测评 |
| `DEVELOPMENT_TENDENCY` | 发展倾向 | `orientation` | 发展倾向量表、发展意向 |
| `PRACTICAL_EXPERIENCE` | 实践经历 | `experience` | 竞赛、项目、实习和其他经历 |

接口和新快照使用稳定编码；推荐模块读取时同时兼容两套键。

## 3. 输入证据

每条证据包含：

- `source`：来源及版本，例如 `HOLLAND_V1`；
- `score`：对应量表先按自身规则归一化后的 0-100 分；
- `weight`：同一维度内的证据权重，必须大于 0。

问卷的题目到维度、选项到分值的映射应由测评版本配置维护。画像计算器不读取题目文案，也不调用大模型。

## 4. 计算公式

单维度有至少一条证据时：

```text
rawScore = Σ(score_j × weight_j) / Σ(weight_j)
normalizedScore = clamp(rawScore, 0, 100)
```

V1 的输入已经统一为 0-100，因此两个分数相同，均四舍五入保留两位小数。保存 `evidenceCount` 和去重后的 `sources`，便于追溯。

单维度没有证据时：

- `available = false`；
- `rawScore = null`；
- `normalizedScore = null`；
- `evidenceCount = 0`。

缺失不等于零分，不得把该维度作为 0 分传给推荐算法。推荐时继续按已有维度重新归一化权重。

## 5. 完整度

```text
completeness = 已完成必填资料和必答题数 / 必填资料和必答题总数 × 100
```

正常流程必须由测评/学生资料模块提供两个计数。仅兼容历史快照且没有计数时，才使用“已有维度数 / 6”作为保守回退值。

## 6. 摘要、优势与待探索问题

这些字段同样由确定性模板生成：

- `strengths`：分数不低于 75 的维度，按分数降序最多取 2 项；
- `explorationQuestions`：先列缺失维度，再列低于 60 的维度，最多取 3 项；
- `summary`：组合完整度、优势与待探索问题，不输出确定性的职业结论。

后续可由 `career-ai` 润色摘要，但必须保留原始规则结果，AI 文本失败时回退到本模板。

## 7. 版本与触发

- 首次测评提交并完成计分后，调用 `StudentProfileService.calculateAndSave(...)`；
- 每次重新计算都向 `profile_snapshot` 插入新记录，不覆盖旧版本；
- `version_no` 按学生从 1 递增；
- 推荐批次必须保存当时的 `profile_snapshot_id`；
- 画像反馈只写入 `profile_snapshot_feedback`，不自动改变分数。

当前 Demo 尚未实现完整的 `assessment_score` 表链路，`refresh` 暂用最近快照中的结构化分数重算。测评模块完成后，应把该兼容逻辑替换为读取最新测评得分和学生资料。

## 8. 接口契约

Apifox 是接口契约的唯一最新来源。正式路径以后端负责人在 Apifox
统一的版本为准；仓库文档仅作同步快照，如有差异应更新仓库，不得用旧文档反向覆盖 Apifox：

```text
GET  /api/v1/students/me/profile/latest
GET  /api/v1/students/me/profile/versions
GET  /api/v1/profile-snapshots/{snapshotId}
POST /api/v1/students/me/profile/refresh
POST /api/v1/profile-snapshots/{snapshotId}/feedback
```

请求使用 Bearer Token；读取请求携带 `X-Request-Id`，写请求同时携带 `Idempotency-Key`。当前 Demo 还没有登录态，临时支持可选 `studentId`，缺省为 `1001`，接入 Spring Security 后应移除。

反馈枚举为：

```text
MATCH
PARTIAL
MISMATCH
```

## 9. 验收样例

测试样例位于 `career-core/src/test/resources/profile-fixtures/`：

- `complete.json`：六维完整；
- `partial.json`：部分维度缺失；
- `no-experience.json`：没有实践经历，验证该维度为 `null` 而非 0。

运行：

```powershell
cd career-core
mvn test
```
