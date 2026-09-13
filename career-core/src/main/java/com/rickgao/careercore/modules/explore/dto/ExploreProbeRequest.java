package com.rickgao.careercore.modules.explore.dto;

import lombok.Data;

import java.util.List;

/**
 * 探索深挖 probe 请求：前端携带已答摘要，后端返回下一道未答题。
 */
@Data
public class ExploreProbeRequest {

    /** 已选路径（graduate/employment/overseas/undecided），供 AI 组织引导语。 */
    private String path;
    /** 已选兴趣标签题 ID（含本次探索内已答的 TAG/PROBE）。 */
    private List<String> answeredQuestionIds;
    /** 已追问次数（前端计数，后端按 PROBE_LIMIT 校验）。 */
    private Integer askedCount;
}
