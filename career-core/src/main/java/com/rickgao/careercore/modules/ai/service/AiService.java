package com.rickgao.careercore.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.ai.dto.AiChatContext;
import com.rickgao.careercore.modules.ai.dto.AiChatFeedbackRequest;
import com.rickgao.careercore.modules.ai.dto.AiChatRequest;
import com.rickgao.careercore.modules.ai.dto.AiExplainBatchRequest;
import com.rickgao.careercore.modules.ai.dto.AiExplainResultItem;
import com.rickgao.careercore.modules.ai.dto.AiPdfParseRequest;
import com.rickgao.careercore.modules.ai.dto.AiPlanGenerateRequest;
import com.rickgao.careercore.modules.ai.dto.AiReviewSummarizeRequest;
import com.rickgao.careercore.modules.ai.entity.AiChatFeedback;
import com.rickgao.careercore.modules.ai.entity.AiChatMessage;
import com.rickgao.careercore.modules.ai.mapper.AiChatFeedbackMapper;
import com.rickgao.careercore.modules.ai.mapper.AiChatMessageMapper;
import com.rickgao.careercore.modules.ai.vo.AiChatHistoryVO;
import com.rickgao.careercore.modules.ai.vo.AiChatVO;
import com.rickgao.careercore.modules.ai.vo.AiExplainBatchVO;
import com.rickgao.careercore.modules.ai.vo.AiExplanationItemVO;
import com.rickgao.careercore.modules.ai.vo.AiMonthlyTaskVO;
import com.rickgao.careercore.modules.ai.vo.AiPdfParseVO;
import com.rickgao.careercore.modules.ai.vo.AiPlanResultVO;
import com.rickgao.careercore.modules.ai.vo.AiReviewSummaryVO;
import com.rickgao.careercore.modules.ai.vo.AiSemesterGoalVO;
import com.rickgao.careercore.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能服务：封装生涯咨询 / 推荐解释 / 计划生成 / 复盘总结 / PDF 解析。
 *
 * <p>由 career-ai（FastAPI）逻辑移植到 career-core，直接调用大模型网关。
 * 2026-09 Phase 1：chat 历史与反馈落 MySQL（ai_chat_message / ai_chat_feedback），
 * 身份以 JWT 优先，请求体 studentRef 仅作兼容兑底（与 JWT 不一致则报错）。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - chat 历史与反馈已落库（取代原 ConcurrentHashMap / CopyOnWriteArrayList）；
 *  - 转人工检测用关键词匹配；
 *  - 大模型不可用（未配置/超时/502/输出非法）时本类直接返回确定性模板内容
 *    （FALLBACK，主业务流程不失败；ai_call_log 仍由 LlmGateway 记录 FAILED）。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String DISCLAIMER = "智能生成，供探索参考";

    /** 需转人工/专业机构的关键词（Demo 精简点，后续接专业意图识别）。 */
    private static final String[] HUMAN_KEYWORDS = {"自杀", "自残", "抑郁", "焦虑", "心理疾病", "法律", "医疗", "诊断"};

    private static final List<String> FEEDBACK_TYPES = List.of("HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED");

    private final LlmGateway llm;
    private final ObjectMapper objectMapper;
    private final AiChatMessageMapper chatMessageMapper;
    private final AiChatFeedbackMapper chatFeedbackMapper;
    private final IdGenerator idGenerator;
    private final Desensitizer desensitizer;

    public AiService(LlmGateway llm, ObjectMapper objectMapper,
                     AiChatMessageMapper chatMessageMapper, AiChatFeedbackMapper chatFeedbackMapper,
                     IdGenerator idGenerator,
                     Desensitizer desensitizer) {
        this.llm = llm;
        this.objectMapper = objectMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatFeedbackMapper = chatFeedbackMapper;
        this.idGenerator = idGenerator;
        this.desensitizer = desensitizer;
    }

    // ---------------------------------------------------------------- chat

    // 注意：此处刻意不用 @Transactional——先调远程 LLM（数十秒），再单语句落库；
    // 若包事务会长期占用 DB 连接导致池耗尽（2026-09 稳定性）。insertBatch 单语句原子性足够。
    public AiChatVO chat(AiChatRequest req) {
        String userId = resolveUserId(req.getStudentRef());
        String messageGroup = java.util.UUID.randomUUID().toString().replace("-", "");
        // 脱敏：先对学生输入做掩码，避免手机号/身份证/学号原样送给大模型
        String sanitizedQuestion = desensitizer.maskFreeText(req.getQuestion());
        boolean needsHuman = detectHumanSupport(req.getQuestion());
        String answer;
        if (needsHuman) {
            answer = "该问题可能涉及心理健康、医疗或法律等专业领域，建议联系辅导员或专业机构获取帮助。";
        } else {
            try {
                answer = llm.generate(buildChatMessages(sanitizedQuestion, req.getContext()), 0.7,
                        llm.sceneMaxTokens("career_chat"),
                        "career_chat", userId, null, messageGroup);
            } catch (BizException exc) {
                // Demo 精简点：网关不可用时回退确定性答复（主流程不失败）
                log.warn("生涯咨询大模型不可用，回退模板答复: {}", exc.getMessage());
                answer = "AI 服务暂时不可用，你可以先对照推荐方向完成一门基础课与一个小项目，稍后再来提问（" + DISCLAIMER + "）。";
            }
        }
        String supportReason = needsHuman ? "涉及心理健康/医疗/法律等话题，建议转人工或专业机构" : "";

        // 持久化：保留脱敏后的问题（与 LLM 看到的一致）
        List<AiChatMessage> rows = new ArrayList<>(2);
        rows.add(buildRow(userId, messageGroup, req.getSessionId(), "user", sanitizedQuestion, false, ""));
        rows.add(buildRow(userId, messageGroup, req.getSessionId(), "assistant", answer, needsHuman, supportReason));
        chatMessageMapper.insertBatch(rows);

        return AiChatVO.builder()
                .messageId(messageGroup)
                .answer(answer)
                .references(List.of())
                .needsHumanSupport(needsHuman)
                .supportReason(supportReason)
                .disclaimer(DISCLAIMER)
                .build();
    }

    /**
     * 会话历史（分页、跨会话合并、按时间倒序）。
     * 入参 sort 保留兼容（当前固定按 created_at DESC；非空时忽略）。
     */
    public AiChatHistoryVO chatHistory(int page, int size, String sort) {
        String userId = SecurityUtils.currentUserId();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        long total = chatMessageMapper.countByUserId(userId);
        List<AiChatMessage> rows = chatMessageMapper.findByUserId(userId, (safePage - 1) * safeSize, safeSize);
        List<AiChatHistoryVO.Message> messages = rows.stream().map(this::toHistoryMessage).toList();
        return AiChatHistoryVO.builder()
                .list(messages)
                .page(safePage)
                .size(safeSize)
                .total(total)
                .build();
    }

    @Transactional
    public void chatFeedback(String messageGroup, AiChatFeedbackRequest req) {
        String userId = SecurityUtils.currentUserId();
        validateFeedbackType(req.getFeedbackType());
        if (chatMessageMapper.existsByMessageGroupAndUserId(messageGroup, userId) <= 0) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "消息不存在");
        }
        AiChatFeedback fb = new AiChatFeedback();
        fb.setId(newAiId("AIF-"));
        fb.setMessageGroup(messageGroup);
        fb.setUserId(userId);
        fb.setFeedbackType(req.getFeedbackType());
        fb.setComment(req.getComment());
        chatFeedbackMapper.upsert(fb);
    }

    /**
     * 兜底反馈：未指定 messageGroup 时对当前用户最新一条 assistant 写入反馈。
     */
    @Transactional
    public void chatFeedbackLatest(AiChatFeedbackRequest req) {
        validateFeedbackType(req.getFeedbackType());
        String userId = SecurityUtils.currentUserId();
        AiChatMessage latest = chatMessageMapper.findLatestAssistant(userId);
        if (latest == null) {
            return;
        }
        AiChatFeedback fb = new AiChatFeedback();
        fb.setId(newAiId("AIF-"));
        fb.setMessageGroup(latest.getMessageGroup());
        fb.setUserId(userId);
        fb.setFeedbackType(req.getFeedbackType());
        fb.setComment(req.getComment());
        chatFeedbackMapper.upsert(fb);
    }

    // ---------------------------------------------------------------- 推荐解释

    public AiExplainBatchVO explain(AiExplainBatchRequest req) {
        String runId = (req.getRunId() == null || req.getRunId().isBlank())
                ? "R-" + java.util.UUID.randomUUID().toString().substring(0, 8) : req.getRunId();
        List<Map<String, String>> messages = List.of(
                mapOf("role", "system", "content", EXPLAIN_SYSTEM_PROMPT),
                mapOf("role", "user", "content", desensitizer.maskFreeText(buildExplainPrompt(req))));
        List<AiExplanationItemVO> vos = new ArrayList<>();
        try {
            String content = llm.generate(messages, 0.7, llm.sceneMaxTokens("recommendation_explain"),
                    "recommendation_explain", runId, null, runId);
            List<Map<String, String>> items = parseExplainJson(content);
            for (Map<String, String> item : items) {
                vos.add(AiExplanationItemVO.builder()
                        .directionId(item.get("directionId"))
                        .summary(item.get("summary"))
                        .confidenceText(item.getOrDefault("confidenceText", "数据基本完整，供参考"))
                        .disclaimer(item.getOrDefault("disclaimer", DISCLAIMER))
                        .build());
            }
        } catch (BizException exc) {
            // Demo 精简点：网关不可用/输出非法时按规则分回退模板解释
            log.warn("推荐解释大模型不可用，回退规则模板: {}", exc.getMessage());
            vos = fallbackExplanations(req);
        }
        return AiExplainBatchVO.builder().runId(runId).explanations(vos).build();
    }

    /**
     * 推荐解释回退：按规则评分生成确定性解释（只引用候选项，不下结论）。
     * Demo 精简点 / 后续迭代替换位置：话术固定，后续可按画像维度细化。
     */
    private List<AiExplanationItemVO> fallbackExplanations(AiExplainBatchRequest req) {
        List<AiExplanationItemVO> out = new ArrayList<>();
        if (req.getResults() == null) {
            return out;
        }
        for (AiExplainResultItem r : req.getResults()) {
            double score = r.getScore() == null ? 0 : r.getScore();
            String level = score >= 75 ? "匹配度较高" : score >= 55 ? "匹配度中等" : "匹配度一般";
            out.add(AiExplanationItemVO.builder()
                    .directionId(r.getDirectionId())
                    .summary("方向" + r.getDirectionId() + "规则评分"
                            + Math.round(score) + "分（排名第" + r.getRank() + "），" + level
                            + "，建议结合兴趣与已修课程进一步探索（模板生成，" + DISCLAIMER + "）。")
                    .confidenceText(score >= 75 ? "数据较完整，供参考"
                            : score >= 55 ? "数据基本完整，供参考" : "数据较少，仅供初步参考")
                    .disclaimer(DISCLAIMER)
                    .build());
        }
        return out;
    }

    // ---------------------------------------------------------------- 计划生成

    // 注意：同 chat，不包事务（本方法无写库，仅组装 LLM 输入输出）。
    public AiPlanResultVO generatePlan(AiPlanGenerateRequest req) {
        String userId = resolveUserId(req.getStudentRef());
        String goal = (req.getGoalSummary() != null && !req.getGoalSummary().isBlank())
                ? req.getGoalSummary()
                : (req.getTemplate() != null && req.getTemplate().getGoalSummary() != null
                    ? req.getTemplate().getGoalSummary() : "围绕目标方向打好基础，完成一个小项目");
        String templateJson = req.getTemplate() == null ? "{}" : writeJson(req.getTemplate());
        String userPrompt = "方向编码：" + nvl(req.getDirectionId())
                + "\n学期：" + nvl(req.getSemester())
                + "\n目标摘要：" + goal
                + "\n参考模板：" + templateJson
                + "\n请生成计划草案 JSON。";
        String reqId = "plan-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        AiPlanResultVO result;
        try {
            String content = llm.generate(List.of(
                    mapOf("role", "system", "content", PLAN_SYSTEM_PROMPT),
                    mapOf("role", "user", "content", desensitizer.maskFreeText(userPrompt))), 0.7,
                    llm.sceneMaxTokens("plan_generate"),
                    "plan_generate", req.getDirectionId(), null, reqId);
            JsonNode node = parseJson(content);
            result = AiPlanResultVO.builder()
                    .goalSummary(textOr(node, "goalSummary", "围绕目标方向完成一学期学习与一个小项目"))
                    .semesterGoals(parseSemesterGoals(node.path("semesterGoals")))
                    .monthlyTasks(parseMonthlyTasks(node.path("monthlyTasks")))
                    .notes(textList(node.path("notes")))
                    .build();
        } catch (BizException exc) {
            // Demo 精简点：网关不可用/输出非法时回退模板计划（有模板用模板，否则默认）
            log.warn("计划生成大模型不可用，回退模板计划: {}", exc.getMessage());
            result = fallbackPlanResult(req, goal);
        }

        return result;
    }

    /**
     * 计划生成回退：优先采用请求自带模板，否则返回默认四段式学期计划。
     * Demo 精简点 / 后续迭代替换位置：默认任务固定，后续可按方向任务模板库生成。
     */
    private AiPlanResultVO fallbackPlanResult(AiPlanGenerateRequest req, String goal) {
        if (req.getTemplate() != null) {
            List<AiSemesterGoalVO> goals = new ArrayList<>();
            if (req.getTemplate().getSemesterGoals() != null) {
                for (var g : req.getTemplate().getSemesterGoals()) {
                    goals.add(AiSemesterGoalVO.builder()
                            .title(g.getTitle()).abilityTag(g.getAbilityTag()).build());
                }
            }
            List<AiMonthlyTaskVO> tasks = new ArrayList<>();
            if (req.getTemplate().getMonthlyTasks() != null) {
                for (var t : req.getTemplate().getMonthlyTasks()) {
                    tasks.add(AiMonthlyTaskVO.builder()
                            .month(t.getMonth()).title(t.getTitle())
                            .taskType(t.getTaskType()).estimatedHours(t.getEstimatedHours()).build());
                }
            }
            return AiPlanResultVO.builder()
                    .goalSummary(req.getTemplate().getGoalSummary() != null
                            ? req.getTemplate().getGoalSummary() : goal)
                    .semesterGoals(goals)
                    .monthlyTasks(tasks)
                    .notes(List.of("模板生成，" + DISCLAIMER))
                    .build();
        }
        String[] titles = {"完成基础课程学习", "参与一个项目实践", "参加行业讲座", "整理学习心得与复盘"};
        String[] types = {"LEARNING", "PRACTICE", "CAREER", "REVIEW"};
        java.time.LocalDate base = java.time.LocalDate.now().withDayOfMonth(1);
        List<AiMonthlyTaskVO> tasks = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            tasks.add(AiMonthlyTaskVO.builder()
                    .month(base.plusMonths(i).toString().substring(0, 7))
                    .title(titles[i]).taskType(types[i]).estimatedHours(20.0).build());
        }
        return AiPlanResultVO.builder()
                .goalSummary(goal)
                .semesterGoals(List.of(AiSemesterGoalVO.builder().title("打好方向基础").build()))
                .monthlyTasks(tasks)
                .notes(List.of("模板生成，" + DISCLAIMER))
                .build();
    }

    // ---------------------------------------------------------------- 复盘总结

    // 注意：同 chat，不包事务（本方法无写库）。
    public AiReviewSummaryVO reviewSummarize(AiReviewSummarizeRequest req) {
        String userId = resolveUserId(req.getStudentRef());
        String cycle = req.getCycle();
        String userPrompt = desensitizer.maskFreeText(buildReviewPrompt(req));
        String reqId = "review-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        AiReviewSummaryVO result;
        try {
            String content = llm.generate(List.of(
                    mapOf("role", "system", "content", REVIEW_SYSTEM_PROMPT),
                    mapOf("role", "user", "content", userPrompt)), 0.5,
                    llm.sceneMaxTokens("review_summarize"),
                    "review_summarize", cycle, null, reqId);
            try {
                JsonNode node = parseJson(content);
                String summary = textOr(node, "summary", content);
                List<String> suggestions = textList(node.path("suggestions"));
                result = AiReviewSummaryVO.builder().summary(summary).suggestions(suggestions).build();
            } catch (BizException exc) {
                // Demo 精简点：复盘总结输出非 JSON 时回退为原文本（对齐 career-ai summarize）
                result = AiReviewSummaryVO.builder().summary(content).suggestions(List.of()).build();
            }
        } catch (BizException exc) {
            // Demo 精简点：网关不可用时回退固定总结（主流程不失败）
            log.warn("复盘总结大模型不可用，回退固定总结: {}", exc.getMessage());
            String done = req.getReviewContent() != null && req.getReviewContent().getDone() != null
                    ? req.getReviewContent().getDone() : "按计划推进中";
            result = AiReviewSummaryVO.builder()
                    .summary("本期已完成：" + done + "。AI 服务暂时不可用，以下建议为模板生成（" + DISCLAIMER + "）。")
                    .suggestions(List.of("对照计划检查未完成任务并更新状态", "下期聚焦一个可验证的小目标", "带着问题去请教辅导员或学长"))
                    .build();
        }

        return result;
    }

    // ---------------------------------------------------------------- PDF 解析

    public AiPdfParseVO pdfParse(AiPdfParseRequest req) {
        byte[] data = fetch(req.getFileUrl());
        if (data == null || data.length == 0) {
            return AiPdfParseVO.builder().jobId(req.getJobId()).status("FAILED").build();
        }
        String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        if (text.isBlank()) {
            return AiPdfParseVO.builder().jobId(req.getJobId()).status("REVIEW_REQUIRED")
                    .itemCount(0).confidence(0.0).build();
        }
        List<String> courses = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (!line.trim().isEmpty()) {
                courses.add(line.trim());
                if (courses.size() >= 200) {
                    break;
                }
            }
        }
        double confidence = Math.round(Math.min(99.0, 50.0 + Math.min(courses.size(), 50)) * 10.0) / 10.0;
        return AiPdfParseVO.builder().jobId(req.getJobId()).status("PARSING")
                .itemCount(courses.size()).confidence(confidence).build();
    }

    // ---------------------------------------------------------------- 内部工具

    /** JWT 优先；请求体 studentRef 为空则用 JWT；非空且与 JWT 不一致则报错。 */
    private String resolveUserId(String studentRef) {
        String jwtUserId = SecurityUtils.currentUserId();
        if (!StringUtils.hasText(studentRef)) {
            return jwtUserId;
        }
        if (!studentRef.equals(jwtUserId)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "请求体 studentRef 与当前登录用户不一致，已拒绝");
        }
        return jwtUserId;
    }

    private AiChatMessage buildRow(String userId, String group, String sessionId, String role, String content,
                                   boolean needsHuman, String supportReason) {
        AiChatMessage row = new AiChatMessage();
        row.setId(newAiId("AIM-"));
        row.setSessionId(sessionId);
        row.setUserId(userId);
        row.setRole(role);
        row.setContent(content);
        row.setNeedsHumanSupport(needsHuman);
        row.setSupportReason(supportReason);
        row.setMessageGroup(group);
        return row;
    }

    private AiChatHistoryVO.Message toHistoryMessage(AiChatMessage row) {
        return AiChatHistoryVO.Message.builder()
                .messageId(row.getMessageGroup())
                .sessionId(row.getSessionId())
                .role(row.getRole())
                .content(row.getContent())
                .needsHumanSupport(Boolean.TRUE.equals(row.getNeedsHumanSupport()))
                .supportReason(row.getSupportReason())
                .createdAt(row.getCreatedAt())
                .build();
    }

    private String newAiId(String prefix) {
        return prefix + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 28);
    }

    private List<Map<String, String>> parseExplainJson(String content) {
        JsonNode node = parseJson(content);
        JsonNode arr = node.path("explanations");
        if (!arr.isArray() || arr.isEmpty()) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "推荐解释输出缺少 explanations");
        }
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode item : arr) {
            String directionId = item.path("directionId").asText("");
            String summary = item.path("summary").asText("");
            if (directionId.isBlank() || summary.isBlank()) {
                throw new BizException(ResultCode.INTERNAL_ERROR, "推荐解释条目缺少 directionId/summary");
            }
            Map<String, String> m = new LinkedHashMap<>();
            m.put("directionId", directionId);
            m.put("summary", summary);
            m.put("confidenceText", item.path("confidenceText").asText("数据基本完整，供参考"));
            m.put("disclaimer", item.path("disclaimer").asText(DISCLAIMER));
            out.add(m);
        }
        return out;
    }

    private List<AiSemesterGoalVO> parseSemesterGoals(JsonNode arr) {
        List<AiSemesterGoalVO> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode g : arr) {
                out.add(AiSemesterGoalVO.builder()
                        .title(textOr(g, "title", "未命名目标"))
                        .abilityTag(g.path("abilityTag").isNull() ? null : g.path("abilityTag").asText())
                        .build());
            }
        }
        return out;
    }

    private List<AiMonthlyTaskVO> parseMonthlyTasks(JsonNode arr) {
        List<AiMonthlyTaskVO> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode t : arr) {
                out.add(AiMonthlyTaskVO.builder()
                        .month(textOr(t, "month", "2026-09"))
                        .title(textOr(t, "title", "学习任务"))
                        .taskType(t.path("taskType").isNull() ? null : t.path("taskType").asText())
                        .estimatedHours(t.path("estimatedHours").isNull() ? null : t.path("estimatedHours").asDouble())
                        .build());
            }
        }
        return out;
    }

    private JsonNode parseJson(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start < 0 || end <= start) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "大模型输出不含合法 JSON");
        }
        try {
            return objectMapper.readTree(text.substring(start, end + 1));
        } catch (Exception exc) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "大模型输出 JSON 解析失败：" + exc.getMessage());
        }
    }

    private boolean detectHumanSupport(String text) {
        if (text == null) {
            return false;
        }
        for (String kw : HUMAN_KEYWORDS) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private void validateFeedbackType(String type) {
        if (type == null || !FEEDBACK_TYPES.contains(type)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "feedbackType 不合法：" + type);
        }
    }

    private byte[] fetch(String url) {
        // 安全：仅 http/https，禁止内网/回环地址（SSRF 防护；行为与 career-ai pdf_parser 对齐）
        if (!isFetchUrlAllowed(url)) {
            log.warn("文件拉取拒绝（地址不合法，已脱敏）");
            return null;
        }
        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(10000);
            var client = org.springframework.web.client.RestClient.builder()
                    .requestFactory(factory).build();
            byte[] data = client.get().uri(URI.create(url)).retrieve().body(byte[].class);
            if (data != null && data.length > 20 * 1024 * 1024) {
                log.warn("文件拉取拒绝（超 20MB）");
                return null;
            }
            return data;
        } catch (Exception exc) {
            log.warn("文件拉取失败：{}", exc.getMessage());
            return null;
        }
    }

    private boolean isFetchUrlAllowed(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }
            for (java.net.InetAddress addr : java.net.InetAddress.getAllByName(host)) {
                if (addr.isSiteLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                        || addr.isMulticastAddress() || addr.isAnyLocalAddress()) {
                    return false;
                }
            }
            return true;
        } catch (Exception exc) {
            return false;
        }
    }

    private List<Map<String, String>> buildChatMessages(String sanitizedQuestion, AiChatContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(mapOf("role", "system", "content", CHAT_SYSTEM_PROMPT));
        String question = sanitizedQuestion;
        if (context != null) {
            List<String> parts = new ArrayList<>();
            parts.add(question);
            if (context.getDirectionId() != null && !context.getDirectionId().isBlank()) {
                parts.add("当前关注方向：" + context.getDirectionId());
            }
            if (context.getGoalSummary() != null && !context.getGoalSummary().isBlank()) {
                parts.add("当前目标摘要：" + context.getGoalSummary());
            }
            question = String.join("\n", parts);
        }
        messages.add(mapOf("role", "user", "content", question));
        return messages;
    }

    private String buildExplainPrompt(AiExplainBatchRequest req) {
        List<String> lines = new ArrayList<>();
        if (req.getProfile() != null) {
            List<String> dims = new ArrayList<>();
            var p = req.getProfile();
            if (p.getInterest() != null) dims.add("interest=" + Math.round(p.getInterest() * 100) + "%");
            if (p.getValues() != null) dims.add("values=" + Math.round(p.getValues() * 100) + "%");
            if (p.getAbility() != null) dims.add("ability=" + Math.round(p.getAbility() * 100) + "%");
            if (p.getAcademic() != null) dims.add("academic=" + Math.round(p.getAcademic() * 100) + "%");
            if (p.getTendency() != null) dims.add("tendency=" + Math.round(p.getTendency() * 100) + "%");
            if (p.getPractice() != null) dims.add("practice=" + Math.round(p.getPractice() * 100) + "%");
            lines.add("画像维度得分：" + (dims.isEmpty() ? "无" : String.join("；", dims)));
        }
        List<String> items = new ArrayList<>();
        if (req.getResults() != null) {
            for (var r : req.getResults()) {
                items.add("- " + r.getDirectionId() + "：得分 " + r.getScore() + "，排名 " + r.getRank());
            }
        }
        lines.add("候选方向：" + (items.isEmpty() ? "无" : String.join("\n", items)));
        lines.add("请为每个候选方向生成解释 JSON。");
        return String.join("\n", lines);
    }

    private String buildReviewPrompt(AiReviewSummarizeRequest req) {
        List<String> parts = new ArrayList<>();
        parts.add("复盘周期：" + nvl(req.getCycle()));
        var c = req.getReviewContent();
        if (c != null) {
            if (c.getDone() != null && !c.getDone().isBlank()) parts.add("本阶段完成情况：" + c.getDone());
            if (c.getUndone() != null && !c.getUndone().isBlank()) parts.add("未完成情况及原因：" + c.getUndone());
            if (c.getInterest() != null && !c.getInterest().isBlank()) parts.add("方向兴趣变化：" + c.getInterest());
            if (c.getAbility() != null && !c.getAbility().isBlank()) parts.add("能力提升：" + c.getAbility());
            if (c.getNext() != null && !c.getNext().isBlank()) parts.add("下一步安排：" + c.getNext());
        }
        if (req.getTaskSummary() != null && !req.getTaskSummary().isBlank()) {
            parts.add("任务完成情况：" + req.getTaskSummary());
        }
        return String.join("\n", parts);
    }

    private String textOr(JsonNode node, String field, String def) {
        JsonNode v = node.path(field);
        if (v.isNull() || v.asText("").isBlank()) {
            return def;
        }
        return v.asText();
    }

    private List<String> textList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                out.add(n.asText());
            }
        }
        return out;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception exc) {
            return "{}";
        }
    }

    private String nvl(String s) {
        return s == null ? "未指定" : s;
    }

    private Map<String, String> mapOf(String k1, String v1) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return m;
    }

    private Map<String, String> mapOf(String k1, String v1, String k2, String v2) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private Map<String, String> mapOf(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    // ---------------------------------------------------------------- 提示词

    private static final String CHAT_SYSTEM_PROMPT =
            "你是生涯规划系统中的「生涯咨询助手」，面向在校大学生提供生涯发展、专业选择、"
            + "职业方向与学习规划方面的咨询。请只依据学生提供的信息给出客观、建设性的建议，"
            + "不虚构事实、不给出医疗或法律等专业意见。用简洁的中文回答，必要时分点说明。";

    private static final String EXPLAIN_SYSTEM_PROMPT =
            "你是生涯规划系统中的「推荐解释生成器」。请基于给定的画像维度得分与候选方向列表，"
            + "为每个候选方向生成通俗解释。只输出 JSON，不要输出任何额外文字或 Markdown 代码块。"
            + "JSON 结构固定为：{\"explanations\": [{\"directionId\": 字符串, \"summary\": 通俗解释, "
            + "\"confidenceText\": 可信程度文字, \"disclaimer\": \"智能生成，供探索参考\"}]}。"
            + "必须为每个候选方向各生成一条解释，directionId 与输入保持一致，summary 简洁客观、"
            + "只依据输入数值，不得虚构。";

    private static final String PLAN_SYSTEM_PROMPT =
            "你是生涯规划系统中的「计划生成器」。请根据给定的目标方向、画像维度与目标摘要，"
            + "生成一份一学期的计划草案。只输出 JSON，不要输出任何额外文字或 Markdown 代码块。"
            + "JSON 结构固定为：{\"goalSummary\": 字符串, \"semesterGoals\": [{\"title\": 字符串, "
            + "\"abilityTag\": 字符串}], \"monthlyTasks\": [{\"month\": \"YYYY-MM\", \"title\": 字符串, "
            + "\"taskType\": \"LEARNING/PRACTICE/CAREER/REVIEW\", \"estimatedHours\": 数字}], "
            + "\"notes\": [字符串]}。monthlyTasks 给出 4-6 个月度任务。";

    private static final String REVIEW_SYSTEM_PROMPT =
            "你是生涯规划系统中的「阶段复盘总结器」。请阅读学生的阶段复盘内容，输出 JSON："
            + "{\"summary\": 一段阶段总结, \"suggestions\": [若干调整建议]}。只输出 JSON，"
            + "不要输出任何额外文字或 Markdown 代码块。";
}
