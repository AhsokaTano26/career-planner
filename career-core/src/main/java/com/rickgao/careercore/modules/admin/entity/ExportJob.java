package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 导出任务(export_job)。 */
@Data
public class ExportJob {

    private String id;
    private String type;
    private String scope;
    private String filtersJson;
    private String status;
    private String downloadUrl;
    private String filePath;
    private String errorMessage;
    private String operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
