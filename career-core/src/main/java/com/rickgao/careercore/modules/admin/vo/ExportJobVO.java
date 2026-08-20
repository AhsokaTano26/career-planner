package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 导出任务。对齐 openapi ExportJob。 */
@Data
public class ExportJobVO {

    private String id;
    private String type;
    private String scope;
    private String status;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private String operator;
}
