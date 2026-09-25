"""RAG 混合检索引擎测试（localrag-kit 迁移组件）。
"""

from __future__ import annotations

import tempfile
from pathlib import Path

import pytest

from agent.rag.local_hasher import FastFeatureEmbeddingProvider
from agent.rag.models import RetrievedDoc
from agent.rag.orchestrator import HybridRetriever
from agent.rag.rrf import reciprocal_rank_fusion
from agent.rag.sqlite_store import SearchResult, SQLiteStore
from agent.rag.synthesizer import ContextSynthesizer
from agent.rag.vector_ops import (
    batch_cosine_similarities,
    cosine_similarity,
    normalize_vector,
    serialize_vector,
    deserialize_vector,
)


class TestVectorOps:
    """向量运算测试。"""

    def test_serialize_deserialize_roundtrip(self):
        vec = [0.1, 0.2, 0.3, 0.4, 0.5]
        blob = serialize_vector(vec)
        restored = deserialize_vector(blob)
        assert len(restored) == len(vec)
        for a, b in zip(vec, restored):
            assert abs(a - b) < 1e-6

    def test_normalize_vector(self):
        vec = [3.0, 4.0]
        normed = normalize_vector(vec)
        assert abs(sum(x * x for x in normed) - 1.0) < 1e-6

    def test_normalize_zero_vector(self):
        assert normalize_vector([0.0, 0.0]) == [0.0, 0.0]

    def test_cosine_similarity_identical(self):
        v = [1.0, 0.0, 0.0]
        assert abs(cosine_similarity(v, v) - 1.0) < 1e-6

    def test_cosine_similarity_orthogonal(self):
        v1 = [1.0, 0.0]
        v2 = [0.0, 1.0]
        assert abs(cosine_similarity(v1, v2)) < 1e-6

    def test_batch_cosine_similarities(self):
        q = [1.0, 0.0]
        candidates = [[1.0, 0.0], [0.0, 1.0]]
        scores = batch_cosine_similarities(q, candidates)
        assert len(scores) == 2
        assert scores[0] > scores[1]


class TestFastFeatureEmbedding:
    """零模型零下载嵌入测试。"""

    def test_embed_returns_384_dim(self):
        provider = FastFeatureEmbeddingProvider()
        vec = provider.embed_text("软件工程师需要编程能力")
        assert len(vec) == 384

    def test_embed_is_normalized(self):
        provider = FastFeatureEmbeddingProvider()
        vec = provider.embed_text("career planning")
        norm = sum(x * x for x in vec) ** 0.5
        assert abs(norm - 1.0) < 1e-5

    def test_embed_empty_text(self):
        provider = FastFeatureEmbeddingProvider()
        vec = provider.embed_text("")
        assert vec == [0.0] * 384

    def test_similar_texts_have_higher_similarity(self):
        provider = FastFeatureEmbeddingProvider()
        v1 = provider.embed_text("软件工程师 编程 开发")
        v2 = provider.embed_text("编程语言 软件开发 工程师")
        v3 = provider.embed_text("厨师 烹饪 美食")
        sim_related = cosine_similarity(v1, v2)
        sim_unrelated = cosine_similarity(v1, v3)
        assert sim_related > sim_unrelated

    def test_embed_batch(self):
        provider = FastFeatureEmbeddingProvider()
        vecs = provider.embed_batch(["text one", "text two"])
        assert len(vecs) == 2
        assert all(len(v) == 384 for v in vecs)


@pytest.fixture
def temp_store():
    with tempfile.TemporaryDirectory() as tmpdir:
        db_path = Path(tmpdir) / "test_rag.db"
        store = SQLiteStore(db_path)
        yield store
        store.close()


class TestSQLiteStore:
    """SQLite 混合存储测试。"""

    def test_init_creates_tables(self, temp_store):
        stats = temp_store.get_stats()
        assert stats.total_files == 0
        assert stats.total_chunks == 0

    def test_upsert_and_search_bm25(self, temp_store):
        from agent.rag.models import Chunk, ChunkMetadata

        chunk = Chunk(
            text="软件工程师需要掌握编程语言和算法设计",
            metadata=ChunkMetadata(chunk_id="c1", relative_path="dir/1"),
        )
        temp_store.upsert_file(relative_path="dir/1")
        temp_store.upsert_chunks([chunk])

        results = temp_store.search_bm25("编程语言", limit=5)
        assert len(results) > 0
        assert "编程" in results[0].chunk.text

    def test_bm25_no_results(self, temp_store):
        results = temp_store.search_bm25("不存在的内容", limit=5)
        assert results == [] or all(r.score >= 0 for r in results)

    def test_upsert_and_search_vector(self, temp_store):
        from agent.rag.models import Chunk, ChunkMetadata
        from agent.rag.local_hasher import FastFeatureEmbeddingProvider

        provider = FastFeatureEmbeddingProvider()
        text = "软件工程师需要掌握编程语言"
        chunk = Chunk(
            text=text,
            metadata=ChunkMetadata(chunk_id="c1", relative_path="dir/1"),
            embedding=provider.embed_text(text),
        )
        temp_store.upsert_file(relative_path="dir/1")
        temp_store.upsert_chunks([chunk])

        qv = provider.embed_text("编程语言 工程师")
        results = temp_store.search_vector(qv, limit=5)
        assert len(results) > 0
        assert results[0].score > 0


class TestRRF:
    """倒数排名融合测试。"""

    def _make_result(self, chunk_id: str, score: float) -> SearchResult:
        from agent.rag.models import Chunk, ChunkMetadata

        return SearchResult(
            chunk=Chunk(
                text=f"content-{chunk_id}",
                metadata=ChunkMetadata(chunk_id=chunk_id, relative_path=chunk_id),
            ),
            score=score,
            match_type="bm25",
        )

    def test_rrf_merges_and_ranks(self):
        bm25 = [self._make_result("a", 0.9), self._make_result("b", 0.8)]
        vector = [self._make_result("b", 0.95), self._make_result("c", 0.7)]
        merged = reciprocal_rank_fusion(bm25, vector, limit=10)
        assert len(merged) == 3
        # b 同时出现在两个列表，应排第一
        assert merged[0].chunk.id == "b"
        assert "hybrid" in merged[0].match_type

    def test_rrf_empty_inputs(self):
        assert reciprocal_rank_fusion([], []) == []


class TestContextSynthesizer:
    """上下文合成测试。"""

    def test_synthesize_empty(self):
        synth = ContextSynthesizer()
        ctx = synth.synthesize([])
        assert ctx.formatted_context == ""
        assert ctx.citations == []

    def test_synthesize_deduplicates(self):
        from agent.rag.models import Chunk, ChunkMetadata

        def make(cid, path, start, end):
            return SearchResult(
                chunk=Chunk(
                    text=f"chunk {cid}",
                    metadata=ChunkMetadata(
                        chunk_id=cid, relative_path=path, start_line=start, end_line=end
                    ),
                ),
                score=0.5,
                match_type="bm25",
            )

        synth = ContextSynthesizer()
        results = [make("a", "f.txt", 1, 5), make("b", "f.txt", 1, 5)]  # 重复行范围
        ctx = synth.synthesize(results)
        assert len(ctx.citations) == 1  # 去重后只保留一个

    def test_synthesize_token_budget(self):
        from agent.rag.models import Chunk, ChunkMetadata

        synth = ContextSynthesizer(max_token_budget=10)
        long_text = "很长的文本" * 100  # 远超预算
        results = [
            SearchResult(
                chunk=Chunk(
                    text=long_text,
                    metadata=ChunkMetadata(chunk_id="a", relative_path="f.txt"),
                ),
                score=0.5,
                match_type="bm25",
            )
        ]
        ctx = synth.synthesize(results)
        # 首块总是保留（即使超预算），后续块会被跳过
        assert len(ctx.citations) >= 1


class TestHybridRetriever:
    """混合检索编排器测试。"""

    @pytest.fixture
    def retriever(self, tmp_path):
        r = HybridRetriever(db_path=str(tmp_path / "test.db"))
        yield r
        r.close()

    def test_index_and_bm25_retrieve(self, retriever):
        docs = [
            {"content": "软件工程师需要掌握编程语言和算法", "source": "direction/1"},
            {"content": "产品经理需要市场分析和用户研究能力", "source": "direction/2"},
        ]
        retriever.index(docs)
        results = retriever.retrieve("编程语言", top_k=5, mode="bm25")
        assert len(results) > 0
        assert "编程" in results[0].content

    def test_index_and_vector_retrieve(self, retriever):
        docs = [
            {"content": "software engineer programming algorithms", "source": "d1"},
            {"content": "chef cooking culinary arts", "source": "d2"},
        ]
        retriever.index(docs)
        results = retriever.retrieve("programming algorithms", top_k=5, mode="vector")
        assert len(results) > 0

    def test_index_and_hybrid_retrieve(self, retriever):
        docs = [
            {"content": "软件工程师需要编程能力", "source": "d1"},
            {"content": "厨师需要烹饪技巧", "source": "d2"},
        ]
        retriever.index(docs)
        results = retriever.retrieve("编程", top_k=5, mode="hybrid")
        assert len(results) > 0
        # 混合模式的 match_type 应包含 hybrid 或降级到 bm25/vector
        assert results[0].match_type in ("hybrid(bm25+vector)", "hybrid(vector+bm25)", "bm25", "vector")

    def test_retrieve_empty_store(self, retriever):
        results = retriever.retrieve("任何查询", top_k=5)
        assert results == []

    def test_index_replaces_old_chunks(self, retriever):
        retriever.index([{"content": "旧内容", "source": "same/source"}])
        retriever.index([{"content": "新内容", "source": "same/source"}])
        results = retriever.retrieve("新内容", top_k=5, mode="bm25")
        # 同 source 应替换而非追加
        assert retriever.store.get_stats().total_chunks == 1

    def test_returns_retrieved_doc_types(self, retriever):
        docs = [{"content": "测试内容", "source": "test"}]
        retriever.index(docs)
        results = retriever.retrieve("测试", top_k=5, mode="bm25")
        for r in results:
            assert isinstance(r, RetrievedDoc)
            assert r.content
            assert r.source
