package com.rickgao.careercore.modules.portrait.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.advisor.vo.ProfileSnapshotVO;
import com.rickgao.careercore.modules.portrait.dto.ProfileFeedbackRequest;
import com.rickgao.careercore.modules.portrait.service.PortraitService;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生画像模块路由。
 */
@RestController
@RequestMapping("/api/v1")
public class PortraitController {

    private final PortraitService portraitService;

    public PortraitController(PortraitService portraitService) {
        this.portraitService = portraitService;
    }

    @GetMapping("/students/me/profile/latest")
    public ApiResponse<ProfileSnapshotVO> getLatest() {
        return ApiResponse.ok(portraitService.getLatest(SecurityUtils.currentUserId()));
    }

    @PostMapping("/students/me/profile/refresh")
    public ApiResponse<ProfileSnapshotVO> refresh() {
        return ApiResponse.ok(portraitService.refresh(SecurityUtils.currentUserId()));
    }

    @GetMapping("/students/me/profile/versions")
    public ApiResponse<List<ProfileSnapshotVO>> listVersions() {
        return ApiResponse.ok(portraitService.listVersions(SecurityUtils.currentUserId()));
    }

    @GetMapping("/profile-snapshots/{snapshotId}")
    public ApiResponse<ProfileSnapshotVO> getSnapshot(@PathVariable String snapshotId) {
        return ApiResponse.ok(portraitService.getSnapshot(snapshotId));
    }

    @PostMapping("/profile-snapshots/{snapshotId}/feedback")
    public ApiResponse<ProfileSnapshotVO> addFeedback(@PathVariable String snapshotId,
                                                      @Valid @RequestBody ProfileFeedbackRequest req) {
        return ApiResponse.ok(portraitService.addFeedback(snapshotId, req));
    }
}
