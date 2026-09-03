package com.rickgao.careercore.modules.planning.dto;

import com.rickgao.careercore.modules.advisor.vo.ReviewVO;
import lombok.Data;

/** 创建 / 保存复盘草稿请求。 */
@Data
public class ReviewDraftRequest {

    private String cycle;
    private ReviewVO.ReviewContent content;
}
