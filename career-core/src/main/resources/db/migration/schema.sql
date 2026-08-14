-- ============================================================
-- 生涯规划系统 · 第一版 Demo 建表脚本（幂等：CREATE ... IF NOT EXISTS）
-- 表结构沿用《生涯规划系统开发设计说明书》8.2 核心表清单，仅裁剪本 Demo
-- 三个接口所需的最小字段集，未新增业务表。
-- ============================================================

-- 学生画像主表（student_profile）
-- Demo 简化：以 user_id 作为“学生ID”对外标识；完整度由接口运行时按非空字段实时计算。
CREATE TABLE IF NOT EXISTS student_profile (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL COMMENT '学生ID（对外标识，对应系统用户ID）',
    student_no     VARCHAR(32)  DEFAULT NULL COMMENT '学号',
    major_category VARCHAR(64)  DEFAULT NULL COMMENT '专业大类',
    grade          VARCHAR(32)  DEFAULT NULL COMMENT '年级',
    class_name     VARCHAR(64)  DEFAULT NULL COMMENT '班级',
    completeness   DECIMAL(5,2) DEFAULT NULL COMMENT '数据完整度（Demo：由接口实时计算，此列暂冗余保留）',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生画像';

-- 画像快照（profile_snapshot）—— 六维画像数据载体
-- dimension_json 存储六维（兴趣 interest / 价值观 values / 学业 academic / 能力 ability / 倾向 orientation / 经历 experience）
CREATE TABLE IF NOT EXISTS profile_snapshot (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    student_id     BIGINT      NOT NULL COMMENT '学生ID（对应 student_profile.user_id）',
    source_version VARCHAR(64) DEFAULT NULL COMMENT '画像来源版本',
    dimension_json JSON        DEFAULT NULL COMMENT '六维画像数据（JSON）',
    summary        VARCHAR(500) DEFAULT NULL COMMENT '画像摘要',
    created_at     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_snapshot_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '画像快照';

-- 学生经历（student_experience）—— 六维中的“经历”明细
CREATE TABLE IF NOT EXISTS student_experience (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    student_id  BIGINT       NOT NULL,
    type        VARCHAR(32)  DEFAULT NULL COMMENT '经历类型（竞赛/项目/实习等）',
    title       VARCHAR(128) DEFAULT NULL,
    start_date  DATE         DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_exp_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生经历';

-- 方向库（career_direction）
-- type 为方向类型（技术研发/数据算法/产品管理 等），供接口2返回“type”字段
-- personality_tags 为霍兰德（Holland RIASEC）人格类型标签（如 R,I,C，逗号分隔），
-- 用于“人格类型→方向”映射：方向配置该列后，推荐引擎可计算学生人格与方向的契合度。
CREATE TABLE IF NOT EXISTS career_direction (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    direction_code   VARCHAR(32)  NOT NULL COMMENT '方向编码',
    name             VARCHAR(64)  NOT NULL COMMENT '方向名称',
    type             VARCHAR(32)  DEFAULT NULL COMMENT '方向类型',
    status           VARCHAR(16)  DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE 启用 / INACTIVE 停用）',
    content          TEXT         DEFAULT NULL COMMENT '方向内容说明',
    personality_tags VARCHAR(64)  DEFAULT NULL COMMENT '霍兰德人格类型标签（RIASEC 编码，逗号分隔）',
    version_no       INT          DEFAULT 1 COMMENT '方向版本',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_direction_code (direction_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '职业方向库';

-- 对已存在的 career_direction 表补充 personality_tags 列（幂等：列存在则跳过）
-- 说明：schema.sql 幂等执行，新库由上方 CREATE TABLE 直接建列；旧库走下方条件 ALTER。
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'career_direction'
      AND COLUMN_NAME = 'personality_tags'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE career_direction ADD COLUMN personality_tags VARCHAR(64) DEFAULT NULL COMMENT ''霍兰德人格类型标签（RIASEC 编码，逗号分隔）'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 方向维度权重与目标值（direction_dimension_weight）
-- 六维目标值 target_value 与全局权重 weight 存放于配置表，避免写死（设计说明书 9.2）
CREATE TABLE IF NOT EXISTS direction_dimension_weight (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    direction_id BIGINT       NOT NULL COMMENT '方向ID',
    dimension    VARCHAR(32)  NOT NULL COMMENT '维度编码（interest/values/academic/ability/orientation/experience）',
    target_value DECIMAL(5,2) DEFAULT NULL COMMENT '该方向在此维度的目标值（0-100）',
    weight       DECIMAL(5,4) DEFAULT NULL COMMENT '该维度在总评分中的权重',
    version_no   INT          DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dir_dim (direction_id, dimension, version_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '方向维度权重';

-- 推荐批次（recommendation_run）
CREATE TABLE IF NOT EXISTS recommendation_run (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    student_id         BIGINT      NOT NULL,
    profile_snapshot_id BIGINT     DEFAULT NULL COMMENT '关联画像快照',
    rule_version       VARCHAR(32) DEFAULT NULL COMMENT '规则版本',
    status             VARCHAR(16) DEFAULT 'SUCCESS' COMMENT '状态（RUNNING/SUCCESS/DEGRADED/FAILED）',
    created_at         DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '推荐批次';

-- 推荐结果（recommendation_result）
CREATE TABLE IF NOT EXISTS recommendation_result (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    run_id            BIGINT       NOT NULL COMMENT '推荐批次ID',
    direction_id      BIGINT       NOT NULL,
    score             DECIMAL(6,4) DEFAULT NULL COMMENT '匹配度评分（0-100）',
    `rank`            INT          DEFAULT NULL COMMENT '排序名次（rank 为 MySQL 保留字，需反引号）',
    explanation_json  JSON         DEFAULT NULL COMMENT '结构化解释（reasons/strengths/gaps/semesterActions）',
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_result_run (run_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '推荐结果';

-- 推荐反馈（recommendation_feedback）
-- 承接 POST /api/v1/recommendation-results/{resultId}/feedback（result_id 指向 recommendation_result.id）
CREATE TABLE IF NOT EXISTS recommendation_feedback (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    result_id     BIGINT       NOT NULL COMMENT '推荐结果ID（recommendation_result.id）',
    feedback_type VARCHAR(32)  DEFAULT NULL COMMENT '反馈类型（HELPFUL/NEUTRAL/MISMATCH/NOT_INTERESTED）',
    comment       VARCHAR(500) DEFAULT NULL COMMENT '反馈意见',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_feedback_result (result_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '推荐反馈';

-- 阶段复盘（stage_review）—— 线上 Apifox 无复盘接口，接口3已按线上为准删除；
-- 表保留以对齐设计文档 8.2 核心表清单；ai_summary / adjustment 为 AI 生成字段，Demo 阶段留空。
CREATE TABLE IF NOT EXISTS stage_review (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    student_id    BIGINT      NOT NULL,
    review_period VARCHAR(32) NOT NULL COMMENT '复盘周期（如 2026-08）',
    raw_text      TEXT        DEFAULT NULL COMMENT '学生原文',
    ai_summary    TEXT        DEFAULT NULL COMMENT 'AI 总结（Demo 阶段跳过生成，留空）',
    adjustment    TEXT        DEFAULT NULL COMMENT '调整建议（Demo 阶段跳过生成，留空）',
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_student_period (student_id, review_period)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '阶段复盘';

-- 学生目标（student_goal）—— 主/备选目标（设计文档 8.2 计划域）
CREATE TABLE IF NOT EXISTS student_goal (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    student_id   BIGINT       NOT NULL,
    direction_id BIGINT       DEFAULT NULL COMMENT '关联方向（career_direction.id）',
    title        VARCHAR(128) DEFAULT NULL COMMENT '目标标题',
    goal_type    VARCHAR(16)  DEFAULT 'MAIN' COMMENT '主/备选目标：MAIN / BACKUP',
    status       VARCHAR(16)  DEFAULT 'ACTIVE',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生目标';

-- 学期计划（semester_plan）—— 学期、目标、来源、确认状态（设计文档 8.2 计划域）
CREATE TABLE IF NOT EXISTS semester_plan (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    student_id BIGINT      NOT NULL,
    goal_id    BIGINT      DEFAULT NULL,
    semester   VARCHAR(32) DEFAULT NULL COMMENT '学期（如 2026-2027学年第1学期）',
    source     VARCHAR(16) DEFAULT 'AI' COMMENT '来源：AI / MANUAL',
    status     VARCHAR(16) DEFAULT 'DRAFT' COMMENT '状态：DRAFT（草案）/ CONFIRMED',
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_plan_student (student_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学期计划';

-- 计划任务（plan_task）—— 月份、任务、状态（设计文档 8.2 计划域）
CREATE TABLE IF NOT EXISTS plan_task (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    plan_id    BIGINT       NOT NULL COMMENT '学期计划ID（semester_plan.id）',
    month      VARCHAR(16)  DEFAULT NULL COMMENT '月份（如 2026-09）',
    title      VARCHAR(128) DEFAULT NULL,
    status     VARCHAR(16)  DEFAULT 'TODO' COMMENT '状态：TODO/DOING/DONE/DELAY/ABANDON',
    created_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_plan (plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '计划任务';
