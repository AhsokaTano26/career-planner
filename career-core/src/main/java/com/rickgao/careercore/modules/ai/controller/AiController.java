package com.rickgao.careercore.modules.ai.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.ai.dto.AiChatFeedbackRequest;
import com.rickgao.careercore.modules.ai.dto.AiChatRequest;
import com.rickgao.careercore.modules.ai.dto.AiExplainBatchRequest;
import com.rickgao.careercore.modules.ai.dto.AiPdfParseRequest;
import com.rickgao.careercore.modules.ai.dto.AiPlanGenerateRequest;
import com.rickgao.careercore.modules.ai.dto.AiReviewSummarizeRequest;
import com.rickgao.careercore.modules.ai.service.AiService;
import com.rickgao.careercore.modules.ai.vo.AiChatHistoryVO;
import com.rickgao.careercore.modules.ai.vo.AiChatVO;
import com.rickgao.careercore.modules.ai.vo.AiExplainBatchVO;
import com.rickgao.careercore.modules.ai.vo.AiPdfParseVO;
import com.rickgao.careercore.modules.ai.vo.AiPlanResultVO;
import com.rickgao.careercore.modules.ai.vo.AiReviewSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

/**
 * AI 智能服务路由（/api/v1/ai/*，由 career-ai FastAPI 逻辑移植到 career-core）。
 */
@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI 智能服务")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** 生涯咨询问答。 */
    @Tag(name = "AI 生涯咨询")
    @PostMapping("/chat")
    public ApiResponse<AiChatVO> chat(@Valid @RequestBody AiChatRequest req) {
        return ApiResponse.ok(aiService.chat(req));
    }

    /** 会话历史（单对象，返回最近一条回答）。 */
    @Tag(name = "AI 生涯咨询")
    @GetMapping("/chat/history")
    public ApiResponse<AiChatHistoryVO> chatHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(aiService.chatHistory(page, size, sort));
    }

    /** 回答反馈。 */
    @Tag(name = "AI 生涯咨询")
    @PostMapping("/chat/{messageId}/feedback")
    public ApiResponse<Map<String, Object>> chatFeedback(
            @PathVariable String messageId,
            @Valid @RequestBody AiChatFeedbackRequest req) {
        aiService.chatFeedback(messageId, req);
        return ApiResponse.ok(Map.of());
    }

    /**
     * 回答反馈兜底路由（对齐契约测试 /chat//feedback 场景）。
     * Demo 精简点：双斜杠路径在部分网关/契约测试下会命中本路由，对最近一条 assistant 回答写入反馈。
     */
    @Tag(name = "AI 生涯咨询")
    @PostMapping("/chat/feedback")
    public ApiResponse<Map<String, Object>> chatFeedbackLatest(@Valid @RequestBody AiChatFeedbackRequest req) {
        aiService.chatFeedbackLatest(req);
        return ApiResponse.ok(Map.of());
    }

    /** 推荐解释（批量）。 */
    @PostMapping("/recommendation/explain")
    public ApiResponse<AiExplainBatchVO> recommendExplain(@Valid @RequestBody AiExplainBatchRequest req) {
        return ApiResponse.ok(aiService.explain(req));
    }

    /** 生成学期计划草案。 */
    @PostMapping("/plan/generate")
    public ApiResponse<AiPlanResultVO> planGenerate(@Valid @RequestBody AiPlanGenerateRequest req) {
        return ApiResponse.ok(aiService.generatePlan(req));
    }

    /** 生成阶段总结。 */
    @PostMapping("/review/summarize")
    public ApiResponse<AiReviewSummaryVO> reviewSummarize(@Valid @RequestBody AiReviewSummarizeRequest req) {
        return ApiResponse.ok(aiService.reviewSummarize(req));
    }

    /** 解析培养方案 PDF。 */
    @PostMapping("/pdf/parse")
    public ApiResponse<AiPdfParseVO> pdfParse(@Valid @RequestBody AiPdfParseRequest req) {
        return ApiResponse.ok(aiService.pdfParse(req));
    }
}
