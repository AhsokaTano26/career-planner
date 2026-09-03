-- ============================================================
-- career-core 核心业务域建表脚本（测评/画像/推荐/目标/计划/任务/复盘/收藏/提醒/问卷/模型配置）
-- 由 dev/advisor-dev-setup.sql 的最小结构升级为正式表（VARCHAR 主键，对齐项目 ID 规范）
-- 幂等:由 spring.sql.init 执行,CREATE TABLE IF NOT EXISTS
-- ============================================================

-- ---------- 测评会话 ----------
CREATE TABLE IF NOT EXISTS assessment_session (
    id                       VARCHAR(32) NOT NULL COMMENT '会话 ID',
    student_id               VARCHAR(32) NOT NULL COMMENT '学生 ID',
    questionnaire_version_id VARCHAR(32) DEFAULT NULL COMMENT '问卷版本 ID',
    status                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/IN_PROGRESS/COMPLETED/SCORED',
    total_questions          INT         NOT NULL DEFAULT 0,
    answered_questions       INT         NOT NULL DEFAULT 0,
    started_at               DATETIME    DEFAULT NULL,
    updated_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    finished_at              DATETIME    DEFAULT NULL,
    score_json               JSON        DEFAULT NULL COMMENT '六维得分',
    request_id               VARCHAR(64) DEFAULT NULL COMMENT '幂等请求 ID',
    created_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评会话';

-- ---------- 画像快照 ----------
CREATE TABLE IF NOT EXISTS profile_snapshot (
    id             VARCHAR(32) NOT NULL COMMENT '快照 ID',
    student_id     VARCHAR(32) NOT NULL COMMENT '学生 ID',
    source_version VARCHAR(64) DEFAULT NULL COMMENT '来源版本(测评/档案版本)',
    dimension_json JSON        DEFAULT NULL COMMENT '六维得分 [{key,name,score}]',
    summary        VARCHAR(1000) DEFAULT NULL COMMENT '画像摘要',
    strengths_json JSON        DEFAULT NULL COMMENT '优势标签',
    explore_json   JSON        DEFAULT NULL COMMENT '待探索点',
    feedback_json  JSON        DEFAULT NULL COMMENT '反馈 {feedbackType,comment}',
    version_no     INT         NOT NULL DEFAULT 1 COMMENT '版本号',
    completeness   INT         NOT NULL DEFAULT 0 COMMENT '完整度(0-100)',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_snapshot_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画像快照';

-- ---------- 推荐批次 ----------
CREATE TABLE IF NOT EXISTS recommendation_run (
    id                  VARCHAR(32) NOT NULL COMMENT '批次 ID',
    student_id          VARCHAR(32) NOT NULL COMMENT '学生 ID',
    profile_snapshot_id VARCHAR(32) DEFAULT NULL COMMENT '画像快照 ID',
    rule_version        VARCHAR(32) DEFAULT NULL COMMENT '规则版本',
    status              VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'RUNNING/SUCCESS/DEGRADED/FAILED',
    generated_at        DATETIME    DEFAULT NULL COMMENT '生成时间',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐批次';

-- ---------- 推荐结果 ----------
CREATE TABLE IF NOT EXISTS recommendation_result (
    id                    VARCHAR(32) NOT NULL COMMENT '结果 ID',
    run_id                VARCHAR(32) NOT NULL COMMENT '批次 ID',
    direction_id          VARCHAR(64) NOT NULL COMMENT '方向编码',
    score                 DECIMAL(5,1) NOT NULL COMMENT '匹配分(0-100)',
    `rank`                INT         NOT NULL COMMENT '名次',
    confidence            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT 'HIGH/MEDIUM/LOW',
    reasons_json          JSON        DEFAULT NULL COMMENT '推荐理由数组',
    strengths_json        JSON        DEFAULT NULL COMMENT '优势数组',
    gaps_json             JSON        DEFAULT NULL COMMENT '差距数组',
    semester_actions_json JSON        DEFAULT NULL COMMENT '学期行动建议数组',
    feedback_json         JSON        DEFAULT NULL COMMENT '反馈 {feedbackType,comment}',
    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_run_direction (run_id, direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果';

-- ---------- 学生目标 ----------
CREATE TABLE IF NOT EXISTS student_goal (
    id           VARCHAR(32) NOT NULL COMMENT '目标 ID',
    student_id   VARCHAR(32) NOT NULL COMMENT '学生 ID',
    goal_type    VARCHAR(10) NOT NULL DEFAULT 'PRIMARY' COMMENT 'PRIMARY/BACKUP',
    direction_id VARCHAR(64) DEFAULT NULL COMMENT '方向编码',
    name         VARCHAR(100) NOT NULL COMMENT '方向名称',
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    chosen_at    DATETIME    DEFAULT NULL COMMENT '选择时间',
    version_no   INT         NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_student (student_id, goal_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生目标';

-- ---------- 目标版本 ----------
CREATE TABLE IF NOT EXISTS goal_version (
    id            VARCHAR(32) NOT NULL COMMENT '版本 ID',
    goal_id       VARCHAR(32) NOT NULL COMMENT '目标 ID',
    version_no    INT         NOT NULL COMMENT '版本号',
    change_reason VARCHAR(255) DEFAULT NULL COMMENT '变更原因',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_version (goal_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标版本';

-- ---------- 学期计划 ----------
CREATE TABLE IF NOT EXISTS semester_plan (
    id                  VARCHAR(32) NOT NULL COMMENT '计划 ID',
    student_id          VARCHAR(32) NOT NULL COMMENT '学生 ID',
    version_no          INT         NOT NULL DEFAULT 1 COMMENT '版本号',
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED',
    source              VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'AI/TEMPLATE/MANUAL',
    goal_summary        VARCHAR(1000) DEFAULT NULL COMMENT '目标摘要',
    semester_goals_json JSON        DEFAULT NULL COMMENT '学期目标数组 [{title,abilityTag}]',
    monthly_tasks_json  JSON        DEFAULT NULL COMMENT '月度任务数组 [{month,title,taskType,estimatedHours}]',
    notes_json          JSON        DEFAULT NULL COMMENT '备注数组',
    confirmed_at        DATETIME    DEFAULT NULL COMMENT '确认时间',
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_plan_student (student_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期计划';

-- ---------- 计划版本 ----------
CREATE TABLE IF NOT EXISTS plan_version (
    id          VARCHAR(32) NOT NULL COMMENT '版本 ID',
    plan_id     VARCHAR(32) NOT NULL COMMENT '计划 ID',
    version_no  INT         NOT NULL COMMENT '版本号',
    content_json JSON       DEFAULT NULL COMMENT '计划内容快照',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_plan_version (plan_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划版本';

-- ---------- 计划任务 ----------
CREATE TABLE IF NOT EXISTS plan_task (
    id               VARCHAR(32) NOT NULL COMMENT '任务 ID',
    plan_id          VARCHAR(32) NOT NULL COMMENT '计划 ID',
    student_id       VARCHAR(32) NOT NULL COMMENT '学生 ID',
    month            VARCHAR(7) NOT NULL COMMENT '月份 YYYY-MM',
    title            VARCHAR(200) NOT NULL COMMENT '任务标题',
    task_type        VARCHAR(20) NOT NULL DEFAULT 'LEARNING' COMMENT 'LEARNING/PRACTICE/CAREER/REVIEW',
    est_hours        DECIMAL(6,1) DEFAULT NULL COMMENT '预估小时',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DOING/DONE/DELAYED/ABANDONED',
    deadline         DATE        DEFAULT NULL COMMENT '截止日期',
    ability_tags_json JSON       DEFAULT NULL COMMENT '能力标签数组',
    note             VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_student (student_id, status, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划任务';

-- ---------- 任务打卡 ----------
CREATE TABLE IF NOT EXISTS task_checkin (
    id            VARCHAR(32) NOT NULL COMMENT '打卡 ID',
    task_id       VARCHAR(32) NOT NULL COMMENT '任务 ID',
    done_desc     VARCHAR(1000) NOT NULL COMMENT '完成说明',
    gains         VARCHAR(1000) DEFAULT NULL COMMENT '收获',
    difficulties  VARCHAR(1000) DEFAULT NULL COMMENT '困难',
    proof_url     VARCHAR(255) DEFAULT NULL COMMENT '证明链接',
    checked_in_at DATETIME    DEFAULT NULL COMMENT '打卡时间',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_checkin_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务打卡';

-- ---------- 阶段复盘 ----------
CREATE TABLE IF NOT EXISTS stage_review (
    id               VARCHAR(32) NOT NULL COMMENT '复盘 ID',
    student_id       VARCHAR(32) NOT NULL COMMENT '学生 ID',
    cycle            VARCHAR(7) NOT NULL COMMENT '周期 YYYY-MM',
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED',
    content_json     JSON        DEFAULT NULL COMMENT '复盘内容 {done,undone,interest,ability,next}',
    ai_summary       VARCHAR(2000) DEFAULT NULL COMMENT 'AI 总结',
    ai_suggest_json  JSON        DEFAULT NULL COMMENT 'AI 建议数组',
    advisor_requested TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否申请辅导员指导',
    advisor_reply    VARCHAR(2000) DEFAULT NULL COMMENT '辅导员回复',
    submitted_at     DATETIME    DEFAULT NULL COMMENT '提交时间',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_student (student_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段复盘';

-- ---------- 学生收藏方向 ----------
CREATE TABLE IF NOT EXISTS favorite (
    id           VARCHAR(32) NOT NULL COMMENT '收藏 ID',
    student_id   VARCHAR(32) NOT NULL COMMENT '学生 ID',
    direction_id VARCHAR(64) NOT NULL COMMENT '方向编码',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite (student_id, direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生收藏方向';

-- ---------- 站内提醒 ----------
CREATE TABLE IF NOT EXISTS reminder (
    id         VARCHAR(32) NOT NULL COMMENT '提醒 ID',
    student_id VARCHAR(32) NOT NULL COMMENT '学生 ID',
    type       VARCHAR(30) NOT NULL DEFAULT 'TASK_DEADLINE' COMMENT 'TASK_DEADLINE/REVIEW_REMIND/ADVISOR_REPLY/PLAN_UPDATE',
    title      VARCHAR(200) NOT NULL COMMENT '标题',
    content    VARCHAR(1000) DEFAULT NULL COMMENT '内容',
    is_read    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_reminder_student (student_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内提醒';

-- ---------- 问卷(管理端+学生测评) ----------
CREATE TABLE IF NOT EXISTS questionnaire (
    id         VARCHAR(32) NOT NULL COMMENT '问卷 ID',
    type       VARCHAR(20) NOT NULL COMMENT 'holland/values/ability/tendency',
    name       VARCHAR(100) NOT NULL COMMENT '问卷名',
    type_name  VARCHAR(50) DEFAULT NULL COMMENT '类型显示名',
    icon       VARCHAR(50) DEFAULT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    version    INT         NOT NULL DEFAULT 1 COMMENT '当前版本号',
    minutes    INT         DEFAULT NULL COMMENT '预计分钟',
    tip        VARCHAR(500) DEFAULT NULL,
    published_at DATETIME  DEFAULT NULL,
    published_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷';

CREATE TABLE IF NOT EXISTS questionnaire_version (
    id           VARCHAR(32) NOT NULL COMMENT '版本 ID',
    questionnaire_id VARCHAR(32) NOT NULL COMMENT '问卷 ID',
    version      INT         NOT NULL COMMENT '版本号',
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    question_count INT       NOT NULL DEFAULT 0,
    change_note  VARCHAR(255) DEFAULT NULL,
    published_at DATETIME    DEFAULT NULL,
    published_by VARCHAR(64) DEFAULT NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_q_version (questionnaire_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷版本';

CREATE TABLE IF NOT EXISTS question (
    id             VARCHAR(32) NOT NULL COMMENT '题目 ID',
    questionnaire_version_id VARCHAR(32) NOT NULL COMMENT '问卷版本 ID',
    text           VARCHAR(500) NOT NULL COMMENT '题干',
    type           VARCHAR(20) NOT NULL DEFAULT 'CHOICE' COMMENT 'CHOICE/RATING',
    dim            VARCHAR(20) DEFAULT NULL COMMENT '维度 code',
    sort_order     INT NOT NULL DEFAULT 0,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_question_version (questionnaire_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷题目';

CREATE TABLE IF NOT EXISTS question_option (
    id         VARCHAR(32) NOT NULL COMMENT '选项 ID',
    question_id VARCHAR(32) NOT NULL COMMENT '题目 ID',
    text       VARCHAR(200) NOT NULL COMMENT '选项文案',
    scores_json JSON DEFAULT NULL COMMENT '六维得分 {interest,values,ability,academic,tendency,practice}',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_option_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷选项';

-- ---------- 模型与提示词配置(admin) ----------
CREATE TABLE IF NOT EXISTS model_config (
    id         VARCHAR(32) NOT NULL COMMENT '配置 ID',
    config_key VARCHAR(100) NOT NULL COMMENT '键,如 llm.provider',
    config_value VARCHAR(1000) DEFAULT NULL COMMENT '值(密钥掩码)',
    updated_by VARCHAR(64) DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型配置';

CREATE TABLE IF NOT EXISTS prompt_version (
    id         VARCHAR(32) NOT NULL COMMENT '提示词版本 ID',
    scene      VARCHAR(50) NOT NULL COMMENT 'recommendation_explain/plan_generate/review_summarize/career_chat',
    version    VARCHAR(32) NOT NULL COMMENT '版本号,如 P1',
    status     VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    content    TEXT COMMENT '提示词内容',
    published_at DATETIME DEFAULT NULL,
    published_by VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prompt_scene_version (scene, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示词版本';

-- 字符串 ID 序列种子（幂等,供 IdGenerator 使用）
INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('assessment_session', 100),
('profile_snapshot', 100),
('recommendation_run', 100),
('recommendation_result', 100),
('student_goal', 100),
('goal_version', 100),
('semester_plan', 100),
('plan_version', 100),
('plan_task', 100),
('task_checkin', 100),
('stage_review', 100),
('favorite', 100),
('reminder', 100),
('questionnaire', 100),
('questionnaire_version', 100),
('question', 203),
('question_option', 2033),
('model_config', 100),
('prompt_version', 100);
