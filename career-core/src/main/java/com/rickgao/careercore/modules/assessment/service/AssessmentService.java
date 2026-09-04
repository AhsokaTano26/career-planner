package com.rickgao.careercore.modules.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.assessment.dto.AnswerItem;
import com.rickgao.careercore.modules.assessment.dto.CreateSessionRequest;
import com.rickgao.careercore.modules.assessment.dto.SaveAnswersRequest;
import com.rickgao.careercore.modules.assessment.entity.AssessmentSession;
import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import com.rickgao.careercore.modules.assessment.mapper.AssessmentMapper;
import com.rickgao.careercore.modules.assessment.vo.AssessmentSessionVO;
import com.rickgao.careercore.modules.assessment.vo.DimensionScoreVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionOptionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireDetailVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVersionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVO;
import com.rickgao.careercore.modules.assessment.vo.ScoreResultVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 测评模块：问卷查询 + 会话管理 + 计分。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - 答案暂存进程内存（ConcurrentHashMap），重启清空；不落 answers 表；
 *  - 计分为选项六维得分累加后归一化到 0-100；
 *  - 依赖 questionnaire/questionnaire_version/question/question_option 种子数据。
 */
@Service
public class AssessmentService {

    private static final String[] DIMENSIONS = {"interest", "values", "ability", "academic", "tendency", "practice"};
    private static final Map<String, String> DIM_NAMES = Map.of(
            "interest", "兴趣", "values", "价值观", "ability", "能力",
            "academic", "学业", "tendency", "倾向", "practice", "实践");

    private final AssessmentMapper assessmentMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    /** sessionId -> 答案（Demo 内存暂存） */
    private final Map<String, List<AnswerItem>> answerStore = new ConcurrentHashMap<>();

    public AssessmentService(AssessmentMapper assessmentMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.assessmentMapper = assessmentMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- 问卷
    public List<QuestionnaireVO> listQuestionnaires() {
        return assessmentMapper.listPublishedQuestionnaires().stream()
                .map(q -> toQuestionnaireVO(q, questionCount(q.getId())))
                .collect(Collectors.toList());
    }

    public QuestionnaireDetailVO getQuestionnaireDetail(String questionnaireId) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        QuestionnaireVersion v = assessmentMapper.findLatestVersion(questionnaireId);
        List<QuestionVO> questions = new ArrayList<>();
        if (v != null) {
            List<Question> qs = assessmentMapper.listQuestions(v.getId());
            List<QuestionOption> opts = qs.isEmpty() ? List.of()
                    : assessmentMapper.listOptions(qs.stream().map(Question::getId).collect(Collectors.toList()));
            Map<String, List<QuestionOption>> optMap = opts.stream()
                    .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
            for (Question qu : qs) {
                List<QuestionOption> qOpts = optMap.getOrDefault(qu.getId(), List.of());
                questions.add(QuestionVO.builder()
                        .id(qu.getId())
                        .text(qu.getText())
                        .type(qu.getType())
                        .dim(qu.getDim())
                        .labels(List.of(DIM_NAMES.getOrDefault(qu.getDim(), qu.getDim())))
                        .options(qOpts.stream().map(this::toOptionVO).collect(Collectors.toList()))
                        .build());
            }
        }
        return QuestionnaireDetailVO.builder()
                .questionnaire(toQuestionnaireVO(q, questions.size()))
                .questions(questions)
                .build();
    }

    public List<QuestionnaireVersionVO> listVersions(String questionnaireId) {
        requireQuestionnaire(questionnaireId);
        return assessmentMapper.listVersions(questionnaireId).stream()
                .map(v -> QuestionnaireVersionVO.builder()
                        .id(v.getId()).version(v.getVersion()).status(v.getStatus())
                        .publishedAt(ts(v.getPublishedAt())).publishedBy(v.getPublishedBy())
                        .questionCount(v.getQuestionCount()).changeNote(v.getChangeNote())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- 会话
    @Transactional
    public AssessmentSessionVO createSession(String studentId, CreateSessionRequest req) {
        Questionnaire q = requireQuestionnaire(req.getQuestionnaireId());
        QuestionnaireVersion v = assessmentMapper.findLatestVersion(q.getId());
        if (v == null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "问卷尚未发布");
        }
        List<Question> qs = assessmentMapper.listQuestions(v.getId());
        AssessmentSession session = new AssessmentSession();
        session.setId(idGenerator.assessmentSessionId());
        session.setStudentId(studentId);
        session.setQuestionnaireVersionId(v.getId());
        session.setStatus("IN_PROGRESS");
        session.setTotalQuestions(qs.size());
        session.setAnsweredQuestions(0);
        session.setStartedAt(LocalDateTime.now());
        assessmentMapper.insertSession(session);
        return toSessionVO(session, q.getName(), v.getVersion());
    }

    @Transactional
    public void saveAnswers(String sessionId, SaveAnswersRequest req) {
        AssessmentSession session = requireOwnSession(sessionId, null);
        if ("COMPLETED".equals(session.getStatus()) || "SCORED".equals(session.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "会话已提交，无法再保存");
        }
        List<AnswerItem> answers = req.getAnswers() == null ? List.of() : req.getAnswers();
        answerStore.put(sessionId, new ArrayList<>(answers));
        int answered = answers.size();
        String status = Boolean.TRUE.equals(req.getFinished()) ? "COMPLETED" : "IN_PROGRESS";
        assessmentMapper.updateSessionAnswers(sessionId, answered, status);
    }

    @Transactional
    public ScoreResultVO submit(String sessionId, String studentId) {
        AssessmentSession session = requireOwnSession(sessionId, studentId);
        if ("SCORED".equals(session.getStatus())) {
            return buildScoreResult(session);
        }
        List<AnswerItem> answers = answerStore.getOrDefault(sessionId, List.of());
        String scoreJson = computeScores(session.getQuestionnaireVersionId(), answers);
        assessmentMapper.updateSessionScore(sessionId, "SCORED", scoreJson, ts(LocalDateTime.now()));
        session.setStatus("SCORED");
        session.setScoreJson(scoreJson);
        return buildScoreResult(session);
    }

    public List<AssessmentSessionVO> listMySessions(String studentId) {
        List<AssessmentSession> sessions = assessmentMapper.listSessions(studentId);
        return sessions.stream().map(s -> {
            QuestionnaireVersion v = s.getQuestionnaireVersionId() == null ? null
                    : assessmentMapper.findVersionById(s.getQuestionnaireVersionId());
            String name = v == null ? "" : questionnaireName(v.getQuestionnaireId());
            return toSessionVO(s, name, v == null ? null : v.getVersion());
        }).collect(Collectors.toList());
    }

    public AssessmentSessionVO getSession(String sessionId, String studentId) {
        AssessmentSession s = requireOwnSession(sessionId, studentId);
        QuestionnaireVersion v = s.getQuestionnaireVersionId() == null ? null
                : assessmentMapper.findVersionById(s.getQuestionnaireVersionId());
        return toSessionVO(s, v == null ? "" : questionnaireName(v.getQuestionnaireId()),
                v == null ? null : v.getVersion());
    }

    public ScoreResultVO getScores(String sessionId, String studentId) {
        AssessmentSession s = requireOwnSession(sessionId, studentId);
        if (!"SCORED".equals(s.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "会话尚未计分");
        }
        return buildScoreResult(s);
    }

    // ---------------------------------------------------------------- 计分
    private String computeScores(String versionId, List<AnswerItem> answers) {
        List<Question> qs = assessmentMapper.listQuestions(versionId);
        List<QuestionOption> opts = qs.isEmpty() ? List.of()
                : assessmentMapper.listOptions(qs.stream().map(Question::getId).collect(Collectors.toList()));
        Map<String, Question> qMap = qs.stream().collect(Collectors.toMap(Question::getId, q -> q));
        Map<String, List<QuestionOption>> optMap = opts.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));

        Map<String, Double> totals = new LinkedHashMap<>();
        for (String d : DIMENSIONS) {
            totals.put(d, 0.0);
        }
        int dimsUsed = 0;
        for (AnswerItem a : answers) {
            Question qu = qMap.get(a.getQuestionId());
            if (qu == null) {
                continue;
            }
            List<QuestionOption> qOpts = optMap.getOrDefault(a.getQuestionId(), List.of());
            if ("RATING".equals(qu.getType())) {
                int rv = a.getRatingValue() == null ? 0 : Math.max(0, Math.min(5, a.getRatingValue()));
                totals.computeIfPresent(qu.getDim(), (k, v) -> v + rv);
                if (qu.getDim() != null) {
                    dimsUsed++;
                }
            } else {
                int idx = a.getOptionIndex() == null ? -1 : a.getOptionIndex();
                if (idx >= 0 && idx < qOpts.size()) {
                    QuestionOption opt = qOpts.get(idx);
                    Map<String, Double> scores = parseScores(opt.getScoresJson());
                    for (String d : DIMENSIONS) {
                        totals.computeIfPresent(d, (k, v) -> v + scores.getOrDefault(d, 0.0));
                    }
                }
            }
        }
        // 归一化到 0-100（每维度按题目数*最大贡献估算，Demo 简化）
        List<DimensionScoreVO> dims = new ArrayList<>();
        for (String d : DIMENSIONS) {
            double raw = totals.getOrDefault(d, 0.0);
            double score = Math.min(100.0, Math.round(raw * 10.0) / 10.0);
            dims.add(DimensionScoreVO.builder().dimensionCode(d)
                    .dimensionName(DIM_NAMES.getOrDefault(d, d)).score(score).build());
        }
        return writeJson(dims);
    }

    private ScoreResultVO buildScoreResult(AssessmentSession s) {
        List<DimensionScoreVO> dims = parseScoreList(s.getScoreJson());
        return ScoreResultVO.builder().sessionId(s.getId()).status("COMPLETED").dimensionScores(dims).build();
    }

    private int questionCount(String questionnaireId) {
        QuestionnaireVersion v = assessmentMapper.findLatestVersion(questionnaireId);
        return v == null ? 0 : (v.getQuestionCount() == null ? 0 : v.getQuestionCount());
    }

    private String questionnaireName(String id) {
        Questionnaire q = assessmentMapper.findQuestionnaireById(id);
        return q == null ? "" : q.getName();
    }

    private AssessmentSession requireOwnSession(String sessionId, String studentId) {
        AssessmentSession s = assessmentMapper.findSessionById(sessionId);
        if (s == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "会话不存在");
        }
        if (studentId != null && !studentId.equals(s.getStudentId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该会话");
        }
        return s;
    }

    private Questionnaire requireQuestionnaire(String id) {
        Questionnaire q = assessmentMapper.findQuestionnaireById(id);
        if (q == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "问卷不存在");
        }
        return q;
    }

    private QuestionnaireVO toQuestionnaireVO(Questionnaire q, int questionCount) {
        return QuestionnaireVO.builder()
                .id(q.getId()).type(q.getType()).typeName(q.getTypeName()).icon(q.getIcon())
                .status(q.getStatus()).version(q.getVersion()).questionCount(questionCount)
                .minutes(q.getMinutes()).tip(q.getTip()).publishedAt(ts(q.getPublishedAt()))
                .build();
    }

    private AssessmentSessionVO toSessionVO(AssessmentSession s, String name, Integer version) {
        return AssessmentSessionVO.builder()
                .id(s.getId()).questionnaireId(s.getQuestionnaireVersionId())
                .questionnaireName(name).questionnaireVersion(version)
                .status(s.getStatus()).totalQuestions(s.getTotalQuestions())
                .answeredQuestions(s.getAnsweredQuestions())
                .startedAt(ts(s.getStartedAt())).updatedAt(ts(s.getUpdatedAt()))
                .finishedAt(ts(s.getFinishedAt()))
                .build();
    }

    private QuestionOptionVO toOptionVO(QuestionOption o) {
        return QuestionOptionVO.builder().id(o.getId()).text(o.getText()).scores(parseScores(o.getScoresJson())).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseScores(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (Exception exc) {
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<DimensionScoreVO> parseScoreList(String json) {
        List<DimensionScoreVO> out = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(DimensionScoreVO.builder()
                            .dimensionCode(n.path("dimensionCode").asText())
                            .dimensionName(n.path("dimensionName").asText())
                            .score(n.path("score").asDouble())
                            .build());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception exc) {
            return "[]";
        }
    }

    private String ts(LocalDateTime t) {
        return t == null ? null : t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}

