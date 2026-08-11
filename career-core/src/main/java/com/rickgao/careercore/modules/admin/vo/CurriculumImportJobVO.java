package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 导入任务。对齐 openapi CurriculumImportJob。 */
@Data
public class CurriculumImportJobVO {

    private String id;
    private String filename;
    private String status;
    private Integer totalItems;
    private Integer parsedItems;
    private Double confidence;
    private LocalDateTime createdAt;
}
