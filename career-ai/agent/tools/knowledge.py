"""工具：检索职业知识库（RAG 混合检索）。
"""

from __future__ import annotations

from langchain_core.tools import tool

from agent.rag import get_retriever


@tool
async def search_knowledge(query: str) -> str:
    """检索职业知识库（职业方向描述、技能要求、课程建议、行业知识等）。
    用于回答关于职业方向、技能要求、课程建议的问题。
    当工具返回的结构化数据不足以回答时，用本工具补充知识。"""
    try:
        retriever = get_retriever()
        docs = retriever.retrieve(query, top_k=5, mode="hybrid")
        if not docs:
            return "知识库中没有找到相关内容。"
        blocks = []
        for i, doc in enumerate(docs, 1):
            blocks.append(f"[来源 {i}: {doc.source}]\n{doc.content}")
        return "\n\n".join(blocks)
    except Exception as e:  # noqa: BLE001
        return f"知识检索失败：{e}"
