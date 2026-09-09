"""提示词模板加载：prompts/*.txt 缺失时用内置兜底，保证服务可独立运行。"""

from __future__ import annotations

from pathlib import Path

_PROMPT_DIR = Path(__file__).resolve().parent.parent / "prompts"


def load_prompt(filename: str, fallback: str) -> str:
    """读取 prompts/ 下模板；缺失或空白时返回内置兜底。"""
    try:
        text = (_PROMPT_DIR / filename).read_text(encoding="utf-8").strip()
    except OSError:
        return fallback
    return text or fallback


__all__ = ["load_prompt"]
