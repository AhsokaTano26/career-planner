package com.rickgao.careercore.modules.explore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.ai.entity.AiChatMessage;
import com.rickgao.careercore.modules.ai.mapper.AiChatMessageMapper;
import com.rickgao.careercore.modules.ai.service.Desensitizer;
import com.rickgao.careercore.modules.ai.service.LlmGateway;
import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import com.rickgao.careercore.modules.assessment.mapper.AssessmentMapper;
import com.rickgao.careercore.modules.explore.dto.ExploreProbeRequest;
import com.rickgao.careercore.modules.explore.vo.ExploreProbeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 探索深挖 probe：AI 从 PROBE 题池选下一道未答题并组织引导语。
 *
 * <p>刻意不用 @Transactional（先调远程 LLM，再单语句落库；仿 AiService.chat，避免长占 DB 连接）。
 * 落库仅 assistant 侧一条（引导语 + 选项文本快照），user_id 隔离供学生端回看。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - 提问不跨会话持久化计数；配额由前端 submitted answers + askedCount 双重约束；
 *  - AI 返回非法 questionId 时回退题池首题（不抛错，保证接口 200）。
 */
@Service
public class ExploreProbeService {

    private static final Logger log = LoggerFactory.getLogger(ExploreProbeService.class);

    /** AI 深挖追问上限（与需求「AI 追问 ≤6」对齐）。 */
    public static final int MAX_PROBE_QUESTIONS = 6;
    public static final String SCENE = "explore_chat";
    public static final String PROMPT_VERSION = "explore.v1";

    private static final String SYSTEM_PROMPT = "你是生涯探索助手的引导员。根据学生的已选发展路径与已答摘要，"
            + "从候选题池中选择一道最值得追问的题目，并用一句话自然地引出这道题。"
            + "严格只输出 JSON：{\"questionId\":\"题目ID\",\"intro\":\"引导语（不超过60字，须引用学生已选路径或标签）\"}。"
            + "不要输出除 JSON 之外的内容。";

    private final AssessmentMapper assessmentMapper;
    private final LlmGateway llm;
    private final ObjectMapper objectMapper;
    private final AiChatMessageMapper chatMessageMapper;
    private final Desensitizer desensitizer;

    public ExploreProbeService(AssessmentMapper assessmentMapper, LlmGateway llm, ObjectMapper objectMapper,
                               AiChatMessageMapper chatMessageMapper, Desensitizer desensitizer) {
        this.assessmentMapper = assessmentMapper;
        this.llm = llm;
        this.objectMapper = objectMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.desensitizer = desensitizer;
    }

    public ExploreProbeVO nextQuestion(String studentId, ExploreProbeRequest req) {
        int asked = req.getAskedCount() == null ? 0 : req.getAskedCount();
        if (asked >= MAX_PROBE_QUESTIONS) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "深挖追问已达上限（" + MAX_PROBE_QUESTIONS + " 次），可直接出结果");
        }
        Set<String> answered = req.getAnsweredQuestionIds() == null ? Set.of()
                : Set.copyOf(req.getAnsweredQuestionIds());
        List<Question> pool = unusedProbeQuestions(answered);
        if (pool.isEmpty()) {
            return ExploreProbeVO.builder().exhausted(true).aiGenerated(false).build();
        }

        Pick pick = pickWithAi(req.getPath(), answered, asked, pool);
        Question question = byId(pool, pick.questionId);
        if (question == null) {
            log.warn("AI 选择了题池外题目 {}，回退首题", pick.questionId);
            question = pool.get(0);
        }
        boolean aiGenerated = pick.aiGenerated && question.getId().equals(pick.questionId);
        String intro = pick.intro == null || pick.intro.isBlank()
                ? templateIntro(req.getPath(), question) : pick.intro;

        List<QuestionOption> options = assessmentMapper.listOptions(List.of(question.getId()));
        String messageGroup = persistProbe(studentId, question, options, intro);

        return ExploreProbeVO.builder()
                .questionId(question.getId())
                .questionText(question.getText())
                .intro(intro)
                .options(options.stream()
                        .map(o -> ExploreProbeVO.Option.builder().id(o.getId()).text(o.getText()).build())
                        .collect(Collectors.toList()))
                .aiGenerated(aiGenerated)
                .exhausted(false)
                .messageGroup(messageGroup)
                .build();
    }

    // ---------------------------------------------------------------- 内部

    private List<Question> unusedProbeQuestions(Set<String> answered) {
        Questionnaire qnr = assessmentMapper.listPublishedQuestionnaires().stream()
                .filter(q -> ExploreService.EXPLORE_TYPE.equals(q.getType()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.RESOURCE_NOT_FOUND, "尚未配置生涯探索问卷"));
        QuestionnaireVersion version = assessmentMapper.findLatestVersion(qnr.getId());
        if (version == null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "生涯探索问卷尚未发布");
        }
        return assessmentMapper.listQuestions(version.getId()).stream()
                .filter(q -> ExploreService.PROBE_DIM.equals(q.getDim()))
                .filter(q -> !answered.contains(q.getId()))
                .collect(Collectors.toList());
    }

    /** AI 选下一题；网关异常或输出非法时回退题池首题 + 模板引导语。 */
    private Pick pickWithAi(String path, Set<String> answered, int asked, List<Question> pool) {
        String poolDesc = pool.stream()
                .map(q -> q.getId() + "：" + q.getText())
                .collect(Collectors.joining("\n"));
        String userPrompt = "已选路径：" + (path == null || path.isBlank() ? "未定" : path)
                + "\n已答题：" + (answered.isEmpty() ? "无" : String.join("、", answered))
                + "\n已追问：" + asked + " 次\n候选题池：\n" + poolDesc;
        try {
            String content = llm.generate(List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", desensitizer.maskFreeText(userPrompt))),
                    0.3, llm.sceneMaxTokens(SCENE), SCENE, null, PROMPT_VERSION,
                    "explore-probe-" + java.util.UUID.randomUUID().toString().substring(0, 8));
            return parsePick(content);
        } catch (BizException exc) {
            log.warn("探索深挖大模型不可用，回退模板选题: {}", exc.getMessage());
            return Pick.fallback(pool.get(0).getId());
        }
    }

    private Pick parsePick(String content) {
        String text = content.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start < 0 || end <= start) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "探索深挖输出不含合法 JSON");
        }
        try {
            JsonNode node = objectMapper.readTree(text.substring(start, end + 1));
            String questionId = node.path("questionId").asText("");
            if (questionId.isBlank()) {
                throw new BizException(ResultCode.INTERNAL_ERROR, "探索深挖输出缺少 questionId");
            }
            return new Pick(questionId, node.path("intro").asText(""), true);
        } catch (BizException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "探索深挖输出 JSON 解析失败：" + exc.getMessage());
        }
    }

    private String templateIntro(String path, Question question) {
        String pathCn = switch (path == null ? "" : path) {
            case "graduate" -> "升学";
            case "employment" -> "就业";
            case "overseas" -> "留学";
            case "undecided" -> "探索方向";
            default -> "探索方向";
        };
        return "结合你对「" + pathCn + "」的兴趣，再深入了解一下：" + question.getText();
    }

    private Question byId(List<Question> pool, String id) {
        if (id == null) {
            return null;
        }
        return pool.stream().filter(q -> id.equals(q.getId())).findFirst().orElse(null);
    }

    private String persistProbe(String studentId, Question question, List<QuestionOption> options, String intro) {
        String group = java.util.UUID.randomUUID().toString().replace("-", "");
        String snapshot = intro + "【" + question.getText() + "】"
                + options.stream().map(QuestionOption::getText).collect(Collectors.joining(" / "));
        AiChatMessage row = new AiChatMessage();
        row.setId("AIM-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 28));
        row.setSessionId("explore");
        row.setUserId(studentId);
        row.setRole("assistant");
        row.setContent(snapshot);
        row.setNeedsHumanSupport(false);
        row.setSupportReason("");
        row.setMessageGroup(group);
        chatMessageMapper.insertBatch(List.of(row));
        return group;
    }

    /** AI 选题结果（questionId + 引导语 + 是否 AI 生成）。 */
    private static class Pick {
        final String questionId;
        final String intro;
        final boolean aiGenerated;

        Pick(String questionId, String intro, boolean aiGenerated) {
            this.questionId = questionId;
            this.intro = intro;
            this.aiGenerated = aiGenerated;
        }

        static Pick fallback(String questionId) {
            return new Pick(questionId, "", false);
        }
    }
}
