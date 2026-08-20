package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 方向启停请求体。对齐 openapi DirectionStatusUpdate。 */
@Data
public class DirectionStatusUpdate {

    @NotBlank(message = "status 不能为空")
    private String status;
}
