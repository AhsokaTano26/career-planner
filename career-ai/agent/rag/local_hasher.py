"""100% 离线的快速特征向量嵌入提供者（直接迁移自 localrag-kit providers/embeddings/local_hasher.py，MIT 协议）。

零外部依赖：基于 n-gram 特征哈希，无需下载任何模型权重、无需外部服务。
输出归一化的 384 维稠密向量。
"""

from __future__ import annotations

import hashlib
import re

from agent.rag.base import BaseEmbeddingProvider
from agent.rag.vector_ops import normalize_vector


class FastFeatureEmbeddingProvider(BaseEmbeddingProvider):
    """零依赖本地嵌入生成器：n-gram 特征投影。

    100% 离线、零下载，输出归一化的 384 维向量。
    """

    def __init__(self, dimension: int = 384):
        self._dim = dimension

    @property
    def name(self) -> str:
        return "fast-local"

    @property
    def dimension(self) -> int:
        return self._dim

    def embed_text(self, text: str) -> list[float]:
        """生成输入文本的稠密归一化向量表示。"""
        if not text or not text.strip():
            return [0.0] * self._dim

        # 分词并清洗文本
        tokens = re.findall(r"\b[a-zA-Z0-9_\-]{2,}\b", text.lower())
        # 中文没有空格分词，补充按字符切分（连续中文拆为单字 + 双字）
        cjk_chars = re.findall(r"[\u4e00-\u9fff]", text)
        if cjk_chars:
            tokens.extend(cjk_chars)
            # 中文双字组合（bigram）提供局部上下文
            cjk_text = re.findall(r"[\u4e00-\u9fff]+", text)
            for seg in cjk_text:
                tokens.extend(seg[i : i + 2] for i in range(len(seg) - 1))

        if not tokens:
            return [0.0] * self._dim

        vec = [0.0] * self._dim

        # 1. Unigram 特征哈希
        for token in tokens:
            self._project_token(token, weight=1.0, vec=vec)

        # 2. Bigram 特征哈希（词序上下文）
        # 注：cjk 双字已在上方以 token 形式加入，此处只对英文 token 做 bigram
        ascii_tokens = [t for t in tokens if re.search(r"[a-z0-9]", t)]
        for i in range(len(ascii_tokens) - 1):
            bigram = f"{ascii_tokens[i]}_{ascii_tokens[i + 1]}"
            self._project_token(bigram, weight=1.5, vec=vec)

        # 3. 子词字符 n-gram（3-gram，容错与词干）
        for token in tokens:
            if len(token) >= 4 and token.isascii():
                for j in range(len(token) - 2):
                    sub = token[j : j + 3]
                    self._project_token(sub, weight=0.4, vec=vec)

        return normalize_vector(vec)

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return [self.embed_text(t) for t in texts]

    def _project_token(self, token: str, weight: float, vec: list[float]) -> None:
        """把 token 哈希到特征索引，带随机符号投影。"""
        h = hashlib.sha256(token.encode("utf-8")).digest()
        # 从 SHA-256 派生两个 32 位整数
        idx1 = int.from_bytes(h[0:4], "little") % self._dim
        sign1 = 1.0 if (h[4] & 1) else -1.0
        vec[idx1] += sign1 * weight

        idx2 = int.from_bytes(h[8:12], "little") % self._dim
        sign2 = 1.0 if (h[12] & 1) else -1.0
        vec[idx2] += sign2 * (weight * 0.5)
