package com.career.core.modules.profile;

/**
 * 一条可追溯的画像计分证据。
 *
 * @param source 证据来源，例如 ASSESSMENT_HOLLAND_V1
 * @param score  已由对应量表规则归一化到 0-100 的分数
 * @param weight 同一维度内的证据权重，必须大于 0
 */
public record ProfileEvidence(String source, double score, double weight) {

    public ProfileEvidence {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("画像证据来源不能为空");
        }
        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalArgumentException("画像证据分数必须在 0-100 之间");
        }
        if (!Double.isFinite(weight) || weight <= 0) {
            throw new IllegalArgumentException("画像证据权重必须大于 0");
        }
    }
}
