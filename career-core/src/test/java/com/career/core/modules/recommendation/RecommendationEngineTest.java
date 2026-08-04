package com.career.core.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础单元测试：仅覆盖推荐引擎核心逻辑（最基础用例）。
 */
class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void filterActive_仅保留启用方向() {
        List<CareerDirection> all = List.of(
                new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null, null),
                new CareerDirection(2L, "D2", "方向2", "技术", "INACTIVE", null, null));

        List<CareerDirection> active = engine.filterActive(all);

        assertEquals(1, active.size());
        assertEquals("D1", active.get(0).directionCode());
    }

    @Test
    void score_评分在0到1之间且结果确定() {
        CareerDirection d = new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null, null);
        Map<String, Double> dims = Map.of(
                "interest", 80.0, "values", 70.0, "academic", 85.0,
                "ability", 76.0, "orientation", 80.0, "experience", 60.0);
        List<DirectionWeight> weights = List.of(
                new DirectionWeight("interest", 85.0, 0.20),
                new DirectionWeight("values", 65.0, 0.15),
                new DirectionWeight("ability", 82.0, 0.25),
                new DirectionWeight("academic", 80.0, 0.15),
                new DirectionWeight("orientation", 85.0, 0.20),
                new DirectionWeight("experience", 65.0, 0.05));

        double s1 = engine.score(d, dims, weights).score();
        double s2 = engine.score(d, dims, weights).score();

        assertTrue(s1 >= 0.0 && s1 <= 1.0);
        assertEquals(s1, s2, 1e-6);
    }

    @Test
    void score_霍兰德契合度影响评分() {
        CareerDirection fit = new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null, "I,R,C");
        CareerDirection misfit = new CareerDirection(2L, "D2", "方向2", "技术", "ACTIVE", null, "E,S,A");
        Map<String, Double> dims = Map.of(
                "interest", 80.0, "values", 70.0, "academic", 85.0,
                "ability", 76.0, "orientation", 80.0, "experience", 60.0);
        List<DirectionWeight> weights = List.of();

        double fitScore = engine.score(fit, dims, weights, "IRC").score();
        double misfitScore = engine.score(misfit, dims, weights, "IRC").score();

        assertTrue(fitScore > misfitScore, "契合方向评分应高于不契合方向");
        assertTrue(fitScore <= 1.0 && misfitScore >= 0.0);
    }

    @Test
    void buildReason_含差距提示与霍兰德契合() {
        CareerDirection d = new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null, "I,R,C");
        Map<String, Double> dims = Map.of(
                "interest", 82.0, "values", 70.0, "academic", 85.0,
                "ability", 76.0, "orientation", 80.0, "experience", 60.0);
        List<DirectionWeight> weights = List.of(
                new DirectionWeight("interest", 85.0, 0.20),
                new DirectionWeight("values", 65.0, 0.15),
                new DirectionWeight("ability", 82.0, 0.25),
                new DirectionWeight("academic", 80.0, 0.15),
                new DirectionWeight("orientation", 85.0, 0.20),
                new DirectionWeight("experience", 65.0, 0.05));

        // 无差距的画像 → 理由不含“建议加强”
        String reasonNoGap = engine.buildReason(d, engine.score(d, dims, weights, "IRC"));
        assertTrue(reasonNoGap.contains("推荐关注【方向1】方向"));
        assertFalse(reasonNoGap.contains("建议加强"));

        // 有差距的画像（experience 60 远低于目标 90）→ 理由含“建议加强：实践经历”
        Map<String, Double> weakDims = new java.util.HashMap<>(dims);
        weakDims.put("experience", 55.0);
        weakDims.put("ability", 60.0);
        String reasonWithGap = engine.buildReason(d, engine.score(d, weakDims, weights, "IRC"));
        assertTrue(reasonWithGap.contains("建议加强"));
    }
}
