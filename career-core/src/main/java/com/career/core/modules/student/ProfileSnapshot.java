package com.career.core.modules.student;

import java.time.LocalDateTime;

/** 画像快照实体（profile_snapshot），dimensionJson 为六维画像 JSON 原文。 */
public record ProfileSnapshot(
        Long id,
        Long studentId,
        String sourceVersion,
        String dimensionJson,
        String summary,
        Integer versionNo,
        Double completeness,
        LocalDateTime generatedAt) {
}
