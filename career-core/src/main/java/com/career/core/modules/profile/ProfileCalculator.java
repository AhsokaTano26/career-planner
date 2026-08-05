package com.career.core.modules.profile;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生六维画像确定性计算器。
 *
 * <p>规则版本 PROFILE_RULE_V1：同一维度按证据权重计算加权平均；
 * 输入分数已统一为 0-100，因此 rawScore 与 normalizedScore 在 V1 中相同。
 * 缺失维度输出 null，绝不按 0 分参与计算。</p>
 */
@Component
public class ProfileCalculator {

    public static final String RULE_VERSION = "PROFILE_RULE_V1";

    public ProfileCalculationResult calculate(ProfileCalculationInput input) {
        Map<String, ProfileDimensionResult> dimensions = new LinkedHashMap<>();
        List<AvailableDimension> available = new ArrayList<>();

        for (ProfileDimension dimension : ProfileDimension.values()) {
            List<ProfileEvidence> evidence = input.evidenceByDimension()
                    .getOrDefault(dimension, List.of());
            if (evidence.isEmpty()) {
                dimensions.put(dimension.canonicalCode(), new ProfileDimensionResult(
                        dimension.canonicalCode(),
                        dimension.displayName(),
                        null,
                        null,
                        0,
                        List.of(),
                        false));
                continue;
            }

            double weightedTotal = evidence.stream()
                    .mapToDouble(item -> item.score() * item.weight())
                    .sum();
            double totalWeight = evidence.stream().mapToDouble(ProfileEvidence::weight).sum();
            double score = round2(weightedTotal / totalWeight);
            List<String> sources = evidence.stream()
                    .map(ProfileEvidence::source)
                    .distinct()
                    .toList();
            dimensions.put(dimension.canonicalCode(), new ProfileDimensionResult(
                    dimension.canonicalCode(),
                    dimension.displayName(),
                    score,
                    score,
                    evidence.size(),
                    sources,
                    true));
            available.add(new AvailableDimension(dimension, score));
        }

        double completeness = calculateCompleteness(input, available.size());
        List<String> strengths = available.stream()
                .filter(item -> item.score() >= 75)
                .sorted(Comparator.comparingDouble(AvailableDimension::score).reversed()
                        .thenComparing(item -> item.dimension().ordinal()))
                .limit(2)
                .map(item -> item.dimension().displayName())
                .toList();
        List<String> explorationQuestions = buildExplorationQuestions(dimensions, available);
        String summary = buildSummary(strengths, explorationQuestions, completeness);

        return new ProfileCalculationResult(
                Collections.unmodifiableMap(new LinkedHashMap<>(dimensions)),
                completeness,
                strengths,
                explorationQuestions,
                summary);
    }

    private double calculateCompleteness(ProfileCalculationInput input, int availableDimensionCount) {
        if (input.totalRequiredItems() > 0) {
            return round2(input.completedRequiredItems() * 100.0 / input.totalRequiredItems());
        }
        // Demo 兼容：历史快照没有题目完成计数时，使用“已有维度数 / 六维”作为保守完整度。
        return round2(availableDimensionCount * 100.0 / ProfileDimension.values().length);
    }

    private List<String> buildExplorationQuestions(
            Map<String, ProfileDimensionResult> dimensions,
            List<AvailableDimension> available) {
        List<String> questions = new ArrayList<>();
        for (ProfileDimension dimension : ProfileDimension.values()) {
            if (!dimensions.get(dimension.canonicalCode()).available()) {
                questions.add("补充" + dimension.displayName() + "相关信息");
            }
        }
        available.stream()
                .filter(item -> item.score() < 60)
                .sorted(Comparator.comparingDouble(AvailableDimension::score)
                        .thenComparing(item -> item.dimension().ordinal()))
                .map(item -> "进一步探索" + item.dimension().displayName())
                .forEach(questions::add);
        return questions.stream().distinct().limit(3).toList();
    }

    private String buildSummary(
            List<String> strengths,
            List<String> explorationQuestions,
            double completeness) {
        StringBuilder summary = new StringBuilder("当前画像完整度为 ")
                .append(Math.round(completeness))
                .append("%。");
        if (!strengths.isEmpty()) {
            summary.append("相对突出的维度为").append(String.join("、", strengths)).append("。");
        }
        if (!explorationQuestions.isEmpty()) {
            summary.append("建议").append(String.join("，", explorationQuestions)).append("。");
        } else {
            summary.append("六个维度均已有数据，可结合后续学习与实践持续更新。");
        }
        return summary.toString();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record AvailableDimension(ProfileDimension dimension, double score) {
    }
}
