package com.career.core.modules.student;

import java.time.LocalDate;

/** 学生经历实体（student_experience），即六维中的“经历”明细 */
public record StudentExperience(
        Long id,
        Long studentId,
        String type,
        String title,
        LocalDate startDate,
        String description) {
}
