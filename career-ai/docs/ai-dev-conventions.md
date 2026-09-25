# career-ai AI 开发约定详解

> 本文件是 `career-ai/AGENTS.md` 中「AI 开发最佳实践约定」的展开说明。
> 被 AGENTS.md 通过 `@docs/ai-dev-conventions.md` 按需引用，仅在你处理相关任务时加载。
> 技术栈基线：Python 3.12 · FastAPI · Uvicorn · LiteLLM 1.98 · SQLAlchemy 2.x · PyMySQL。
> 每条约定给出：适用场景 → 实现要点 → 落地路径 → 注意事项。

---

## 1. 流式响应（Streaming）

**适用场景**：任何面向用户的生成式接口，尤其 `career_chat`。当前 `/api/v1/ai/chat` 为非流式，用户等待整段响应才看到内容，体验差。

**实现要点**：
- 用 FastAPI 的 `StreamingResponse` + 生成器（`async def` yield）输出 SSE 格式：
  ```
  data: {"delta":"片段"}\n\n
  ...
  data: [DONE]\n\n
  ```
- `gateway/client.py` 需新增 `generate_stream()`：调用 LiteLLM `acompletion(..., stream=True)`，逐 chunk 透传；保留现有非流式 `generate()` 不破坏。
- 每个 chunk 仍走脱敏（脱敏在**输入侧**做，输出侧可流式但需保证危机关键词命中时立即中止流并改发人工引导）。
- `ai_call_log` 流式场景在**流结束**后一次性落库（累计 token 数），失败记 `FAILURE`。

**落地路径**：
- 新增 `gateway/client.py: GatewayClient.generate_stream()`
- 新增路由 `POST /api/v1/ai/chat/stream`（保留原 `/chat` 非流式以兼容 career-core 现有调用）
- career-core 侧 `LlmGateway` 增加 SSE 消费分支，前端 `fronted/` 用 `EventSource` 或 `fetch` + `ReadableStream` 接收。

**注意事项**：
- 流式无法预知最终 token 成本，日志与计费须在流完成后结算。
- 中断处理：客户端断开时 abort 上游流，避免浪费 token。
- 危机关键词检测在首屏前做（基于**已脱敏的用户输入**），命中直接不发起流式 LLM 调用。

---

## 2. RAG（检索增强生成）

**适用场景**：需要回答「领域内事实」的能力——如职业方向库、培养方案/课程、政策/法规解释。当前所有能力都直接靠模型内置知识生成，存在幻觉风险。

**实现要点**：
- 检索与生成分离两阶段：先从向量库取 Top-K 相关片段 → 注入 system/user prompt 上下文 → 再让 LLM 基于上下文生成。
- 向量库选型（推荐顺序）：
  1. **PGVector**（PostgreSQL 扩展）——若后续引入 PG；
  2. **Chroma / Qdrant** 独立服务——轻量、独立容器；
  3. 短期 PoC 可用内存向量（numpy 余弦相似度）验证效果，勿用于生产。
- Embedding 模型：优先与主模型同源的 embedding API（如 DeepSeek/通义 embedding），或开源 BGE-M3（中文友好）。
- 文档分块（chunking）：按语义边界切分（段落/小节），每块 300–800 token，保留重叠 10–15%；保留元数据（来源、章节、更新时间）用于引用。
- 重排（re-ranking）：Top-K 初检（如 20）后用 rerank 模型精排到 Top-5，显著提升相关性。
- 输出须带**引用**：在响应中附 `sources: [{source, chunk_id}]`，便于人工核验。

**落地路径**：
- 新增 `services/rag/` 包：`embedder.py`（向量化）、`store.py`（向量库适配）、`retriever.py`（检索+重排）。
- 知识库数据源来自 career-core 的方向/课程表（经 HTTP 拉取快照，不直接连库——遵守「不读业务表」边界）。
- 在 `plan_generator` / `recommendation_explainer` 中引入可选 `context` 入参。

**注意事项**：
- 检索质量决定 RAG 成败：分块策略、embedding 模型、重排三者都要评估。
- 遵守隐私边界：RAG 数据源不含学生个人身份信息；检索结果注入前再过一遍脱敏。
- 知识库更新需有增量同步机制，避免每次全量重算 embedding。

---

## 3. Prompt 版本管理

**适用场景**：当前 Prompt 散落在各 service 的 `_SYSTEM_PROMPT` 常量 + `prompts/recommendation_explainer.txt`，改动无版本、无记录、无法回滚。

**实现要点**：
- Prompt 一律外部化到 `prompts/*.txt`（或 `.md`），代码 import 时加载，缺失回退内置字符串（沿用现有模式）。
- 版本化：文件名带语义版本 `recommendation_explainer.txt`（当前）→ 用 Git 管理即可，每次改动一个 commit；或引入 `prompts/versions/{name}/v{n}.txt` 目录。
- 变更记录：`prompts/CHANGELOG.md`，每条记录：`版本 / 日期 / 修改人 / 修改原因 / 效果评估结论`。
- 生效开关：`GATEWAY_MODEL_GROUPS` 或独立 env `PROMPT_VERSION_{SCENE}` 指定当前生效版本，支持灰度。
- 评估挂钩：每次 Prompt 变更必须先跑评估用例集（见第 7 节），记录前后指标再合并。

**落地路径**：
- 把 `services/*.py` 中的 `_SYSTEM_PROMPT` 常量迁移到 `prompts/{scene}.txt`。
- 新建 `prompts/CHANGELOG.md` 与 `prompts/loader.py`（统一加载 + 版本选择 + 回退）。

**注意事项**：
- Prompt 是代码，按代码标准走评审与版本管理，禁止「口头改了 prompt 直接上线」。
- 结构化输出类 Prompt（plan/review/batch）改动后必须跑 Schema 契约测试确认 JSON 仍可解析。

---

## 4. 语义缓存（Semantic Cache）

**适用场景**：高频、可重复的查询——如相同职业方向的解释、常见咨询问题。命中缓存可省 LLM 调用成本与延迟。

**实现要点**：
- 优先用 LiteLLM 内置 cache：`litellm_settings: {cache: True, cache_type: "semantic"}`，配 `similarity_threshold`（建议 0.95 起调）。
- 后端：单实例先用内存缓存；多实例/持久需求上 **Redis**（`CACHE_TYPE=redis`）。
- 只对**确定性高、无个性化**的场景开缓存（推荐解释、通用咨询）；个性化聊天、含学号的请求**禁止缓存**（隐私 + 结果差异）。
- TTL：领域知识类（方向解释）可较长（数小时~天）；时效性强的设短 TTL。
- 缓存键须脱敏后生成，避免把 PII 写入缓存键。

**落地路径**：
- `gateway/config.py` 增加缓存开关与阈值配置；`gateway/client.py` 在 `generate()` 中透传 `metadata` 控制是否缓存。
- 依赖：`requirements.txt` 增加 `litellm` 已含（semantic cache 需 `sentence-transformers` 或配 embedding API，注意体积）。

**注意事项**：
- 语义缓存可能把「语义相近但语义不同」的请求错配（如「适合内向的人」vs「适合外向的人」），阈值宁高勿低。
- 缓存命中也要记 `ai_call_log`（标记 `source=CACHE`），保证可观测与成本口径一致。

---

## 5. 多模型路由（Multi-Model Routing）

**适用场景**：当前仅 DeepSeek 单渠道。不同任务对模型能力/成本/延迟要求不同，统一走大模型既贵又慢。

**实现要点**：
- LiteLLM `Router` 已支持按模型组路由 + fallback。在 `GATEWAY_MODEL_GROUPS` 中定义多组：
  ```json
  [
    {"group":"default","models":["deepseek/deepseek-v4-flash"],"fallbacks":[]},
    {"group":"light","models":["deepseek/deepseek-v4-flash"],"fallbacks":["openai/gpt-4o-mini"]},
    {"group":"reasoning","models":["deepseek/deepseek-reasoner"],"fallbacks":["default"]}
  ]
  ```
- 路由规则：简单任务（脱敏、分类、短咨询）→ 小/快模型；复杂推理（计划生成、深度复盘）→ 大/强模型。
- 路由依据：任务标签（scene）+ prompt 长度 + 调用方指定的 `model` 组名。career-core 调网关时通过 `model` 字段选组。
- fallback 链：整组降级顺序，主模型超时可用时切备选，网关透明完成，调用方无感。

**落地路径**：
- 扩展 `gateway/config.py` 的 provider 凭据解析（已支持 openai/anthropic 前缀，补对应 env）。
- 各 service 调用 `generate(scene=..., model_group=...)` 时显式指定组名。

**注意事项**：
- 换模型后**必须重跑该 scene 的评估集**——不同模型输出风格/JSON 遵从度不同。
- 成本口径：不同模型单价不同，`ai_call_log` 记录 model 名便于按模型统计成本。
- 密钥管理：多 provider 密钥（OPENAI_API_KEY 等）走 env，禁止硬编码。

---

## 6. 队列模式（Queue-Based Processing）

**适用场景**：非实时、长耗时、批处理任务——如批量 PDF 解析、批量推荐解释、定时报告生成。当前 `pdf_parser` 同步执行，PDF 大/多时阻塞 HTTP 请求。

**实现要点**：
- 解耦请求与 AI 工作：接口收任务 → 入队 → 立即返回 job_id → worker 消费 → 写结果 → 客户端轮询或 webhook 通知。
- 队列选型：**RQ**（Redis，轻）或 **Celery**（功能全）。Redis 同时可复用为语义缓存/限流后端。
- 任务幂等：以业务键（如 `pdf_id + version`）去重，重复投递不重复生成。
- 重试与退避：失败任务按指数退避重试 N 次后转死信队列（DLQ），人工介入。
- 背压：队列天然平滑突发流量，避免直接打爆 provider 限流。

**落地路径**：
- 新增 `workers/` 包：`tasks.py`（任务定义）、`queue_client.py`（入队）、`result_store.py`（结果落库/内存）。
- 新增路由 `POST /api/v1/ai/pdf/parse/async`（入队返回 job_id）+ `GET /api/v1/ai/job/{id}`（查结果）。
- 保留同步接口给小文件场景。

**注意事项**：
- worker 同样经 `gateway/client.py` 调 LLM（继承缓存/fallback/限流/日志）。
- 结果持久化：Demo 阶段可内存，生产须落 MySQL（新增 job 表）或对象存储。
- 超时兜底：worker 对单次 LLM 调用设超时，避免任务卡死。

---

## 7. 评估驱动开发（Evaluation-Driven Development）

**适用场景**：当前只有 Schema/契约测试和脱敏合规测试，缺少「输出质量」的量化评估。Prompt/模型变更后无法判断效果好坏。

**实现要点**：
- 为每个 AI scene 建评估用例集：`tests/evals/{scene}.jsonl`，每条含 `输入 / 期望要点 / 评分标准`。
- 评分维度：
  - 结构化输出（plan/review/batch）：JSON 可解析率、必填字段齐全率。
  - 自由文本（chat/explain）：相关性、无幻觉（事实是否被上下文支持）、无 PII 泄露、语气得体。
  - 可用 LLM-as-judge（大模型按 rubric 打分）+ 人工抽检结合。
- 回归：Prompt/模型变更 → 自动跑评估集 → 生成前后指标对比报告 → 不达标不合并。
- 指标基线：记录每个 scene 的当前指标作为 baseline，CI 中对比。

**落地路径**：
- 新增 `tests/evals/` 目录 + `tests/test_evals.py`（跑评估集，输出 markdown 报告到 `docs/reports/`）。
- 复用现有 `test_desensitizer.py` 的合规断言作为评估子集。

**注意事项**：
- 评估用例会随业务演进，需定期补充（尤其新增 scene 时同步建用例）。
- LLM-as-judge 本身有偏差，关键决策保留人工复核。
- 评估集**不含真实 PII**，用合成/脱敏数据。

---

## 8. 可观测性增强（Observability）

**适用场景**：当前 `ai_call_log` 只记哈希与基础字段，缺调用链、成本、错误分类，排障和成本核算受限。

**实现要点**：
- `ai_call_log` 扩展字段：`scene`、`model`、`prompt_tokens`、`completion_tokens`、`total_tokens`、`latency_ms`、`cache_hit`、`retry_count`、`fallback_used`、`cost_estimate`。
- 调用链：贯穿 `X-Request-Id`（career-core → career-ai → provider），日志统一带 request_id 便于串联。
- 状态机（沿用现有）：`SUCCESS` / `FAILURE` / `DEGRADED`（先失败后成功）+ 新增 `CACHE_HIT`。
- 成本核算：按 model 单价 × token 数估算，支持按 scene/时间维度聚合。
- 仪表盘：基于 `ai_call_log` 出 QPS、成功率、P95 延迟、日均成本、Top 失败原因。

**落地路径**：
- 扩展 `gateway/db.py` 的 `insert_ai_call_log()` 字段 + `admin-log.sql` 的 DDL（career-core 侧同步）。
- `gateway/logging_callback.py` 补全 token/latency/cost 采集。
- 只扩字段不破坏现有「仅写哈希、不存原文」的隐私红线。

**注意事项**：
- 隐私红线不变：任何扩展字段都不得存原始 prompt/响应内容，只存哈希与统计量。
- 成本单价配置化（env `MODEL_PRICES` JSON），避免硬编码。

---

## 9. 安全加固（Security Hardening）

**适用场景**：面向学生的对话系统，需防 Prompt 注入、输出有害内容、数据越权。现有脱敏 + 危机关键词是良好基础，需补齐。

**实现要点**：
- **Prompt 注入防御**：
  - 用户输入在拼进 prompt 前做隔离（明确分隔 system 指令与用户内容，标注「以下为不可信用户输入」）。
  - 系统 Prompt 声明忽略任何「忽略以上指令」类劫持尝试。
  - 对结构化输出 scene 强制 Schema 校验，注入导致的畸形输出被拦截。
- **输出过滤**：生成内容回检危机关键词/违禁词，命中则替换为人工引导（与输入侧对称）。
- **限流增强**：进程内令牌桶（现状）→ 多实例/精细需求迁移 Redis 限流；按用户/接口维度分级限流。
- **鉴权**：网关 `GATEWAY_API_KEY` 走 env + 定期轮换；禁止日志打印 key。
- **审计**：`ai_call_log` 保留足够字段支持事后审计（谁、何时、哪个 scene、结果状态），但**不含敏感原文**。
- **依赖安全**：`litellm` 等核心库固定版本（已 pin `==1.98.0`），升级前跑安全扫描（`pip-audit`）。

**落地路径**：
- `services/desensitizer.py` 扩展注入模式识别（可选）。
- 各 service 输出侧增加 `_validate_output()` 回检（部分已有，统一化）。
- CI 增加 `pip-audit` 步骤。

**注意事项**：
- 脱敏是必要非充分：正则脱敏有漏网（如变体手机号），需配合评估集的「无 PII 泄露」断言持续兜底。
- 危机关键词命中策略要保守（宁可误报转人工，不可漏报），词表随业务评审维护。

---

## 约定速查表

| # | 约定 | 当前状态 | 优先级 | 主要落地点 |
|---|------|---------|--------|-----------|
| 1 | 流式响应 | 未实现 | 高 | `gateway/client.py` + `routes_ai.py` |
| 2 | RAG | 未实现 | 中 | 新增 `services/rag/` |
| 3 | Prompt 版本管理 | 部分（单文件） | 高 | `prompts/` + `prompts/loader.py` |
| 4 | 语义缓存 | 未实现 | 中 | `gateway/config.py` + Redis |
| 5 | 多模型路由 | 部分（单渠道） | 中 | `GATEWAY_MODEL_GROUPS` |
| 6 | 队列模式 | 未实现 | 中 | 新增 `workers/` |
| 7 | 评估驱动 | 仅契约/合规测试 | 高 | 新增 `tests/evals/` |
| 8 | 可观测性 | 基础日志 | 中 | `gateway/db.py` + DDL |
| 9 | 安全加固 | 基础（脱敏+危机词） | 高 | `desensitizer.py` + 输出回检 |

> 新增 AI 能力时，对照本表逐项评估适用项；不适用须在 PR 说明中标注原因。
