package com.career.core.modules.profile;

import java.util.List;

/**
 * 单个画像维度的确定性计算结果。
 *
 * <p>available=false 时 rawScore/normalizedScore 必须为 null，明确区分“缺失”与“0 分”。</p>
 */
public record ProfileDimensionResult(
        String code,
        String name,
        Double rawScore,
        Double normalizedScore,
        int evidenceCount,
        List<String> sources,
        boolean available) {
}
