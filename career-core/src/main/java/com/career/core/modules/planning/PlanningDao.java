package com.career.core.modules.planning;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 计划模块数据访问层（JdbcTemplate，Demo 最小实现）。
 */
@Repository
public class PlanningDao {

    private final JdbcTemplate jdbc;

    public PlanningDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 新增主/备选目标，返回自增主键 */
    public long insertGoal(Long studentId, Long directionId, String title, String goalType, String status) {
        return insertAndReturnKey(
                "INSERT INTO student_goal (student_id, direction_id, title, goal_type, status) VALUES (?, ?, ?, ?, ?)",
                studentId, directionId, title, goalType, status);
    }

    /** 新增学期计划（草案），返回自增主键 */
    public long insertPlan(Long studentId, Long goalId, String semester, String source, String status) {
        return insertAndReturnKey(
                "INSERT INTO semester_plan (student_id, goal_id, semester, source, status) VALUES (?, ?, ?, ?, ?)",
                studentId, goalId, semester, source, status);
    }

    /** 新增计划任务，返回自增主键 */
    public long insertTask(Long planId, String month, String title, String status) {
        return insertAndReturnKey(
                "INSERT INTO plan_task (plan_id, month, title, status) VALUES (?, ?, ?, ?)",
                planId, month, title, status);
    }

    // ---------- 目标 / 计划 / 任务查询与更新（补齐线上「目标计划」模块） ----------

    private static final RowMapper<GoalDto> GOAL_MAPPER = (rs, i) -> new GoalDto(
            rs.getLong("id"),
            rs.getObject("direction_id", Long.class),
            rs.getString("title"),
            rs.getString("goal_type"),
            rs.getString("status"));

    private static final RowMapper<PlanDto> PLAN_MAPPER = (rs, i) -> new PlanDto(
            rs.getLong("id"),
            rs.getObject("goal_id", Long.class),
            rs.getString("semester"),
            rs.getString("source"),
            rs.getString("status"));

    private static final RowMapper<TaskDto> TASK_MAPPER = (rs, i) -> new TaskDto(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getString("month"));

    /** 查询学生目标列表（按 id 倒序） */
    public List<GoalDto> findGoals(Long studentId) {
        return jdbc.query(
                "SELECT id, direction_id, title, goal_type, status FROM student_goal WHERE student_id = ? ORDER BY id DESC",
                GOAL_MAPPER, studentId);
    }

    /** 查询学生计划列表（按 id 倒序） */
    public List<PlanDto> findPlans(Long studentId) {
        return jdbc.query(
                "SELECT id, goal_id, semester, source, status FROM semester_plan WHERE student_id = ? ORDER BY id DESC",
                PLAN_MAPPER, studentId);
    }

    /** 按计划ID查询计划 */
    public PlanDto findPlanById(Long planId) {
        List<PlanDto> list = jdbc.query(
                "SELECT id, goal_id, semester, source, status FROM semester_plan WHERE id = ?",
                PLAN_MAPPER, planId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 更新计划状态（确认/编辑） */
    public int updatePlan(Long planId, String semester, String status) {
        return jdbc.update("UPDATE semester_plan SET semester = ?, status = ? WHERE id = ?", semester, status, planId);
    }

    /** 查询学生任务列表（按 id 倒序） */
    public List<TaskDto> findTasks(Long studentId) {
        return jdbc.query(
                "SELECT id, title, status, month FROM task WHERE student_id = ? ORDER BY id DESC",
                TASK_MAPPER, studentId);
    }

    /** 按任务ID查询任务 */
    public TaskDto findTaskById(Long taskId) {
        List<TaskDto> list = jdbc.query(
                "SELECT id, title, status, month FROM task WHERE id = ?", TASK_MAPPER, taskId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 新增学生任务，返回自增主键 */
    public long insertTask(Long studentId, String title, String month) {
        return insertAndReturnKey(
                "INSERT INTO task (student_id, title, status, month) VALUES (?, ?, 'TODO', ?)",
                studentId, title, month);
    }

    /** 更新任务（标题/状态） */
    public int updateTask(Long taskId, String title, String status) {
        return jdbc.update("UPDATE task SET title = ?, status = ? WHERE id = ?", title, status, taskId);
    }

    /** 删除任务 */
    public int deleteTask(Long taskId) {
        return jdbc.update("DELETE FROM task WHERE id = ?", taskId);
    }

    private long insertAndReturnKey(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }
}
