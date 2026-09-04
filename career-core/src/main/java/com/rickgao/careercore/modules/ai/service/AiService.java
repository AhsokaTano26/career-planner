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
 *  - 大模型失败时抛 BizException(INTERNAL_ERROR)，由调用方决定降级。
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

    @Transactional
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
            answer = llm.generate(buildChatMessages(sanitizedQuestion, req.getContext()), 0.7, 2000,
                    "career_chat", userId, null, messageGroup);
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
        String content = llm.generate(messages, 0.7, 2000, "recommendation_explain", runId, null, runId);
        List<Map<String, String>> items = parseExplainJson(content);
        List<AiExplanationItemVO> vos = new ArrayList<>();
        for (Map<String, String> item : items) {
            vos.add(AiExplanationItemVO.builder()
                    .directionId(item.get("directionId"))
                    .summary(item.get("summary"))
                    .confidenceText(item.getOrDefault("confidenceText", "数据基本完整，供参考"))
                    .disclaimer(item.getOrDefault("disclaimer", DISCLAIMER))
                    .build());
        }
        return AiExplainBatchVO.builder().runId(runId).explanations(vos).build();
    }

    // ---------------------------------------------------------------- 计划生成

    @Transactional
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
        String content = llm.generate(List.of(
                mapOf("role", "system", "content", PLAN_SYSTEM_PROMPT),
                mapOf("role", "user", "content", desensitizer.maskFreeText(userPrompt))), 0.7, 2000,
                "plan_generate", req.getDirectionId(), null, reqId);
        JsonNode node = parseJson(content);
        AiPlanResultVO result = AiPlanResultVO.builder()
                .goalSummary(textOr(node, "goalSummary", "围绕目标方向完成一学期学习与一个小项目"))
                .semesterGoals(parseSemesterGoals(node.path("semesterGoals")))
                .monthlyTasks(parseMonthlyTasks(node.path("monthlyTasks")))
                .notes(textList(node.path("notes")))
                .build();

        return result;
    }

    // ---------------------------------------------------------------- 复盘总结

    @Transactional
    public AiReviewSummaryVO reviewSummarize(AiReviewSummarizeRequest req) {
        String userId = resolveUserId(req.getStudentRef());
        String cycle = req.getCycle();
        String userPrompt = desensitizer.maskFreeText(buildReviewPrompt(req));
        String reqId = "review-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String content = llm.generate(List.of(
                mapOf("role", "system", "content", REVIEW_SYSTEM_PROMPT),
                mapOf("role", "user", "content", userPrompt)), 0.5, 1500,
                "review_summarize", cycle, null, reqId);
        AiReviewSummaryVO result;
        try {
            JsonNode node = parseJson(content);
            String summary = textOr(node, "summary", content);
            List<String> suggestions = textList(node.path("suggestions"));
            result = AiReviewSummaryVO.builder().summary(summary).suggestions(suggestions).build();
        } catch (BizException exc) {
            // Demo 精简点：复盘总结输出非 JSON 时回退为原文本（对齐 career-ai summarize）
            result = AiReviewSummaryVO.builder().summary(content).suggestions(List.of()).build();
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
        try {
            var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(10000);
            var client = org.springframework.web.client.RestClient.builder()
                    .requestFactory(factory).build();
            return client.get().uri(URI.create(url)).retrieve().body(byte[].class);
        } catch (Exception exc) {
            return null;
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
