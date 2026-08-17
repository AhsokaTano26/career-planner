package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生档案基础信息查询行(列表用) */
@Data
public class ProfileRow {

    private String studentId;
    private String name;
    private String className;
    private Integer completeness;
    private String developmentIntention;
    private LocalDateTime updatedAt;
}
