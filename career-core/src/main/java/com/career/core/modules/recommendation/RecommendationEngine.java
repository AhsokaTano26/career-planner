package com.career.core.modules.recommendation;

import com.career.core.common.Constants;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐引擎（Demo 阶段实现“规则过滤 + 结构化评分 + 模板理由”）。
 * 说明（Demo 精简点 / 后续迭代替换位置）：
 *   1. 评分在六维加权得分基础上叠加霍兰德（Holland）人格契合度软加分（HOLLAND_WEIGHT）。
 *   2. 每个维度计算“差距 gap”（学生分低于目标值的程度），写入评分结果供理由/大模型使用，不直接淘汰。
 *   3. buildReason 为模板兜底理由；解释生成环节优先调用 FastAPI 大模型（career-ai），失败回退此处模板。
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

    /** 霍兰德人格契合度在综合评分中的权重（Demo 精简点：人格映射作为第 7 维软加分，可后续调参） */
    private static final double HOLLAND_WEIGHT = 0.10;

    /** 差距提示阈值：单维差距 >= 10%（学生分低于目标值 10 分及以上）时在理由中提示“建议加强” */
    private static final double GAP_THRESHOLD = 0.10;

    /** 匹配优势阈值：单维匹配度 >= 85% 时计入“优势” */
    private static final double STRENGTH_THRESHOLD = 0.85;

    /** 置信度分档（百分制）：>= 80 HIGH，>= 60 MEDIUM，否则 LOW */
    private static final double CONFIDENCE_HIGH = 80.0;
    private static final double CONFIDENCE_MEDIUM = 60.0;

    /** 未分类方向的兜底行动建议 */
    private static final List<String> DEFAULT_ACTIONS = List.of(
            "明确主攻方向并制定学习路线", "完成一项实践项目", "阶段复盘与调整");

    /** 评分结果：方向 + 综合得分 + 各维度匹配度 + 各维度差距(0-1，仅含学生缺失/低于目标值) + 霍兰德契合度(-1 表示无人格/方向标签数据) */
    public record ScoredDirection(CareerDirection direction, double score, Map<String, Double> matches,
                                  Map<String, Double> gaps, double hollandMatch) {
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
     * 结构化评分（兼容无霍兰德人格数据场景，personality 传 null 即可）。
     * 详见 {@link #score(CareerDirection, Map, List, String)}。
     */
    public ScoredDirection score(CareerDirection direction,
                                 Map<String, Double> studentDims,
                                 List<DirectionWeight> weights) {
        return score(direction, studentDims, weights, null);
    }

    /**
     * 结构化评分：Score(direction) = Σ[weight_i × match(student_i, target_i)]。
     * 缺失维度不直接计 0，按已用权重重新归一化（设计说明书 9.3）。
     * 差距 gap_i = max(0, target_i - student_i) / 100，写入结果供理由/大模型提示“建议加强”，不直接淘汰。
     * Demo 精简点：六维加权得分基础上叠加霍兰德人格契合度软加分（HOLLAND_WEIGHT）。
     */
    public ScoredDirection score(CareerDirection direction,
                                 Map<String, Double> studentDims,
                                 List<DirectionWeight> weights,
                                 String studentPersonality) {
        Map<String, DirectionWeight> weightMap = weights.stream()
                .collect(Collectors.toMap(DirectionWeight::dimension, Function.identity(), (a, b) -> a));

        double total = 0.0;
        double usedWeight = 0.0;
        Map<String, Double> matches = new LinkedHashMap<>();
        Map<String, Double> gaps = new LinkedHashMap<>();
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
            // 差距提示：学生分低于目标值即记入差距（0-1），供理由模板/大模型提示“建议加强”
            double gap = Math.max(0.0, (target - student) / 100.0);
            if (gap > 0) {
                gaps.put(dim, round4(gap));
            }
        }
        double baseScore = usedWeight > 0 ? total / usedWeight : 0.0;
        double hollandMatch = hollandMatch(studentPersonality, direction.personalityTags());
        // Demo 精简点：人格契合度软加分，无任何一方数据时保持纯六维得分（hollandMatch < 0）
        double finalScore = hollandMatch >= 0
                ? baseScore * (1 - HOLLAND_WEIGHT) + hollandMatch * HOLLAND_WEIGHT
                : baseScore;
        return new ScoredDirection(direction, round4(finalScore), matches, gaps, round4(hollandMatch));
    }

    /**
     * 预置模板拼接推荐理由（兜底）。
     * 文案固定句式，依次包含：Top3 维度匹配、综合匹配度、霍兰德人格契合（若有）、差距提示“建议加强”（若有）。
     * Demo 精简：此为 career-ai 大模型解释失败时的模板回退；后续迭代替换点见 RecommendationService。
     */
    public String buildReason(CareerDirection direction, ScoredDirection scored) {
        List<String> top = scored.matches().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey())
                        + "匹配" + Math.round(e.getValue() * 100) + "%")
                .toList();
        StringBuilder sb = new StringBuilder("根据你的").append(String.join("、", top))
                .append("，综合匹配度 ").append(Math.round(scored.score() * 100)).append("%");
        // 霍兰德人格契合提示（仅当双方都有标签数据时可计算）
        if (scored.hollandMatch() >= 0 && scored.direction().personalityTags() != null
                && !scored.direction().personalityTags().isBlank()) {
            sb.append("，你的霍兰德人格类型（")
                    .append(String.join("/", parseTags(direction.personalityTags())))
                    .append("）与【").append(direction.name()).append("】方向契合度 ")
                    .append(Math.round(scored.hollandMatch() * 100)).append("%");
        }
        sb.append("，推荐关注【").append(direction.name()).append("】方向。");
        // 差距提示（skill gap）：差距达到阈值的维度按差距降序列出
        List<String> gapDims = scored.gaps().entrySet().stream()
                .filter(e -> e.getValue() >= GAP_THRESHOLD)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey()))
                .toList();
        if (!gapDims.isEmpty()) {
            sb.append("建议加强：").append(String.join("、", gapDims)).append("。");
        }
        return sb.toString();
    }

    /**
     * 推荐理由（数组）：每个维度一条短句，按匹配度降序取 Top3。
     * 线上字段 reasons 为 string[]，此处对应模板回退；AI 成功时由调用方替换为自然语言解释（单条）。
     */
    public List<String> buildReasons(ScoredDirection scored) {
        return scored.matches().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey())
                        + "匹配" + Math.round(e.getValue() * 100) + "%")
                .toList();
    }

    /**
     * 匹配优势（数组）：匹配度达到 85% 的维度，按匹配度降序。
     * 线上字段 strengths 为 string[]（模板生成，Demo 精简点）。
     */
    public List<String> buildStrengths(ScoredDirection scored) {
        List<String> strengths = scored.matches().entrySet().stream()
                .filter(e -> e.getValue() >= STRENGTH_THRESHOLD)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey())
                        + "表现较好")
                .toList();
        return strengths.isEmpty() ? List.of() : strengths;
    }

    /**
     * 主要差距（数组）：差距达到阈值的维度，按差距降序。
     * 线上字段 gaps 为 string[]（模板生成，Demo 精简点）。
     */
    public List<String> buildGaps(ScoredDirection scored) {
        List<String> gaps = scored.gaps().entrySet().stream()
                .filter(e -> e.getValue() >= GAP_THRESHOLD)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey())
                        + "需要加强")
                .toList();
        return gaps.isEmpty() ? List.of() : gaps;
    }

    /**
     * 本学期探索建议（数组）：按方向类型预置行动建议（模板，Demo 精简点，后续可替换为大模型生成）。
     * 线上字段 semesterActions 为 string[]。
     */
    public List<String> buildSemesterActions(CareerDirection direction) {
        String type = direction.type();
        if (type == null) {
            return DEFAULT_ACTIONS;
        }
        return switch (type) {
            case "数据算法" -> List.of("学习 Python 与数据分析基础", "完成一个数据分析小项目");
            case "技术研发" -> List.of("巩固编程语言与数据结构基础", "完成一个课程/开源项目");
            case "产品管理" -> List.of("学习产品方法与需求分析", "完成一份产品分析报告");
            default -> DEFAULT_ACTIONS;
        };
    }

    /**
     * 置信度枚举：按百分制综合得分映射 HIGH / MEDIUM / LOW。
     * 线上字段 confidence 为 string（Demo 精简点：替代原先 softmax 归一化概率表达）。
     */
    public String confidenceOf(double score01) {
        double percent = score01 * 100.0;
        if (percent >= CONFIDENCE_HIGH) {
            return "HIGH";
        }
        if (percent >= CONFIDENCE_MEDIUM) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 霍兰德人格契合度：交集大小 / min(学生人格数, 方向标签数)，取值 0-1。
     * 任一方缺失人格数据时返回 -1（表示“无数据”，不参与评分加分）。
     */
    public double hollandMatch(String studentPersonality, String directionTags) {
        if (studentPersonality == null || studentPersonality.isBlank()
                || directionTags == null || directionTags.isBlank()) {
            return -1.0;
        }
        List<String> student = parseTags(studentPersonality);
        List<String> direction = parseTags(directionTags);
        if (student.isEmpty() || direction.isEmpty()) {
            return -1.0;
        }
        long inter = student.stream().filter(direction::contains).count();
        return (double) inter / Math.min(student.size(), direction.size());
    }

    /**
     * 解析霍兰德标签串。支持逗号/斜杠分隔（"R,I,C"）与连续编码（"IRC"）两种写法，
     * 统一拆分为单字母 RIASEC 编码、大写并去重（非 RIASEC 字符忽略）。
     */
    private List<String> parseTags(String tags) {
        return java.util.Arrays.stream(tags.split("[,/]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .flatMap(s -> s.chars()
                        .mapToObj(c -> String.valueOf((char) c))
                        .filter(Constants.HOLLAND_CODES::contains))
                .distinct()
                .toList();
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
