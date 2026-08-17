"""PDF 解析：解析培养方案 PDF，结构化抽取课程信息。

Demo 实现：未引入第三方 PDF 库，仅从文件字节中抽取可读文本片段，
课程结构化抽取采用朴素行提取（后续迭代替换：接入 PyMuPDF/pdfplumber 做版式解析）。
"""

from __future__ import annotations

import httpx

_FETCH_TIMEOUT = 10.0


def parse_from_url(file_url: str, filename: str) -> dict:
    """按 Apifox PdfParseRequest 从内网 fileUrl 拉取并解析，返回
    {status, itemCount?, confidence?}。

    - 拉取失败 → status=FAILED
    - 无可用文本（扫描件/二进制）→ status=REVIEW_REQUIRED
    - 有可用文本 → status=PARSING（Demo 同步抽取，itemCount/confidence 为启发式估算）
    """
    data = _fetch(file_url)
    if not data:
        return {"status": "FAILED"}
    raw_text = _extract_text(data)
    if not raw_text.strip():
        return {"status": "REVIEW_REQUIRED", "itemCount": 0, "confidence": 0.0}
    courses = _naive_courses(raw_text)
    return {
        "status": "PARSING",
        "itemCount": len(courses),
        "confidence": _estimate_confidence(courses),
    }


def _fetch(url: str) -> bytes:
    """从内网地址拉取文件字节；失败返回空字节（Demo：不重试）。"""
    try:
        with httpx.Client(timeout=_FETCH_TIMEOUT, follow_redirects=True) as client:
            resp = client.get(url)
        resp.raise_for_status()
        return resp.content
    except Exception:
        return b""


def _extract_text(data: bytes) -> str:
    """从原始字节中尽力提取可读文本（Demo 兜底，非严谨 PDF 解析）。"""
    try:
        return data.decode("utf-8", errors="ignore")
    except Exception:
        return ""


def _naive_courses(text: str) -> list[str]:
    """Demo 精简点：按行粗提取疑似课程行（真实版式解析后续接入）。"""
    lines = [ln.strip() for ln in text.splitlines() if ln.strip()]
    return lines[:200]


def _estimate_confidence(courses: list[str]) -> float:
    """Demo 精简点：基于行数的启发式置信度。"""
    return round(min(99.0, 50.0 + min(len(courses), 50)), 1)


__all__ = ["parse_from_url"]
