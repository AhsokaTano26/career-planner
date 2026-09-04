package com.rickgao.careercore.modules.admin.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理端问卷创建/更新/新建版本请求。
 * questions 仅用于"创建问卷(带首版)"或"新建版本"。
 */
@Data
public class QuestionnaireRequest {

    private String type;
    private String name;
    private String typeName;
    private String icon;
    private Integer minutes;
    private String tip;
    private String changeNote;

    private List<QuestionItem> questions;

    @Data
    public static class QuestionItem {
        private String text;
        /** CHOICE / RATING */
        private String type;
        private String dim;
        private List<OptionItem> options;
    }

    @Data
    public static class OptionItem {
        private String text;
        /** 六维得分 {interest,values,ability,academic,tendency,practice} */
        private Map<String, Double> scores;
    }
}

