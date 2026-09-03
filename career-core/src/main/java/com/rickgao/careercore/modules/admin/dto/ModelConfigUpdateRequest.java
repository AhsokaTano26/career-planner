package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 管理端模型配置更新请求。 */
@Data
public class ModelConfigUpdateRequest {

    @NotNull(message = "configValue 不能为空")
    private String configValue;
}
