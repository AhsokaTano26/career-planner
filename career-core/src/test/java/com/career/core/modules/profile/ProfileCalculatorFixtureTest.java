package com.career.core.modules.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCalculatorFixtureTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProfileCalculator calculator = new ProfileCalculator();

    @Test
    void completeFixture_六维均按权重确定性计算() throws Exception {
        assertFixture("complete.json");
    }

    @Test
    void partialFixture_缺失维度不按零分处理() throws Exception {
        ProfileCalculationResult result = assertFixture("partial.json");

        ProfileDimensionResult missing = result.dimensions().get("ACADEMIC_FOUNDATION");
        assertFalse(missing.available());
        assertNull(missing.rawScore());
        assertNull(missing.normalizedScore());
        assertEquals(0, missing.evidenceCount());
    }

    @Test
    void noExperienceFixture_没有实践经历时保持缺失而不是零分() throws Exception {
        ProfileCalculationResult result = assertFixture("no-experience.json");

        ProfileDimensionResult experience = result.dimensions().get("PRACTICAL_EXPERIENCE");
        assertFalse(experience.available());
        assertNull(experience.normalizedScore());
        assertTrue(result.explorationQuestions().contains("补充实践经历相关信息"));
    }

    @Test
    void calculate_相同输入始终产生相同结果() throws Exception {
        Fixture fixture = readFixture("complete.json");

        ProfileCalculationResult first = calculator.calculate(fixture.input());
        ProfileCalculationResult second = calculator.calculate(fixture.input());

        assertEquals(first, second);
        assertEquals(
                List.of(
                        "INTEREST",
                        "WORK_VALUES",
                        "ACADEMIC_FOUNDATION",
                        "ABILITY",
                        "DEVELOPMENT_TENDENCY",
                        "PRACTICAL_EXPERIENCE"),
                new ArrayList<>(first.dimensions().keySet()));
    }

    @Test
    void evidence_拒绝超出范围的分数() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileEvidence("INVALID", 101, 1));
    }

    private ProfileCalculationResult assertFixture(String fileName) throws Exception {
        Fixture fixture = readFixture(fileName);
        ProfileCalculationResult result = calculator.calculate(fixture.input());

        assertEquals(fixture.expectedCompleteness(), result.completeness(), 0.001);
        fixture.expectedScores().forEach((code, score) -> {
            ProfileDimensionResult dimension = result.dimensions().get(code);
            assertTrue(dimension.available(), code + " 应有有效分数");
            assertEquals(score, dimension.normalizedScore(), 0.001);
        });
        fixture.missingDimensions().forEach(code -> {
            ProfileDimensionResult dimension = result.dimensions().get(code);
            assertFalse(dimension.available(), code + " 应保持缺失状态");
            assertNull(dimension.normalizedScore());
        });
        return result;
    }

    private Fixture readFixture(String fileName) throws Exception {
        String path = "/profile-fixtures/" + fileName;
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("测试样例不存在: " + path);
            }
            JsonNode root = objectMapper.readTree(stream);
            EnumMap<ProfileDimension, List<ProfileEvidence>> evidence =
                    new EnumMap<>(ProfileDimension.class);
            Iterator<Map.Entry<String, JsonNode>> fields =
                    root.path("evidence").properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                ProfileDimension dimension = ProfileDimension.valueOf(field.getKey());
                List<ProfileEvidence> items = new ArrayList<>();
                for (JsonNode item : field.getValue()) {
                    items.add(new ProfileEvidence(
                            item.path("source").asText(),
                            item.path("score").asDouble(),
                            item.path("weight").asDouble()));
                }
                evidence.put(dimension, items);
            }

            Map<String, Double> expectedScores = new java.util.LinkedHashMap<>();
            root.path("expectedScores").properties().forEach(
                    field -> expectedScores.put(field.getKey(), field.getValue().asDouble()));
            List<String> missing = new ArrayList<>();
            root.path("missingDimensions").forEach(item -> missing.add(item.asText()));

            return new Fixture(
                    new ProfileCalculationInput(
                            evidence,
                            root.path("completedRequiredItems").asInt(),
                            root.path("totalRequiredItems").asInt()),
                    expectedScores,
                    root.path("expectedCompleteness").asDouble(),
                    missing);
        }
    }

    private record Fixture(
            ProfileCalculationInput input,
            Map<String, Double> expectedScores,
            double expectedCompleteness,
            List<String> missingDimensions) {
    }
}
