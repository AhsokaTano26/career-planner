package com.rickgao.careercore.modules.recommendation.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.recommendation.dto.CreateRecommendationRequest;
import com.rickgao.careercore.modules.recommendation.dto.RecommendationFeedbackRequest;
import com.rickgao.careercore.modules.recommendation.service.RecommendationService;
import com.rickgao.careercore.modules.recommendation.vo.RecResultVO;
import com.rickgao.careercore.modules.recommendation.vo.RecRunVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 方向推荐模块路由。
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "方向推荐")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/students/me/recommendations/runs")
    public ApiResponse<RecRunVO> createRun(@Valid @RequestBody CreateRecommendationRequest req) {
        return ApiResponse.ok(recommendationService.createRun(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/students/me/recommendations/latest")
    public ApiResponse<RecRunVO> getLatest() {
        return ApiResponse.ok(recommendationService.getLatest(SecurityUtils.currentUserId()));
    }

    @GetMapping("/students/me/recommendations")
    public ApiResponse<PageResult<RecRunVO>> listRuns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<RecRunVO> list = recommendationService.listRuns(SecurityUtils.currentUserId(), page, size);
        long total = recommendationService.countRuns(SecurityUtils.currentUserId());
        return ApiResponse.ok(PageResult.of(list, total, page, size));
    }

    @GetMapping("/recommendation-runs/{runId}")
    public ApiResponse<RecRunVO> getRunDetail(@PathVariable String runId) {
        return ApiResponse.ok(recommendationService.getRunDetail(runId));
    }

    @PostMapping("/recommendation-results/{resultId}/feedback")
    public ApiResponse<RecResultVO> addFeedback(@PathVariable String resultId,
                                                           @Valid @RequestBody RecommendationFeedbackRequest req) {
        return ApiResponse.ok(recommendationService.addFeedback(resultId, req));
    }
}

