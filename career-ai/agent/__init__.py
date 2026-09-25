"""智能体模块（Agent）：LangGraph 驱动的生涯对话体。

架构：安全节点（危机关键词检测）→ 模型节点（LiteLLM 网关 → DeepSeek）→
      工具节点（career-core API 调用 + RAG 检索）→ 条件边循环。
"""
