"""PDF 解析：解析培养方案 PDF，结构化抽取课程信息。

Demo 实现：未引入第三方 PDF 库，仅从文件字节中抽取可读 ASCII/UTF-8 文本片段，
课程结构化抽取留空（后续迭代替换：接入 PyMuPDF/pdfplumber 做版式解析）。
"""

from __future__ import annotations


def parse(file_bytes: bytes, filename: str) -> dict:
    """解析 PDF 字节，返回 {filename, raw_text, courses}。

    Demo 精简点：courses 恒为空列表，待接入 PDF 版式解析后填充结构化课程。
    """
    raw_text = _extract_text(file_bytes)
    return {
        "filename": filename,
        "raw_text": raw_text[:2000],
        "courses": [],
    }


def _extract_text(data: bytes) -> str:
    """从原始字节中尽力提取可读文本（Demo 兜底，非严谨 PDF 解析）。"""
    try:
        return data.decode("utf-8", errors="ignore")
    except Exception:
        return ""


__all__ = ["parse"]
