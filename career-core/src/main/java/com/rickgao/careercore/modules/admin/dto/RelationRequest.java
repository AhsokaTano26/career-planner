package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量建立辅导员学生关系请求体。对齐 openapi RelationRequest。
 */
@Data
public class RelationRequest {

    @NotBlank(message = "辅导员 ID 不能为空")
    private String advisorId;

    @NotEmpty(message = "学生 ID 列表不能为空")
    @Size(max = 100, message = "单次批量最多 100 名学生")
    private List<String> studentIds;
}
