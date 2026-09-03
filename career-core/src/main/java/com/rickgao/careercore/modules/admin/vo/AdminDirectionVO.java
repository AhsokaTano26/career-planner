package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端-方向库列表项。对齐 openapi AdminDirection。 */
@Data
public class AdminDirectionVO {

    private String id;
    private String name;
    private String path;
    private String icon;
    private String intro;
    private String status;
    private Integer sortOrder;
    private Target target;
    private Double minAbility;
    private Double minAcademic;
    private List<String> learning;
    private List<String> abilities;
    private List<String> courses;
    private List<String> activities;
    private List<String> pathDesc;
    private List<String> misconceptions;
    private List<String> applicableMajors;
    private LocalDateTime updatedAt;

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
