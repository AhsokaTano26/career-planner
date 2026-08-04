package com.career.core.modules.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础单元测试：仅覆盖推荐引擎核心逻辑（最基础用例）。
 */
class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void filterActive_仅保留启用方向() {
        List<CareerDirection> all = List.of(
                new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null),
                new CareerDirection(2L, "D2", "方向2", "技术", "INACTIVE", null));

        List<CareerDirection> active = engine.filterActive(all);

        assertEquals(1, active.size());
        assertEquals("D1", active.get(0).directionCode());
    }

    @Test
    void score_评分在0到1之间且结果确定() {
        CareerDirection d = new CareerDirection(1L, "D1", "方向1", "技术", "ACTIVE", null);
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
}
