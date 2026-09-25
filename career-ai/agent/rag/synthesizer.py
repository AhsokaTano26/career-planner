"""上下文打包、来源引用与 token 预算管理（直接迁移自 localrag-kit retrieval/synthesizer.py，MIT 协议）。
"""

from __future__ import annotations

from pydantic import BaseModel, Field

from agent.rag.sqlite_store import SearchResult


def _estimate_tokens(text: str) -> int:
    """粗略估算 token 数：中文按字，英文按词。"""
    import re

    cjk = len(re.findall(r"[\u4e00-\u9fff]", text))
    ascii_words = len(re.findall(r"[a-zA-Z0-9_]+", text))
    return cjk + ascii_words


class SourceCitation(BaseModel):
    """检索到的文本块的引用溯源。"""

    source_index: int = Field(description="1-based 引用序号（如 [Source #1]）")
    relative_path: str = Field(description="来源相对路径")
    start_line: int = Field(description="起始行")
    end_line: int = Field(description="结束行")
    section_title: str | None = Field(description="章节标题")
    score: float = Field(description="相关性或 RRF 融合分数")
    text_snippet: str = Field(description="引用的文本内容")

    @property
    def label(self) -> str:
        sec = f" | {self.section_title}" if self.section_title else ""
        return (
            f"[Source #{self.source_index}: {self.relative_path}"
            f":{self.start_line}-{self.end_line}{sec}]"
        )


class SynthesizedContext(BaseModel):
    """已打包好、可直接注入 LLM 提示词的上下文。"""

    formatted_context: str
    citations: list[SourceCitation]
    total_tokens: int


class ContextSynthesizer:
    """在 token 预算内去重并打包检索结果为编号上下文块。"""

    def __init__(self, max_token_budget: int = 3500):
        self.max_token_budget = max_token_budget

    def synthesize(self, results: list[SearchResult]) -> SynthesizedContext:
        """过滤、去重并组装前 N 个结果为编号上下文块。"""
        if not results:
            return SynthesizedContext(formatted_context="", citations=[], total_tokens=0)

        citations: list[SourceCitation] = []
        blocks: list[str] = []
        seen_line_ranges: set[tuple[str, int, int]] = set()
        accumulated_tokens = 0

        source_idx = 1
        for res in results:
            chunk = res.chunk
            meta = chunk.metadata

            # 避免完全重复的文件/行范围
            line_key = (meta.relative_path, meta.start_line, meta.end_line)
            if line_key in seen_line_ranges:
                continue
            seen_line_ranges.add(line_key)

            chunk_tokens = meta.estimated_tokens or _estimate_tokens(chunk.text)
            if accumulated_tokens + chunk_tokens > self.max_token_budget and citations:
                # token 预算已达上限，跳过其余低排名块
                break

            citation = SourceCitation(
                source_index=source_idx,
                relative_path=meta.relative_path,
                start_line=meta.start_line,
                end_line=meta.end_line,
                section_title=meta.section_title,
                score=res.score,
                text_snippet=chunk.text.strip(),
            )
            citations.append(citation)

            sec_header = (
                f" | Section: {citation.section_title}" if citation.section_title else ""
            )
            block = (
                f"--- [Source #{citation.source_index}] {citation.relative_path} "
                f"(行 {citation.start_line}-{citation.end_line}{sec_header}) ---\n"
                f"{citation.text_snippet}\n"
            )
            blocks.append(block)

            accumulated_tokens += chunk_tokens
            source_idx += 1

        formatted_text = "\n".join(blocks).strip()
        return SynthesizedContext(
            formatted_context=formatted_text,
            citations=citations,
            total_tokens=accumulated_tokens,
        )
