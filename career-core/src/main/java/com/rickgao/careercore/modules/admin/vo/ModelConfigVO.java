package com.rickgao.careercore.modules.admin.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/** 模型配置 VO（敏感值掩码展示）。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelConfigVO {

    private String configKey;
    private String configValue;
    /** 是否敏感值（掩码显示） */
    private Boolean masked;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

