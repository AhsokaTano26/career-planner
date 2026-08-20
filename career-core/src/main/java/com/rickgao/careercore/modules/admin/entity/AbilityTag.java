package com.rickgao.careercore.modules.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 能力标签(ability_tag)。 */
@Data
public class AbilityTag {

    private String id;
    private String name;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
