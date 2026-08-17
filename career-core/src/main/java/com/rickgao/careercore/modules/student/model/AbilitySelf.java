package com.rickgao.careercore.modules.student.model;

import lombok.Data;

/**
 * 能力自评(请求/响应/JSON 列共用,1-5)。
 */
@Data
public class AbilitySelf {

    private Integer programming;
    private Integer math;
    private Integer english;
    private Integer communication;
    private Integer organization;
}
