package com.rickgao.careercore;

import com.rickgao.careercore.config.DatabaseSchemaMigration;
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

    @Test
    void contextLoads() {
    }

    @Test
    void initializesAiDomainTablesForJdbcAccess() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assessment_session", Integer.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM profile_snapshot", Integer.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_chat_message", Integer.class)).isNotNull();
    }

}
