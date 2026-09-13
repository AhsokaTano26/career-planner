package com.rickgao.careercore.modules.explore.service;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.assessment.dto.AnswerItem;
import com.rickgao.careercore.modules.assessment.dto.CreateSessionRequest;
import com.rickgao.careercore.modules.assessment.dto.SaveAnswersRequest;
import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import com.rickgao.careercore.modules.assessment.mapper.AssessmentMapper;
import com.rickgao.careercore.modules.assessment.service.AssessmentService;
import com.rickgao.careercore.modules.assessment.vo.AssessmentSessionVO;
import com.rickgao.careercore.modules.assessment.vo.DimensionScoreVO;
import com.rickgao.careercore.modules.assessment.vo.ScoreResultVO;
import com.rickgao.careercore.modules.advisor.vo.ProfileSnapshotVO;
import com.rickgao.careercore.modules.explore.dto.ExploreRequest;
import com.rickgao.careercore.modules.explore.vo.ExploreResultVO;
import com.rickgao.careercore.modules.portrait.service.PortraitService;
import com.rickgao.careercore.modules.recommendation.dto.CreateRecommendationRequest;
import com.rickgao.careercore.modules.recommendation.service.RecommendationService;
import com.rickgao.careercore.modules.recommendation.vo.RecRunVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生涯探索聚合服务（只换采集层）。
 *
 * <p>把「路径卡 + 兴趣标签」的点选结果折算为 EXPLORE 问卷答案，走既有链路一次产出
 * 计分 → 画像 → 方向推荐；不新建评分/画像/推荐算法。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - 聚合采用「每次出结果新建一个测评会话」的语义，答案暂存依赖 assessment 模块的内存 answerStore；
 *  - AI 引导式深挖（M2）追加 PROBE 区答案到同一会话时复用本服务校验入口。
 */
@Service
public class ExploreService {

    /** 兴趣标签点选上限（与需求「≤3 个」对齐）。 */
    public static final int MAX_TAG_ANSWERS = 3;
    /** 探索作答总量上限（标签 ≤3 + 深挖 ≤6 = ≤9，与需求总配额对齐，前端路径卡另计 1）。 */
    public static final int MAX_TOTAL_ANSWERS = 9;
    /** EXPLORE 问卷 type 标识（种子数据定义）。 */
    public static final String EXPLORE_TYPE = "EXPLORE";
    /** 兴趣标签题目分组（存于 question.dim，CHOICE 计分不读 dim，无副作用）。 */
    public static final String TAG_DIM = "EXPLORE_TAG";
    /** AI 深挖题目分组（M2 选题与作答均落此区）。 */
    public static final String PROBE_DIM = "EXPLORE_PROBE";
    private static final Set<String> ALLOWED_PATHS = Set.of("graduate", "employment", "overseas", "undecided");

    private final AssessmentMapper assessmentMapper;
    private final AssessmentService assessmentService;
    private final PortraitService portraitService;
    private final RecommendationService recommendationService;

    public ExploreService(AssessmentMapper assessmentMapper, AssessmentService assessmentService,
                          PortraitService portraitService, RecommendationService recommendationService) {
        this.assessmentMapper = assessmentMapper;
        this.assessmentService = assessmentService;
        this.portraitService = portraitService;
        this.recommendationService = recommendationService;
    }

    @Transactional
    public ExploreResultVO run(String studentId, ExploreRequest req) {
        String path = normalizePath(req.getPath());
        List<AnswerItem> answers = req.getAnswers() == null ? List.of() : req.getAnswers();
        if (answers.size() > MAX_TOTAL_ANSWERS) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "探索作答最多 " + MAX_TOTAL_ANSWERS + " 项（标签 ≤3 + 深挖 ≤6）");
        }

        ExploreQuestionnaire explore = resolveExploreQuestionnaire();
        int tagCount = 0;
        for (AnswerItem a : answers) {
            String dim = explore.dimByQuestion.get(a.getQuestionId());
            if (dim == null) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "仅可选择探索问卷题目作答");
            }
            if (TAG_DIM.equals(dim)) {
                tagCount++;
            }
            validateAnswer(a, explore);
        }
        if (tagCount > MAX_TAG_ANSWERS) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "探索兴趣标签最多选择 " + MAX_TAG_ANSWERS + " 个");
        }

        // 1) 有标签才走测评计分；只选路径时跳过，画像走历史旧测评或档案估算兜底
        List<DimensionScoreVO> dimensionScores = new ArrayList<>();
        String portraitSource;
        if (!answers.isEmpty()) {
            CreateSessionRequest create = new CreateSessionRequest();
            create.setQuestionnaireId(explore.questionnaireId);
            AssessmentSessionVO session = assessmentService.createSession(studentId, create);

            SaveAnswersRequest save = new SaveAnswersRequest();
            save.setAnswers(answers);
            save.setFinished(true);
            assessmentService.saveAnswers(session.getId(), studentId, save);

            ScoreResultVO scored = assessmentService.submit(session.getId(), studentId);
            dimensionScores = scored.getDimensionScores() == null ? List.of() : scored.getDimensionScores();
            portraitSource = "EXPLORE";
        } else {
            portraitSource = assessmentMapper.findLatestScoredByStudent(studentId) == null
                    ? "PROFILE" : "LEGACY";
        }

        // 2) 画像（规则生成，幂等新增版本）
        ProfileSnapshotVO portrait = portraitService.refresh(studentId);

        // 3) 方向推荐（undecided/未选路径不过滤）
        String pathFilter = path == null || "undecided".equals(path) ? null : path;
        CreateRecommendationRequest rec = new CreateRecommendationRequest();
        rec.setPathFilter(pathFilter);
        RecRunVO recommendation = recommendationService.createRun(studentId, rec);

        return ExploreResultVO.builder()
                .dimensionScores(dimensionScores)
                .portrait(portrait)
                .recommendation(recommendation)
                .portraitSource(portraitSource)
                .build();
    }

    // ---------------------------------------------------------------- 内部

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (!ALLOWED_PATHS.contains(path)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "path 仅支持 graduate/employment/overseas/undecided");
        }
        return path;
    }

    private void validateAnswer(AnswerItem answer, ExploreQuestionnaire explore) {
        if (answer == null || answer.getQuestionId() == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "探索答案缺少 questionId");
        }
        Integer optionCount = explore.optionCountByQuestion.get(answer.getQuestionId());
        if (optionCount == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "仅可选择探索问卷题目作答");
        }
        if (answer.getOptionIndex() == null || answer.getOptionIndex() < 0 || answer.getOptionIndex() >= optionCount) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "探索选项不合法");
        }
    }

    /** 定位已发布的 EXPLORE 问卷及其 TAG/PROBE 题目与选项数。 */
    private ExploreQuestionnaire resolveExploreQuestionnaire() {
        Questionnaire qnr = assessmentMapper.listPublishedQuestionnaires().stream()
                .filter(q -> EXPLORE_TYPE.equals(q.getType()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.RESOURCE_NOT_FOUND, "尚未配置生涯探索问卷"));
        QuestionnaireVersion version = assessmentMapper.findLatestVersion(qnr.getId());
        if (version == null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "生涯探索问卷尚未发布");
        }
        List<Question> questions = assessmentMapper.listQuestions(version.getId());
        List<String> exploreIds = questions.stream()
                .filter(q -> TAG_DIM.equals(q.getDim()) || PROBE_DIM.equals(q.getDim()))
                .map(Question::getId)
                .collect(Collectors.toList());
        if (exploreIds.isEmpty()) {
            throw new BizException(ResultCode.STATE_CONFLICT, "生涯探索问卷未配置可作答题目");
        }
        List<String> ids = new ArrayList<>(exploreIds);
        Map<String, String> dimByQuestion = questions.stream()
                .filter(q -> ids.contains(q.getId()))
                .collect(Collectors.toMap(Question::getId, Question::getDim, (a, b) -> a, LinkedHashMap::new));
        Map<String, Integer> optionCountByQuestion = assessmentMapper.listOptions(ids).stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId,
                        LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), List::size)));
        return new ExploreQuestionnaire(qnr.getId(), dimByQuestion, optionCountByQuestion);
    }

    /** EXPLORE 问卷解析结果（各题分组 + 可选项数，用于答案校验）。 */
    private static class ExploreQuestionnaire {
        final String questionnaireId;
        final Map<String, String> dimByQuestion;
        final Map<String, Integer> optionCountByQuestion;

        ExploreQuestionnaire(String questionnaireId, Map<String, String> dimByQuestion,
                             Map<String, Integer> optionCountByQuestion) {
            this.questionnaireId = questionnaireId;
            this.dimByQuestion = dimByQuestion;
            this.optionCountByQuestion = optionCountByQuestion;
        }
    }
}
