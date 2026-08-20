-- ============================================================
-- admin 配置模块建表脚本(MySQL 8, utf8mb4)
-- 管理端·配置:能力标签 / 方向库 / 任务模板 / 推荐权重
-- 幂等:由 spring.sql.init 执行,CREATE TABLE IF NOT EXISTS
-- 问卷 / 模型与提示词表由对应模块队友负责,不在此文件
-- ============================================================

-- 能力标签
CREATE TABLE IF NOT EXISTS ability_tag (
    id         VARCHAR(64)  NOT NULL COMMENT '标签编码(主键,前端传入)',
    name       VARCHAR(100) NOT NULL COMMENT '标签名称',
    category   VARCHAR(50)  DEFAULT NULL COMMENT '分类,如 能力/课程/任务',
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能力标签';

-- 方向库(单行 + updatedAt 版本语义,状态机 DRAFT/PUBLISHED/DISABLED)
CREATE TABLE IF NOT EXISTS career_direction (
    id                     VARCHAR(64)   NOT NULL COMMENT '方向编码(主键,前端传入)',
    name                   VARCHAR(100)  NOT NULL COMMENT '方向名称',
    path                   VARCHAR(20)   NOT NULL COMMENT 'graduate/employment/overseas',
    icon                   VARCHAR(50)   DEFAULT NULL COMMENT '图标',
    intro                  VARCHAR(2000) DEFAULT NULL COMMENT '方向简介',
    target_json            JSON          DEFAULT NULL COMMENT '六维目标值 {interest,values,ability,academic,tendency,practice}',
    min_ability            DECIMAL(5,2)  DEFAULT NULL COMMENT '最低能力要求(0-100)',
    min_academic           DECIMAL(5,2)  DEFAULT NULL COMMENT '最低学业要求(0-100)',
    learning_json          JSON          DEFAULT NULL COMMENT '学习内容数组',
    abilities_json         JSON          DEFAULT NULL COMMENT '能力要求标签数组',
    courses_json           JSON          DEFAULT NULL COMMENT '推荐课程数组',
    activities_json        JSON          DEFAULT NULL COMMENT '实践活动建议数组',
    path_desc_json         JSON          DEFAULT NULL COMMENT '发展路径描述数组',
    misconceptions_json    JSON          DEFAULT NULL COMMENT '常见误区数组',
    sort_order             INT           NOT NULL DEFAULT 0 COMMENT '展示顺序',
    applicable_majors_json JSON          DEFAULT NULL COMMENT '适用专业数组',
    status                 VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    updated                DATE          DEFAULT NULL COMMENT '更新时间(日)',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    KEY idx_direction_path_status (path, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='方向库';

-- 任务模板(计划生成回退来源)
CREATE TABLE IF NOT EXISTS task_template (
    id                  VARCHAR(64)  NOT NULL COMMENT '模板ID(主键,前端传入)',
    direction_id        VARCHAR(64)  NOT NULL COMMENT '方向编码(career_direction.id)',
    name                VARCHAR(100) NOT NULL COMMENT '模板名称',
    goal_summary        VARCHAR(2000) DEFAULT NULL COMMENT '目标摘要模板',
    semester_goals_json JSON         DEFAULT NULL COMMENT '学期目标数组 {title,abilityTag}',
    monthly_tasks_json  JSON         DEFAULT NULL COMMENT '月度任务数组 {month,title,taskType,estimatedHours}',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    KEY idx_template_direction (direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务模板';

-- 推荐权重版本(DRAFT -> PUBLISHED,发布接口后续由队友/后续接口提供)
CREATE TABLE IF NOT EXISTS recommendation_weight (
    id             VARCHAR(64)  NOT NULL,
    version        VARCHAR(32)  NOT NULL COMMENT '规则版本号,如 R1.0',
    weights_json   JSON         NOT NULL COMMENT '六维权重 {interest,values,ability,academic,tendency,practice}',
    min_confidence DECIMAL(4,3) NOT NULL DEFAULT 0 COMMENT '最低可信阈值(0-1)',
    top_n          INT          NOT NULL DEFAULT 5 COMMENT '返回推荐数量上限',
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    published_at   DATETIME     DEFAULT NULL,
    published_by   VARCHAR(64)  DEFAULT NULL COMMENT '发布人用户ID',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_weight_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐权重版本';

-- 种子:初始能力标签 / 发布版权重 R1.0 / ID 序列
INSERT IGNORE INTO ability_tag (id, name, category) VALUES
('programming_basic', '编程基础', '能力'),
('math_basic',        '数学基础', '能力'),
('english_basic',     '英语基础', '能力');

INSERT INTO recommendation_weight (id, version, weights_json, min_confidence, top_n, status, published_at)
SELECT 'WGT-001', 'R1.0',
       JSON_OBJECT('interest', 0.20, 'values', 0.15, 'ability', 0.25,
                   'academic', 0.15, 'tendency', 0.20, 'practice', 0.05),
       0, 5, 'PUBLISHED', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM recommendation_weight WHERE version = 'R1.0');

INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('recommendation_weight', 100);
