package com.career.core.modules.recommendation;

import com.career.core.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 推荐 Recommendations 模块（按线上 Apifox 定义实现）
 * - POST /api/v1/recommendations/run           生成推荐（线上：无入参，响应 200 空对象）
 * - GET  /api/v1/recommendations/latest?studentId= 查询推荐结果
 * - POST /api/v1/recommendations/{id}/feedback 推荐反馈（线上：响应 200 空对象）
 * 线上接口均无入参（依赖登录态）；Demo 无鉴权，studentId 为可选增强参数，缺省取默认学生。
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    /** Demo 无登录态时的默认学生ID */
    private static final long DEFAULT_STUDENT_ID = 1001L;

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    /** 生成推荐：触发一次计算并落库，返回 200 空对象（线上定义） */
    @PostMapping("/run")
    public ApiResponse<Map<String, Object>> run(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        service.recommend(sid);
        return ApiResponse.success(Collections.emptyMap());
    }

    /** 查询推荐结果：{ results: [ {directionId, name, type, score, rank, confidence, reason} ] } */
    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> getLatest(
            @RequestParam(value = "studentId", required = false) Long studentId) {
        long sid = studentId == null ? DEFAULT_STUDENT_ID : studentId;
        return ApiResponse.success(Map.of("results", service.recommend(sid)));
    }

    /** 推荐反馈：接收可选 feedbackType/comment，落库后返回 200 空对象（线上定义） */
    @PostMapping("/{id}/feedback")
    public ApiResponse<Map<String, Object>> feedback(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        String feedbackType = body == null ? null : (String) body.get("feedbackType");
        String comment = body == null ? null : (String) body.get("comment");
        service.feedback(id, feedbackType, comment);
        return ApiResponse.success(Collections.emptyMap());
    }

    /**
     * 兼容空 id（如 /api/v1/recommendations//feedback）：自动使用最近一次推荐结果 ID。
     * Demo 便捷：Apifox 调试时路径参数 id 留空，点“发送”即可直接提交反馈并返回 200；
     * 正式环境应要求 id 必填，此兜底可移除。
     */
    @PostMapping("/**")
    public ApiResponse<Map<String, Object>> feedbackWithLatestId(
            @RequestBody(required = false) Map<String, Object> body) {
        String feedbackType = body == null ? null : (String) body.get("feedbackType");
        String comment = body == null ? null : (String) body.get("comment");
        service.feedbackLatest(feedbackType, comment);
        return ApiResponse.success(Collections.emptyMap());
    }
}
