package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布培养方案请求体。
 * 对齐 openapi CurriculumVersion,补充 jobId(一次发布一个任务);
 * id/courseCount/status/publishedBy 由后端生成计算。
 */
@Data
public class CurriculumPublishRequest {

    @NotBlank(message = "jobId 不能为空")
    private String jobId;

    @NotBlank(message = "方案名称不能为空")
    @Size(max = 200, message = "方案名称不能超过 200 字")
    private String name;

    @NotBlank(message = "适用专业不能为空")
    @Size(max = 100, message = "适用专业不能超过 100 字")
    private String major;
}
