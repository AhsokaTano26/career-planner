package com.rickgao.careercore.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 对已存在的本地数据库执行小范围、幂等的结构补齐。
 * SQL 初始化脚本只会 CREATE IF NOT EXISTS，无法为旧表补列；此处先检测再执行 ALTER，兼容较低版本 MySQL。
 */
@Component
public class DatabaseSchemaMigration implements InitializingBean {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        ensurePasswordChangeRequiredColumn();
        ensureTokenVersionColumn();
        ensureRefreshTokenHashColumn();
        ensureReviewCycleUniqueKey();
        ensureAssessmentSessionColumns();
        ensureProfileSnapshotColumns();
    }

    /**
     * 令牌版本列（2026-09 稳定性：改密/停用后旧 accessToken 即刻失效，见 JwtAuthFilter）。
     * Demo 精简点 / 后续迭代替换位置：正式环境请改用 Flyway/Liquibase 版本化迁移。
     */
    private void ensureTokenVersionColumn() {
        ensureColumn("sys_user", "token_version",
                "ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本'");
    }

    /**
     * 刷新令牌哈希列（2026-09 复审：refresh 明文入库→哈希入库；双写兼容一轮，
     * 老行 token_hash 为空时读取回退明文并回填；下轮切读后可停写 token 明文）。
     */
    private void ensureRefreshTokenHashColumn() {
        ensureColumn("refresh_token", "token_hash",
                "ADD COLUMN token_hash VARCHAR(64) DEFAULT NULL COMMENT '刷新令牌SHA-256'");
        ensureIndex("refresh_token", "uk_token_hash",
                "ALTER TABLE refresh_token ADD UNIQUE KEY uk_token_hash (token_hash)");
    }

    private void ensurePasswordChangeRequiredColumn() {        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE table_schema = DATABASE() AND table_name = 'sys_user'
                  AND column_name = 'password_change_required'
                """, Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN password_change_required "
                    + "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改初始密码'");
        }
    }

    /**
     * core-domains.sql 曾在此处使用 MariaDB 方言 {@code ADD COLUMN IF NOT EXISTS}，
     * 标准 MySQL 8 会直接报语法错导致启动失败（2026-09 实测）。
     * Demo 精简点 / 后续迭代替换位置：正式环境请改用 Flyway/Liquibase 版本化迁移。
     */
    private void ensureAssessmentSessionColumns() {
        ensureColumn("assessment_session", "total_questions",
                "ADD COLUMN total_questions INT NOT NULL DEFAULT 0 COMMENT '题目总数'");
        ensureColumn("assessment_session", "answered_questions",
                "ADD COLUMN answered_questions INT NOT NULL DEFAULT 0 COMMENT '已答题数'");
        ensureColumn("assessment_session", "started_at",
                "ADD COLUMN started_at DATETIME DEFAULT NULL COMMENT '开始时间'");
        ensureColumn("assessment_session", "updated_at",
                "ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
        ensureColumn("assessment_session", "finished_at",
                "ADD COLUMN finished_at DATETIME DEFAULT NULL COMMENT '完成时间'");
        ensureColumn("assessment_session", "score_json",
                "ADD COLUMN score_json JSON DEFAULT NULL COMMENT '六维得分'");
    }

    private void ensureProfileSnapshotColumns() {
        ensureColumn("profile_snapshot", "source_version",
                "ADD COLUMN source_version VARCHAR(64) DEFAULT NULL COMMENT '来源版本(测评/档案版本)'");
        ensureColumn("profile_snapshot", "dimension_json",
                "ADD COLUMN dimension_json JSON DEFAULT NULL COMMENT '六维得分 [{key,name,score}]'");
        ensureColumn("profile_snapshot", "summary",
                "ADD COLUMN summary VARCHAR(1000) DEFAULT NULL COMMENT '画像摘要'");
        ensureColumn("profile_snapshot", "strengths_json",
                "ADD COLUMN strengths_json JSON DEFAULT NULL COMMENT '优势标签'");
        ensureColumn("profile_snapshot", "explore_json",
                "ADD COLUMN explore_json JSON DEFAULT NULL COMMENT '待探索点'");
        ensureColumn("profile_snapshot", "feedback_json",
                "ADD COLUMN feedback_json JSON DEFAULT NULL COMMENT '反馈 {feedbackType,comment}'");
        ensureColumn("profile_snapshot", "version_no",
                "ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '版本号'");
        ensureColumn("profile_snapshot", "completeness",
                "ADD COLUMN completeness INT NOT NULL DEFAULT 0 COMMENT '完整度(0-100)'");
        ensureColumn("profile_snapshot", "created_at",
                "ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
    }

    /**
     * 复盘周期唯一键（2026-09 复审：createReviewDraft 先查后写并发双击会产生重复 DRAFT；
     * UK(student_id, cycle) 让 DB 兜底，冲突走 409/回读更新。同一周期草稿与提交为同一行流转，
     * 故周期内只允许一行符合现有业务）。
     */
    private void ensureReviewCycleUniqueKey() {
        ensureIndex("stage_review", "uk_review_student_cycle",
                "ALTER TABLE stage_review ADD UNIQUE KEY uk_review_student_cycle (student_id, cycle)");
    }

    private void ensureIndex(String table, String index, String addIndexDdl) {
        if (!tableExists(table)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE table_schema = DATABASE() AND table_name = '""" + table + "' AND index_name = '" + index + "'",
                Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute(addIndexDdl);
        }
    }
    private void ensureColumn(String table, String column, String addColumnDdl) {
        // 复审 Batch4：表不存在时跳过（空库首启由 sql.init 建全量表；双实例赛跑时后建表者同样跳过）
        if (!tableExists(table)) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE table_schema = DATABASE() AND table_name = ?
                  AND column_name = ?
                """, Integer.class, table, column);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " " + addColumnDdl);
        }
    }

    /** 表不存在（空库首启 sql.init 尚未建表）时返回 false，调用方跳过补齐。 */
    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.TABLES
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

}
