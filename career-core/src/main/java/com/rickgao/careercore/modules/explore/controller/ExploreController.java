package com.rickgao.careercore.modules.explore.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.explore.dto.ExploreProbeRequest;
import com.rickgao.careercore.modules.explore.dto.ExploreRequest;
import com.rickgao.careercore.modules.explore.service.ExploreProbeService;
import com.rickgao.careercore.modules.explore.service.ExploreService;
import com.rickgao.careercore.modules.explore.vo.ExploreProbeVO;
import com.rickgao.careercore.modules.explore.vo.ExploreResultVO;
import com.rickgao.careercore.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 生涯探索路由（学生端「点选→出结果」聚合入口）。
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "生涯探索")
public class ExploreController {

    private final ExploreService exploreService;
    private final ExploreProbeService probeService;

    public ExploreController(ExploreService exploreService, ExploreProbeService probeService) {
        this.exploreService = exploreService;
        this.probeService = probeService;
    }

    @PostMapping("/students/me/explorations")
    public ApiResponse<ExploreResultVO> run(@RequestBody ExploreRequest req) {
        return ApiResponse.ok(exploreService.run(SecurityUtils.currentUserId(), req));
    }

    @PostMapping("/students/me/explorations/probe")
    public ApiResponse<ExploreProbeVO> probe(@RequestBody ExploreProbeRequest req) {
        return ApiResponse.ok(probeService.nextQuestion(SecurityUtils.currentUserId(), req));
    }
}
