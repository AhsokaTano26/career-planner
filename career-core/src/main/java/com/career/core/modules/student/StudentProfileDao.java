package com.career.core.modules.student;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
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
            rs.getInt("version_no"),
            rs.getObject("completeness") == null ? null : rs.getDouble("completeness"),
            rs.getTimestamp("generated_at") == null ? null : rs.getTimestamp("generated_at").toLocalDateTime());

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
                snapshotSelect() +
                        " FROM profile_snapshot WHERE student_id = ? ORDER BY version_no DESC, id DESC LIMIT 1",
                SNAPSHOT_MAPPER, studentId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询属于当前学生的指定画像快照，防止通过 snapshotId 越权读取。 */
    public ProfileSnapshot findSnapshotById(Long snapshotId, Long studentId) {
        List<ProfileSnapshot> list = jdbc.query(
                snapshotSelect() +
                        " FROM profile_snapshot WHERE id = ? AND student_id = ?",
                SNAPSHOT_MAPPER, snapshotId, studentId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 按版本号倒序查询历史画像。 */
    public List<ProfileSnapshot> findSnapshotVersions(Long studentId, int offset, int size) {
        return jdbc.query(
                snapshotSelect() +
                        " FROM profile_snapshot WHERE student_id = ?" +
                        " ORDER BY version_no DESC, id DESC LIMIT ? OFFSET ?",
                SNAPSHOT_MAPPER, studentId, size, offset);
    }

    public int countSnapshots(Long studentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM profile_snapshot WHERE student_id = ?",
                Integer.class,
                studentId);
        return count == null ? 0 : count;
    }

    /** 下一版本号只在事务内调用，保证一次重算新增一个版本。 */
    public int nextSnapshotVersion(Long studentId) {
        Integer version = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no), 0) + 1 FROM profile_snapshot WHERE student_id = ?",
                Integer.class,
                studentId);
        return version == null ? 1 : version;
    }

    public long insertSnapshot(
            Long studentId,
            String sourceVersion,
            String dimensionJson,
            String summary,
            int versionNo,
            double completeness) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO profile_snapshot " +
                            "(student_id, source_version, dimension_json, summary, version_no, completeness) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, studentId);
            statement.setString(2, sourceVersion);
            statement.setString(3, dimensionJson);
            statement.setString(4, summary);
            statement.setInt(5, versionNo);
            statement.setDouble(6, completeness);
            return statement;
        }, keyHolder);
        if (keyHolder.getKey() == null) {
            throw new IllegalStateException("创建画像快照后未返回主键");
        }
        return keyHolder.getKey().longValue();
    }

    public void insertProfileFeedback(
            Long snapshotId,
            Long studentId,
            String feedbackType,
            String comment) {
        jdbc.update(
                "INSERT INTO profile_snapshot_feedback " +
                        "(snapshot_id, student_id, feedback_type, comment) VALUES (?, ?, ?, ?)",
                snapshotId, studentId, feedbackType, comment);
    }

    /** 查询学生经历列表 */
    public List<StudentExperience> findExperiences(Long studentId) {
        return jdbc.query(
                "SELECT id, student_id, type, title, start_date, description " +
                        "FROM student_experience WHERE student_id = ? ORDER BY id ASC",
                EXPERIENCE_MAPPER, studentId);
    }

    private static String snapshotSelect() {
        return "SELECT id, student_id, source_version, dimension_json, summary, " +
                "version_no, completeness, created_at AS generated_at";
    }
}
