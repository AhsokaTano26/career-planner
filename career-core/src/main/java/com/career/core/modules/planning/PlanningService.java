package com.career.core.modules.planning;

import com.career.core.modules.recommendation.RecommendationDto;
import com.career.core.modules.recommendation.RecommendationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计划模块服务。
 * Demo 精简逻辑说明：
 *   1. plans/generate 的数据来源：调用推荐接口（RecommendationService.recommend）的推荐结果，
 *      取排名第一的方向作为主目标——即“生成计划草案需要调用推荐数据”。
 *   2. Demo 阶段由规则模板生成任务，不调用大模型（后续迭代替换为 career-ai plan_generator）。
 *   3. 生成后落库（student_goal / semester_plan / plan_task），返回草案，不做异步处理。
 */
@Service
public class PlanningService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_SEMESTER = "2026-2027学年第1学期";

    private final RecommendationService recommendationService;
    private final PlanningDao planningDao;

    public PlanningService(RecommendationService recommendationService, PlanningDao planningDao) {
        this.recommendationService = recommendationService;
        this.planningDao = planningDao;
    }

    /**
     * 生成学期计划草案（调用推荐数据）。
     *
     * @param studentId 学生ID
     * @return 计划草案 Map；无推荐结果时返回空 Map
     */
    public Map<String, Object> generatePlanDraft(Long studentId) {
        // 1. 调用推荐数据（Demo：规则引擎结果；正式接入 career-ai 前复用同一数据源）
        List<RecommendationDto> recs = recommendationService.recommend(studentId);
        if (recs.isEmpty()) {
            return Collections.emptyMap();
        }
        RecommendationDto top = recs.get(0);

        // 2. 以排名第一方向生成主目标并落库
        String goalTitle = "以「" + top.name() + "」为主攻方向的发展计划";
        long goalId = planningDao.insertGoal(studentId, top.directionId(), goalTitle, "MAIN", "ACTIVE");

        // 3. 生成学期计划草案并落库
        long planId = planningDao.insertPlan(studentId, goalId, DEFAULT_SEMESTER, "AI", "DRAFT");

        // 4. 按方向类型套用任务模板生成月度任务并落库
        List<String> titles = taskTemplatesFor(top.type());
        LocalDate month = LocalDate.of(2026, 9, 1);
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            String m = month.plusMonths(i).format(MONTH_FMT);
            long taskId = planningDao.insertTask(planId, m, titles.get(i), "TODO");
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", taskId);
            t.put("month", m);
            t.put("title", titles.get(i));
            t.put("status", "TODO");
            tasks.add(t);
        }

        // 5. 组装响应
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentId", studentId);
        data.put("direction", Map.of(
                "directionId", top.directionId(), "name", top.name(),
                "type", top.type(), "score", top.score()));
        data.put("goal", Map.of("id", goalId, "title", goalTitle, "goalType", "MAIN"));
        data.put("semester", DEFAULT_SEMESTER);
        data.put("status", "DRAFT");
        data.put("tasks", tasks);
        return data;
    }

    /** 任务模板（按方向类型选择，Demo 预置；后续替换为大模型生成） */
    private List<String> taskTemplatesFor(String type) {
        if (type == null) {
            return List.of("明确主攻方向并制定学习路线", "完成一项实践项目", "阶段复盘与调整");
        }
        switch (type) {
            case "数据算法":
                return List.of("学习 Python 与数据分析基础", "完成一个数据分析小项目", "系统学习机器学习入门", "阶段复盘并确定下一步");
            case "技术研发":
                return List.of("巩固编程语言与数据结构基础", "完成一个课程/开源项目", "学习主攻方向核心框架", "阶段复盘并明确主攻领域");
            case "产品管理":
                return List.of("学习产品方法与需求分析", "完成一份产品分析报告", "参与一次项目协作实践", "阶段复盘并调整计划");
            default:
                return List.of("明确主攻方向并制定学习路线", "完成一项实践项目", "阶段复盘与调整");
        }
    }
}
