package com.career.core.modules.student;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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
            rs.getString("summary"));

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
                "SELECT id, student_id, source_version, dimension_json, summary " +
                        "FROM profile_snapshot WHERE student_id = ? ORDER BY id DESC LIMIT 1",
                SNAPSHOT_MAPPER, studentId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询学生经历列表 */
    public List<StudentExperience> findExperiences(Long studentId) {
        return jdbc.query(
                "SELECT id, student_id, type, title, start_date, description " +
                        "FROM student_experience WHERE student_id = ? ORDER BY id ASC",
                EXPERIENCE_MAPPER, studentId);
    }
}
