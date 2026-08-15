package com.career.core.modules.planning;

import java.util.List;

/**
 * 学生任务（线上 Apifox Task）。
 * id 为字符串任务ID（如 T1）；type 取值 LEARNING/PRACTICE/CAREER/REVIEW；
 * status 取值 PENDING/DOING/DONE/DELAYED/ABANDONED。
 */
public record TaskDto(
        String id,
        String month,
        String title,
        String type,
        double estHours,
        String status,
        String deadline,
        List<String> abilityTags,
        String note,
        String checkedInAt,
        TaskCheckinDto checkin) {

    /** 任务打卡（线上 TaskCheckin：id/taskId/doneDesc 必填） */
    public record TaskCheckinDto(
            String id,
            String taskId,
            String doneDesc,
            String gains,
            String difficulties,
            String proofUrl,
            String checkedInAt) {
    }
}
