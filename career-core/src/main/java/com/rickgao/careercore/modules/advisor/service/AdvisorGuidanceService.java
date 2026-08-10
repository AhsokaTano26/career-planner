package com.rickgao.careercore.modules.advisor.service;

import com.rickgao.careercore.modules.advisor.dto.GuidanceCommentRequest;
import com.rickgao.careercore.modules.advisor.vo.GuidanceCommentVO;

import java.util.List;

/**
 * 辅导员指导意见 / 建议应用服务。
 */
public interface AdvisorGuidanceService {

    /** 某学生的历史指导意见(时间正序) */
    List<GuidanceCommentVO> listGuidance(String advisorId, String studentId);

    /** 填写指导意见 / 建议任务 / 建议重新测评(幂等写入) */
    GuidanceCommentVO writeGuidance(String advisorId, String studentId, String endpoint, String idempotencyKey,
                                    GuidanceCommentRequest request);
}
