-- H2-safe representation of the pre-core advisor-domain tables.
-- It lets the test prove that core-domains.sql upgrades an existing deployment
-- instead of relying on CREATE TABLE IF NOT EXISTS to create a fresh table.
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
