package com.career.core.modules.planning;

import com.career.core.common.BadRequestException;
import com.career.core.modules.recommendation.CareerDirection;
import com.career.core.modules.recommendation.RecommendationDao;
import com.career.core.modules.recommendation.RecommendationRunDto;
import com.career.core.modules.recommendation.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 计划模块服务（按线上 Apifox「目标计划」PlanDraft 结构实现）。
 * <p>
 * Demo 精简逻辑说明：
 *   1. 数据来源：优先请求体 directionId；未传时调用推荐接口（/students/me/recommendations/latest）取排名第一方向。
 *   2. Demo 阶段由规则模板生成任务（不调用大模型），useAi=true 亦回退模板（后续迭代替换为 career-ai plan_generator）。
 *   3. 草案落库（student_goal / semester_plan / plan_task）后返回，不做异步处理。
 */
@Service
public class PlanningService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_SEMESTER = "2026-2027学年第1学期";

    private final RecommendationService recommendationService;
    private final RecommendationDao recommendationDao;
    private final PlanningDao planningDao;

    public PlanningService(RecommendationService recommendationService,
                           RecommendationDao recommendationDao,
                           PlanningDao planningDao) {
        this.recommendationService = recommendationService;
        this.recommendationDao = recommendationDao;
        this.planningDao = planningDao;
    }

    /**
     * 生成学期计划草案。
     *
     * @param studentId   学生ID
     * @param directionId 目标方向编码（可空：为空则取最新推荐第一名）
     * @param useAi       是否调用 AI（Demo 阶段忽略，规则模板）
     * @return PlanDraftDto；无法确定目标方向时返回 null（Controller 转 400）
     */
    public PlanDraftDto generatePlanDraft(Long studentId, String directionId, boolean useAi) {
        // 1. 确定目标方向编码：优先入参；否则取最新推荐第一名
        String dirCode = directionId;
        if (dirCode == null || dirCode.isBlank()) {
            RecommendationRunDto latest = recommendationService.getLatest(studentId);
            if (latest != null && latest.results() != null && !latest.results().isEmpty()) {
                dirCode = latest.results().get(0).directionId();
            }
        }
        if (dirCode == null || dirCode.isBlank()) {
            throw new BadRequestException("无法确定目标方向，请传入 directionId 或先生成推荐");
        }
        CareerDirection direction = recommendationDao.findDirectionByCode(dirCode);
        if (direction == null) {
            throw new BadRequestException("方向不存在：" + dirCode);
        }

        // 2. 以目标方向生成主目标并落库（direction_id 关联 career_direction.id）
        String goalSummary = "本学期围绕【" + direction.name() + "】方向打好基础："
                + "巩固核心课程，完成 1 个可运行的小项目，并在过程中明确后续主攻领域。";
        long goalId = planningDao.insertGoal(studentId, direction.id(), "以「" + direction.name() + "」为主攻方向的发展计划",
                "MAIN", "ACTIVE");
        long planId = planningDao.insertPlan(studentId, goalId, DEFAULT_SEMESTER, useAi ? "AI" : "TEMPLATE", "DRAFT");

        // 3. 套用任务模板生成月度任务并落库
        List<PlanDraftDto.MonthlyTaskDto> monthlyTasks = buildMonthlyTasks(planId, direction.type());

        // 4. 组装 PlanDraft 响应
        return new PlanDraftDto(
                goalSummary,
                List.of(
                        new PlanDraftDto.SemesterGoalDto("巩固专业核心课程与编程基础", "programming_basic"),
                        new PlanDraftDto.SemesterGoalDto("完成 1 个可运行的小项目", "project_basic")),
                monthlyTasks,
                List.of("任务可随课程安排与兴趣变化调整。"));
    }

    /** 月度任务（按方向类型选择模板，Demo 预置；后续替换为大模型生成） */
    private List<PlanDraftDto.MonthlyTaskDto> buildMonthlyTasks(long planId, String type) {
        List<String[]> template = taskTemplatesFor(type);
        LocalDate month = LocalDate.of(2026, 9, 1);
        List<PlanDraftDto.MonthlyTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < template.size(); i++) {
            String[] item = template.get(i);
            String m = month.plusMonths(i).format(MONTH_FMT);
            planningDao.insertTask(planId, m, item[0], "TODO");
            tasks.add(new PlanDraftDto.MonthlyTaskDto(m, item[0], item[1], Double.parseDouble(item[2])));
        }
        return tasks;
    }

    /** 任务模板：[标题, 任务类型, 预计小时数] */
    private List<String[]> taskTemplatesFor(String type) {
        if (type == null) {
            return List.of(
                    new String[]{"明确主攻方向并制定学习路线", "CAREER", "6"},
                    new String[]{"完成一项实践项目", "PRACTICE", "12"},
                    new String[]{"阶段复盘与调整", "REVIEW", "4"});
        }
        return switch (type) {
            case "数据算法" -> List.of(
                    new String[]{"学习 Python 与数据分析基础", "LEARNING", "12"},
                    new String[]{"完成一个数据分析小项目", "PRACTICE", "12"},
                    new String[]{"系统学习机器学习入门", "LEARNING", "10"},
                    new String[]{"阶段复盘并确定下一步", "REVIEW", "4"});
            case "技术研发" -> List.of(
                    new String[]{"巩固编程语言与数据结构基础", "LEARNING", "12"},
                    new String[]{"完成一个课程/开源项目", "PRACTICE", "12"},
                    new String[]{"学习主攻方向核心框架", "LEARNING", "10"},
                    new String[]{"阶段复盘并明确主攻领域", "REVIEW", "4"});
            case "产品管理" -> List.of(
                    new String[]{"学习产品方法与需求分析", "LEARNING", "8"},
                    new String[]{"完成一份产品分析报告", "PRACTICE", "8"},
                    new String[]{"参与一次项目协作实践", "PRACTICE", "10"},
                    new String[]{"阶段复盘并调整计划", "REVIEW", "4"});
            default -> List.of(
                    new String[]{"明确主攻方向并制定学习路线", "CAREER", "6"},
                    new String[]{"完成一项实践项目", "PRACTICE", "12"},
                    new String[]{"阶段复盘与调整", "REVIEW", "4"});
        };
    }
}
