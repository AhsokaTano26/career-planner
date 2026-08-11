package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 批量校核请求体。对齐 openapi BatchReviewRequest。 */
@Data
public class BatchReviewRequest {

    @NotEmpty(message = "actions 不能为空")
    @Size(max = 100, message = "单次批量最多 100 条")
    @Valid
    private List<BatchReviewAction> actions;

    @Data
    public static class BatchReviewAction {

        private String itemId;

        /** APPROVE / REJECT / MERGE */
        private String action;

        /** MERGE 时的合并目标条目 ID */
        private String targetItemId;

        private List<String> abilityTags;
    }
}
