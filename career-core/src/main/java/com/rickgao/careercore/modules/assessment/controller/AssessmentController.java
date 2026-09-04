package com.rickgao.careercore.modules.assessment.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.assessment.dto.CreateSessionRequest;
import com.rickgao.careercore.modules.assessment.dto.SaveAnswersRequest;
import com.rickgao.careercore.modules.assessment.service.AssessmentService;
import com.rickgao.careercore.modules.assessment.vo.AssessmentSessionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireDetailVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVersionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVO;
import com.rickgao.careercore.modules.assessment.vo.ScoreResultVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测评模块路由（问卷 + 测评会话）。
 */
@RestController
@RequestMapping("/api/v1")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    // ---- 测评会话 ----

    @PostMapping("/assessment-sessions")
    public ApiResponse<AssessmentSessionVO> createSession(@Valid @RequestBody CreateSessionRequest req,
                                                          HttpServletRequest http) {
        return ApiResponse.ok(assessmentService.createSession(SecurityUtils.currentUserId(), req));
    }

    @GetMapping("/assessment-sessions")
    public ApiResponse<List<AssessmentSessionVO>> listMySessions() {
        return ApiResponse.ok(assessmentService.listMySessions(SecurityUtils.currentUserId()));
    }

    @GetMapping("/assessment-sessions/{sessionId}")
    public ApiResponse<AssessmentSessionVO> getSession(@PathVariable String sessionId) {
        return ApiResponse.ok(assessmentService.getSession(sessionId, SecurityUtils.currentUserId()));
    }

    @PutMapping("/assessment-sessions/{sessionId}/answers")
    public ApiResponse<Void> saveAnswers(@PathVariable String sessionId,
                                         @Valid @RequestBody SaveAnswersRequest req) {
        assessmentService.saveAnswers(sessionId, req);
        return ApiResponse.ok();
    }

    @PostMapping("/assessment-sessions/{sessionId}/submit")
    public ApiResponse<ScoreResultVO> submit(@PathVariable String sessionId) {
        return ApiResponse.ok(assessmentService.submit(sessionId, SecurityUtils.currentUserId()));
    }

    @GetMapping("/assessment-sessions/{sessionId}/scores")
    public ApiResponse<ScoreResultVO> getScores(@PathVariable String sessionId) {
        return ApiResponse.ok(assessmentService.getScores(sessionId, SecurityUtils.currentUserId()));
    }

    // ---- 问卷 ----

    @GetMapping("/questionnaires")
    public ApiResponse<List<QuestionnaireVO>> listQuestionnaires() {
        return ApiResponse.ok(assessmentService.listQuestionnaires());
    }

    @GetMapping("/questionnaires/{questionnaireId}")
    public ApiResponse<QuestionnaireDetailVO> getQuestionnaire(@PathVariable String questionnaireId) {
        return ApiResponse.ok(assessmentService.getQuestionnaireDetail(questionnaireId));
    }

    @GetMapping("/questionnaires/{questionnaireId}/versions")
    public ApiResponse<List<QuestionnaireVersionVO>> listVersions(@PathVariable String questionnaireId) {
        return ApiResponse.ok(assessmentService.listVersions(questionnaireId));
    }
}

