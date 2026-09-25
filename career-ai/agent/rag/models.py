"""RAG 数据模型（精简自 localrag-kit core/models.py，MIT 协议，署名参考）。

迁移说明：只保留 Agent 检索所需的 Chunk / ChunkMetadata / RetrievedDoc；
去掉 FileMetadata / Document（文件系统扫描相关，Agent 场景不需要）。
"""

from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class FileType(str, Enum):
    """文档类别（保留枚举以兼容 schema.py 的 chunks 表 file_type 字段）。"""

    MARKDOWN = "markdown"
    CODE = "code"
    PDF = "pdf"
    TEXT = "text"
    HTML = "html"
    UNKNOWN = "unknown"


class ChunkMetadata(BaseModel):
    """文本块来源与结构上下文。"""

    chunk_id: str = Field(description="块唯一 ID")
    file_path: str = Field(default="", description="来源文件绝对路径")
    relative_path: str = Field(default="", description="来源相对路径（作为检索结果 source）")
    file_type: FileType = Field(default=FileType.TEXT, description="文件类别")
    start_line: int = Field(default=1, description="起始行（1-indexed）")
    end_line: int = Field(default=1, description="结束行（1-indexed）")
    char_count: int = Field(default=0, description="字符数")
    estimated_tokens: int = Field(default=0, description="预估 token 数")
    section_title: str | None = Field(default=None, description="章节标题")
    extra: dict[str, Any] = Field(default_factory=dict, description="附加元数据")


class Chunk(BaseModel):
    """一个可检索的文本块，含完整来源信息。"""

    text: str = Field(description="文本内容")
    metadata: ChunkMetadata = Field(description="来源元数据")
    embedding: list[float] | None = Field(default=None, description="稠密向量嵌入")

    @property
    def id(self) -> str:
        return self.metadata.chunk_id


class RetrievedDoc(BaseModel):
    """统一检索结果（RAGRetriever 接口的返回类型）。"""

    content: str = Field(description="检索到的文本内容")
    source: str = Field(default="", description="来源标识（相对路径或文档 ID）")
    score: float = Field(default=0.0, description="相关性分数（越高越好）")
    match_type: str = Field(default="bm25", description="检索机制：bm25 / vector / hybrid")


__all__ = ["FileType", "ChunkMetadata", "Chunk", "RetrievedDoc"]
