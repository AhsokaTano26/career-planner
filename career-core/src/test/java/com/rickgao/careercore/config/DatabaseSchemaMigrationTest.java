package com.rickgao.careercore.config;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseSchemaMigrationTest {

    @Test
    void upgradesLegacyAssessmentSessionAndCanRunMoreThanOnce() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:schema_migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE SCHEMA schema_migration");
        jdbcTemplate.execute("SET SCHEMA schema_migration");
        jdbcTemplate.execute("CREATE TABLE sys_user (id VARCHAR(32) PRIMARY KEY)");
        jdbcTemplate.execute("""
                CREATE TABLE assessment_session (
                    id VARCHAR(32) PRIMARY KEY,
                    student_id VARCHAR(32) NOT NULL,
                    questionnaire_version_id VARCHAR(32),
                    status VARCHAR(20) NOT NULL,
                    request_id VARCHAR(64),
                    created_at DATETIME NOT NULL
                )
                """);

        DatabaseSchemaMigration migration = new DatabaseSchemaMigration(jdbcTemplate);
        migration.afterPropertiesSet();
        migration.afterPropertiesSet();

        assertThat(columnNames(jdbcTemplate, "assessment_session")).contains(
                "total_questions", "answered_questions", "started_at", "updated_at", "finished_at", "score_json");
    }

    private static java.util.List<String> columnNames(JdbcTemplate jdbcTemplate, String tableName) {
        return jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE LOWER(table_name) = LOWER(?)
                """, String.class, tableName);
    }
}
