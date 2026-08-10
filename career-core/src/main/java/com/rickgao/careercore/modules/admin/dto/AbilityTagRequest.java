package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增/更新能力标签请求体。对齐 openapi AbilityTag。 */
@Data
public class AbilityTagRequest {

    @Size(max = 64, message = "标签编码不能超过 64 位")
    private String id;

    @Size(max = 100, message = "标签名称不能超过 100 字")
    private String name;

    @Size(max = 50, message = "分类不能超过 50 字")
    private String category;

    private String status;
}
