-- ============================================================
-- advisor 模块建表脚本(MySQL 8, utf8mb4)
-- 依据《开发设计说明书》第 8 章与 openapi「辅导员端」契约
-- 幂等:由 spring.sql.init 在 schema.sql 之后执行,CREATE TABLE IF NOT EXISTS
-- 约定:snake_case;主键为字符串业务 ID;业务表含 created_at/updated_at,可软删表含 deleted
-- ============================================================

-- 辅导员-学生管理关系(辅导员数据范围校验的唯一依据)
CREATE TABLE IF NOT EXISTS advisor_student_relation (
    id         VARCHAR(32) NOT NULL COMMENT '关系ID,如 AR-001',
    advisor_id VARCHAR(32) NOT NULL COMMENT '辅导员用户ID(sys_user.id)',
    student_id VARCHAR(32) NOT NULL COMMENT '学生用户ID(student_profile.user_id)',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_advisor_student (advisor_id, student_id),
    KEY idx_relation_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅导员-学生管理关系';

-- 指导意见/建议记录(COMMENT/SUGGEST_TASK/SUGGEST_RETEST 共用)
CREATE TABLE IF NOT EXISTS advisor_comment (
    id             VARCHAR(32)   NOT NULL COMMENT '意见ID,如 GC-001',
    student_id     VARCHAR(32)   NOT NULL COMMENT '学生用户ID(student_profile.user_id)',
    advisor_id     VARCHAR(32)   NOT NULL COMMENT '填写辅导员ID(sys_user.id)',
    content        VARCHAR(2000) NOT NULL COMMENT '指导意见正文',
    advice_type    VARCHAR(20)   NOT NULL COMMENT 'COMMENT/SUGGEST_TASK/SUGGEST_RETEST',
    suggested_task VARCHAR(500)  DEFAULT NULL COMMENT '建议任务(SUGGEST_TASK 时必填)',
    retest_reason  VARCHAR(500)  DEFAULT NULL COMMENT '建议重新测评原因(SUGGEST_RETEST 时必填)',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    KEY idx_comment_student_created (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅导员指导意见/建议';

-- 写接口幂等记录(Idempotency-Key: user_id + endpoint + request_key 唯一)
CREATE TABLE IF NOT EXISTS idempotency_record (
    id            VARCHAR(32) NOT NULL COMMENT '记录ID',
    user_id       VARCHAR(32) NOT NULL COMMENT '操作者用户ID',
    endpoint      VARCHAR(100) NOT NULL COMMENT '接口路径(不含路径参数)',
    request_key   VARCHAR(64) NOT NULL COMMENT 'Idempotency-Key(UUID)',
    request_hash  VARCHAR(64) DEFAULT NULL COMMENT '请求体摘要(可选)',
    status        VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCESS',
    response_code VARCHAR(20) DEFAULT NULL COMMENT '首次成功响应的业务码',
    response_body TEXT        DEFAULT NULL COMMENT '首次成功响应的 data JSON(重放用)',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at    DATETIME    NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL 24 HOUR) COMMENT '24 小时后可清理',
    PRIMARY KEY (id),
    UNIQUE KEY uk_idem_user_endpoint_key (user_id, endpoint, request_key),
    KEY idx_idem_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='写接口幂等记录';

-- 字符串 ID 序列初始值(幂等,补充到 data.sql 的 id_sequence 种子)
INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('advisor_student_relation', 1),
('advisor_comment', 1),
('idempotency_record', 1);
