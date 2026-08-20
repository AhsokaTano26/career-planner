package com.rickgao.careercore.modules.advisor.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.advisor.service.AdvisorStudentService;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStatisticsVO;
import com.rickgao.careercore.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 辅导员端-群体统计接口。
 * openapi:GET /api/v1/advisor/statistics。
 */
@RestController
@RequestMapping("/api/v1/advisor")
@PreAuthorize("hasRole('ADVISOR')")
public class AdvisorStatisticsController {

    private final AdvisorStudentService advisorStudentService;

    public AdvisorStatisticsController(AdvisorStudentService advisorStudentService) {
        this.advisorStudentService = advisorStudentService;
    }

    /** 群体统计(路径分布/测评完成率/计划制定率/任务完成率) */
    @GetMapping("/statistics")
    public ApiResponse<AdvisorStatisticsVO> getStatistics() {
        return ApiResponse.ok(advisorStudentService.getStatistics(SecurityUtils.currentUserId()));
    }
}
