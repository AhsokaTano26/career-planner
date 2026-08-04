package com.career.core.common;

import java.util.List;
import java.util.Map;

/**
 * 业务常量：六维画像维度编码、中文名与初始权重。
 * 注意：推荐权重最终应存放于配置表（direction_dimension_weight），
 * 此处权重仅作为“该方向未配置权重时的兜底默认值”。
 */
public final class Constants {

    private Constants() {
    }

    /** 六维画像维度编码（与 profile_snapshot.dimension_json 键一致） */
    public static final String DIM_INTEREST = "interest";
    public static final String DIM_VALUES = "values";
    public static final String DIM_ACADEMIC = "academic";
    public static final String DIM_ABILITY = "ability";
    public static final String DIM_ORIENTATION = "orientation";
    public static final String DIM_EXPERIENCE = "experience";

    /** 全部六维编码，用于完整度计算：非空字段数 / 总字段数 */
    public static final List<String> ALL_DIMENSIONS = List.of(
            DIM_INTEREST, DIM_VALUES, DIM_ACADEMIC, DIM_ABILITY, DIM_ORIENTATION, DIM_EXPERIENCE);

    /** 维度中文名（用于推荐理由模板拼接） */
    public static final Map<String, String> DIMENSION_NAMES = Map.of(
            DIM_INTEREST, "兴趣",
            DIM_VALUES, "职业价值观",
            DIM_ACADEMIC, "学业基础",
            DIM_ABILITY, "能力",
            DIM_ORIENTATION, "发展倾向",
            DIM_EXPERIENCE, "实践经历");

    /** 推荐引擎规则版本号 */
    public static final String RULE_VERSION = "RULE_V1";
}
