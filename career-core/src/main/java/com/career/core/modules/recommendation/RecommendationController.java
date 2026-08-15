package com.career.core.modules.recommendation;

import com.career.core.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 方向推荐模块（按线上 Apifox 定义，5 个接口）。
 * <p>
 * 契约对齐说明（2026-08-15）：线上 200 响应 schema 为裸业务对象（RecommendationRun 等，
 * 无 {code,message,data} 包装），Apifox 契约测试按整个响应体校验；
 * 故本模块成功响应直接返回业务对象（错误响应仍由 GlobalExceptionHandler 统一返回 {code,message,data}）。
 * 线上接口依赖登录态确定当前学生；Demo 无鉴权，统一以可选 studentId 增强参数缺省 1001。
 */
@RestController
@RequestMapping("/api/v1")
public class RecommendationController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    /** 创建推荐批次：POST /students/me/recommendations/runs */
    @PostMapping("/students/me/recommendations/runs")
    public RecommendationRunDto createRun(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String pathFilter = body == null ? null : (String) body.get("pathFilter");
        return service.run(sid, pathFilter);
    }

    /** 最新推荐结果：GET /students/me/recommendations/latest */
    @GetMapping("/students/me/recommendations/latest")
    public RecommendationRunDto getLatest(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        RecommendationRunDto run = service.getLatest(sid);
        if (run == null) {
            throw new NotFoundException("暂无推荐批次，请先生成推荐");
        }
        return run;
    }

    /** 推荐批次历史：GET /students/me/recommendations */
    @GetMapping("/students/me/recommendations")
    public List<RecommendationRunDto> getHistory(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return service.getHistory(sid, page, size);
    }

    /** 推荐批次详情：GET /recommendation-runs/{runId} */
    @GetMapping("/recommendation-runs/{runId}")
    public RecommendationRunDto getRunDetail(@PathVariable String runId) {
        RecommendationRunDto run = service.getRunDetail(runId);
        if (run == null) {
            throw new NotFoundException("推荐批次不存在：" + runId);
        }
        return run;
    }

    /** 推荐反馈：POST /recommendation-results/{resultId}/feedback（resultId 为方向编码字符串，契约定义） */
    @PostMapping("/recommendation-results/{resultId}/feedback")
    public RecommendationFeedbackDto feedback(
            @PathVariable String resultId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String feedbackType = body == null ? null : (String) body.get("feedbackType");
        String comment = body == null ? null : (String) body.get("comment");
        service.feedback(resultId, feedbackType, comment, sid);
        return new RecommendationFeedbackDto(feedbackType, comment);
    }
}
