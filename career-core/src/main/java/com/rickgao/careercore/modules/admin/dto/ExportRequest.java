package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/** 创建导出任务请求体。对齐 openapi ExportRequest。 */
@Data
public class ExportRequest {

    /** STUDENT_DATA / WHITELIST / OPERATION_LOG / AI_LOG / DIRECTION_LIB */
    @NotBlank(message = "type 不能为空")
    private String type;

    @Size(max = 500, message = "导出范围描述不能超过 500 字")
    private String scope;

    private Map<String, Object> filters;
}
