package com.rickgao.careercore.modules.advisor.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.advisor.dto.StudentListQuery;
import com.rickgao.careercore.modules.advisor.entity.AdvisorComment;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorCommentMapper;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentQueryMapper;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import com.rickgao.careercore.modules.advisor.query.ActiveGoalRow;
import com.rickgao.careercore.modules.advisor.query.CheckinRow;
import com.rickgao.careercore.modules.advisor.query.GoalChangeRow;
import com.rickgao.careercore.modules.advisor.query.LastReviewRow;
import com.rickgao.careercore.modules.advisor.query.PathCountRow;
import com.rickgao.careercore.modules.advisor.query.PlanRateRow;
import com.rickgao.careercore.modules.advisor.query.PlanRow;
import com.rickgao.careercore.modules.advisor.query.ProfileRow;
import com.rickgao.careercore.modules.advisor.query.ResultRow;
import com.rickgao.careercore.modules.advisor.query.ReviewRow;
import com.rickgao.careercore.modules.advisor.query.RunRow;
import com.rickgao.careercore.modules.advisor.query.SnapshotRow;
import com.rickgao.careercore.modules.advisor.query.TaskRow;
import com.rickgao.careercore.modules.advisor.service.AdvisorScopeService;
import com.rickgao.careercore.modules.advisor.service.AdvisorStudentService;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStatisticsVO;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStudentVO;
import com.rickgao.careercore.modules.advisor.vo.AttentionStudentVO;
import com.rickgao.careercore.modules.advisor.vo.GuidanceCommentVO;
import com.rickgao.careercore.modules.advisor.vo.GoalVO;
import com.rickgao.careercore.modules.advisor.vo.PlanVO;
import com.rickgao.careercore.modules.advisor.vo.ProfileSnapshotVO;
import com.rickgao.careercore.modules.advisor.vo.RecommendationRunVO;
import com.rickgao.careercore.modules.advisor.vo.ReviewVO;
import com.rickgao.careercore.modules.advisor.vo.StudentDetailViewVO;
import com.rickgao.careercore.modules.advisor.vo.TaskVO;
import com.rickgao.careercore.modules.student.entity.StudentExperience;
import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.mapper.StudentExperienceMapper;
import com.rickgao.careercore.modules.student.mapper.StudentProfileMapper;
import com.rickgao.careercore.modules.student.vo.ExperienceVO;
import com.rickgao.careercore.modules.student.vo.StudentProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 辅导员端学生查询服务实现。
 * 业务口径:
 * - 长期未复盘:30 天内无已提交复盘(含从未复盘);
 * - 多次调整目标:近 90 天目标版本变更 >= 3 次;
 * - status: review(待指导) > late(超期未复盘) > todo(未测评或未设主目标) > good;
 * - planRate:已确认计划中 DONE 任务 / 总任务。
 */
@Service
public class AdvisorStudentServiceImpl implements AdvisorStudentService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int LONG_NO_REVIEW_DAYS = 30;
    private static final int GOAL_CHANGE_DAYS = 90;
    private static final long GOAL_CHANGE_LIMIT = 3;
    private static final Set<String> PATH_ENUM = Set.of("graduate", "employment", "overseas");
    private static final Set<String> GOAL_STATUS_ENUM = Set.of("HAS_GOAL", "NO_GOAL");
    private static final Set<String> REVIEW_STATUS_ENUM = Set.of("REVIEWED_THIS_MONTH", "LONG_NO_REVIEW");
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "name", "className", "completeness", "planRate", "lastReview");
    private static final List<String> PATH_BUCKETS = List.of("graduate", "employment", "overseas", "undecided");

    private final AdvisorScopeService advisorScopeService;
    private final AdvisorStudentRelationMapper relationMapper;
    private final AdvisorStudentQueryMapper queryMapper;
    private final AdvisorCommentMapper advisorCommentMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final StudentExperienceMapper studentExperienceMapper;

    public AdvisorStudentServiceImpl(AdvisorScopeService advisorScopeService,
                                     AdvisorStudentRelationMapper relationMapper,
                                     AdvisorStudentQueryMapper queryMapper,
                                     AdvisorCommentMapper advisorCommentMapper,
                                     StudentProfileMapper studentProfileMapper,
                                     StudentExperienceMapper studentExperienceMapper) {
        this.advisorScopeService = advisorScopeService;
        this.relationMapper = relationMapper;
        this.queryMapper = queryMapper;
        this.advisorCommentMapper = advisorCommentMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.studentExperienceMapper = studentExperienceMapper;
    }

    @Override
    public PageResult<AdvisorStudentVO> listStudents(String advisorId, StudentListQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? DEFAULT_PAGE : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? DEFAULT_SIZE : Math.min(query.getSize(), MAX_SIZE);
        LocalDateTime now = LocalDateTime.now();
        List<String> ids = queryMapper.selectFilteredStudentIds(
                advisorId,
                validatePath(query.getPath()),
                query.getDirectionId(),
                parseGoalStatus(query.getGoalStatus()),
                Boolean.TRUE.equals(parseReviewStatus(query.getReviewStatus(), true)),
                Boolean.TRUE.equals(parseReviewStatus(query.getReviewStatus(), false)),
                query.getGuidanceRequested(),
                query.getKeyword(),
                now.withDayOfMonth(1).toLocalDate().atStartOfDay(),
                now.minusDays(LONG_NO_REVIEW_DAYS));
        Map<String, StudentAggregate> aggregates = loadAggregates(ids, now);
        List<AdvisorStudentVO> all = aggregates.values().stream()
                .map(a -> a.vo)
                .collect(Collectors.toCollection(ArrayList::new));
        applySort(all, query.getSort(), aggregates);
        long total = all.size();
        int from = (page - 1) * size;
        List<AdvisorStudentVO> pageList = from >= all.size()
                ? List.of()
                : all.subList(from, Math.min(from + size, all.size()));
        return PageResult.of(pageList, total, page, size);
    }

    @Override
    public StudentDetailViewVO getDetail(String advisorId, String studentId) {
        advisorScopeService.assertAssigned(advisorId, studentId);
        StudentProfile profile = studentProfileMapper.findByUserId(studentId);
        if (profile == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "学生不存在");
        }
        StudentDetailViewVO vo = new StudentDetailViewVO();
        vo.setProfile(toProfileVO(profile));
        vo.setPortrait(buildPortrait(studentId));
        vo.setRecommendation(buildRecommendation(studentId));
        vo.setGoal(buildGoal(studentId));
        PlanRow planRow = queryMapper.selectLatestConfirmedPlan(studentId);
        vo.setPlan(planRow == null ? null : buildPlan(planRow));
        vo.setTasks(planRow == null ? List.of() : buildTasks(planRow.getId()));
        vo.setReviews(buildReviews(studentId));
        vo.setGuidance(advisorCommentMapper.findByStudentId(studentId).stream()
                .map(this::toGuidanceVO)
                .toList());
        return vo;
    }

    @Override
    public List<AttentionStudentVO> listAttention(String advisorId) {
        LocalDateTime now = LocalDateTime.now();
        List<String> ids = relationMapper.findStudentIdsByAdvisor(advisorId);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, StudentAggregate> aggregates = loadAggregates(ids, now);
        List<AttentionStudentVO> result = new ArrayList<>();
        for (StudentAggregate a : aggregates.values()) {
            List<String> reasons = new ArrayList<>();
            if (a.askGuidance) {
                reasons.add("已申请辅导员指导");
            }
            if (a.longNoReview) {
                reasons.add("长期未复盘");
            }
            if (a.goalChangeCount >= GOAL_CHANGE_LIMIT) {
                reasons.add("多次调整目标");
            }
            if (!reasons.isEmpty()) {
                AttentionStudentVO item = new AttentionStudentVO();
                item.setStudent(a.vo);
                item.setReasons(reasons);
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public AdvisorStatisticsVO getStatistics(String advisorId) {
        LocalDateTime now = LocalDateTime.now();
        AdvisorStatisticsVO vo = new AdvisorStatisticsVO();
        vo.setTotalStudents(queryMapper.countAssignedStudents(advisorId));
        vo.setAssessedCount(queryMapper.countAssessedStudents(advisorId));
        vo.setPlanMadeCount(queryMapper.countPlanMadeStudents(advisorId));
        vo.setReviewedCount(queryMapper.countReviewedThisMonthStudents(
                advisorId, now.withDayOfMonth(1).toLocalDate().atStartOfDay()));
        Map<String, Long> counts = new HashMap<>();
        for (PathCountRow row : queryMapper.selectPathCounts(advisorId)) {
            counts.put(row.getPath(), row.getCount());
        }
        List<AdvisorStatisticsVO.PathDistribution> distribution = new ArrayList<>();
        for (String path : PATH_BUCKETS) {
            AdvisorStatisticsVO.PathDistribution item = new AdvisorStatisticsVO.PathDistribution();
            item.setPath(path);
            item.setCount(counts.getOrDefault(path, 0L).intValue());
            distribution.add(item);
        }
        vo.setPathDistribution(distribution);
        double sum = 0;
        int n = 0;
        for (PlanRateRow row : queryMapper.selectCompletionRates(advisorId)) {
            if (row.getTotalTasks() != null && row.getTotalTasks() > 0) {
                sum += row.getDoneTasks() * 100.0 / row.getTotalTasks();
                n++;
            }
        }
        vo.setTaskCompletionRate(n == 0 ? null : Math.round(sum / n * 10) / 10.0);
        return vo;
    }

    // ---------- 列表聚合 ----------

    private Map<String, StudentAggregate> loadAggregates(List<String> ids, LocalDateTime now) {
        Map<String, StudentAggregate> result = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        LocalDateTime thirtyDaysAgo = now.minusDays(LONG_NO_REVIEW_DAYS);
        Set<String> assessed = Set.copyOf(queryMapper.selectAssessedStudentIds(ids));
        Map<String, ActiveGoalRow> primaryGoals = new HashMap<>();
        Map<String, ActiveGoalRow> backupGoals = new HashMap<>();
        for (ActiveGoalRow goal : queryMapper.selectActiveGoals(ids)) {
            if ("PRIMARY".equals(goal.getGoalType())) {
                primaryGoals.put(goal.getStudentId(), goal);
            } else if ("BACKUP".equals(goal.getGoalType())) {
                backupGoals.put(goal.getStudentId(), goal);
            }
        }
        Map<String, PlanRateRow> planRates = queryMapper.selectPlanRate(ids).stream()
                .collect(Collectors.toMap(PlanRateRow::getStudentId, Function.identity()));
        Map<String, LocalDateTime> lastReviews = queryMapper.selectLastReview(ids).stream()
                .collect(Collectors.toMap(LastReviewRow::getStudentId, LastReviewRow::getLastReviewAt));
        Set<String> pendingGuidance = Set.copyOf(queryMapper.selectPendingGuidanceStudentIds(ids));
        Map<String, Long> goalChanges = queryMapper.selectGoalChangeCounts(ids, now.minusDays(GOAL_CHANGE_DAYS)).stream()
                .collect(Collectors.toMap(GoalChangeRow::getStudentId, GoalChangeRow::getChangeCount));
        Map<String, ProfileRow> profiles = queryMapper.selectProfileBasics(ids).stream()
                .collect(Collectors.toMap(ProfileRow::getStudentId, Function.identity()));

        for (String studentId : ids) {
            ProfileRow profile = profiles.get(studentId);
            if (profile == null) {
                continue;
            }
            ActiveGoalRow primary = primaryGoals.get(studentId);
            PlanRateRow rate = planRates.get(studentId);
            LocalDateTime lastReviewAt = lastReviews.get(studentId);
            boolean askGuidance = pendingGuidance.contains(studentId);
            boolean longNoReview = lastReviewAt == null || lastReviewAt.isBefore(thirtyDaysAgo);
            boolean hasGoal = primary != null;
            boolean isAssessed = assessed.contains(studentId);
            long changeCount = goalChanges.getOrDefault(studentId, 0L);

            AdvisorStudentVO vo = new AdvisorStudentVO();
            vo.setId(studentId);
            vo.setName(profile.getName());
            vo.setClassName(profile.getClassName());
            vo.setCompleteness(profile.getCompleteness());
            vo.setAssessed(isAssessed);
            vo.setPath(PATH_ENUM.contains(profile.getDevelopmentIntention()) ? profile.getDevelopmentIntention() : null);
            vo.setDirection(primary == null ? null : primary.getDirectionName());
            vo.setPrimaryGoal(primary == null ? null : primary.getGoalName());
            vo.setPlanRate(rate == null || rate.getTotalTasks() == null || rate.getTotalTasks() == 0
                    ? null
                    : (int) Math.round(rate.getDoneTasks() * 100.0 / rate.getTotalTasks()));
            vo.setLastReview(lastReviewAt == null ? null : lastReviewAt.toLocalDate());
            vo.setAskGuidance(askGuidance);
            vo.setStatus(computeStatus(askGuidance, longNoReview, isAssessed, hasGoal));

            StudentAggregate aggregate = new StudentAggregate();
            aggregate.vo = vo;
            aggregate.askGuidance = askGuidance;
            aggregate.longNoReview = longNoReview;
            aggregate.hasGoal = hasGoal;
            aggregate.assessed = isAssessed;
            aggregate.goalChangeCount = changeCount;
            aggregate.createdAt = profile.getUpdatedAt();
            result.put(studentId, aggregate);
        }
        return result;
    }

    private String computeStatus(boolean askGuidance, boolean longNoReview, boolean assessed, boolean hasGoal) {
        if (askGuidance) {
            return "review";
        }
        if (longNoReview) {
            return "late";
        }
        if (!assessed || !hasGoal) {
            return "todo";
        }
        return "good";
    }

    private void applySort(List<AdvisorStudentVO> list, String sort, Map<String, StudentAggregate> aggregates) {
        if (list.size() <= 1) {
            return;
        }
        String field = "createdAt";
        boolean desc = true;
        if (StringUtils.hasText(sort)) {
            String raw = sort.startsWith("-") ? sort.substring(1) : sort;
            if (SORTABLE_FIELDS.contains(raw)) {
                field = raw;
                desc = sort.startsWith("-");
            }
        }
        Comparator<AdvisorStudentVO> comparator = switch (field) {
            case "name" -> Comparator.comparing(AdvisorStudentVO::getName, Comparator.nullsLast(String::compareTo));
            case "className" -> Comparator.comparing(AdvisorStudentVO::getClassName, Comparator.nullsLast(String::compareTo));
            case "completeness" -> Comparator.comparing(AdvisorStudentVO::getCompleteness, Comparator.nullsLast(Integer::compareTo));
            case "planRate" -> Comparator.comparing(AdvisorStudentVO::getPlanRate, Comparator.nullsLast(Integer::compareTo));
            case "lastReview" -> Comparator.comparing(AdvisorStudentVO::getLastReview, Comparator.nullsLast(LocalDate::compareTo));
            default -> Comparator.comparing(
                    (AdvisorStudentVO v) -> aggregates.get(v.getId()) == null ? null : aggregates.get(v.getId()).createdAt,
                    Comparator.nullsLast(LocalDateTime::compareTo));
        };
        if (desc) {
            comparator = comparator.reversed();
        }
        list.sort(comparator);
    }

    // ---------- 详情组装 ----------

    private StudentProfileVO toProfileVO(StudentProfile profile) {
        List<ExperienceVO> experiences = studentExperienceMapper.findAllByStudentId(profile.getUserId()).stream()
                .map(this::toExperienceVO)
                .toList();
        return StudentProfileVO.builder()
                .userId(profile.getUserId())
                .name(profile.getName())
                .className(profile.getClassName())
                .grade(profile.getGrade())
                .majorCategory(profile.getMajorCategory())
                .basic(profile.getBasic())
                .academic(profile.getAcademic())
                .interestPrefs(profile.getInterestPrefs())
                .abilitySelf(profile.getAbilitySelf())
                .values(profile.getValues())
                .experiences(experiences)
                .developmentIntention(profile.getDevelopmentIntention())
                .constraints(profile.getConstraints())
                .completeness(profile.getCompleteness())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private ExperienceVO toExperienceVO(StudentExperience e) {
        return ExperienceVO.builder()
                .id(e.getId())
                .type(e.getType())
                .title(e.getTitle())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .description(e.getDescription())
                .attachmentUrl(e.getAttachmentUrl())
                .build();
    }

    private ProfileSnapshotVO buildPortrait(String studentId) {
        SnapshotRow row = queryMapper.selectLatestSnapshot(studentId);
        if (row == null) {
            return null;
        }
        ProfileSnapshotVO vo = new ProfileSnapshotVO();
        vo.setId(row.getId());
        vo.setVersion(row.getVersionNo());
        vo.setGeneratedAt(row.getCreatedAt());
        vo.setSourceVersion(row.getSourceVersion());
        vo.setCompleteness(row.getCompleteness());
        vo.setDimensions(parseList(row.getDimensionJson(), new TypeReference<List<ProfileSnapshotVO.DimensionValue>>() {
        }));
        vo.setSummary(row.getSummary());
        vo.setStrengths(parseList(row.getStrengthsJson(), new TypeReference<List<String>>() {
        }));
        vo.setExplore(parseList(row.getExploreJson(), new TypeReference<List<String>>() {
        }));
        vo.setFeedback(parseObject(row.getFeedbackJson(), ProfileSnapshotVO.ProfileFeedback.class));
        return vo;
    }

    private RecommendationRunVO buildRecommendation(String studentId) {
        RunRow run = queryMapper.selectLatestRun(studentId);
        if (run == null) {
            return null;
        }
        RecommendationRunVO vo = new RecommendationRunVO();
        vo.setRunId(run.getId());
        vo.setProfileVersion(run.getProfileVersion());
        vo.setRuleVersion(run.getRuleVersion());
        vo.setGeneratedAt(run.getGeneratedAt());
        vo.setStatus(run.getStatus());
        List<RecommendationRunVO.RecommendationResult> results = new ArrayList<>();
        for (ResultRow row : queryMapper.selectResultsByRunId(run.getId())) {
            RecommendationRunVO.RecommendationResult r = new RecommendationRunVO.RecommendationResult();
            r.setDirectionId(row.getDirectionId());
            r.setRank(row.getRank());
            r.setScore(row.getScore());
            r.setConfidence(row.getConfidence());
            r.setReasons(parseList(row.getReasonsJson(), new TypeReference<List<String>>() {
            }));
            r.setStrengths(parseList(row.getStrengthsJson(), new TypeReference<List<String>>() {
            }));
            r.setGaps(parseList(row.getGapsJson(), new TypeReference<List<String>>() {
            }));
            r.setSemesterActions(parseList(row.getSemesterActionsJson(), new TypeReference<List<String>>() {
            }));
            r.setFeedback(parseObject(row.getFeedbackJson(), RecommendationRunVO.RecommendationFeedback.class));
            results.add(r);
        }
        vo.setResults(results);
        return vo;
    }

    private GoalVO buildGoal(String studentId) {
        List<ActiveGoalRow> goals = queryMapper.selectActiveGoals(List.of(studentId));
        if (goals.isEmpty()) {
            return null;
        }
        GoalVO vo = new GoalVO();
        ActiveGoalRow primary = goals.stream()
                .filter(g -> "PRIMARY".equals(g.getGoalType()))
                .findFirst().orElse(null);
        ActiveGoalRow backup = goals.stream()
                .filter(g -> "BACKUP".equals(g.getGoalType()))
                .findFirst().orElse(null);
        if (primary != null) {
            vo.setPrimary(toGoalItem(primary));
            vo.setVersion("v" + (primary.getVersionNo() == null ? 1 : primary.getVersionNo()));
            vo.setUpdatedAt(primary.getUpdatedAt());
        }
        if (backup != null) {
            vo.setBackup(toGoalItem(backup));
        }
        return vo;
    }

    private GoalVO.GoalItem toGoalItem(ActiveGoalRow row) {
        GoalVO.GoalItem item = new GoalVO.GoalItem();
        item.setDirectionId(row.getDirectionId());
        item.setName(row.getGoalName());
        item.setChosenAt(row.getChosenAt());
        return item;
    }

    private PlanVO buildPlan(PlanRow row) {
        PlanVO vo = new PlanVO();
        vo.setId(row.getId());
        vo.setVersion("P-v" + (row.getVersionNo() == null ? 1 : row.getVersionNo()));
        vo.setStatus(row.getStatus());
        vo.setSource(row.getSource());
        vo.setGoalSummary(row.getGoalSummary());
        vo.setSemesterGoals(parseList(row.getSemesterGoalsJson(), new TypeReference<List<PlanVO.SemesterGoal>>() {
        }));
        vo.setMonthlyTasks(parseList(row.getMonthlyTasksJson(), new TypeReference<List<PlanVO.MonthlyTask>>() {
        }));
        vo.setNotes(parseList(row.getNotesJson(), new TypeReference<List<String>>() {
        }));
        vo.setConfirmedAt(row.getConfirmedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private List<TaskVO> buildTasks(String planId) {
        List<TaskRow> tasks = queryMapper.selectTasksByPlanId(planId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<String> taskIds = tasks.stream().map(TaskRow::getId).toList();
        Map<String, CheckinRow> latestCheckins = new HashMap<>();
        for (CheckinRow checkin : queryMapper.selectCheckinsByTaskIds(taskIds)) {
            CheckinRow current = latestCheckins.get(checkin.getTaskId());
            if (current == null
                    || (checkin.getCheckedInAt() != null
                    && (current.getCheckedInAt() == null || checkin.getCheckedInAt().isAfter(current.getCheckedInAt())))) {
                latestCheckins.put(checkin.getTaskId(), checkin);
            }
        }
        List<TaskVO> result = new ArrayList<>();
        for (TaskRow task : tasks) {
            TaskVO vo = new TaskVO();
            vo.setId(task.getId());
            vo.setMonth(task.getMonth());
            vo.setTitle(task.getTitle());
            vo.setType(task.getTaskType());
            vo.setEstHours(task.getEstHours());
            vo.setStatus(task.getStatus());
            vo.setDeadline(task.getDeadline());
            vo.setAbilityTags(parseList(task.getAbilityTagsJson(), new TypeReference<List<String>>() {
            }));
            vo.setNote(task.getNote());
            CheckinRow checkin = latestCheckins.get(task.getId());
            if (checkin != null) {
                TaskVO.TaskCheckin checkinVO = new TaskVO.TaskCheckin();
                checkinVO.setId(checkin.getId());
                checkinVO.setTaskId(checkin.getTaskId());
                checkinVO.setDoneDesc(checkin.getDoneDesc());
                checkinVO.setGains(checkin.getGains());
                checkinVO.setDifficulties(checkin.getDifficulties());
                checkinVO.setProofUrl(checkin.getProofUrl());
                checkinVO.setCheckedInAt(checkin.getCheckedInAt());
                vo.setCheckin(checkinVO);
                vo.setCheckedInAt(checkin.getCheckedInAt());
            }
            result.add(vo);
        }
        return result;
    }

    private List<ReviewVO> buildReviews(String studentId) {
        List<ReviewVO> result = new ArrayList<>();
        for (ReviewRow row : queryMapper.selectSubmittedReviews(studentId)) {
            ReviewVO vo = new ReviewVO();
            vo.setId(row.getId());
            vo.setCycle(row.getCycle());
            vo.setStatus(row.getStatus());
            vo.setContent(parseObject(row.getContentJson(), ReviewVO.ReviewContent.class));
            vo.setAiSummary(row.getAiSummary());
            vo.setAiSuggest(parseList(row.getAiSuggestJson(), new TypeReference<List<String>>() {
            }));
            vo.setAdvisorRequested(row.getAdvisorRequested());
            vo.setAdvisorReply(row.getAdvisorReply());
            vo.setSubmittedAt(row.getSubmittedAt());
            result.add(vo);
        }
        return result;
    }

    private GuidanceCommentVO toGuidanceVO(AdvisorComment comment) {
        GuidanceCommentVO vo = new GuidanceCommentVO();
        vo.setId(comment.getId());
        vo.setStudentId(comment.getStudentId());
        vo.setContent(comment.getContent());
        vo.setAdviceType(comment.getAdviceType());
        vo.setSuggestedTask(comment.getSuggestedTask());
        vo.setRetestReason(comment.getRetestReason());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }

    // ---------- 参数校验与工具 ----------

    private String validatePath(String path) {
        if (StringUtils.hasText(path) && !PATH_ENUM.contains(path)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "path 仅支持 graduate/employment/overseas");
        }
        return path;
    }

    private Boolean parseGoalStatus(String goalStatus) {
        if (!StringUtils.hasText(goalStatus)) {
            return null;
        }
        if (!GOAL_STATUS_ENUM.contains(goalStatus)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "goalStatus 仅支持 HAS_GOAL/NO_GOAL");
        }
        return "HAS_GOAL".equals(goalStatus);
    }

    /** reviewed=true 时返回 REVIEWED_THIS_MONTH 是否命中;reviewed=false 时返回 LONG_NO_REVIEW 是否命中 */
    private Boolean parseReviewStatus(String reviewStatus, boolean reviewed) {
        if (!StringUtils.hasText(reviewStatus)) {
            return null;
        }
        if (!REVIEW_STATUS_ENUM.contains(reviewStatus)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "reviewStatus 仅支持 REVIEWED_THIS_MONTH/LONG_NO_REVIEW");
        }
        return reviewed
                ? "REVIEWED_THIS_MONTH".equals(reviewStatus)
                : "LONG_NO_REVIEW".equals(reviewStatus);
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
        return StringUtils.hasText(json) ? JsonUtil.parse(json, typeReference) : List.of();
    }

    private <T> T parseObject(String json, Class<T> type) {
        return StringUtils.hasText(json) ? JsonUtil.parse(json, type) : null;
    }

    private static class StudentAggregate {
        private AdvisorStudentVO vo;
        private boolean askGuidance;
        private boolean longNoReview;
        private boolean hasGoal;
        private boolean assessed;
        private long goalChangeCount;
        private LocalDateTime createdAt;
    }
}
