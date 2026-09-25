"""倒数排名融合（RRF），融合稠密向量与 BM25 关键词排序（直接迁移自 localrag-kit retrieval/rrf.py，MIT 协议）。
"""

from __future__ import annotations

from agent.rag.sqlite_store import SearchResult


def reciprocal_rank_fusion(
    bm25_results: list[SearchResult],
    vector_results: list[SearchResult],
    k_rrf: int = 60,
    bm25_weight: float = 1.0,
    vector_weight: float = 1.0,
    limit: int = 10,
) -> list[SearchResult]:
    """用倒数排名融合合并 BM25 关键词结果与稠密向量相似度结果。

    公式：RRF_score(d) = sum( weight_m / (k_rrf + rank_m(d)) )，对每个排序系统 m。
    默认 k_rrf = 60（现代信息检索的标准值）。
    """
    scores: dict[str, float] = {}
    chunk_map: dict[str, SearchResult] = {}
    sources: dict[str, list[str]] = {}

    # 1. 对 BM25 排序计分
    for rank, res in enumerate(bm25_results, start=1):
        cid = res.chunk.id
        chunk_map[cid] = res
        sources.setdefault(cid, []).append("bm25")
        rrf_val = bm25_weight / (k_rrf + rank)
        scores[cid] = scores.get(cid, 0.0) + rrf_val

    # 2. 对向量排序计分
    for rank, res in enumerate(vector_results, start=1):
        cid = res.chunk.id
        chunk_map[cid] = res
        sources.setdefault(cid, []).append("vector")
        rrf_val = vector_weight / (k_rrf + rank)
        scores[cid] = scores.get(cid, 0.0) + rrf_val

    # 按融合分数降序排序
    sorted_cids = sorted(scores.keys(), key=lambda cid: scores[cid], reverse=True)

    merged: list[SearchResult] = []
    for cid in sorted_cids[:limit]:
        original_res = chunk_map[cid]
        match_types = "+".join(sources.get(cid, ["unknown"]))
        merged.append(
            SearchResult(
                chunk=original_res.chunk,
                score=scores[cid],
                match_type=f"hybrid({match_types})",
            )
        )

    return merged
