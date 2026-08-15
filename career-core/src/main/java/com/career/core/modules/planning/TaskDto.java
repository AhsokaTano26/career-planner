package com.career.core.modules.planning;

/** 学生任务（线上 Apifox Task 简化版）。 */
public record TaskDto(
        Long id,
        String title,
        String status,
        String month) {
}
