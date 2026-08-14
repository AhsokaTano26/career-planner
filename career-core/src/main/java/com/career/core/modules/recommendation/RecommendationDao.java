package com.career.core.modules.recommendation;

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
            rs.getString("content"),
            rs.getString("personality_tags"));

    private static final RowMapper<DirectionWeight> WEIGHT_MAPPER = (rs, i) -> new DirectionWeight(
            rs.getString("dimension"),
            rs.getBigDecimal("target_value") == null ? null : rs.getBigDecimal("target_value").doubleValue(),
            rs.getBigDecimal("weight") == null ? null : rs.getBigDecimal("weight").doubleValue());

    /** 查询全部方向库 */
    public List<CareerDirection> findAllDirections() {
        return jdbc.query(
                "SELECT id, direction_code, name, type, status, content, personality_tags FROM career_direction ORDER BY id ASC",
                DIRECTION_MAPPER);
    }

    /** 按方向编码查询方向（不存在返回 null） */
    public CareerDirection findDirectionByCode(String directionCode) {
        List<CareerDirection> list = jdbc.query(
                "SELECT id, direction_code, name, type, status, content, personality_tags " +
                        "FROM career_direction WHERE direction_code = ? LIMIT 1",
                DIRECTION_MAPPER, directionCode);
        return list.isEmpty() ? null : list.get(0);
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

    /** 新增推荐结果记录（score 为百分制 0-100；explanationJson 存结构化解释 JSON） */
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

    /** 批次行（查询用投影） */
    public record RunRow(long id, long studentId, long profileSnapshotId, String ruleVersion,
                         String status, LocalDateTime createdAt) {
    }

    /** 结果行（查询用投影） */
    public record ResultRow(long id, long directionId, double score, int rank,
                            String explanationJson, LocalDateTime createdAt) {
    }

    private static final RowMapper<RunRow> RUN_ROW_MAPPER = (rs, i) -> new RunRow(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getLong("profile_snapshot_id"),
            rs.getString("rule_version"),
            rs.getString("status"),
            toLocalDateTime(rs.getTimestamp("created_at")));

    private static final RowMapper<ResultRow> RESULT_ROW_MAPPER = (rs, i) -> new ResultRow(
            rs.getLong("id"),
            rs.getLong("direction_id"),
            rs.getDouble("score"),
            rs.getInt("rank"),
            rs.getString("explanation_json"),
            toLocalDateTime(rs.getTimestamp("created_at")));

    /** 查询学生最新一次推荐批次（按 id 倒序取第一条；无则返回 null） */
    public RunRow findLatestRun(Long studentId) {
        List<RunRow> list = jdbc.query(
                "SELECT id, student_id, profile_snapshot_id, rule_version, status, created_at " +
                        "FROM recommendation_run WHERE student_id = ? ORDER BY id DESC LIMIT 1",
                RUN_ROW_MAPPER, studentId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 按批次 ID 查询批次（无则返回 null） */
    public RunRow findRunById(long runId) {
        List<RunRow> list = jdbc.query(
                "SELECT id, student_id, profile_snapshot_id, rule_version, status, created_at " +
                        "FROM recommendation_run WHERE id = ? LIMIT 1",
                RUN_ROW_MAPPER, runId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询学生历史推荐批次（按 id 倒序，分页） */
    public List<RunRow> findRunsByStudent(Long studentId, int page, int size) {
        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        return jdbc.query(
                "SELECT id, student_id, profile_snapshot_id, rule_version, status, created_at " +
                        "FROM recommendation_run WHERE student_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                RUN_ROW_MAPPER, studentId, size, offset);
    }

    /** 查询批次下的推荐结果（按 rank 升序） */
    public List<ResultRow> findResultsByRunId(long runId) {
        return jdbc.query(
                "SELECT id, direction_id, score, `rank`, explanation_json, created_at " +
                        "FROM recommendation_result WHERE run_id = ? ORDER BY `rank` ASC",
                RESULT_ROW_MAPPER, runId);
    }

    /** 查询推荐结果ID是否存在（反馈接口校验） */
    public boolean existsResult(long resultId) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM recommendation_result WHERE id = ?", Integer.class, resultId);
        return cnt != null && cnt > 0;
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
