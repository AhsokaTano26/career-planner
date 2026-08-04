package com.career.core.modules.recommendation;

/** 方向维度权重与目标值实体（direction_dimension_weight） */
public record DirectionWeight(
        String dimension,
        Double targetValue,
        Double weight) {
}
