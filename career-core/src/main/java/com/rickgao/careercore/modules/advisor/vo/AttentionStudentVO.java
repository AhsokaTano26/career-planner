package com.rickgao.careercore.modules.advisor.vo;

import lombok.Data;

import java.util.List;

/**
 * 需关注学生。对齐 openapi AttentionStudent。
 */
@Data
public class AttentionStudentVO {

    private AdvisorStudentVO student;
    private List<String> reasons;
}
