package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 模型配置，对齐 model_config 表。 */
@Data
public class ModelConfig {

    private String id;
    private String configKey;
    private String configValue;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}

