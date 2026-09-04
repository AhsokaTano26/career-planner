package com.rickgao.careercore;

import com.rickgao.careercore.config.DatabaseSchemaMigration;
import com.rickgao.careercore.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CareerCoreApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DatabaseSchemaMigration databaseSchemaMigration;

    @MockBean
    private DataInitializer dataInitializer;

    @Test
    void contextLoads() {
    }

    @Test
    void initializesAiDomainTablesForJdbcAccess() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assessment_session", Integer.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM profile_snapshot", Integer.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_chat_message", Integer.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_chat_feedback", Integer.class)).isNotNull();
    }

    @Test
    void initializesAiDomainTablesWithServiceRequiredColumns() {
        jdbcTemplate.queryForList("""
                SELECT total_questions, answered_questions, started_at, updated_at, finished_at, score_json, request_id
                FROM assessment_session
                WHERE 1 = 0
                """);
        jdbcTemplate.queryForList("""
                SELECT source_version, dimension_json, feedback_json, version_no, completeness
                FROM profile_snapshot
                WHERE 1 = 0
                """);
        jdbcTemplate.queryForList("""
                SELECT session_id, user_id, role, content, needs_human_support, support_reason, message_group, created_at
                FROM ai_chat_message
                WHERE 1 = 0
                """);
        jdbcTemplate.queryForList("""
                SELECT message_group, user_id, feedback_type, comment, created_at
                FROM ai_chat_feedback
                WHERE 1 = 0
                """);
    }

}
