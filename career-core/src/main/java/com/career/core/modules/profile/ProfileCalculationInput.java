package com.career.core.modules.profile;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 画像计算输入。
 *
 * @param evidenceByDimension 六维证据；允许某些维度完全缺失
 * @param completedRequiredItems 已完成的必填资料和必答题数量
 * @param totalRequiredItems 必填资料和必答题总数
 */
public record ProfileCalculationInput(
        Map<ProfileDimension, List<ProfileEvidence>> evidenceByDimension,
        int completedRequiredItems,
        int totalRequiredItems) {

    public ProfileCalculationInput {
        if (completedRequiredItems < 0 || totalRequiredItems < 0
                || completedRequiredItems > totalRequiredItems) {
            throw new IllegalArgumentException("画像完整度计数不合法");
        }
        EnumMap<ProfileDimension, List<ProfileEvidence>> copy = new EnumMap<>(ProfileDimension.class);
        if (evidenceByDimension != null) {
            evidenceByDimension.forEach((dimension, evidence) -> {
                if (dimension == null) {
                    throw new IllegalArgumentException("画像维度不能为空");
                }
                copy.put(dimension, evidence == null ? List.of() : List.copyOf(evidence));
            });
        }
        evidenceByDimension = Collections.unmodifiableMap(copy);
    }
}
