package com.rickgao.careercore.modules.ai.dto;

import lombok.Data;

/**
 * 推荐解释请求（POST /api/v1/ai/recommendation/explain）。
 */
@Data
public class AiExplainBatchRequest {

    private String studentRef;

    private String ruleVersion;

    private Integer profileVersion;

    private AiExplainProfile profile;

    /** 候选方向列表。 */
    private java.util.List<AiExplainResultItem> results;

    /** Demo 扩展：允许调用方回传批次 ID，缺省服务端生成。 */
    private String runId;
}
