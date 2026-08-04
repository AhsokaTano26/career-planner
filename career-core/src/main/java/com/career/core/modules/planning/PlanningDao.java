package com.career.core.modules.planning;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

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
