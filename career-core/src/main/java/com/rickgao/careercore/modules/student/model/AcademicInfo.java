package com.rickgao.careercore.modules.student.model;

import lombok.Data;

/**
 * 学业基础(请求/响应/JSON 列共用,自评 1-5)。
 */
@Data
public class AcademicInfo {

    private Integer math;
    private Integer english;
    private Integer programming;
    /** 学业备注(选填) */
    private String note;
}
