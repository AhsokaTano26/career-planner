package com.rickgao.careercore.modules.admin.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireRequest;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireStatusUpdate;
import com.rickgao.careercore.modules.admin.service.AdminQuestionnaireService;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireDetailVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVersionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-问卷管理接口。openapi:/api/v1/admin/questionnaires。
 */
@RestController
@RequestMapping("/api/v1/admin/questionnaires")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionnaireController {

    private final AdminQuestionnaireService service;

    public AdminQuestionnaireController(AdminQuestionnaireService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<QuestionnaireVO>> listQuestionnaires(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.listQuestionnaires(keyword, page, size));
    }

    @PostMapping
    public ApiResponse<QuestionnaireVO> createQuestionnaire(@Valid @RequestBody QuestionnaireRequest req) {
        return ApiResponse.ok(service.createQuestionnaire(SecurityUtils.currentUserId(), req));
    }

    @PatchMapping("/{questionnaireId}")
    public ApiResponse<QuestionnaireVO> updateQuestionnaire(@PathVariable String questionnaireId,
                                                            @Valid @RequestBody QuestionnaireRequest req) {
        return ApiResponse.ok(service.updateQuestionnaire(SecurityUtils.currentUserId(), questionnaireId, req));
    }

    @PatchMapping("/{questionnaireId}/status")
    public ApiResponse<QuestionnaireVO> setStatus(@PathVariable String questionnaireId,
                                                  @Valid @RequestBody QuestionnaireStatusUpdate req) {
        return ApiResponse.ok(service.setStatus(SecurityUtils.currentUserId(), questionnaireId, req));
    }

    @GetMapping("/{questionnaireId}/versions")
    public ApiResponse<List<QuestionnaireVersionVO>> listVersions(@PathVariable String questionnaireId) {
        return ApiResponse.ok(service.listVersions(questionnaireId));
    }

    @GetMapping("/{questionnaireId}/versions/{versionId}")
    public ApiResponse<QuestionnaireDetailVO> getVersionDetail(@PathVariable String questionnaireId,
                                                               @PathVariable String versionId) {
        return ApiResponse.ok(service.getVersionDetail(questionnaireId, versionId));
    }

    @PostMapping("/{questionnaireId}/versions")
    public ApiResponse<QuestionnaireVersionVO> createVersion(@PathVariable String questionnaireId,
                                                             @Valid @RequestBody QuestionnaireRequest req) {
        return ApiResponse.ok(service.createVersion(SecurityUtils.currentUserId(), questionnaireId, req));
    }

    @PostMapping("/{questionnaireId}/versions/{versionId}/publish")
    public ApiResponse<QuestionnaireVersionVO> publishVersion(@PathVariable String questionnaireId,
                                                              @PathVariable String versionId) {
        return ApiResponse.ok(service.publishVersion(SecurityUtils.currentUserId(), questionnaireId, versionId));
    }
}
