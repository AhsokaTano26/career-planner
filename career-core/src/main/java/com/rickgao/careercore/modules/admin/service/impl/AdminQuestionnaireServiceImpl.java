package com.rickgao.careercore.modules.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireRequest;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireStatusUpdate;
import com.rickgao.careercore.modules.admin.mapper.AdminQuestionnaireMapper;
import com.rickgao.careercore.modules.admin.service.AdminQuestionnaireService;
import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import com.rickgao.careercore.modules.assessment.vo.QuestionOptionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireDetailVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVersionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端-问卷管理业务实现。
 */
@Service
public class AdminQuestionnaireServiceImpl implements AdminQuestionnaireService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Map<String, String> DIM_NAMES = Map.of(
            "interest", "兴趣", "values", "价值观", "ability", "能力",
            "academic", "学业", "tendency", "倾向", "practice", "实践");
    private static final List<String> Q_TYPES = List.of("CHOICE", "RATING");

    private final AdminQuestionnaireMapper mapper;
    private final IdGenerator idGenerator;

    public AdminQuestionnaireServiceImpl(AdminQuestionnaireMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public PageResult<QuestionnaireVO> listQuestionnaires(String keyword, int page, int size) {
        int p = page < 1 ? DEFAULT_PAGE : page;
        int s = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int offset = (p - 1) * s;
        List<QuestionnaireVO> list = mapper.selectQuestionnairePage(keyword, offset, s).stream()
                .map(q -> toQuestionnaireVO(q, questionCount(q.getId())))
                .collect(Collectors.toList());
        long total = mapper.countQuestionnaires(keyword);
        return PageResult.of(list, total, p, s);
    }

    @Override
    @Transactional
    public QuestionnaireVO createQuestionnaire(String adminId, QuestionnaireRequest req) {
        if (!StringUtils.hasText(req.getType()) || !StringUtils.hasText(req.getName())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "type 与 name 不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        Questionnaire q = new Questionnaire();
        q.setId(idGenerator.questionnaireId());
        q.setType(req.getType());
        q.setName(req.getName());
        q.setTypeName(req.getTypeName());
        q.setIcon(req.getIcon());
        q.setStatus("DRAFT");
        q.setVersion(1);
        q.setMinutes(req.getMinutes());
        q.setTip(req.getTip());
        q.setCreatedAt(now);
        mapper.insertQuestionnaire(q);

        QuestionnaireVersion v = new QuestionnaireVersion();
        v.setId(idGenerator.questionnaireVersionId());
        v.setQuestionnaireId(q.getId());
        v.setVersion(1);
        v.setStatus("DRAFT");
        v.setChangeNote(req.getChangeNote());
        insertVersionWithQuestions(v, req);
        return toQuestionnaireVO(q, v.getQuestionCount() == null ? 0 : v.getQuestionCount());
    }

    @Override
    @Transactional
    public QuestionnaireVO updateQuestionnaire(String adminId, String questionnaireId, QuestionnaireRequest req) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        // 部分更新：仅覆盖传入的非空字段（PATCH 语义）
        if (StringUtils.hasText(req.getType())) {
            q.setType(req.getType());
        }
        if (StringUtils.hasText(req.getName())) {
            q.setName(req.getName());
        }
        if (req.getTypeName() != null) {
            q.setTypeName(req.getTypeName());
        }
        if (req.getIcon() != null) {
            q.setIcon(req.getIcon());
        }
        if (req.getMinutes() != null) {
            q.setMinutes(req.getMinutes());
        }
        if (req.getTip() != null) {
            q.setTip(req.getTip());
        }
        mapper.updateQuestionnaireMeta(q);
        return toQuestionnaireVO(q, questionCount(q.getId()));
    }

    @Override
    @Transactional
    public QuestionnaireVO setStatus(String adminId, String questionnaireId, QuestionnaireStatusUpdate req) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        String status = req.getStatus();
        if (!List.of("PUBLISHED", "DISABLED", "DRAFT").contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 PUBLISHED/DISABLED/DRAFT");
        }
        if ("PUBLISHED".equals(status)) {
            // 发布问卷：若无已发布版本则发布最新版本
            QuestionnaireVersion latest = latestVersion(questionnaireId);
            if (latest == null) {
                throw new BizException(ResultCode.STATE_CONFLICT, "问卷无版本，无法发布");
            }
            if (!"PUBLISHED".equals(latest.getStatus())) {
                publishVersionRecord(adminId, latest);
            }
        }
        mapper.updateQuestionnaireStatus(questionnaireId, status);
        q.setStatus(status);
        return toQuestionnaireVO(q, questionCount(q.getId()));
    }

    @Override
    public List<QuestionnaireVersionVO> listVersions(String questionnaireId) {
        requireQuestionnaire(questionnaireId);
        return mapper.listVersions(questionnaireId).stream()
                .map(this::toVersionVO)
                .collect(Collectors.toList());
    }

    @Override
    public QuestionnaireDetailVO getVersionDetail(String questionnaireId, String versionId) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        QuestionnaireVersion v = requireVersion(versionId);
        if (!questionnaireId.equals(v.getQuestionnaireId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "版本不属于该问卷");
        }
        List<QuestionVO> questions = buildQuestions(v.getId());
        return QuestionnaireDetailVO.builder()
                .questionnaire(toQuestionnaireVO(q, questions.size()))
                .questions(questions)
                .build();
    }

    @Override
    @Transactional
    public QuestionnaireVersionVO createVersion(String adminId, String questionnaireId, QuestionnaireRequest req) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        List<QuestionnaireVersion> existing = mapper.listVersions(questionnaireId);
        int nextVersion = existing.stream()
                .map(QuestionnaireVersion::getVersion)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        QuestionnaireVersion v = new QuestionnaireVersion();
        v.setId(idGenerator.questionnaireVersionId());
        v.setQuestionnaireId(questionnaireId);
        v.setVersion(nextVersion);
        v.setStatus("DRAFT");
        v.setChangeNote(req.getChangeNote());
        insertVersionWithQuestions(v, req);

        q.setVersion(nextVersion);
        mapper.updateQuestionnaireMeta(q);
        return toVersionVO(v);
    }

    @Override
    @Transactional
    public QuestionnaireVersionVO publishVersion(String adminId, String questionnaireId, String versionId) {
        Questionnaire q = requireQuestionnaire(questionnaireId);
        QuestionnaireVersion v = requireVersion(versionId);
        if (!questionnaireId.equals(v.getQuestionnaireId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "版本不属于该问卷");
        }
        publishVersionRecord(adminId, v);
        // 同步问卷状态与当前版本
        mapper.updateQuestionnairePublish(questionnaireId, "PUBLISHED", v.getVersion(),
                v.getPublishedAt(), v.getPublishedBy());
        q.setStatus("PUBLISHED");
        q.setVersion(v.getVersion());
        return toVersionVO(v);
    }

    // ---------------------------------------------------------------- 内部

    private void publishVersionRecord(String adminId, QuestionnaireVersion v) {
        LocalDateTime now = LocalDateTime.now();
        mapper.updateVersionPublish(v.getId(), "PUBLISHED", now, adminId);
        v.setStatus("PUBLISHED");
        v.setPublishedAt(now);
        v.setPublishedBy(adminId);
    }

    private void insertVersionWithQuestions(QuestionnaireVersion v, QuestionnaireRequest req) {
        int count = 0;
        if (req.getQuestions() != null) {
            for (QuestionnaireRequest.QuestionItem item : req.getQuestions()) {
                if (!StringUtils.hasText(item.getText())) {
                    continue;
                }
                String type = StringUtils.hasText(item.getType()) ? item.getType() : "CHOICE";
                if (!Q_TYPES.contains(type)) {
                    throw new BizException(ResultCode.VALIDATION_ERROR, "题目 type 仅支持 CHOICE/RATING");
                }
                Question qu = new Question();
                qu.setId(idGenerator.questionId());
                qu.setQuestionnaireVersionId(v.getId());
                qu.setText(item.getText());
                qu.setType(type);
                qu.setDim(item.getDim());
                qu.setSortOrder(count);
                qu.setCreatedAt(LocalDateTime.now());
                mapper.insertQuestion(qu);
                if (item.getOptions() != null) {
                    int oi = 0;
                    for (QuestionnaireRequest.OptionItem opt : item.getOptions()) {
                        if (!StringUtils.hasText(opt.getText())) {
                            continue;
                        }
                        QuestionOption o = new QuestionOption();
                        o.setId(idGenerator.questionOptionId());
                        o.setQuestionId(qu.getId());
                        o.setText(opt.getText());
                        o.setScoresJson(opt.getScores() == null ? null : JsonUtil.toJson(opt.getScores()));
                        o.setSortOrder(oi++);
                        o.setCreatedAt(LocalDateTime.now());
                        mapper.insertOption(o);
                    }
                }
                count++;
            }
        }
        v.setQuestionCount(count);
        v.setCreatedAt(LocalDateTime.now());
        // insertVersion 在 insertVersionWithQuestions 前需先有 id/version/status/changeNote
        mapper.insertVersion(v);
    }

    private List<QuestionVO> buildQuestions(String versionId) {
        List<Question> qs = mapper.listQuestions(versionId);
        if (qs.isEmpty()) {
            return List.of();
        }
        List<QuestionOption> opts = mapper.listOptions(qs.stream().map(Question::getId).collect(Collectors.toList()));
        Map<String, List<QuestionOption>> optMap = opts.stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId, LinkedHashMap::new, Collectors.toList()));
        List<QuestionVO> result = new ArrayList<>();
        for (Question qu : qs) {
            List<QuestionOption> qOpts = optMap.getOrDefault(qu.getId(), List.of());
            result.add(QuestionVO.builder()
                    .id(qu.getId())
                    .text(qu.getText())
                    .type(qu.getType())
                    .dim(qu.getDim())
                    .labels(List.of(DIM_NAMES.getOrDefault(qu.getDim(), qu.getDim())))
                    .options(qOpts.stream().map(o -> QuestionOptionVO.builder()
                            .id(o.getId()).text(o.getText()).scores(parseScores(o.getScoresJson()))
                            .build()).collect(Collectors.toList()))
                    .build());
        }
        return result;
    }

    private Questionnaire requireQuestionnaire(String id) {
        Questionnaire q = mapper.findQuestionnaireById(id);
        if (q == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "问卷不存在");
        }
        return q;
    }

    private QuestionnaireVersion requireVersion(String id) {
        QuestionnaireVersion v = mapper.findVersionById(id);
        if (v == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "问卷版本不存在");
        }
        return v;
    }

    private QuestionnaireVersion latestVersion(String questionnaireId) {
        List<QuestionnaireVersion> list = mapper.listVersions(questionnaireId);
        return list.isEmpty() ? null : list.get(0);
    }

    private int questionCount(String questionnaireId) {
        QuestionnaireVersion v = latestVersion(questionnaireId);
        return v == null || v.getQuestionCount() == null ? 0 : v.getQuestionCount();
    }

    private QuestionnaireVO toQuestionnaireVO(Questionnaire q, int questionCount) {
        return QuestionnaireVO.builder()
                .id(q.getId()).type(q.getType()).typeName(q.getTypeName()).icon(q.getIcon())
                .status(q.getStatus()).version(q.getVersion()).questionCount(questionCount)
                .minutes(q.getMinutes()).tip(q.getTip()).publishedAt(ts(q.getPublishedAt()))
                .build();
    }

    private QuestionnaireVersionVO toVersionVO(QuestionnaireVersion v) {
        return QuestionnaireVersionVO.builder()
                .id(v.getId()).version(v.getVersion()).status(v.getStatus())
                .publishedAt(ts(v.getPublishedAt())).publishedBy(v.getPublishedBy())
                .questionCount(v.getQuestionCount()).changeNote(v.getChangeNote())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseScores(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonUtil.parse(json, new TypeReference<Map<String, Double>>() {
            });
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String ts(LocalDateTime t) {
        return t == null ? null : t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
