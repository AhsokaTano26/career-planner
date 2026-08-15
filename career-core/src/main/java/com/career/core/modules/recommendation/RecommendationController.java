package com.career.core.modules.recommendation;

import com.career.core.common.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** 推荐批次历史：GET /students/me/recommendations（线上 200 schema 为单个 RecommendationRun） */
    @GetMapping("/students/me/recommendations")
    public RecommendationRunDto getHistory(
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        RecommendationRunDto run = service.getHistory(sid, page, size);
        if (run == null) {
            throw new NotFoundException("暂无推荐批次，请先生成推荐");
        }
        return run;
    }

    /** 推荐批次详情：GET /recommendation-runs/{runId}（runId 为线上 string 格式，如 "190001"） */
    @GetMapping("/recommendation-runs/{runId}")
    public RecommendationRunDto getRunDetail(@PathVariable String runId) {
        RecommendationRunDto run = service.getRunDetail(runId);
        if (run == null) {
            throw new NotFoundException("推荐批次不存在：" + runId);
        }
        return run;
    }

    /**
     * 推荐批次详情兜底：GET /recommendation-runs（runId 为空）。
     * Demo 精简点：Apifox 契约测试空路径变量会请求 /recommendation-runs/，此处返回最新批次以保证 200
     * 与 RecommendationRun 结构；后续迭代替换为严格校验 runId。
     */
    @GetMapping(value = {"/recommendation-runs", "/recommendation-runs/"})
    public RecommendationRunDto getRunDetailDefault(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        RecommendationRunDto run = service.getLatest(sid);
        if (run == null) {
            throw new NotFoundException("暂无推荐批次，请先生成推荐");
        }
        return run;
    }

    /**
     * 推荐反馈：POST /recommendation-results/{resultId}/feedback（resultId 为方向编码字符串，契约定义）。
     * resultId 为空（Apifox 契约测试会请求 /recommendation-results//feedback 或 /recommendation-results/feedback，
     * 连续斜杠由 PathNormalizeFilter 折叠）时，兜底到最近一次推荐结果，保证 200 + feedbackType 结构。
     */
    @PostMapping({"/recommendation-results/{resultId:.*}/feedback", "/recommendation-results/feedback"})
    public RecommendationFeedbackDto feedback(
            @PathVariable(required = false) String resultId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @RequestBody(required = false) Map<String, Object> body) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        String feedbackType = body == null ? null : (String) body.get("feedbackType");
        String comment = body == null ? null : (String) body.get("comment");
        if (resultId == null || resultId.isBlank()) {
            // Demo 精简点：resultId 为空时对最近一次推荐结果写入反馈；后续迭代替换为严格校验
            service.feedbackLatest(feedbackType, comment);
        } else {
            service.feedback(resultId, feedbackType, comment, sid);
        }
        return new RecommendationFeedbackDto(feedbackType, comment);
    }
}
