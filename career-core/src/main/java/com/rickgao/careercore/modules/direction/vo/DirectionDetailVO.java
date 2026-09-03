package com.rickgao.careercore.modules.direction.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 方向学生端浏览 VO：已发布方向的完整可读信息 + 收藏状态。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectionDetailVO {

    private String id;
    private String name;
    private String path;
    private String icon;
    private String intro;
    /** 六维目标值 {interest,values,ability,academic,tendency,practice} */
    private Map<String, Object> target;
    private List<String> learning;
    private List<String> abilities;
    private List<String> courses;
    private List<String> activities;
    private List<String> pathDesc;
    private List<String> misconceptions;
    private List<String> applicableMajors;
    private Boolean favorited;
}
