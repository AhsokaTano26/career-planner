-- ============================================================
-- admin 培养方案模块建表脚本(MySQL 8, utf8mb4)
-- 管理端·培养方案:PDF/Word 导入 -> 解析任务 -> 待审核条目 -> 发布版本
-- 幂等:由 spring.sql.init 执行,CREATE TABLE IF NOT EXISTS
-- AI 解析由 career-ai 组负责,本文件不含 AI 相关表
-- ============================================================

-- 导入任务
CREATE TABLE IF NOT EXISTS curriculum_import_job (
    id            VARCHAR(32)   NOT NULL COMMENT '任务ID,如 CJ-001',
    filename      VARCHAR(255)  NOT NULL COMMENT '原文件名',
    file_path     VARCHAR(500)  DEFAULT NULL COMMENT '存储路径',
    file_type     VARCHAR(20)   DEFAULT NULL COMMENT 'PDF/WORD',
    status        VARCHAR(20)   NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/PARSING/REVIEW_REQUIRED/PUBLISHED/FAILED',
    total_items   INT           NOT NULL DEFAULT 0 COMMENT '识别课程总数',
    parsed_items  INT           NOT NULL DEFAULT 0 COMMENT '已解析条目数',
    confidence    DECIMAL(5,2)  DEFAULT NULL COMMENT '整体解析置信度(0-100)',
    error_message VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    created_by    VARCHAR(32)   DEFAULT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_job_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案导入任务';

-- 待审核课程条目
CREATE TABLE IF NOT EXISTS curriculum_import_item (
    id                VARCHAR(32)  NOT NULL COMMENT '条目ID,如 IT-001',
    job_id            VARCHAR(32)  NOT NULL COMMENT '任务ID',
    course_code       VARCHAR(64)  NOT NULL COMMENT '课程代码',
    course_name       VARCHAR(200) NOT NULL COMMENT '课程名称',
    semester          VARCHAR(50)  DEFAULT NULL COMMENT '开课学期',
    credits           DECIMAL(4,1) DEFAULT NULL COMMENT '学分',
    hours             DECIMAL(6,1) DEFAULT NULL COMMENT '学时',
    category          VARCHAR(50)  DEFAULT NULL COMMENT '课程类别',
    module            VARCHAR(50)  DEFAULT NULL COMMENT '课程模块',
    prerequisites_json JSON        DEFAULT NULL COMMENT '先修课程代码数组',
    ability_tags_json JSON         DEFAULT NULL COMMENT '能力标签数组',
    confidence        DECIMAL(5,2) DEFAULT NULL COMMENT '单条置信度(0-100)',
    page_ref          VARCHAR(255) DEFAULT NULL COMMENT '来源页码/原文片段',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/MERGED',
    merged_into       VARCHAR(32)  DEFAULT NULL COMMENT '合并目标条目ID',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_item_job_status (job_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案待审核课程条目';

-- 培养方案版本
CREATE TABLE IF NOT EXISTS curriculum_version (
    id           VARCHAR(32)  NOT NULL COMMENT '版本ID,如 CV-001',
    name         VARCHAR(200) NOT NULL COMMENT '方案名称',
    major        VARCHAR(100) NOT NULL COMMENT '适用专业',
    course_count INT          NOT NULL DEFAULT 0 COMMENT '课程数',
    status       VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED',
    source_job_id VARCHAR(32) DEFAULT NULL COMMENT '发布来源任务ID',
    published_at DATETIME     DEFAULT NULL,
    published_by VARCHAR(64)  DEFAULT NULL COMMENT '发布人用户ID',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_version_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='培养方案版本';

-- 正式课程(发布时由 APPROVED 条目生成)
CREATE TABLE IF NOT EXISTS course (
    id                VARCHAR(32)  NOT NULL,
    version_id        VARCHAR(32)  NOT NULL COMMENT '所属方案版本',
    course_code       VARCHAR(64)  NOT NULL COMMENT '课程代码',
    course_name       VARCHAR(200) NOT NULL COMMENT '课程名称',
    semester          VARCHAR(50)  DEFAULT NULL,
    credits           DECIMAL(4,1) DEFAULT NULL,
    hours             DECIMAL(6,1) DEFAULT NULL,
    category          VARCHAR(50)  DEFAULT NULL,
    module            VARCHAR(50)  DEFAULT NULL,
    prerequisites_json JSON        DEFAULT NULL,
    source_item_id    VARCHAR(32)  DEFAULT NULL COMMENT '来源审核条目',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_version_code (version_id, course_code),
    KEY idx_course_version (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='正式课程';

-- 课程-能力标签
CREATE TABLE IF NOT EXISTS course_ability_tag (
    id          VARCHAR(32) NOT NULL,
    course_id   VARCHAR(32) NOT NULL,
    ability_tag VARCHAR(64) NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_ability (course_id, ability_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程能力标签';

-- ID 序列
INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('curriculum_import_job', 100),
('curriculum_import_item', 100),
('curriculum_version', 100),
('course', 100),
('course_ability_tag', 100);

-- 种子数据:培养方案导入任务 + 待审核条目(供 CRUD 冒烟测试使用)
INSERT IGNORE INTO curriculum_import_job
    (id, filename, file_type, status, total_items, parsed_items, confidence, created_at)
VALUES
    ('CJ-001', '2026培养方案.pdf', 'PDF', 'REVIEW_REQUIRED', 3, 3, 86.00, '2026-08-26 10:00:00');

INSERT IGNORE INTO curriculum_import_item
    (id, job_id, course_code, course_name, semester, credits, hours, category, module,
     prerequisites_json, ability_tags_json, confidence, page_ref, status)
VALUES
    ('IT-001', 'CJ-001', 'CS101', '程序设计基础', '2026-2027-1', 4, 64, '专业基础', '必修',
     '["CS100"]', '["programming_basic"]', 92.00, '第 12 页', 'PENDING'),
    ('IT-002', 'CJ-001', 'CS201', '数据结构', '2026-2027-2', 4, 64, '专业核心', '必修',
     '["CS101"]', '["programming_basic","algorithm"]', 88.00, '第 30 页', 'PENDING'),
    ('IT-003', 'CJ-001', 'CS202', '数据结构(重修版)', '2026-2027-2', 4, 64, '专业核心', '必修',
     '["CS101"]', '["algorithm"]', 61.00, '第 31 页', 'PENDING');
