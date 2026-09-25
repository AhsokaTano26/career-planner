# Memory — career-ai AI 服务

> 生涯规划系统的统一 LLM 网关 + AI 能力服务。Python 3.12 · FastAPI · Uvicorn · LiteLLM 1.98。
> 本文件为 career-ai 子目录级记忆：编辑本目录代码时自动加载（外层项目记忆见根 `AGENTS.md`）。

## 服务定位与边界
- FastAPI 服务，端口 **8000**（Docker 内网，不映射宿主），career-core 经 `POST /v1/chat/completions` 调用，`Authorization: Bearer GATEWAY_API_KEY`。
- 职责：统一大模型入口（多渠道路由/重试/降级/限流/调用日志）+ 5 个 AI 能力（career_chat、recommendation_explain、plan_generate、review_summarize、pdf_parse）。
- **隐私边界（红线，不可破坏）**：
  - 不连 MySQL 业务表、不持有学生身份信息，`student_id` 可选留空。
  - 只写 `ai_call_log` 日志表，且**仅写哈希**（`input_hash`/`request_hash`），禁止存原始 Prompt/响应。
  - 日志 fail-open：写日志失败不阻塞 LLM 调用。
- 大模型失败由 career-core 回退规则模板（本服务失败抛 502）。

## 架构约定
- 所有 LLM 调用必须经 `gateway/client.py` 的 `GatewayClient.generate()`，**禁止绕过网关直连 provider**。
- 模型组：`GATEWAY_MODEL_GROUPS`（JSON）配置，支持多渠道负载均衡 + `fallbacks` 整组降级。
- 限流：进程内令牌桶（`GATEWAY_RPM`），超限返回 429（多实例需迁 Redis）。
- 重试：LiteLLM `Router` `num_retries` + 指数退避（`GATEWAY_MAX_RETRIES`）。
- 配置解析：`gateway/config.py`，provider 前缀支持 `deepseek/*`、`openai/*`、`anthropic/*`。
- 兼容层：`services/llm_gateway.py`、`providers/deepseek.py` 是旧转发层，新代码直接 import `gateway.client`。

## 隐私与安全约定
- 所有自由文本输入**必须先经** `services/desensitizer.py` 的 `mask_free_text()` 脱敏，再送 LLM。
  - 脱敏覆盖：邮箱 / 手机号 / 身份证号（15/18 位）/ 学号；并截断到业务长度上限。
- 危机关键词（自杀/自残/抑郁/焦虑/心理疾病/法律/医疗/诊断）命中时**不送 LLM**，直接返回人工支持引导（`needsHumanSupport=true`），见 `routes_ai.py`。
- 结构化输出 scene 必须做 Schema 校验，畸形/注入导致的异常输出要拦截。

## Prompt 管理约定
- 系统 Prompt 优先外部化到 `prompts/*.txt`，import 时加载，文件缺失回退内置字符串（沿用 `recommendation_explainer` 模式）。
- 结构化输出：指示模型输出**纯 JSON**（无 markdown 围栏），用容错解析（`{`…`}` 切片 + `json.loads`）+ `_normalize` 补默认值。
- Prompt 变更须版本管理并记录原因/效果（见下文最佳实践第 3 节）。

## AI 开发最佳实践约定（新增/改造能力须遵循）
> 详细实现方案、落地路径与注意事项见 @docs/ai-dev-conventions.md
> 新增 AI 能力时对照该文档末尾「约定速查表」逐项评估适用项。

- **流式响应**：新增聊天类接口必须支持 SSE 流式，非整段等待。
- **RAG**：涉及领域事实（方向库/课程/政策）的场景用检索增强，不靠模型内置知识。
- **Prompt 版本管理**：Prompt 即代码，Git 版本化 + 变更记录 + 生效开关。
- **语义缓存**：高频非个性化查询开缓存；含学号/个性化请求禁止缓存。
- **多模型路由**：简单任务小模型、复杂推理大模型，按 scene 指定模型组。
- **队列模式**：批量/长耗时任务（PDF 批量、报告）入队解耦，返回 job_id。
- **评估驱动**：每个 scene 维护评估用例集，Prompt/模型变更须跑评估对比再合并。
- **可观测性**：`ai_call_log` 带 scene/model/token/latency/cost，贯穿 `X-Request-Id`。
- **安全加固**：Prompt 注入隔离、输出侧回检危机词、依赖固定版本 + `pip-audit`。

## 测试约定
- pytest，测试在 `tests/` 下。
- 网关测试 **mock Router**，不调真实 LLM（`test_gateway.py`）。
- 脱敏测试必须含「无原始 PII 泄露」合规断言（`test_desensitizer.py`）。
- 接口 Schema 测试对齐 Apifox 契约（`test_ai_endpoints_schema.py`）。
- 端到端测试对运行中的 :8000（`test_five_ai_endpoints.py`），key 无效时可能 503。

## 运行与配置
- 环境变量见 @.env.example（LLM_API_KEY / GATEWAY_* / DB_*；`.env` 已 gitignore）。
- 依赖见 @requirements.txt（litellm pin `==1.98.0`，升级前跑安全扫描）。
- 本地启动：`career-ai/` 目录下 `uvicorn api.main:app --host 127.0.0.1 --port 8000`。
- 入口 `api/main.py`（lifespan 预热网关、双斜杠归一化中间件、统一错误响应 `{code,message,data,traceId,timestamp}`）。

## 已知 Demo 限制（后续迭代项）
- 聊天历史/反馈为**进程内内存**存储，重启丢失——需持久化。
- 限流为进程内，多实例需 Redis。
- PDF 解析为朴素行级文本抽取，未用真正的 PDF 库（PyMuPDF/pdfplumber）。
- 无语义缓存、无 RAG、无流式、无队列——见最佳实践约定。
