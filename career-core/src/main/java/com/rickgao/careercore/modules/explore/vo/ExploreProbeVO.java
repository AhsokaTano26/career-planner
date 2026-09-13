package com.rickgao.careercore.modules.explore.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 探索深挖 probe 结果：下一道题 + AI 引导语 + 选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExploreProbeVO {

    private String questionId;
    private String questionText;
    /** AI 组织的问题引导语（兜底为模板文案）。 */
    private String intro;
    private List<Option> options;
    /** true=AI 生成引导语；false=模板兜底。 */
    private Boolean aiGenerated;
    /** 题池耗尽时 true（questionId 为空）。 */
    private Boolean exhausted;
    /** 本次提问的落库消息组（assistant 侧，供学生端回看）。 */
    private String messageGroup;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Option {
        private String id;
        private String text;
    }
}
