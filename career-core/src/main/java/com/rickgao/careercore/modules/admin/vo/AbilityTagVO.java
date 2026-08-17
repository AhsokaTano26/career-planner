package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

/** 能力标签。对齐 openapi AbilityTag。 */
@Data
public class AbilityTagVO {

    private String id;
    private String name;
    private String category;
    private String status;
}
