-- ============================================================
-- admin 日志与导出模块建表脚本(MySQL 8, utf8mb4)
-- 管理端·日志与导出:导出任务 / AI 调用日志(只读)
-- 幂等:由 spring.sql.init 执行,CREATE TABLE IF NOT EXISTS
-- operation_audit_log 已存在于 schema.sql,不在此重复
-- ============================================================

-- 导出任务
CREATE TABLE IF NOT EXISTS export_job (
    id           VARCHAR(32)  NOT NULL COMMENT '任务ID,如 EX-001',
    type         VARCHAR(32)  NOT NULL COMMENT 'STUDENT_DATA/WHITELIST/OPERATION_LOG/AI_LOG/DIRECTION_LIB',
    scope        VARCHAR(500) DEFAULT NULL COMMENT '导出范围描述',
    filters_json JSON         DEFAULT NULL COMMENT '导出筛选条件(原始保存)',
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED',
    download_url VARCHAR(255) DEFAULT NULL COMMENT '下载地址',
    file_path    VARCHAR(500) DEFAULT NULL COMMENT '生成文件路径',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    operator_id  VARCHAR(32)  DEFAULT NULL COMMENT '操作人用户ID',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_export_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导出任务';

-- AI 调用日志(由 career-ai 组写入,管理端只读)
CREATE TABLE IF NOT EXISTS ai_call_log (
    id             VARCHAR(32) NOT NULL COMMENT '日志ID,如 AI-001',
    request_id     VARCHAR(64) DEFAULT NULL COMMENT '链路请求ID(X-Request-Id)',
    user_ref       VARCHAR(64) DEFAULT NULL COMMENT '脱敏用户引用',
    scene          VARCHAR(32) NOT NULL COMMENT 'recommendation_explain/plan_generate/review_summarize/career_chat/pdf_parse',
    model_name     VARCHAR(64) DEFAULT NULL,
    prompt_version VARCHAR(32) DEFAULT NULL,
    duration_ms    INT         DEFAULT NULL COMMENT '耗时(毫秒)',
    status         VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED/TIMEOUT/DEGRADED',
    token_estimate INT         DEFAULT NULL,
    request_hash   VARCHAR(64) DEFAULT NULL COMMENT '脱敏请求哈希',
    input_hash     VARCHAR(64) DEFAULT NULL COMMENT '输入哈希',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_request_id (request_id),
    KEY idx_ai_scene_status (scene, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用日志';

INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('export_job', 100);
