package com.career.core.modules.recommendation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 推荐模块数据访问层（JdbcTemplate，Demo 最小实现）。
 */
@Repository
public class RecommendationDao {

    private final JdbcTemplate jdbc;

    public RecommendationDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<CareerDirection> DIRECTION_MAPPER = (rs, i) -> new CareerDirection(
            rs.getLong("id"),
            rs.getString("direction_code"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("status"),
            rs.getString("content"));

    private static final RowMapper<DirectionWeight> WEIGHT_MAPPER = (rs, i) -> new DirectionWeight(
            rs.getString("dimension"),
            rs.getBigDecimal("target_value") == null ? null : rs.getBigDecimal("target_value").doubleValue(),
            rs.getBigDecimal("weight") == null ? null : rs.getBigDecimal("weight").doubleValue());

    /** 查询全部方向库 */
    public List<CareerDirection> findAllDirections() {
        return jdbc.query(
                "SELECT id, direction_code, name, type, status, content FROM career_direction ORDER BY id ASC",
                DIRECTION_MAPPER);
    }

    /** 查询某方向的维度权重/目标值（按指定规则版本） */
    public List<DirectionWeight> findWeightsByDirection(Long directionId, int versionNo) {
        return jdbc.query(
                "SELECT dimension, target_value, weight FROM direction_dimension_weight " +
                        "WHERE direction_id = ? AND version_no = ?",
                WEIGHT_MAPPER, directionId, versionNo);
    }

    /** 新增推荐批次，返回自增主键 */
    public long insertRun(Long studentId, Long snapshotId, String ruleVersion, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO recommendation_run (student_id, profile_snapshot_id, rule_version, status) " +
                            "VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, studentId);
            ps.setObject(2, snapshotId);
            ps.setString(3, ruleVersion);
            ps.setString(4, status);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }

    /** 新增推荐结果记录 */
    public void insertResult(long runId, long directionId, double score, int rank, String explanationJson) {
        jdbc.update(
                "INSERT INTO recommendation_result (run_id, direction_id, score, `rank`, explanation_json) " +
                        "VALUES (?, ?, ?, ?, ?)",
                runId, directionId, score, rank, explanationJson);
    }

    /** 新增推荐反馈记录（Demo 最小实现，不做存在性校验） */
    public void insertFeedback(Long resultId, String feedbackType, String comment) {
        jdbc.update(
                "INSERT INTO recommendation_feedback (result_id, feedback_type, comment) VALUES (?, ?, ?)",
                resultId, feedbackType, comment);
    }

    /** 查询最近一条推荐结果ID（Demo：反馈空 id 时兜底使用） */
    public Long findLatestResultId() {
        List<Long> ids = jdbc.query(
                "SELECT id FROM recommendation_result ORDER BY id DESC LIMIT 1",
                (rs, i) -> rs.getLong("id"));
        return ids.isEmpty() ? null : ids.get(0);
    }
}
