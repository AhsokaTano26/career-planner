package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 方向库(career_direction)。JSON 列以字符串承载,由服务层解析。 */
@Data
public class CareerDirection {

    private String id;
    private String name;
    private String path;
    private String icon;
    private String intro;
    private String targetJson;
    private BigDecimal minAbility;
    private BigDecimal minAcademic;
    private String learningJson;
    private String abilitiesJson;
    private String coursesJson;
    private String activitiesJson;
    private String pathDescJson;
    private String misconceptionsJson;
    private Integer sortOrder;
    private String applicableMajorsJson;
    private String status;
    private LocalDate updated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
