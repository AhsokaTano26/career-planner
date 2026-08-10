package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 新增/更新方向请求体。对齐 openapi Direction(忽略 favorited/status 等学生端/状态字段)。 */
@Data
public class DirectionRequest {

    @Size(max = 64, message = "方向编码不能超过 64 位")
    private String id;

    @Size(max = 100, message = "方向名称不能超过 100 字")
    private String name;

    private String path;
    private String icon;
    private String intro;
    private Target target;
    private Double minAbility;
    private Double minAcademic;
    private List<String> learning;
    private List<String> abilities;
    private List<String> courses;
    private List<String> activities;
    private List<String> pathDesc;
    private List<String> misconceptions;
    private Integer sortOrder;
    private List<String> applicableMajors;

    @Data
    public static class Target {
        private Double interest;
        private Double values;
        private Double ability;
        private Double academic;
        private Double tendency;
        private Double practice;
    }
}
