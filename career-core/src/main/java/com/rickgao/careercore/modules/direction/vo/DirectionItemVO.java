package com.rickgao.careercore.modules.direction.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** 方向学生端列表项。 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectionItemVO {

    private String id;
    private String name;
    private String path;
    private String icon;
    private String intro;
    private Boolean favorited;
}
