package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 培养方案导入任务(curriculum_import_job)。 */
@Data
public class CurriculumImportJob {

    private String id;
    private String filename;
    private String filePath;
    private String fileType;
    private String status;
    private Integer totalItems;
    private Integer parsedItems;
    private BigDecimal confidence;
    private String errorMessage;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
