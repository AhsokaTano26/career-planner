package com.career.core.modules.planning;

/**
 * 学生目标（线上 Apifox Goal）：主/备选目标 + 版本信息。
 * primary 为必填主目标；backup 可空；version 为目标版本号（如 G-v3）。
 */
public record GoalDto(
        GoalItemDto primary,
        GoalItemDto backup,
        String version,
        String updatedAt) {

    /** 单个目标项（线上 Goal.primary / Goal.backup） */
    public record GoalItemDto(String directionId, String name, String chosenAt) {
    }
}
