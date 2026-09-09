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
    /** 路径中文（graduate=国内升学/employment=就业发展/overseas=出国留学）。 */
    private String pathName;
    private String icon;
    private String intro;
    private Boolean favorited;
}

