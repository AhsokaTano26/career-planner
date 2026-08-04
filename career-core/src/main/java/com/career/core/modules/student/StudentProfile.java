package com.career.core.modules.student;

import java.time.LocalDate;

/** 学生画像主表实体（student_profile） */
public record StudentProfile(
        Long id,
        Long userId,
        String studentNo,
        String majorCategory,
        String grade,
        String className) {
}
