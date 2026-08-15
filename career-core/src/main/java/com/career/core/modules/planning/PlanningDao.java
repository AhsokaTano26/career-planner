package com.career.core.modules.planning;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 计划模块数据访问层（JdbcTemplate，Demo 最小实现）。
 * 返回中间行投影（GoalRow/PlanRow/TaskRow），由 Service 组装线上 DTO。
 */
@Repository
public class PlanningDao {

    private final JdbcTemplate jdbc;

    public PlanningDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 目标行投影 */
    public record GoalRow(Long id, Long directionId, String title, String goalType,
                          String status, LocalDateTime createdAt) {
    }

    /** 计划行投影 */
    public record PlanRow(Long id, Long goalId, String semester, String source,
                          String status, LocalDateTime createdAt) {
    }

    /** 任务行投影 */
    public record TaskRow(Long id, Long studentId, String title, String status,
                          String month, LocalDateTime createdAt) {
    }

    /** 计划内任务行投影（plan_task） */
    public record PlanTaskRow(Long id, Long planId, String month, String title,
                              String status, LocalDateTime createdAt) {
    }

    private static final RowMapper<GoalRow> GOAL_MAPPER = (rs, i) -> new GoalRow(
            rs.getLong("id"),
            rs.getObject("direction_id", Long.class),
            rs.getString("title"),
            rs.getString("goal_type"),
            rs.getString("status"),
            toLdt(rs.getTimestamp("created_at")));

    private static final RowMapper<PlanRow> PLAN_MAPPER = (rs, i) -> new PlanRow(
            rs.getLong("id"),
            rs.getObject("goal_id", Long.class),
            rs.getString("semester"),
            rs.getString("source"),
            rs.getString("status"),
            toLdt(rs.getTimestamp("created_at")));

    private static final RowMapper<TaskRow> TASK_MAPPER = (rs, i) -> new TaskRow(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getString("month"),
            toLdt(rs.getTimestamp("created_at")));

    private static final RowMapper<PlanTaskRow> PLAN_TASK_MAPPER = (rs, i) -> new PlanTaskRow(
            rs.getLong("id"),
            rs.getLong("plan_id"),
            rs.getString("month"),
            rs.getString("title"),
            rs.getString("status"),
            toLdt(rs.getTimestamp("created_at")));

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

    /** 新增计划内任务（草案月度任务），返回自增主键 */
    public long insertPlanTask(Long planId, String month, String title, String status) {
        return insertAndReturnKey(
                "INSERT INTO plan_task (plan_id, month, title, status) VALUES (?, ?, ?, ?)",
                planId, month, title, status);
    }

    /** 查询学生目标列表（按 id 倒序） */
    public List<GoalRow> findGoals(Long studentId) {
        return jdbc.query(
                "SELECT id, direction_id, title, goal_type, status, created_at " +
                        "FROM student_goal WHERE student_id = ? ORDER BY id DESC",
                GOAL_MAPPER, studentId);
    }

    /** 查询学生计划列表（按 id 倒序） */
    public List<PlanRow> findPlans(Long studentId) {
        return jdbc.query(
                "SELECT id, goal_id, semester, source, status, created_at " +
                        "FROM semester_plan WHERE student_id = ? ORDER BY id DESC",
                PLAN_MAPPER, studentId);
    }

    /** 按计划ID查询计划 */
    public PlanRow findPlanById(Long planId) {
        List<PlanRow> list = jdbc.query(
                "SELECT id, goal_id, semester, source, status, created_at " +
                        "FROM semester_plan WHERE id = ?",
                PLAN_MAPPER, planId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 更新计划（学期/状态） */
    public int updatePlan(Long planId, String semester, String status) {
        return jdbc.update("UPDATE semester_plan SET semester = ?, status = ? WHERE id = ?", semester, status, planId);
    }

    /** 查询学生任务列表（按 id 倒序） */
    public List<TaskRow> findTasks(Long studentId) {
        return jdbc.query(
                "SELECT id, student_id, title, status, month, created_at " +
                        "FROM task WHERE student_id = ? ORDER BY id DESC",
                TASK_MAPPER, studentId);
    }

    /** 查询计划内任务列表（按 id 升序，保持月份顺序） */
    public List<PlanTaskRow> findPlanTasks(Long planId) {
        return jdbc.query(
                "SELECT id, plan_id, month, title, status, created_at " +
                        "FROM plan_task WHERE plan_id = ? ORDER BY id ASC",
                PLAN_TASK_MAPPER, planId);
    }

    /** 按任务ID查询任务 */
    public TaskRow findTaskById(Long taskId) {
        List<TaskRow> list = jdbc.query(
                "SELECT id, student_id, title, status, month, created_at " +
                        "FROM task WHERE id = ?",
                TASK_MAPPER, taskId);
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

    private static LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
