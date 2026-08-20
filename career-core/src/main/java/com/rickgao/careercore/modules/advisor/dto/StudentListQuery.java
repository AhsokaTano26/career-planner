package com.rickgao.careercore.modules.advisor.dto;

import lombok.Data;

/**
 * 所带学生列表查询参数(openapi GET /advisor/students query 参数)。
 */
@Data
public class StudentListQuery {

    /** graduate / employment / overseas */
    private String path;
    private String directionId;
    /** HAS_GOAL / NO_GOAL */
    private String goalStatus;
    /** REVIEWED_THIS_MONTH / LONG_NO_REVIEW */
    private String reviewStatus;
    private Boolean guidanceRequested;
    private String keyword;
    private Integer page;
    private Integer size;
    private String sort;
}
