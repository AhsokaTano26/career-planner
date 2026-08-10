-- ============================================================
-- advisor 模块【本地联调专用】建表 + 种子数据
-- 仅手动执行:mysql -u<user> -p --default-character-set=utf8mb4 career_core < advisor-dev-setup.sql
-- 不要加入 application.yml 的 schema-locations,不要在生产环境执行
-- 说明:依赖表(测评/画像/推荐/目标/计划/任务/复盘/方向)由对应模块队友正式建表,
--       此处仅提供最小结构用于 advisor 接口本地验证;正式建表以队友的 schema 为准。
-- ============================================================

-- ---------- 依赖表(最小结构) ----------

CREATE TABLE IF NOT EXISTS assessment_session (
    id                        VARCHAR(32) NOT NULL,
    student_id                VARCHAR(32) NOT NULL,
    questionnaire_version_id  VARCHAR(32) DEFAULT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    request_id                VARCHAR(64) DEFAULT NULL,
    created_at                DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_student_status (student_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测评会话(dev 最小结构)';

CREATE TABLE IF NOT EXISTS profile_snapshot (
    id             VARCHAR(32) NOT NULL,
    student_id     VARCHAR(32) NOT NULL,
    source_version VARCHAR(64) DEFAULT NULL,
    dimension_json JSON        DEFAULT NULL,
    summary        VARCHAR(1000) DEFAULT NULL,
    strengths_json JSON        DEFAULT NULL,
    explore_json   JSON        DEFAULT NULL,
    feedback_json  JSON        DEFAULT NULL,
    version_no     INT         NOT NULL DEFAULT 1,
    completeness   INT         NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_snapshot_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画像快照(dev 最小结构)';

CREATE TABLE IF NOT EXISTS career_direction (
    id         VARCHAR(32) NOT NULL,
    code       VARCHAR(64) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    path       VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_direction_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='方向库(dev 最小结构)';

CREATE TABLE IF NOT EXISTS recommendation_run (
    id                   VARCHAR(32) NOT NULL,
    student_id           VARCHAR(32) NOT NULL,
    profile_snapshot_id  VARCHAR(32) DEFAULT NULL,
    rule_version         VARCHAR(32) DEFAULT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    generated_at         DATETIME    DEFAULT NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_run_student (student_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐批次(dev 最小结构)';

CREATE TABLE IF NOT EXISTS recommendation_result (
    id                   VARCHAR(32) NOT NULL,
    run_id               VARCHAR(32) NOT NULL,
    direction_id         VARCHAR(64) NOT NULL,
    score                DECIMAL(5,1) NOT NULL,
    `rank`               INT         NOT NULL,
    confidence           VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    reasons_json         JSON DEFAULT NULL,
    strengths_json       JSON DEFAULT NULL,
    gaps_json            JSON DEFAULT NULL,
    semester_actions_json JSON DEFAULT NULL,
    feedback_json        JSON DEFAULT NULL,
    created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_run_direction (run_id, direction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果(dev 最小结构)';

CREATE TABLE IF NOT EXISTS student_goal (
    id           VARCHAR(32) NOT NULL,
    student_id   VARCHAR(32) NOT NULL,
    goal_type    VARCHAR(10) NOT NULL DEFAULT 'PRIMARY',
    direction_id VARCHAR(64) DEFAULT NULL,
    name         VARCHAR(100) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    chosen_at    DATETIME    DEFAULT NULL,
    version_no   INT         NOT NULL DEFAULT 1,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_student (student_id, goal_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生目标(dev 最小结构)';

CREATE TABLE IF NOT EXISTS goal_version (
    id            VARCHAR(32) NOT NULL,
    goal_id       VARCHAR(32) NOT NULL,
    version_no    INT         NOT NULL,
    change_reason VARCHAR(255) DEFAULT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_goal_version (goal_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标版本(dev 最小结构)';

CREATE TABLE IF NOT EXISTS semester_plan (
    id                  VARCHAR(32) NOT NULL,
    student_id          VARCHAR(32) NOT NULL,
    version_no          INT         NOT NULL DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source              VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    goal_summary        VARCHAR(1000) DEFAULT NULL,
    semester_goals_json JSON DEFAULT NULL,
    monthly_tasks_json  JSON DEFAULT NULL,
    notes_json          JSON DEFAULT NULL,
    confirmed_at        DATETIME    DEFAULT NULL,
    created_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_plan_student (student_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学期计划(dev 最小结构)';

CREATE TABLE IF NOT EXISTS plan_task (
    id               VARCHAR(32) NOT NULL,
    plan_id          VARCHAR(32) NOT NULL,
    student_id       VARCHAR(32) NOT NULL,
    month            VARCHAR(7) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    task_type        VARCHAR(20) NOT NULL DEFAULT 'LEARNING',
    est_hours        DECIMAL(6,1) DEFAULT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    deadline         DATE DEFAULT NULL,
    ability_tags_json JSON DEFAULT NULL,
    note             VARCHAR(1000) DEFAULT NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_student (student_id, status, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划任务(dev 最小结构)';

CREATE TABLE IF NOT EXISTS task_checkin (
    id            VARCHAR(32) NOT NULL,
    task_id       VARCHAR(32) NOT NULL,
    done_desc     VARCHAR(1000) NOT NULL,
    gains         VARCHAR(1000) DEFAULT NULL,
    difficulties  VARCHAR(1000) DEFAULT NULL,
    proof_url     VARCHAR(255) DEFAULT NULL,
    checked_in_at DATETIME    DEFAULT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_checkin_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务打卡(dev 最小结构)';

CREATE TABLE IF NOT EXISTS stage_review (
    id               VARCHAR(32) NOT NULL,
    student_id       VARCHAR(32) NOT NULL,
    cycle            VARCHAR(7) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    content_json     JSON DEFAULT NULL,
    ai_summary       VARCHAR(2000) DEFAULT NULL,
    ai_suggest_json  JSON DEFAULT NULL,
    advisor_requested TINYINT(1) NOT NULL DEFAULT 0,
    advisor_reply    VARCHAR(2000) DEFAULT NULL,
    submitted_at     DATETIME    DEFAULT NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_review_student (student_id, submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阶段复盘(dev 最小结构)';

-- ---------- 种子数据(幂等:INSERT IGNORE,可重复执行) ----------

-- 账号密码:dev 统一初始密码 advisor123(BCrypt cost 12)
INSERT IGNORE INTO sys_user (id, student_no, username, name, password_hash, role, status, grade, major_category, class_name, consent_agreed) VALUES
('A1001', NULL, 'zhanglaoshi', '张老师', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'ADVISOR', 'ACTIVE', NULL, NULL, NULL, 1),
('A1002', NULL, 'lilaoshi',    '李老师', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'ADVISOR', 'ACTIVE', NULL, NULL, NULL, 1);

-- 学生账号
INSERT IGNORE INTO sys_user (id, student_no, username, name, password_hash, role, status, grade, major_category, class_name, consent_agreed) VALUES
('S1001', '2026011301', 'liming',  '李明', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '计科2601', 1),
('S1002', '2026011309', 'zhangyu', '张雨', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '计科2602', 1),
('S1003', '2026011310', 'wangfang','王芳', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '软工2601', 1),
('S1004', '2026011311', 'zhaolei', '赵磊', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '计科2601', 1),
('S1005', '2026011312', 'chenchen','陈晨', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '计科2602', 1),
('S1006', '2026011313', 'liuyang', '刘洋', '$2b$12$VzCHng9qNtjpybdolbJ38eZJ8ZmmPjtrwanLqni3oyvgiEPHNHXrO', 'STUDENT', 'ACTIVE', '2026级', '计算机类', '软工2601', 1);

-- 学生档案
INSERT IGNORE INTO student_profile (id, user_id, name, class_name, grade, major_category, basic_json, academic_json, interest_prefs_json, ability_self_json, values_json, development_intention, constraints_json, completeness) VALUES
('P1001', 'S1001', '李明', '计科2601', '2026级', '计算机类',
 '{"gender":"男","hometown":"重庆","birthday":"2008-05-14","phone":"138****6721"}',
 '{"math":4,"english":3,"programming":2,"note":"高中数学较好,英语一般,编程刚起步"}',
 '["编程","算法","数学建模"]',
 '{"programming":2,"math":4,"english":3,"communication":4,"organization":3}',
 '["成长","影响力"]',
 'employment', '["愿意在课余投入学习"]', 92),
('P1002', 'S1002', '张雨', '计科2602', '2026级', '计算机类',
 '{"gender":"女","hometown":"成都","birthday":"2008-08-21","phone":"139****1122"}',
 '{"math":3,"english":4,"programming":3,"note":""}',
 '["考研","人工智能"]',
 '{"programming":3,"math":3,"english":4,"communication":3,"organization":4}',
 '["成长","稳定"]',
 'graduate', NULL, 85),
('P1003', 'S1003', '王芳', '软工2601', '2026级', '计算机类',
 '{"gender":"女","hometown":"贵阳","birthday":"2008-03-09","phone":"137****3344"}',
 '{"math":3,"english":3,"programming":3,"note":""}',
 '["前端开发","设计"]',
 '{"programming":3,"math":3,"english":3,"communication":5,"organization":4}',
 '["稳定","影响力"]',
 'undecided', NULL, 70),
('P1004', 'S1004', '赵磊', '计科2601', '2026级', '计算机类',
 '{"gender":"男","hometown":"武汉","birthday":"2008-11-02","phone":"136****5566"}',
 '{"math":4,"english":2,"programming":3,"note":""}',
 '["后端开发","数据库"]',
 '{"programming":3,"math":4,"english":2,"communication":2,"organization":3}',
 '["收入","成长"]',
 'employment', NULL, 88),
('P1006', 'S1006', '刘洋', '软工2601', '2026级', '计算机类',
 '{"gender":"男","hometown":"昆明","birthday":"2008-06-18","phone":"135****7788"}',
 '{"math":2,"english":2,"programming":1,"note":""}',
 '[]',
 '{"programming":1,"math":2,"english":2,"communication":3,"organization":2}',
 '[]',
 'undecided', NULL, 45);

-- 学生经历
INSERT IGNORE INTO student_experience (id, student_id, type, title, start_date, end_date, description, attachment_url) VALUES
('EXP-001', 'S1001', '竞赛', '数学建模校赛·二等奖', '2026-05', '2026-06', '三人组队,负责建模与论文撰写,完成校园快递点选址问题。', NULL);

-- 辅导员-学生关系:A1001 带 S1001~S1004、S1006;S1005 归 A1002(用于越权 403 验证)
INSERT IGNORE INTO advisor_student_relation (id, advisor_id, student_id) VALUES
('AR-001', 'A1001', 'S1001'),
('AR-002', 'A1001', 'S1002'),
('AR-003', 'A1001', 'S1003'),
('AR-004', 'A1001', 'S1004'),
('AR-005', 'A1001', 'S1006'),
('AR-006', 'A1002', 'S1005');

-- 测评会话
INSERT IGNORE INTO assessment_session (id, student_id, questionnaire_version_id, status, request_id) VALUES
('AS1001', 'S1001', 'QV1', 'SCORED', 'req-s1001-1'),
('AS1002', 'S1002', 'QV1', 'SCORED', 'req-s1002-1'),
('AS1003', 'S1003', 'QV1', 'SCORED', 'req-s1003-1'),
('AS1004', 'S1004', 'QV1', 'SCORED', 'req-s1004-1'),
('AS1006', 'S1006', 'QV1', 'DRAFT', 'req-s1006-1');

-- 方向库
INSERT IGNORE INTO career_direction (id, code, name, path, status) VALUES
('D001', 'employment_backend', '后端开发工程师', 'employment', 'PUBLISHED'),
('D002', 'data_analysis',      '数据分析师',       'employment', 'PUBLISHED'),
('D003', 'graduate_software',  '计算机技术考研',   'graduate',   'PUBLISHED');

-- 画像快照
INSERT IGNORE INTO profile_snapshot (id, student_id, source_version, dimension_json, summary, strengths_json, explore_json, feedback_json, version_no, completeness, created_at) VALUES
('PS-1001', 'S1001', 'Q v2 + 自主填报 v3',
 '[{"key":"interest","name":"兴趣","score":78},{"key":"values","name":"价值观","score":72},{"key":"ability","name":"能力","score":65},{"key":"academic","name":"学业","score":80},{"key":"tendency","name":"发展倾向","score":70},{"key":"practice","name":"经历","score":45}]',
 '兴趣集中在技术问题求解与动手实践,数学基础较好,编程实践有待积累。',
 '["数学基础较好,是算法与数据方向的加分项"]',
 '["编程实践有待积累,建议从完成小项目开始"]',
 '{"feedbackType":"MATCH","comment":"学习能力描述与我实际情况基本一致"}',
 2, 92, '2026-09-01 09:13:00'),
('PS-1002', 'S1002', 'Q v1 + 自主填报 v2',
 '[{"key":"interest","name":"兴趣","score":70},{"key":"values","name":"价值观","score":75},{"key":"ability","name":"能力","score":60},{"key":"academic","name":"学业","score":75},{"key":"tendency","name":"发展倾向","score":80},{"key":"practice","name":"经历","score":30}]',
 '升学意向明确,学习基础均衡,需加强实践经历。',
 '["英语基础较好"]',
 '["缺少科研或竞赛经历"]',
 NULL, 1, 85, '2026-08-20 10:00:00'),
('PS-1004', 'S1004', 'Q v1 + 自主填报 v2',
 '[{"key":"interest","name":"兴趣","score":72},{"key":"values","name":"价值观","score":70},{"key":"ability","name":"能力","score":66},{"key":"academic","name":"学业","score":78},{"key":"tendency","name":"发展倾向","score":74},{"key":"practice","name":"经历","score":40}]',
 '就业方向明确,后端开发匹配度较高。',
 '["数学基础较好"]',
 '["缺少系统编程实践"]',
 NULL, 1, 88, '2026-08-15 14:00:00');

-- 推荐批次与结果
INSERT IGNORE INTO recommendation_run (id, student_id, profile_snapshot_id, rule_version, status, generated_at) VALUES
('R1001', 'S1001', 'PS-1001', 'R1.0', 'SUCCESS', '2026-09-01 09:13:30'),
('R1004', 'S1004', 'PS-1004', 'R1.0', 'SUCCESS', '2026-08-15 14:05:00');

INSERT IGNORE INTO recommendation_result (id, run_id, direction_id, score, `rank`, confidence, reasons_json, strengths_json, gaps_json, semester_actions_json, feedback_json) VALUES
('RR1001', 'R1001', 'employment_backend', 82.4, 1, 'MEDIUM',
 '["偏好结构化问题求解(兴趣维度)","重视技术成长(价值观维度)"]',
 '["数学与逻辑基础较好"]',
 '["缺少系统编程实践"]',
 '["完成《程序设计基础》课程"]',
 '{"feedbackType":"HELPFUL","comment":"与我预期的方向基本一致"}'),
('RR1002', 'R1001', 'data_analysis', 76.0, 2, 'MEDIUM',
 '["对数据与图表感兴趣","沟通表达较好"]',
 '["数学基础较好"]',
 '["缺少数据分析工具实践"]',
 '["学习 SQL 与 Excel 基础"]',
 NULL),
('RR1003', 'R1004', 'employment_backend', 80.1, 1, 'HIGH',
 '["就业意向明确","重视收入与成长"]',
 '["数学基础较好"]',
 '["编程实践不足"]',
 '["完成 Java 基础课程"]',
 NULL);

-- 目标:主/备选;S1002 主目标带 3 条版本变更(近 90 天,触发"多次调整目标")
INSERT IGNORE INTO student_goal (id, student_id, goal_type, direction_id, name, status, chosen_at, version_no) VALUES
('G1001', 'S1001', 'PRIMARY', 'employment_backend', '后端开发工程师', 'ACTIVE', '2026-09-02 10:00:00', 1),
('G1002', 'S1001', 'BACKUP',  'data_analysis',     '数据分析师',     'ACTIVE', '2026-09-02 10:00:00', 1),
('G1003', 'S1002', 'PRIMARY', 'graduate_software', '计算机技术考研', 'ACTIVE', '2026-08-01 09:00:00', 4),
('G1004', 'S1004', 'PRIMARY', 'employment_backend', '后端开发工程师', 'ACTIVE', '2026-08-10 11:00:00', 1);

INSERT IGNORE INTO goal_version (id, goal_id, version_no, change_reason, created_at) VALUES
('GV3001', 'G1003', 1, '初选',       '2026-06-01 09:00:00'),
('GV3002', 'G1003', 2, '方向调整',   '2026-07-05 10:00:00'),
('GV3003', 'G1003', 3, '目标细化',   '2026-08-01 09:00:00');

-- 学期计划
INSERT IGNORE INTO semester_plan (id, student_id, version_no, status, source, goal_summary, semester_goals_json, monthly_tasks_json, notes_json, confirmed_at) VALUES
('PLAN1001', 'S1001', 2, 'CONFIRMED', 'AI',
 '本学期完成后端技术基础入门,掌握 Java 与数据库基础。',
 '[{"title":"掌握 Java 基础与面向对象编程","abilityTag":"programming_basic"}]',
 '[{"month":"2026-09","title":"完成 Java 语法与面向对象章节学习","taskType":"LEARNING","estimatedHours":12}]',
 '["任务可随课程安排调整"]',
 '2026-09-02 10:05:00'),
('PLAN1002', 'S1002', 1, 'DRAFT', 'TEMPLATE',
 '围绕考研目标制定基础课程学习计划。',
 '[{"title":"完成数学基础一轮复习","abilityTag":"math_basic"}]',
 '[{"month":"2026-08","title":"完成高数上册学习","taskType":"LEARNING","estimatedHours":20}]',
 '[]',
 NULL),
('PLAN1004', 'S1004', 1, 'CONFIRMED', 'MANUAL',
 '本学期掌握 Java 基础并完成一个小项目。',
 '[{"title":"完成 Java 基础课程","abilityTag":"programming_basic"}]',
 '[{"month":"2026-08","title":"完成 Java 基础课程","taskType":"LEARNING","estimatedHours":16}]',
 '[]',
 '2026-08-10 11:30:00');

-- 任务与打卡
INSERT IGNORE INTO plan_task (id, plan_id, student_id, month, title, task_type, est_hours, status, deadline, ability_tags_json, note) VALUES
('T1001', 'PLAN1001', 'S1001', '2026-09', '完成 Java 语法与面向对象章节学习', 'LEARNING', 12, 'DONE',   '2026-09-30', '["programming_basic"]', '已完成'),
('T1002', 'PLAN1001', 'S1001', '2026-09', '完成通讯录小项目',               'PRACTICE', 8, 'DONE',   '2026-09-25', '["programming_basic"]', NULL),
('T1003', 'PLAN1001', 'S1001', '2026-10', 'LeetCode 简单题每日一题',        'PRACTICE', 10, 'PENDING','2026-10-31', '["algorithm"]', NULL),
('T1004', 'PLAN1004', 'S1004', '2026-08', '完成 Java 基础课程',             'LEARNING', 16, 'DONE',   '2026-08-20', '["programming_basic"]', NULL),
('T1005', 'PLAN1004', 'S1004', '2026-09', '完成图书管理系统项目',           'PRACTICE', 12, 'PENDING','2026-09-20', '["programming_basic"]', NULL);

INSERT IGNORE INTO task_checkin (id, task_id, done_desc, gains, difficulties, proof_url, checked_in_at) VALUES
('TC-001', 'T1001', '已完成,掌握了类与对象、集合基础。', '理解了面向对象三大特性。', '泛型部分较抽象。', NULL, '2026-09-20 10:00:00'),
('TC-002', 'T1002', '已完成通讯录控制台版。', '熟悉了 List/Map 使用。', NULL, NULL, '2026-09-24 18:00:00'),
('TC-003', 'T1004', '已完成 Java 基础课程学习。', '掌握了基本语法。', '线程部分未深入。', NULL, '2026-08-18 20:00:00');

-- 复盘:S1001 本月已复盘;S1002 超期未复盘且待指导;S1004 超期未复盘
INSERT IGNORE INTO stage_review (id, student_id, cycle, status, content_json, ai_summary, ai_suggest_json, advisor_requested, advisor_reply, submitted_at) VALUES
('R1', 'S1001', '2026-08', 'SUBMITTED',
 '{"done":"完成 Java 语法与通讯录项目","undone":"LeetCode 练习未完成一半","interest":"后端开发兴趣比预期更强","ability":"能独立写 300 行左右程序","next":"10 月聚焦数据结构与 SQL"}',
 '8 月编程基础快速提升,建议收敛并行任务。',
 '["将任务从 6 条收敛到 3 条主线"]',
 0, NULL, '2026-08-02 09:00:00'),
('R2', 'S1002', '2026-06', 'SUBMITTED',
 '{"done":"完成高数上册","undone":"英语听力练习不足","interest":"考研方向确定","ability":"学习自律性提升","next":"开始数据结构复习"}',
 '6 月学业推进平稳。',
 '["增加英语听力训练"]',
 1, NULL, '2026-06-20 15:00:00'),
('R3', 'S1004', '2026-07', 'SUBMITTED',
 '{"done":"完成 Java 基础课程","undone":"项目未开始","interest":"后端方向明确","ability":"编程入门","next":"完成图书管理系统"}',
 '7 月完成入门课程。',
 '["尽快启动项目实践"]',
 0, NULL, '2026-07-01 10:00:00');

-- 指导意见/建议
INSERT IGNORE INTO advisor_comment (id, student_id, advisor_id, content, advice_type, suggested_task, retest_reason) VALUES
('GC-001', 'S1001', 'A1001', '建议 10 月聚焦数据结构主线,减少并行任务。', 'COMMENT', NULL, NULL),
('GC-002', 'S1002', 'A1001', '建议补充一次霍兰德复测,确认兴趣是否变化。', 'SUGGEST_RETEST', NULL, '方向兴趣变化较大');
