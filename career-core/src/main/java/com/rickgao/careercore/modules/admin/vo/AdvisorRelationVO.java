package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端-辅导员学生关系项。对齐 openapi AdvisorRelation。
 */
@Data
public class AdvisorRelationVO {

    private String id;
    private String advisorId;
    private String advisorName;
    private String studentId;
    private String studentName;
    private LocalDateTime createdAt;
}
