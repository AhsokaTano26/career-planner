package com.career.core.modules.planning;

/**
 * 站内提醒（线上 Apifox Reminder）。
 * type 取值 TASK_DEADLINE / REVIEW_REMIND / ADVISOR_REPLY / PLAN_UPDATE。
 */
public record ReminderDto(
        String id,
        String type,
        String title,
        String content,
        boolean read,
        String createdAt) {
}
