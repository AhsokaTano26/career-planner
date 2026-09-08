package com.rickgao.careercore.modules.planning.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.advisor.vo.GoalVO;
import com.rickgao.careercore.modules.advisor.vo.PlanVO;
import com.rickgao.careercore.modules.advisor.vo.ReviewVO;
import com.rickgao.careercore.modules.advisor.vo.TaskVO;
import com.rickgao.careercore.modules.ai.dto.AiPlanGenerateRequest;
import com.rickgao.careercore.modules.ai.dto.AiReviewSummarizeRequest;
import com.rickgao.careercore.modules.ai.service.AiService;
import com.rickgao.careercore.modules.ai.vo.AiMonthlyTaskVO;
import com.rickgao.careercore.modules.ai.vo.AiPlanResultVO;
import com.rickgao.careercore.modules.ai.vo.AiReviewSummaryVO;
import com.rickgao.careercore.modules.ai.vo.AiSemesterGoalVO;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.planning.dto.AdoptAdviceRequest;
import com.rickgao.careercore.modules.planning.dto.GoalRequest;
import com.rickgao.careercore.modules.planning.dto.GuidanceRequestPayload;
import com.rickgao.careercore.modules.planning.dto.PlanConfirmRequest;
import com.rickgao.careercore.modules.planning.dto.PlanDraftRequest;
import com.rickgao.careercore.modules.planning.dto.PlanUpdateRequest;
import com.rickgao.careercore.modules.planning.dto.ReviewDraftRequest;
import com.rickgao.careercore.modules.planning.dto.TaskCheckinRequest;
import com.rickgao.careercore.modules.planning.dto.TaskRequest;
import com.rickgao.careercore.modules.planning.dto.TaskStatusUpdate;
import com.rickgao.careercore.modules.planning.entity.GoalVersion;
import org.springframework.context.annotation.Lazy;
import com.rickgao.careercore.modules.planning.entity.PlanTask;
import com.rickgao.careercore.modules.planning.entity.PlanVersion;
import com.rickgao.careercore.modules.planning.entity.Reminder;
import com.rickgao.careercore.modules.planning.entity.SemesterPlan;
import com.rickgao.careercore.modules.planning.entity.StageReview;
import com.rickgao.careercore.modules.planning.entity.StudentGoal;
import com.rickgao.careercore.modules.planning.entity.TaskCheckin;
import com.rickgao.careercore.modules.planning.mapper.PlanningMapper;
import com.rickgao.careercore.modules.planning.service.PlanningService;
import com.rickgao.careercore.modules.planning.vo.GoalVersionVO;
import com.rickgao.careercore.modules.planning.vo.ReminderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 规划模块业务实现。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - AI 计划生成 / 复盘总结调用 AiService（未配置 LLM_API_KEY 时抛 BizException），此处捕获后回退为模板 / 默认文案；
 *  - 提醒生成采用"近 7 天截止任务 + 未提交复盘"的简化规则，后续可扩展定时调度。
 */
@Service
public class PlanningServiceImpl implements PlanningService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PlanningMapper mapper;
    private final AdminDirectionMapper directionMapper;
    private final AiService aiService;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;

    private final PlanningServiceImpl self;

    public PlanningServiceImpl(PlanningMapper mapper,
                               AdminDirectionMapper directionMapper,
                               AiService aiService,
                               IdGenerator idGenerator,
                               IdempotencyService idempotencyService,
                               @Lazy PlanningServiceImpl self) {
        this.mapper = mapper;
        this.directionMapper = directionMapper;
        this.aiService = aiService;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
        this.self = self;
    }

    // ================================================================ 目标

    @Override
    public GoalVO getGoals(String studentId) {
        List<StudentGoal> goals = mapper.selectGoals(studentId).stream()
                .filter(g -> "ACTIVE".equals(g.getStatus()))
                .toList();
        if (goals.isEmpty()) {
            return null;
        }
        return toGoalVO(goals);
    }

    @Override
    @Transactional
    public GoalVO setGoal(String studentId, GoalRequest req) {
        if (!StringUtils.hasText(req.getPrimaryDirectionId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "primaryDirectionId 不能为空");
        }
        return applyGoalChange(studentId, req, false);
    }

    @Override
    @Transactional
    public GoalVO changeGoal(String studentId, GoalRequest req) {
        if (!StringUtils.hasText(req.getPrimaryDirectionId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "primaryDirectionId 不能为空");
        }
        return applyGoalChange(studentId, req, true);
    }

    private GoalVO applyGoalChange(String studentId, GoalRequest req, boolean isChange) {
        List<StudentGoal> existing = mapper.selectGoals(studentId).stream()
                .filter(g -> "ACTIVE".equals(g.getStatus()))
                .toList();
        int nextVersion = existing.stream()
                .map(StudentGoal::getVersionNo)
                .filter(v -> v != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        for (StudentGoal g : existing) {
            mapper.disableGoal(g.getId());
        }

        List<StudentGoal> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        StudentGoal primary = createGoalRow(studentId, "PRIMARY", req.getPrimaryDirectionId(), nextVersion, now);
        mapper.insertGoal(primary);
        created.add(primary);

        if (StringUtils.hasText(req.getBackupDirectionId())) {
            StudentGoal backup = createGoalRow(studentId, "BACKUP", req.getBackupDirectionId(), nextVersion, now);
            mapper.insertGoal(backup);
            created.add(backup);
        }

        for (StudentGoal g : created) {
            GoalVersion v = new GoalVersion();
            v.setId(idGenerator.goalVersionId());
            v.setGoalId(g.getId());
            v.setVersionNo(nextVersion);
            v.setChangeReason(req.getChangeReason());
            v.setCreatedAt(now);
            mapper.insertGoalVersion(v);
        }
        return toGoalVO(created);
    }

    private StudentGoal createGoalRow(String studentId, String goalType, String directionId,
                                      int versionNo, LocalDateTime now) {
        CareerDirection direction = directionMapper.findById(directionId);
        if (direction == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向不存在：" + directionId);
        }
        StudentGoal goal = new StudentGoal();
        goal.setId(idGenerator.studentGoalId());
        goal.setStudentId(studentId);
        goal.setGoalType(goalType);
        goal.setDirectionId(directionId);
        goal.setName(direction.getName());
        goal.setStatus("ACTIVE");
        goal.setChosenAt(now);
        goal.setVersionNo(versionNo);
        goal.setCreatedAt(now);
        goal.setUpdatedAt(now);
        return goal;
    }

    private GoalVO toGoalVO(List<StudentGoal> goals) {
        GoalVO vo = new GoalVO();
        for (StudentGoal g : goals) {
            if ("PRIMARY".equals(g.getGoalType())) {
                GoalVO.GoalItem item = new GoalVO.GoalItem();
                item.setDirectionId(g.getDirectionId());
                item.setName(g.getName());
                item.setChosenAt(g.getChosenAt());
                vo.setPrimary(item);
                vo.setVersion("v" + (g.getVersionNo() == null ? 1 : g.getVersionNo()));
                vo.setUpdatedAt(g.getUpdatedAt());
            } else if ("BACKUP".equals(g.getGoalType())) {
                GoalVO.GoalItem item = new GoalVO.GoalItem();
                item.setDirectionId(g.getDirectionId());
                item.setName(g.getName());
                item.setChosenAt(g.getChosenAt());
                vo.setBackup(item);
            }
        }
        return vo;
    }

    @Override
    public List<GoalVersionVO> listGoalVersions(String studentId) {
        List<StudentGoal> goals = mapper.selectGoals(studentId);
        if (goals.isEmpty()) {
            return List.of();
        }
        // 目标版本按 PRIMARY 目标的历史记录聚合展示
        StudentGoal primary = goals.stream()
                .filter(g -> "PRIMARY".equals(g.getGoalType()) && "ACTIVE".equals(g.getStatus()))
                .findFirst()
                .orElse(goals.get(0));
        List<GoalVersion> versions = mapper.selectGoalVersions(primary.getId());
        if (versions.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<GoalVersion>> byVersion = versions.stream()
                .collect(Collectors.groupingBy(v -> v.getVersionNo() == null ? 0 : v.getVersionNo()));
        List<GoalVersionVO> result = new ArrayList<>();
        for (Map.Entry<Integer, List<GoalVersion>> e : byVersion.entrySet()) {
            GoalVersionVO vo = new GoalVersionVO();
            vo.setVersion("v" + e.getKey());
            vo.setChangeReason(e.getValue().get(0).getChangeReason());
            vo.setChangedAt(e.getValue().get(0).getCreatedAt());
            vo.setChangedBy("student");
            // 当前版本记录主/备方向快照
            List<StudentGoal> snapshot = goals.stream()
                    .filter(g -> e.getKey().equals(g.getVersionNo()))
                    .toList();
            snapshot.stream().filter(g -> "PRIMARY".equals(g.getGoalType())).findFirst()
                    .ifPresent(g -> vo.setPrimaryDirectionId(g.getDirectionId()));
            snapshot.stream().filter(g -> "BACKUP".equals(g.getGoalType())).findFirst()
                    .ifPresent(g -> vo.setBackupDirectionId(g.getDirectionId()));
            result.add(vo);
        }
        result.sort((a, b) -> b.getVersion().compareTo(a.getVersion()));
        return result;
    }

    // ================================================================ 计划

    @Override
    public PlanVO getLatestPlan(String studentId) {
        SemesterPlan plan = mapper.selectLatestPlan(studentId);
        return plan == null ? null : toPlanVO(plan);
    }

    @Override
    public PlanVO getPlan(String studentId, String planId) {
        SemesterPlan plan = mapper.selectPlanById(planId);
        if (plan == null || !studentId.equals(plan.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "计划不存在");
        }
        return toPlanVO(plan);
    }

    @Override
    public List<PlanVO> listPlans(String studentId) {
        return mapper.selectPlansByStudent(studentId).stream()
                .map(this::toPlanVO)
                .toList();
    }

    @Override
    public PlanVO generateDraft(String studentId, PlanDraftRequest req) {
        // 稳定性：远程 LLM 调用必须在事务外（否则长持 DB 连接）；落库走代理进事务
        String directionId = req.getDirectionId();
        if (!StringUtils.hasText(directionId)) {
            directionId = primaryDirectionId(studentId);
        }
        CareerDirection direction = directionId == null ? null : directionMapper.findById(directionId);
        String goalSummary = direction == null ? "围绕目标方向打好基础，完成一个小项目" : "围绕「" + direction.getName() + "」方向完成一学期学习与一个小项目";

        List<PlanVO.SemesterGoal> semesterGoals;
        List<PlanVO.MonthlyTask> monthlyTasks;
        List<String> notes;
        String source;

        if (Boolean.TRUE.equals(req.getUseAi())) {
            AiPlanResultVO ai = safeGeneratePlan(studentId, directionId, goalSummary);
            semesterGoals = ai.getSemesterGoals().stream()
                    .map(g -> {
                        PlanVO.SemesterGoal sg = new PlanVO.SemesterGoal();
                        sg.setTitle(g.getTitle());
                        sg.setAbilityTag(g.getAbilityTag());
                        return sg;
                    }).collect(Collectors.toCollection(ArrayList::new));
            monthlyTasks = ai.getMonthlyTasks().stream()
                    .map(this::toPlanMonthlyTask)
                    .collect(Collectors.toCollection(ArrayList::new));
            notes = ai.getNotes() == null ? List.of() : ai.getNotes();
            source = "AI";
        } else {
            semesterGoals = new ArrayList<>();
            PlanVO.SemesterGoal sg = new PlanVO.SemesterGoal();
            sg.setTitle(goalSummary);
            sg.setAbilityTag(direction == null ? null : abilityTagsOf(direction));
            semesterGoals.add(sg);
            monthlyTasks = buildDefaultMonthlyTasks();
            notes = List.of("模板生成，可手动调整");
            source = "TEMPLATE";
        }

        // 保存为新 DRAFT 计划（事务内多写原子）
        return self.persistDraft(studentId, goalSummary, semesterGoals, monthlyTasks, notes, source);
    }

    @Transactional
    public PlanVO persistDraft(String studentId, String goalSummary,
                               List<PlanVO.SemesterGoal> semesterGoals,
                               List<PlanVO.MonthlyTask> monthlyTasks,
                               List<String> notes, String source) {
        SemesterPlan plan = new SemesterPlan();
        plan.setId(idGenerator.semesterPlanId());
        plan.setStudentId(studentId);
        plan.setVersionNo(1);
        plan.setStatus("DRAFT");
        plan.setSource(source);
        plan.setGoalSummary(goalSummary);
        plan.setSemesterGoalsJson(JsonUtil.toJson(semesterGoals));
        plan.setMonthlyTasksJson(JsonUtil.toJson(monthlyTasks));
        plan.setNotesJson(JsonUtil.toJson(notes));
        plan.setCreatedAt(LocalDateTime.now());
        mapper.insertPlan(plan);

        // 落库月度任务为 plan_task
        saveTasksFromMonthly(studentId, plan.getId(), monthlyTasks);

        return toPlanVO(plan);
    }

    @Override
    @Transactional
    public PlanVO confirmPlan(String studentId, PlanConfirmRequest req) {
        // 复审加固：加锁读，并发确认串行化
        SemesterPlan draft = mapper.selectLatestPlanByStatusForUpdate(studentId, "DRAFT");
        if (draft == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "没有可确认的计划草案");
        }
        if (!studentId.equals(draft.getStudentId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作该计划");
        }
        if (Boolean.FALSE.equals(req.getConfirm())) {
            // 不确认：保持 DRAFT
            return toPlanVO(draft);
        }
        draft.setStatus("CONFIRMED");
        draft.setConfirmedAt(LocalDateTime.now());
        mapper.updatePlan(draft);
        savePlanVersion(draft);
        return toPlanVO(draft);
    }

    @Override
    @Transactional
    public PlanVO updatePlan(String studentId, PlanUpdateRequest req) {
        // 复审加固：加锁读，并发编辑串行化，防静默后写覆盖
        SemesterPlan plan = mapper.selectLatestPlanByStatusForUpdate(studentId, "DRAFT");
        if (plan == null) {
            plan = mapper.selectLatestPlanByStatusForUpdate(studentId, "CONFIRMED");
        }
        if (plan == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "没有可编辑的计划");
        }
        if (!studentId.equals(plan.getStudentId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作该计划");
        }
        plan.setSource("MANUAL");
        plan.setStatus(plan.getStatus() == null ? "DRAFT" : plan.getStatus());
        if (req.getGoalSummary() != null) {
            plan.setGoalSummary(req.getGoalSummary());
        }
        if (req.getSemesterGoals() != null) {
            plan.setSemesterGoalsJson(JsonUtil.toJson(req.getSemesterGoals()));
        }
        if (req.getMonthlyTasks() != null) {
            plan.setMonthlyTasksJson(JsonUtil.toJson(req.getMonthlyTasks()));
        }
        if (req.getNotes() != null) {
            plan.setNotesJson(JsonUtil.toJson(req.getNotes()));
        }
        mapper.updatePlan(plan);
        savePlanVersion(plan);
        return toPlanVO(plan);
    }

    private void savePlanVersion(SemesterPlan plan) {
        PlanVersion pv = new PlanVersion();
        pv.setId(idGenerator.planVersionId());
        pv.setPlanId(plan.getId());
        pv.setVersionNo(plan.getVersionNo() == null ? 1 : plan.getVersionNo());
        pv.setContentJson(JsonUtil.toJson(plan));
        pv.setCreatedAt(LocalDateTime.now());
        mapper.insertPlanVersion(pv);
    }

    private void saveTasksFromMonthly(String studentId, String planId, List<PlanVO.MonthlyTask> monthlyTasks) {
        for (PlanVO.MonthlyTask mt : monthlyTasks) {
            PlanTask task = new PlanTask();
            task.setId(idGenerator.planTaskId());
            task.setPlanId(planId);
            task.setStudentId(studentId);
            task.setMonth(mt.getMonth());
            task.setTitle(mt.getTitle());
            task.setTaskType(mt.getTaskType() == null ? "LEARNING" : mt.getTaskType());
            task.setEstHours(mt.getEstimatedHours() == null ? null : BigDecimal.valueOf(mt.getEstimatedHours()));
            task.setStatus("PENDING");
            task.setCreatedAt(LocalDateTime.now());
            mapper.insertTask(task);
        }
    }

    private AiPlanResultVO safeGeneratePlan(String studentId, String directionId, String goalSummary) {
        AiPlanGenerateRequest req = new AiPlanGenerateRequest();
        req.setStudentRef(studentId);
        req.setDirectionId(directionId);
        req.setGoalSummary(goalSummary);
        try {
            return aiService.generatePlan(req);
        } catch (BizException exc) {
            // Demo 精简点：AI 不可用时回退为模板计划
            return fallbackPlan();
        }
    }

    private AiPlanResultVO fallbackPlan() {
        return AiPlanResultVO.builder()
                .goalSummary("围绕目标方向完成一学期学习与一个小项目")
                .semesterGoals(List.of(AiSemesterGoalVO.builder().title("打好方向基础").build()))
                .monthlyTasks(buildDefaultAiTasks())
                .notes(List.of("模板生成"))
                .build();
    }

    private List<PlanVO.MonthlyTask> buildDefaultMonthlyTasks() {
        String[] titles = {"完成基础课程学习", "参与一个项目实践", "参加行业讲座", "整理学习心得与复盘"};
        String[] types = {"LEARNING", "PRACTICE", "CAREER", "REVIEW"};
        List<PlanVO.MonthlyTask> list = new ArrayList<>();
        LocalDate base = LocalDate.now().withDayOfMonth(1);
        for (int i = 0; i < 4; i++) {
            PlanVO.MonthlyTask mt = new PlanVO.MonthlyTask();
            mt.setMonth(base.plusMonths(i).format(MONTH_FMT));
            mt.setTitle(titles[i]);
            mt.setTaskType(types[i]);
            mt.setEstimatedHours(20.0);
            list.add(mt);
        }
        return list;
    }

    private List<AiMonthlyTaskVO> buildDefaultAiTasks() {
        List<PlanVO.MonthlyTask> tasks = buildDefaultMonthlyTasks();
        return tasks.stream().map(mt -> AiMonthlyTaskVO.builder()
                .month(mt.getMonth())
                .title(mt.getTitle())
                .taskType(mt.getTaskType())
                .estimatedHours(mt.getEstimatedHours())
                .build()).toList();
    }

    private PlanVO.MonthlyTask toPlanMonthlyTask(AiMonthlyTaskVO mt) {
        PlanVO.MonthlyTask m = new PlanVO.MonthlyTask();
        m.setMonth(mt.getMonth());
        m.setTitle(mt.getTitle());
        m.setTaskType(mt.getTaskType());
        m.setEstimatedHours(mt.getEstimatedHours());
        return m;
    }

    private String primaryDirectionId(String studentId) {
        StudentGoal primary = mapper.selectGoalByType(studentId, "PRIMARY");
        return primary == null ? null : primary.getDirectionId();
    }

    private String abilityTagsOf(CareerDirection direction) {
        // Demo 精简点：取能力要求标签数组首项作为默认能力标签
        List<String> tags = parseStringList(direction.getAbilitiesJson());
        return tags.isEmpty() ? null : tags.get(0);
    }

    // ================================================================ 任务

    @Override
    public PageResult<TaskVO> listTasks(String studentId, String month, String status, int page, int size) {
        int p = page < 1 ? DEFAULT_PAGE : page;
        int s = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int offset = (p - 1) * s;
        List<PlanTask> tasks = mapper.selectTasksByStudent(studentId, month, status, offset, s);
        // 稳定性：最新打卡批量一次查出（此前每任务一查，N+1）
        Map<String, TaskCheckin> latestCheckins = latestCheckinMap(
                tasks.stream().map(PlanTask::getId).collect(Collectors.toList()));
        List<TaskVO> list = tasks.stream()
                .map(t -> toTaskVO(t, latestCheckins.get(t.getId())))
                .toList();
        long total = mapper.countTasks(studentId, month, status);
        return PageResult.of(list, total, p, s);
    }

    private Map<String, TaskCheckin> latestCheckinMap(List<String> taskIds) {
        Map<String, TaskCheckin> out = new java.util.LinkedHashMap<>();
        if (taskIds == null || taskIds.isEmpty()) {
            return out;
        }
        // SQL 已按 task_id, checked_in_at DESC 排序，首条即最新
        for (TaskCheckin checkin : mapper.selectLatestCheckinsByTaskIds(taskIds)) {
            out.putIfAbsent(checkin.getTaskId(), checkin);
        }
        return out;
    }

    @Override
    public TaskVO getTask(String studentId, String taskId) {
        PlanTask task = mapper.selectTaskById(taskId);
        if (task == null || !studentId.equals(task.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        return toTaskVO(task, mapper.selectLatestCheckinByTask(task.getId()));
    }

    @Override
    @Transactional
    public TaskVO createTask(String studentId, TaskRequest req) {
        return createTask(studentId, req, null);
    }

    /**
     * 复审加固：Idempotency-Key 可选——携带时走幂等（双击只建一个任务），
     * 缺省时保持原行为（兼容不断 key 的老客户端）。
     */
    @Override
    @Transactional
    public TaskVO createTask(String studentId, TaskRequest req, String idempotencyKey) {        if (!StringUtils.hasText(idempotencyKey)) {
            return doCreateTask(studentId, req);
        }
        ApiResponse<TaskVO> response = idempotencyService.execute(
                studentId, "planning:createTask", idempotencyKey, TaskVO.class,
                () -> ApiResponse.ok(doCreateTask(studentId, req)));
        return response.getData();
    }

    private TaskVO doCreateTask(String studentId, TaskRequest req) {
        if (!StringUtils.hasText(req.getTitle())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "任务标题不能为空");
        }
        SemesterPlan plan = mapper.selectLatestPlanByStatus(studentId, "CONFIRMED");
        String planId = plan == null ? null : plan.getId();
        if (planId == null) {
            // 复审 Batch4：此前 plan_id 置空后由 DB 非空约束抛 500，现前置为明确 400
            throw new BizException(ResultCode.VALIDATION_ERROR, "暂无已确认的计划，请先生成并确认计划");
        }
        PlanTask task = new PlanTask();
        task.setId(idGenerator.planTaskId());
        task.setPlanId(planId);
        task.setStudentId(studentId);
        task.setMonth(req.getMonth() == null ? LocalDate.now().format(MONTH_FMT) : req.getMonth());
        task.setTitle(req.getTitle());
        task.setTaskType(req.getType() == null ? "LEARNING" : req.getType());
        task.setEstHours(req.getEstHours());
        task.setStatus("PENDING");
        task.setDeadline(req.getDeadline());
        task.setAbilityTagsJson(req.getAbilityTags() == null ? null : JsonUtil.toJson(req.getAbilityTags()));
        task.setNote(req.getNote());
        task.setCreatedAt(LocalDateTime.now());
        mapper.insertTask(task);
        return toTaskVO(task, null);
    }

    @Override
    @Transactional
    public TaskVO updateTask(String studentId, String taskId, TaskStatusUpdate req) {
        PlanTask task = mapper.selectTaskById(taskId);
        if (task == null || !studentId.equals(task.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        if (req.getMonth() != null) {
            task.setMonth(req.getMonth());
        }
        if (req.getTitle() != null) {
            task.setTitle(req.getTitle());
        }
        if (req.getEstHours() != null) {
            task.setEstHours(req.getEstHours());
        }
        if (req.getStatus() != null) {
            validateTaskStatus(req.getStatus());
            task.setStatus(req.getStatus());
        }
        if (req.getNote() != null) {
            task.setNote(req.getNote());
        }
        mapper.updateTask(task);
        return toTaskVO(task, mapper.selectLatestCheckinByTask(task.getId()));
    }

    @Override
    @Transactional
    public TaskVO checkinTask(String studentId, String taskId, TaskCheckinRequest req) {
        return checkinTask(studentId, taskId, req, null);
    }

    /** 复审加固：打卡幂等（同上，key 可选）。 */
    @Override
    @Transactional
    public TaskVO checkinTask(String studentId, String taskId, TaskCheckinRequest req, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return doCheckinTask(studentId, taskId, req);
        }
        ApiResponse<TaskVO> response = idempotencyService.execute(
                studentId, "planning:checkinTask", idempotencyKey, TaskVO.class,
                () -> ApiResponse.ok(doCheckinTask(studentId, taskId, req)));
        return response.getData();
    }

    private TaskVO doCheckinTask(String studentId, String taskId, TaskCheckinRequest req) {
        PlanTask task = mapper.selectTaskById(taskId);
        if (task == null || !studentId.equals(task.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "任务不存在");
        }
        if (!StringUtils.hasText(req.getDoneDesc())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "完成说明不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        TaskCheckin checkin = new TaskCheckin();
        checkin.setId(idGenerator.taskCheckinId());
        checkin.setTaskId(taskId);
        checkin.setDoneDesc(req.getDoneDesc());
        checkin.setGains(req.getGains());
        checkin.setDifficulties(req.getDifficulties());
        checkin.setProofUrl(req.getProofUrl());
        checkin.setCheckedInAt(now);
        checkin.setCreatedAt(now);
        mapper.insertCheckin(checkin);

        task.setStatus("DONE");
        mapper.updateTask(task);
        return toTaskVO(task, checkin);
    }

    private void validateTaskStatus(String status) {
        if (!List.of("PENDING", "DOING", "DONE", "DELAYED", "ABANDONED").contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 不合法：" + status);
        }
    }

    private TaskVO toTaskVO(PlanTask task, TaskCheckin checkin) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setMonth(task.getMonth());
        vo.setTitle(task.getTitle());
        vo.setType(task.getTaskType());
        vo.setEstHours(task.getEstHours() == null ? null : task.getEstHours().doubleValue());
        vo.setStatus(task.getStatus());
        vo.setDeadline(task.getDeadline());
        vo.setAbilityTags(parseStringList(task.getAbilityTagsJson()));
        vo.setNote(task.getNote());
        if (checkin != null) {
            TaskVO.TaskCheckin c = new TaskVO.TaskCheckin();
            c.setId(checkin.getId());
            c.setTaskId(checkin.getTaskId());
            c.setDoneDesc(checkin.getDoneDesc());
            c.setGains(checkin.getGains());
            c.setDifficulties(checkin.getDifficulties());
            c.setProofUrl(checkin.getProofUrl());
            c.setCheckedInAt(checkin.getCheckedInAt());
            vo.setCheckin(c);
            vo.setCheckedInAt(checkin.getCheckedInAt());
        }
        return vo;
    }

    // ================================================================ 复盘

    @Override
    public ReviewVO getReview(String studentId, String reviewId) {
        StageReview review = mapper.selectReviewById(reviewId);
        if (review == null || !studentId.equals(review.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "复盘不存在");
        }
        return toReviewVO(review);
    }

    @Override
    public List<ReviewVO> listReviews(String studentId) {
        return mapper.selectReviewsByStudent(studentId).stream()
                .map(this::toReviewVO)
                .toList();
    }

    @Override
    @Transactional
    public ReviewVO createReviewDraft(String studentId, ReviewDraftRequest req) {
        String cycle = req.getCycle();
        if (!StringUtils.hasText(cycle)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "复盘周期不能为空");
        }
        StageReview review = mapper.selectReviewsByStudent(studentId).stream()
                .filter(r -> "DRAFT".equals(r.getStatus()) && cycle.equals(r.getCycle()))
                .findFirst().orElse(null);
        if (review == null) {
            review = new StageReview();
            review.setId(idGenerator.stageReviewId());
            review.setStudentId(studentId);
            review.setCycle(cycle);
            review.setStatus("DRAFT");
            review.setAdvisorRequested(false);
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            review.setContentJson(req.getContent() == null ? null : JsonUtil.toJson(req.getContent()));
            try {
                mapper.insertReview(review);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 复审加固：并发双击同时走到 insert 时 UK(student_id, cycle) 兜底，
                // 回读已存在的行并更新内容（幂等语义），而非 500。
                review = mapper.selectReviewsByStudent(studentId).stream()
                        .filter(r -> cycle.equals(r.getCycle()))
                        .findFirst()
                        .orElseThrow(() -> e);
                review.setContentJson(req.getContent() == null ? null : JsonUtil.toJson(req.getContent()));
                mapper.updateReview(review);
            }
        } else {
            review.setContentJson(req.getContent() == null ? null : JsonUtil.toJson(req.getContent()));
            mapper.updateReview(review);
        }
        return toReviewVO(review);
    }

    @Override
    @Transactional
    public ReviewVO updateReviewDraft(String studentId, String reviewId, ReviewDraftRequest req) {
        StageReview review = loadOwnReview(studentId, reviewId);
        if (!"DRAFT".equals(review.getStatus())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "仅草稿状态可保存");
        }
        review.setContentJson(req.getContent() == null ? null : JsonUtil.toJson(req.getContent()));
        mapper.updateReview(review);
        return toReviewVO(review);
    }

    @Override
    public ReviewVO submitReview(String studentId, String reviewId) {
        // 稳定性：AI 总结（远程）在事务外，落库走代理进事务
        StageReview review = loadOwnReview(studentId, reviewId);
        review.setStatus("SUBMITTED");
        review.setSubmittedAt(LocalDateTime.now());
        applyAiSummary(review);
        return self.persistReview(review);
    }

    @Override
    public ReviewVO summarizeReview(String studentId, String reviewId) {
        StageReview review = loadOwnReview(studentId, reviewId);
        applyAiSummary(review);
        return self.persistReview(review);
    }

    @Transactional
    public ReviewVO persistReview(StageReview review) {
        // 复审加固：条件写——仅当行仍为 DRAFT/SUBMITTED 才落库，并发提交/总结冲突时 0 行
        // 抛 409 而非静默后写覆盖（调用方 submit/summarize 已在事务外做完 AI，此处只做原子落点）
        int rows = mapper.updateReviewIfSubmittable(review);
        if (rows == 0) {
            throw new BizException(ResultCode.STATE_CONFLICT, "复盘状态已变化，请刷新后重试");
        }
        return toReviewVO(review);
    }

    private void applyAiSummary(StageReview review) {
        ReviewVO.ReviewContent content = parseObject(review.getContentJson(), ReviewVO.ReviewContent.class);
        AiReviewSummaryVO result = safeSummarize(review.getCycle(), content);
        review.setAiSummary(result.getSummary());
        review.setAiSuggestJson(JsonUtil.toJson(result.getSuggestions() == null ? List.of() : result.getSuggestions()));
    }

    private AiReviewSummaryVO safeSummarize(String cycle, ReviewVO.ReviewContent content) {
        AiReviewSummarizeRequest req = new AiReviewSummarizeRequest();
        req.setCycle(cycle);
        if (content != null) {
            com.rickgao.careercore.modules.ai.dto.AiReviewContent c =
                    new com.rickgao.careercore.modules.ai.dto.AiReviewContent();
            c.setDone(content.getDone());
            c.setUndone(content.getUndone());
            c.setInterest(content.getInterest());
            c.setAbility(content.getAbility());
            c.setNext(content.getNext());
            req.setReviewContent(c);
        }
        try {
            return aiService.reviewSummarize(req);
        } catch (BizException exc) {
            // Demo 精简点：AI 不可用时回退为简单文案
            String summary = content == null || !StringUtils.hasText(content.getDone())
                    ? "本阶段已完成复盘，建议持续跟踪目标进展。"
                    : "本阶段完成情况：" + content.getDone();
            return AiReviewSummaryVO.builder().summary(summary).suggestions(List.of()).build();
        }
    }

    @Override
    @Transactional
    public ReviewVO adoptAdvice(String studentId, String reviewId, AdoptAdviceRequest req) {
        StageReview review = loadOwnReview(studentId, reviewId);
        if (Boolean.TRUE.equals(req.getAdopt())) {
            // Demo 精简点：采纳建议后，将 AI 建议合并生成新 DRAFT 计划
            List<String> suggestions = parseStringList(review.getAiSuggestJson());
            SemesterPlan draft = new SemesterPlan();
            draft.setId(idGenerator.semesterPlanId());
            draft.setStudentId(studentId);
            draft.setVersionNo(1);
            draft.setStatus("DRAFT");
            draft.setSource("MANUAL");
            draft.setGoalSummary(review.getAiSummary() == null ? "根据复盘调整计划" : review.getAiSummary());
            List<PlanVO.MonthlyTask> tasks = new ArrayList<>();
            for (int i = 0; i < suggestions.size(); i++) {
                PlanVO.MonthlyTask mt = new PlanVO.MonthlyTask();
                mt.setMonth(LocalDate.now().plusMonths(i).withDayOfMonth(1).format(MONTH_FMT));
                mt.setTitle(suggestions.get(i));
                mt.setTaskType("LEARNING");
                mt.setEstimatedHours(15.0);
                tasks.add(mt);
            }
            draft.setSemesterGoalsJson(JsonUtil.toJson(List.of()));
            draft.setMonthlyTasksJson(JsonUtil.toJson(tasks));
            draft.setNotesJson(JsonUtil.toJson(suggestions));
            draft.setCreatedAt(LocalDateTime.now());
            mapper.insertPlan(draft);
            saveTasksFromMonthly(studentId, draft.getId(), tasks);
        }
        return toReviewVO(review);
    }

    @Override
    @Transactional
    public ReviewVO requestGuidance(String studentId, String reviewId, GuidanceRequestPayload req) {
        StageReview review = loadOwnReview(studentId, reviewId);
        review.setAdvisorRequested(true);
        mapper.updateReview(review);
        return toReviewVO(review);
    }

    private StageReview loadOwnReview(String studentId, String reviewId) {
        StageReview review = mapper.selectReviewById(reviewId);
        if (review == null || !studentId.equals(review.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "复盘不存在");
        }
        return review;
    }

    private ReviewVO toReviewVO(StageReview review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setCycle(review.getCycle());
        vo.setStatus(review.getStatus());
        vo.setContent(parseObject(review.getContentJson(), ReviewVO.ReviewContent.class));
        vo.setAiSummary(review.getAiSummary());
        vo.setAiSuggest(parseStringList(review.getAiSuggestJson()));
        vo.setAdvisorRequested(review.getAdvisorRequested());
        vo.setAdvisorReply(review.getAdvisorReply());
        vo.setSubmittedAt(review.getSubmittedAt());
        return vo;
    }

    // ================================================================ 提醒

    @Override
    public PageResult<ReminderVO> listReminders(String studentId, boolean unreadOnly, int page, int size) {
        int p = page < 1 ? DEFAULT_PAGE : page;
        int s = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int offset = (p - 1) * s;
        List<ReminderVO> list = mapper.selectRemindersByStudent(studentId, unreadOnly, offset, s).stream()
                .map(this::toReminderVO)
                .toList();
        long total = mapper.countReminders(studentId, unreadOnly);
        return PageResult.of(list, total, p, s);
    }

    @Override
    public int unreadReminderCount(String studentId) {
        return mapper.countUnreadReminders(studentId);
    }

    @Override
    @Transactional
    public void markReminderRead(String studentId, String reminderId) {
        Reminder reminder = mapper.selectReminderById(reminderId);
        if (reminder == null || !studentId.equals(reminder.getStudentId())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "提醒不存在");
        }
        mapper.markReminderRead(reminderId);
    }

    @Override
    @Transactional
    public List<ReminderVO> generateReminders(String studentId) {
        List<ReminderVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(7);
        // 复审加固：已存在的未读提醒不再重复生成（此前每次调用都 insert，轮询即膨胀）
        List<Reminder> unread = mapper.selectRemindersByStudent(studentId, true, 0, 200);
        // 近 7 天截止的未完成任务
        List<PlanTask> tasks = mapper.selectTasksByStudent(studentId, null, null, 0, Integer.MAX_VALUE).stream()
                .filter(t -> t.getDeadline() != null
                        && !t.getDeadline().isBefore(today)
                        && !t.getDeadline().isAfter(soon)
                        && !List.of("DONE", "ABANDONED").contains(t.getStatus()))
                .toList();
        for (PlanTask task : tasks) {
            if (hasUnreadFor(unread, "TASK_DEADLINE", task.getTitle())) {
                continue;
            }
            Reminder r = new Reminder();
            r.setId(idGenerator.reminderId());
            r.setStudentId(studentId);
            r.setType("TASK_DEADLINE");
            r.setTitle("任务即将截止");
            r.setContent("任务「" + task.getTitle() + "」将于 " + task.getDeadline() + " 截止，请及时完成打卡。");
            r.setIsRead(false);
            r.setCreatedAt(LocalDateTime.now());
            mapper.insertReminder(r);
            result.add(toReminderVO(r));
        }
        // 本月未提交复盘提醒
        StageReview latest = mapper.selectLatestReviewByStudent(studentId);
        String month = today.format(MONTH_FMT);
        boolean reviewedThisMonth = latest != null && month.equals(latest.getCycle())
                && "SUBMITTED".equals(latest.getStatus());
        if (!reviewedThisMonth && !hasUnreadFor(unread, "REVIEW_REMIND", month)) {
            Reminder r = new Reminder();
            r.setId(idGenerator.reminderId());
            r.setStudentId(studentId);
            r.setType("REVIEW_REMIND");
            r.setTitle("本月复盘提醒");
            r.setContent("本月（" + month + "）尚未提交阶段复盘，建议抽时间回顾一下目标进展。");
            r.setIsRead(false);
            r.setCreatedAt(LocalDateTime.now());
            mapper.insertReminder(r);
            result.add(toReminderVO(r));
        }
        return result;
    }

    /** 已有同类型未读提醒且内容包含同一标识（任务标题/月份）时视为重复，不再生成。 */
    private boolean hasUnreadFor(List<Reminder> unread, String type, String marker) {
        if (marker == null) {
            return false;
        }
        for (Reminder r : unread) {
            if (type.equals(r.getType()) && r.getContent() != null && r.getContent().contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private ReminderVO toReminderVO(Reminder r) {
        ReminderVO vo = new ReminderVO();
        vo.setId(r.getId());
        vo.setType(r.getType());
        vo.setTitle(r.getTitle());
        vo.setContent(r.getContent());
        vo.setRead(r.getIsRead());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    // ================================================================ 通用工具

    private PlanVO toPlanVO(SemesterPlan plan) {
        PlanVO vo = new PlanVO();
        vo.setId(plan.getId());
        vo.setVersion("P-v" + (plan.getVersionNo() == null ? 1 : plan.getVersionNo()));
        vo.setStatus(plan.getStatus());
        vo.setSource(plan.getSource());
        vo.setGoalSummary(plan.getGoalSummary());
        vo.setSemesterGoals(parseList(plan.getSemesterGoalsJson(), new TypeReference<List<PlanVO.SemesterGoal>>() {
        }));
        vo.setMonthlyTasks(parseList(plan.getMonthlyTasksJson(), new TypeReference<List<PlanVO.MonthlyTask>>() {
        }));
        vo.setNotes(parseList(plan.getNotesJson(), new TypeReference<List<String>>() {
        }));
        vo.setConfirmedAt(plan.getConfirmedAt());
        vo.setUpdatedAt(plan.getUpdatedAt());
        return vo;
    }

    private List<String> parseStringList(String json) {
        return StringUtils.hasText(json)
                ? JsonUtil.parse(json, new TypeReference<List<String>>() {
        })
                : List.of();
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
        return StringUtils.hasText(json) ? JsonUtil.parse(json, typeReference) : List.of();
    }

    private <T> T parseObject(String json, Class<T> type) {
        return StringUtils.hasText(json) ? JsonUtil.parse(json, type) : null;
    }
}
