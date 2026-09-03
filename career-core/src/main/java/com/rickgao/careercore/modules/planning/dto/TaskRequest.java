package com.rickgao.careercore.modules.planning.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 新增任务请求。 */
@Data
public class TaskRequest {

    private String month;
    private String title;
    private String type;
    private BigDecimal estHours;
    private LocalDate deadline;
    private List<String> abilityTags;
    private String note;
}
