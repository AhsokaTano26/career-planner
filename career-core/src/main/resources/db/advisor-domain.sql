-- 供辅导员工作台查询的领域表。
-- 各模块尚未提供独立迁移时，在正式初始化阶段创建最小兼容结构；所有建表均为幂等操作。

CREATE TABLE IF NOT EXISTS assessment_session (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    questionnaire_version_id VARCHAR(32) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    request_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评会话';

CREATE TABLE IF NOT EXISTS profile_snapshot (
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
    PRIMARY KEY (id),
    KEY idx_snapshot_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画像快照';

CREATE TABLE IF NOT EXISTS recommendation_run (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    profile_snapshot_id VARCHAR(32) DEFAULT NULL,
    rule_version VARCHAR(32) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    generated_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐批次';

CREATE TABLE IF NOT EXISTS recommendation_result (
    id VARCHAR(32) NOT NULL,
    run_id VARCHAR(32) NOT NULL,
    direction_id VARCHAR(64) NOT NULL,
    score DECIMAL(5,1) NOT NULL,
    `rank` INT NOT NULL,
    confidence VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    reasons_json JSON DEFAULT NULL,
    strengths_json JSON DEFAULT NULL,
    gaps_json JSON DEFAULT NULL,
    semester_actions_json JSON DEFAULT NULL,
    feedback_json JSON DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_run_direction (run_id, direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果';

CREATE TABLE IF NOT EXISTS student_goal (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    goal_type VARCHAR(10) NOT NULL DEFAULT 'PRIMARY',
    direction_id VARCHAR(64) DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    chosen_at DATETIME DEFAULT NULL,
    version_no INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_student (student_id, goal_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生目标';

CREATE TABLE IF NOT EXISTS goal_version (
    id VARCHAR(32) NOT NULL,
    goal_id VARCHAR(32) NOT NULL,
    version_no INT NOT NULL,
    change_reason VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_version (goal_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标版本';

CREATE TABLE IF NOT EXISTS semester_plan (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    goal_summary VARCHAR(1000) DEFAULT NULL,
    semester_goals_json JSON DEFAULT NULL,
    monthly_tasks_json JSON DEFAULT NULL,
    notes_json JSON DEFAULT NULL,
    confirmed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_plan_student (student_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期计划';

CREATE TABLE IF NOT EXISTS plan_task (
    id VARCHAR(32) NOT NULL,
    plan_id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    month VARCHAR(7) NOT NULL,
    title VARCHAR(200) NOT NULL,
    task_type VARCHAR(20) NOT NULL DEFAULT 'LEARNING',
    est_hours DECIMAL(6,1) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    deadline DATE DEFAULT NULL,
    ability_tags_json JSON DEFAULT NULL,
    note VARCHAR(1000) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_student (student_id, status, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划任务';

CREATE TABLE IF NOT EXISTS task_checkin (
    id VARCHAR(32) NOT NULL,
    task_id VARCHAR(32) NOT NULL,
    done_desc VARCHAR(1000) NOT NULL,
    gains VARCHAR(1000) DEFAULT NULL,
    difficulties VARCHAR(1000) DEFAULT NULL,
    proof_url VARCHAR(255) DEFAULT NULL,
    checked_in_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_checkin_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务打卡';

CREATE TABLE IF NOT EXISTS stage_review (
    id VARCHAR(32) NOT NULL,
    student_id VARCHAR(32) NOT NULL,
    cycle VARCHAR(7) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    content_json JSON DEFAULT NULL,
    ai_summary VARCHAR(2000) DEFAULT NULL,
    ai_suggest_json JSON DEFAULT NULL,
    advisor_requested TINYINT(1) NOT NULL DEFAULT 0,
    advisor_reply VARCHAR(2000) DEFAULT NULL,
    submitted_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_student (student_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段复盘';
