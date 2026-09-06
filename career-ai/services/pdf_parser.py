"""PDF 解析：解析培养方案 PDF，结构化抽取课程信息。

Demo 实现：未引入第三方 PDF 库，仅从文件字节中抽取可读文本片段，
课程结构化抽取采用朴素行提取（后续迭代替换：接入 PyMuPDF/pdfplumber 做版式解析）。
"""

from __future__ import annotations

import ipaddress
import logging
import os
import socket
from urllib.parse import urlparse

import httpx

logger = logging.getLogger("pdf_parser")

_FETCH_TIMEOUT = 10.0
_MAX_BYTES = 20 * 1024 * 1024  # 与网关 20MB 上限对齐
_ALLOWED_CONTENT_TYPES = ("application/pdf", "application/octet-stream")


def _allow_private() -> bool:
    """开发联调用：PDF_FETCH_ALLOW_PRIVATE=1 时放行内网地址（生产必须保持默认关闭）。

    复审加固：开启时记 warning（防生产误开长期裸奔）；后续迭代替换位置：按环境 profile
    收紧（仅 dev/test 允许，prod 即使置 1 也拒绝）。
    """
    enabled = os.getenv("PDF_FETCH_ALLOW_PRIVATE", "") == "1"
    if enabled:
        logger.warning("pdf 内网拉取旁路已开启（PDF_FETCH_ALLOW_PRIVATE=1），生产环境必须关闭")
    return enabled


_MAX_REDIRECTS = 3


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
    """从内网地址拉取文件字节；失败返回空字节（Demo：不重试）。

    安全（2026-09 稳定性 + 复审加固）：仅 http(s)；禁止内网/回环/链路本地/组播/保留地址；
    重定向手动跟随（≤3 跳，每跳重新过 allowlist，防重定向 SSRF）；Content-Length/实际读取
    双上限 20MB；Content-Type 仅接受 PDF/字节流。命中任一拒绝即记日志并返回空字节。
    残留风险（TOCTOU）：域名检查与建连分两次 DNS，极端 DNS-rebinding 下仍有窗口；
    后续迭代替换位置：解析到 IP 后直连 IP + Host 头/SNI 固定（需自管 TLS 校验）。
    """
    current = url
    try:
        with httpx.Client(timeout=_FETCH_TIMEOUT, follow_redirects=False) as client:
            for _ in range(_MAX_REDIRECTS + 1):
                if not _url_allowed(current):
                    logger.warning("pdf 拉取拒绝（地址不合法）：%s", _safe_host(current))
                    return b""
                resp = client.get(current)
                if 300 <= resp.status_code < 400:
                    location = resp.headers.get("location")
                    if not location:
                        logger.warning("pdf 拉取拒绝（重定向无 Location）：%s", _safe_host(current))
                        return b""
                    # 相对 Location 按当前 URL 解析，下一跳重新过 allowlist
                    from urllib.parse import urljoin

                    current = urljoin(current, location)
                    logger.info("pdf 拉取跟随重定向：%s", _safe_host(current))
                    continue
                resp.raise_for_status()
                length = resp.headers.get("content-length")
                if length is not None:
                    try:
                        if int(length) > _MAX_BYTES:
                            logger.warning("pdf 拉取拒绝（超 20MB）：%s", _safe_host(current))
                            return b""
                    except (TypeError, ValueError):
                        return b""
                content_type = (resp.headers.get("content-type") or "").split(";")[0].strip().lower()
                if content_type and not content_type.startswith(_ALLOWED_CONTENT_TYPES):
                    logger.warning("pdf 拉取拒绝（Content-Type=%s）：%s", content_type, _safe_host(current))
                    return b""
                data = resp.content
                if len(data) > _MAX_BYTES:
                    logger.warning("pdf 拉取拒绝（实际超 20MB）：%s", _safe_host(current))
                    return b""
                return data
            logger.warning("pdf 拉取拒绝（重定向超 %d 跳）：%s", _MAX_REDIRECTS, _safe_host(url))
            return b""
    except Exception as exc:
        logger.warning("pdf 拉取失败（%s）：%s", _safe_host(url), exc)
        return b""


def _url_allowed(url: str) -> bool:
    """仅公网/允许的 http(s) 地址；域名解析后逐 IP 检查。

    复审加固：拒绝带 userinfo（user:pass@host 易藏真实目标）与非标准端口。
    """
    try:
        parsed = urlparse(url)
    except Exception:
        return False
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        return False
    if parsed.username or parsed.password:
        return False
    if parsed.port is not None and parsed.port not in (80, 443):
        return False
    if _allow_private():
        return True
    try:
        infos = socket.getaddrinfo(parsed.hostname, None)
    except OSError:
        return False
    for info in infos:
        try:
            ip = ipaddress.ip_address(info[4][0])
        except ValueError:
            return False
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_multicast or ip.is_reserved:
            return False
    return True


def _safe_host(url: str) -> str:
    """日志用主机名（不打完整 URL，避免 query 中的敏感信息进日志）。"""
    try:
        return urlparse(url).hostname or "?"
    except Exception:
        return "?"


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
