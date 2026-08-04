package com.career.core.modules.recommendation;

import com.career.core.common.Constants;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐引擎（Demo 阶段仅实现“规则过滤 + 结构化评分 + 模板理由”）。
 * 后续迭代：解释生成环节替换为调用 FastAPI 大模型（career-ai），此处即替换点。
 */
@Component
public class RecommendationEngine {

    /** 兜底默认权重（设计说明书 9.2 初始权重），仅当方向未配置 direction_dimension_weight 时使用 */
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            Constants.DIM_INTEREST, 0.20,
            Constants.DIM_VALUES, 0.15,
            Constants.DIM_ABILITY, 0.25,
            Constants.DIM_ACADEMIC, 0.15,
            Constants.DIM_ORIENTATION, 0.20,
            Constants.DIM_EXPERIENCE, 0.05);

    /** 方向未配置目标值时的中间兜底值 */
    private static final double DEFAULT_TARGET = 75.0;

    /** 评分结果：方向 + 综合得分 + 各维度匹配度 */
    public record ScoredDirection(CareerDirection direction, double score, Map<String, Double> matches) {
    }

    /**
     * 规则过滤：仅保留启用（ACTIVE）的方向。
     * Demo 精简：只按状态过滤；后续可叠加“适用专业/必要条件/路径”等过滤规则。
     */
    public List<CareerDirection> filterActive(List<CareerDirection> all) {
        return all.stream()
                .filter(d -> "ACTIVE".equalsIgnoreCase(d.status()))
                .toList();
    }

    /**
     * 结构化评分：Score(direction) = Σ[weight_i × match(student_i, target_i)]。
     * 缺失维度不直接计 0，按已用权重重新归一化（设计说明书 9.3）。
     */
    public ScoredDirection score(CareerDirection direction,
                                 Map<String, Double> studentDims,
                                 List<DirectionWeight> weights) {
        Map<String, DirectionWeight> weightMap = weights.stream()
                .collect(Collectors.toMap(DirectionWeight::dimension, Function.identity(), (a, b) -> a));

        double total = 0.0;
        double usedWeight = 0.0;
        Map<String, Double> matches = new LinkedHashMap<>();
        for (String dim : Constants.ALL_DIMENSIONS) {
            Double student = studentDims.get(dim);
            if (student == null) {
                continue; // 学生该维度缺失：跳过，不参与评分
            }
            double w = weightFor(dim, weightMap);
            double target = targetFor(dim, weightMap);
            double m = match(student, target);
            total += w * m;
            usedWeight += w;
            matches.put(dim, m);
        }
        double finalScore = usedWeight > 0 ? total / usedWeight : 0.0;
        return new ScoredDirection(direction, round4(finalScore), matches);
    }

    /**
     * 预置模板拼接推荐理由。
     * Demo 精简：不使用大模型，取匹配度最高的 3 个维度拼接固定句式；
     * 后续迭代替换为 FastAPI 生成的自然语言解释。
     */
    public String buildReason(CareerDirection direction, ScoredDirection scored) {
        List<String> top = scored.matches().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey())
                        + "匹配" + Math.round(e.getValue() * 100) + "%")
                .toList();
        return "根据你的" + String.join("、", top)
                + "，综合匹配度 " + Math.round(scored.score() * 100)
                + "%，推荐关注【" + direction.name() + "】方向。";
    }

    private double weightFor(String dim, Map<String, DirectionWeight> weightMap) {
        DirectionWeight w = weightMap.get(dim);
        if (w != null && w.weight() != null) {
            return w.weight();
        }
        return DEFAULT_WEIGHTS.getOrDefault(dim, 0.0);
    }

    private double targetFor(String dim, Map<String, DirectionWeight> weightMap) {
        DirectionWeight w = weightMap.get(dim);
        if (w != null && w.targetValue() != null) {
            return w.targetValue();
        }
        return DEFAULT_TARGET;
    }

    /** 连续维度匹配函数：1 - |学生分 - 目标值|/100，截断至 0-1 */
    private double match(double student, double target) {
        return Math.max(0.0, Math.min(1.0, 1.0 - Math.abs(student - target) / 100.0));
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
