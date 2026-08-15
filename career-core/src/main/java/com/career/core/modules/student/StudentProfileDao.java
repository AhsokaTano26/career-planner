package com.career.core.modules.student;

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
 * 学生画像数据访问层（JdbcTemplate 直连，Demo 最小实现；
 * 后续迭代可替换为 MyBatis/MyBatis-Plus Mapper）。
 */
@Repository
public class StudentProfileDao {

    private final JdbcTemplate jdbc;

    public StudentProfileDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<StudentProfile> STUDENT_MAPPER = (rs, i) -> new StudentProfile(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("student_no"),
            rs.getString("major_category"),
            rs.getString("grade"),
            rs.getString("class_name"));

    private static final RowMapper<ProfileSnapshot> SNAPSHOT_MAPPER = (rs, i) -> new ProfileSnapshot(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getString("source_version"),
            rs.getString("dimension_json"),
            rs.getString("summary"),
            toLocalDateTime(rs.getTimestamp("created_at")));

    private static final RowMapper<StudentExperience> EXPERIENCE_MAPPER = (rs, i) -> new StudentExperience(
            rs.getLong("id"),
            rs.getLong("student_id"),
            rs.getString("type"),
            rs.getString("title"),
            rs.getObject("start_date", java.sql.Date.class) != null
                    ? rs.getObject("start_date", java.sql.Date.class).toLocalDate() : null,
            rs.getString("description"));

    /** 按学生ID（user_id）查询学生画像主表 */
    public StudentProfile findStudentByUserId(Long userId) {
        List<StudentProfile> list = jdbc.query(
                "SELECT id, user_id, student_no, major_category, grade, class_name " +
                        "FROM student_profile WHERE user_id = ?", STUDENT_MAPPER, userId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询学生最新画像快照（按 id 倒序取最新一条） */
    public ProfileSnapshot findLatestSnapshot(Long studentId) {
        List<ProfileSnapshot> list = jdbc.query(
                "SELECT id, student_id, source_version, dimension_json, summary, created_at " +
                        "FROM profile_snapshot WHERE student_id = ? ORDER BY id DESC LIMIT 1",
                SNAPSHOT_MAPPER, studentId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询学生历史画像快照（按 id 倒序，分页） */
    public List<ProfileSnapshot> findSnapshots(Long studentId, int page, int size) {
        int offset = (Math.max(page, 1) - 1) * Math.max(size, 1);
        return jdbc.query(
                "SELECT id, student_id, source_version, dimension_json, summary, created_at " +
                        "FROM profile_snapshot WHERE student_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
                SNAPSHOT_MAPPER, studentId, size, offset);
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    /** 查询学生经历列表 */
    public List<StudentExperience> findExperiences(Long studentId) {
        return jdbc.query(
                "SELECT id, student_id, type, title, start_date, description " +
                        "FROM student_experience WHERE student_id = ? ORDER BY id ASC",
                EXPERIENCE_MAPPER, studentId);
    }

    /** 按快照ID查询画像快照（详情） */
    public ProfileSnapshot findSnapshotById(Long snapshotId) {
        List<ProfileSnapshot> list = jdbc.query(
                "SELECT id, student_id, source_version, dimension_json, summary, created_at " +
                        "FROM profile_snapshot WHERE id = ?",
                SNAPSHOT_MAPPER, snapshotId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 新增画像反馈 */
    public void insertFeedback(Long snapshotId, Long studentId, String feedbackType, String comment) {
        jdbc.update(
                "INSERT INTO profile_snapshot_feedback (snapshot_id, student_id, feedback_type, comment) VALUES (?, ?, ?, ?)",
                snapshotId, studentId, feedbackType, comment);
    }

    /** 新增画像快照（返回自增主键），供「重新生成画像」复制最新快照 */
    public long insertSnapshot(Long studentId, String sourceVersion, String dimensionJson, String summary) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO profile_snapshot (student_id, source_version, dimension_json, summary) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, studentId);
            ps.setString(2, sourceVersion);
            ps.setString(3, dimensionJson);
            ps.setString(4, summary);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0L : key.longValue();
    }
}
