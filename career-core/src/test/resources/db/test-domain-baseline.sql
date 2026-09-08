-- H2-safe representation of the pre-core advisor-domain tables.
-- It lets the test prove that an existing deployment gets upgraded instead of
-- relying on CREATE TABLE IF NOT EXISTS to create a fresh table.
-- 注意（2026-09 复审）：core-domains.sql 的 MariaDB 方言 ALTER 已移除，生产升级路径
-- 改由 DatabaseSchemaMigration（本测试按既有设计继续 mock）；以下标准 DDL 与生产
-- 补列等价，保持“最小表 + 升级”的测试意图。MariaDB 方言（ADD COLUMN IF NOT EXISTS）禁止。
CREATE TABLE id_sequence (
    seq_name VARCHAR(64) NOT NULL,
    next_val BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (seq_name)
);

CREATE TABLE assessment_session (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    questionnaire_version_id VARCHAR(32) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    request_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 等价于 DatabaseSchemaMigration.ensureAssessmentSessionColumns（生产升级路径）
ALTER TABLE assessment_session ADD COLUMN total_questions INT NOT NULL DEFAULT 0;
ALTER TABLE assessment_session ADD COLUMN answered_questions INT NOT NULL DEFAULT 0;
ALTER TABLE assessment_session ADD COLUMN started_at DATETIME DEFAULT NULL;
ALTER TABLE assessment_session ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE assessment_session ADD COLUMN finished_at DATETIME DEFAULT NULL;
ALTER TABLE assessment_session ADD COLUMN score_json JSON DEFAULT NULL;

CREATE TABLE profile_snapshot (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    source_version VARCHAR(64) DEFAULT NULL,
    dimension_json JSON DEFAULT NULL,
    summary VARCHAR(1000) DEFAULT NULL,
    strengths_json JSON DEFAULT NULL,
    explore_json JSON DEFAULT NULL,
    feedback_json JSON DEFAULT NULL,
    version_no INT NOT NULL DEFAULT 1,
    completeness INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
