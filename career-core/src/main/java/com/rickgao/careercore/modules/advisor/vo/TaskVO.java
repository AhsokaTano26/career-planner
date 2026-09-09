package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划任务。对齐 openapi Task。
 */
@Data
public class TaskVO {

    private String id;
    private String month;
    private String title;
    /** LEARNING / PRACTICE / CAREER / REVIEW */
    private String type;
    /** 任务类型中文（LEARNING=学习/PRACTICE=实践/CAREER=职业探索/REVIEW=复盘）。 */
    private String typeName;
    private Double estHours;
    /** PENDING / DOING / DONE / DELAYED / ABANDONED */
    private String status;
    /** 状态中文（PENDING=待开始/DOING=进行中/DONE=已完成/DELAYED=已延期/ABANDONED=已放弃）。 */
    private String statusName;
    private LocalDate deadline;
    private List<String> abilityTags;
    private String note;
    private LocalDateTime checkedInAt;
    private TaskCheckin checkin;

    @Data
    public static class TaskCheckin {
        private String id;
        private String taskId;
        private String doneDesc;
        private String gains;
        private String difficulties;
        private String proofUrl;
        private LocalDateTime checkedInAt;
    }
}
