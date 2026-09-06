package com.rickgao.careercore.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        ensureAssessmentSessionColumns();
        ensureProfileSnapshotColumns();
    }

    private void ensurePasswordChangeRequiredColumn() {
        ensureColumn("sys_user", "password_change_required",
                "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改初始密码'");
    }

    private void ensureAssessmentSessionColumns() {
        ensureColumn("assessment_session", "total_questions", "INT NOT NULL DEFAULT 0 COMMENT '题目总数'");
        ensureColumn("assessment_session", "answered_questions", "INT NOT NULL DEFAULT 0 COMMENT '已答题数'");
        ensureColumn("assessment_session", "started_at", "DATETIME DEFAULT NULL COMMENT '开始时间'");
        ensureColumn("assessment_session", "updated_at",
                "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
        ensureColumn("assessment_session", "finished_at", "DATETIME DEFAULT NULL COMMENT '完成时间'");
        ensureColumn("assessment_session", "score_json", "JSON DEFAULT NULL COMMENT '六维得分'");
    }

    private void ensureProfileSnapshotColumns() {
        ensureColumn("profile_snapshot", "source_version", "VARCHAR(64) DEFAULT NULL COMMENT '来源版本(测评/档案版本)'");
        ensureColumn("profile_snapshot", "dimension_json", "JSON DEFAULT NULL COMMENT '六维得分 [{key,name,score}]'");
        ensureColumn("profile_snapshot", "summary", "VARCHAR(1000) DEFAULT NULL COMMENT '画像摘要'");
        ensureColumn("profile_snapshot", "strengths_json", "JSON DEFAULT NULL COMMENT '优势标签'");
        ensureColumn("profile_snapshot", "explore_json", "JSON DEFAULT NULL COMMENT '待探索点'");
        ensureColumn("profile_snapshot", "feedback_json", "JSON DEFAULT NULL COMMENT '反馈 {feedbackType,comment}'");
        ensureColumn("profile_snapshot", "version_no", "INT NOT NULL DEFAULT 1 COMMENT '版本号'");
        ensureColumn("profile_snapshot", "completeness", "INT NOT NULL DEFAULT 0 COMMENT '完整度(0-100)'");
        ensureColumn("profile_snapshot", "created_at", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
    }

    private void ensureColumn(String tableName, String columnName, String definition) {
        if (!columnExists(tableName, columnName) && tableExists(tableName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                metadataContains(connection.getMetaData(), connection.getCatalog(), tableName, columnName, false)));
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                metadataContains(connection.getMetaData(), connection.getCatalog(), tableName, null, true)));
    }

    private boolean metadataContains(DatabaseMetaData metadata, String catalog, String tableName,
                                     String columnName, boolean tableOnly) throws SQLException {
        if (tableOnly) {
            try (ResultSet tables = metadata.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
                return tables.next();
            }
        }
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }

}
