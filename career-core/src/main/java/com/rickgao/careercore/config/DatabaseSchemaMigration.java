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
    }

    private void ensurePasswordChangeRequiredColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE table_schema = DATABASE() AND table_name = 'sys_user'
                  AND column_name = 'password_change_required'
                """, Integer.class);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN password_change_required "
                    + "TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改初始密码'");
        }
    }

}
