-- ============================================================
-- 生涯规划系统 · 第一版 Demo 种子数据（幂等：显式主键 + ON DUPLICATE KEY UPDATE）
-- 预置 2 名学生与若干方向库数据，便于验收三个接口。
-- ============================================================

-- 学生画像：1001 完整画像；1002 仅注册（无画像快照，用于验证“画像未生成返回空对象”）
INSERT INTO student_profile (id, user_id, student_no, major_category, grade, class_name) VALUES
    (1, 1001, '2026001', '计算机与软件大类', '2026级', '计软2601班')
ON DUPLICATE KEY UPDATE student_no = VALUES(student_no), major_category = VALUES(major_category),
    grade = VALUES(grade), class_name = VALUES(class_name);

INSERT INTO student_profile (id, user_id, student_no, major_category, grade, class_name) VALUES
    (2, 1002, '2026002', '计算机与软件大类', '2026级', '计软2602班')
ON DUPLICATE KEY UPDATE student_no = VALUES(student_no), major_category = VALUES(major_category),
    grade = VALUES(grade), class_name = VALUES(class_name);

-- 画像快照：学生 1001 的六维画像（维度数值 0-100）+ 霍兰德人格类型（personality，RIASEC 编码）
INSERT INTO profile_snapshot (id, student_id, source_version, dimension_json, summary) VALUES
    (1, 1001, 'ASSESSMENT_V1',
     '{"interest":82,"values":70,"academic":85,"ability":76,"orientation":80,"experience":60,"personality":"IRC"}',
     '对软件工程与算法方向兴趣浓厚，学业基础扎实，具备较强的编程与逻辑能力，实践经历尚在积累中。')
ON DUPLICATE KEY UPDATE dimension_json = VALUES(dimension_json), summary = VALUES(summary);

-- 学生经历（学生 1001）
INSERT INTO student_experience (id, student_id, type, title, start_date, description) VALUES
    (1, 1001, '竞赛', '蓝桥杯省赛', '2026-05-10', '参与 C++ 组省级竞赛，获省二等奖。')
ON DUPLICATE KEY UPDATE type = VALUES(type), title = VALUES(title), start_date = VALUES(start_date), description = VALUES(description);

INSERT INTO student_experience (id, student_id, type, title, start_date, description) VALUES
    (2, 1001, '项目', '班级成绩管理系统', '2026-03-01', '独立完成课程设计，使用 Java + MySQL 实现。')
ON DUPLICATE KEY UPDATE type = VALUES(type), title = VALUES(title), start_date = VALUES(start_date), description = VALUES(description);

-- 方向库（DIR009 为 INACTIVE，用于验证规则过滤仅取 ACTIVE）
-- personality_tags 为霍兰德（RIASEC）人格类型标签，用于“人格类型→方向”映射（R现实型/I研究型/A艺术型/S社会型/E企业型/C常规型）
INSERT INTO career_direction (id, direction_code, name, type, status, content, personality_tags) VALUES
    (1, 'DIR001', '软件开发工程师',   '技术研发', 'ACTIVE',   '从事后端/全栈软件开发，负责业务系统设计与实现。', 'R,I,C'),
    (2, 'DIR002', '数据科学与算法工程师', '数据算法', 'ACTIVE', '面向大数据与机器学习，负责算法设计与模型落地。', 'I,R,C'),
    (3, 'DIR003', '产品经理',         '产品管理', 'ACTIVE',   '负责产品规划、需求分析与项目推进。', 'E,S,A'),
    (4, 'DIR004', '网络安全工程师',   '技术研发', 'ACTIVE',   '负责系统安全防护、渗透测试与安全运营。', 'I,R,C'),
    (5, 'DIR005', '游戏开发工程师',   '技术研发', 'ACTIVE',   '从事游戏客户端/服务端开发与游戏逻辑实现。', 'A,I,R'),
    (6, 'DIR006', '测试开发工程师',   '技术研发', 'ACTIVE',   '负责自动化测试框架与质量保障体系建设。', 'C,I,R'),
    (7, 'DIR007', '前端开发工程师',   '技术研发', 'ACTIVE',   '从事 Web 前端界面开发与交互实现。', 'A,I,C'),
    (8, 'DIR008', '数据分析师',       '数据算法', 'ACTIVE',   '负责业务数据分析、报表与决策支持。', 'I,C,E'),
    (9, 'DIR009', '项目管理',         '项目管理', 'INACTIVE', '负责项目计划、进度与资源协调管理。', 'E,S,C')
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), status = VALUES(status),
    content = VALUES(content), personality_tags = VALUES(personality_tags);

-- 方向维度权重与目标值（全局权重沿用设计说明书 9.2：兴趣0.20/价值观0.15/能力0.25/学业0.15/倾向0.20/经历0.05）
INSERT INTO direction_dimension_weight (id, direction_id, dimension, target_value, weight, version_no) VALUES
    -- DIR001 软件开发
    (1,  1, 'interest',     85, 0.2000, 1), (2,  1, 'values',     65, 0.1500, 1),
    (3,  1, 'ability',      82, 0.2500, 1), (4,  1, 'academic',   80, 0.1500, 1),
    (5,  1, 'orientation',  85, 0.2000, 1), (6,  1, 'experience', 65, 0.0500, 1),
    -- DIR002 数据科学与算法
    (7,  2, 'interest',     80, 0.2000, 1), (8,  2, 'values',     70, 0.1500, 1),
    (9,  2, 'ability',      78, 0.2500, 1), (10, 2, 'academic',   88, 0.1500, 1),
    (11, 2, 'orientation',  85, 0.2000, 1), (12, 2, 'experience', 60, 0.0500, 1),
    -- DIR003 产品经理
    (13, 3, 'interest',     70, 0.2000, 1), (14, 3, 'values',     85, 0.1500, 1),
    (15, 3, 'ability',      70, 0.2500, 1), (16, 3, 'academic',   70, 0.1500, 1),
    (17, 3, 'orientation',  60, 0.2000, 1), (18, 3, 'experience', 75, 0.0500, 1),
    -- DIR004 网络安全
    (19, 4, 'interest',     75, 0.2000, 1), (20, 4, 'values',     70, 0.1500, 1),
    (21, 4, 'ability',      80, 0.2500, 1), (22, 4, 'academic',   78, 0.1500, 1),
    (23, 4, 'orientation',  80, 0.2000, 1), (24, 4, 'experience', 60, 0.0500, 1),
    -- DIR005 游戏开发
    (25, 5, 'interest',     88, 0.2000, 1), (26, 5, 'values',     60, 0.1500, 1),
    (27, 5, 'ability',      85, 0.2500, 1), (28, 5, 'academic',   75, 0.1500, 1),
    (29, 5, 'orientation',  78, 0.2000, 1), (30, 5, 'experience', 70, 0.0500, 1),
    -- DIR006 测试开发
    (31, 6, 'interest',     60, 0.2000, 1), (32, 6, 'values',     60, 0.1500, 1),
    (33, 6, 'ability',      70, 0.2500, 1), (34, 6, 'academic',   70, 0.1500, 1),
    (35, 6, 'orientation',  60, 0.2000, 1), (36, 6, 'experience', 60, 0.0500, 1),
    -- DIR007 前端开发
    (37, 7, 'interest',     80, 0.2000, 1), (38, 7, 'values',     65, 0.1500, 1),
    (39, 7, 'ability',      75, 0.2500, 1), (40, 7, 'academic',   75, 0.1500, 1),
    (41, 7, 'orientation',  75, 0.2000, 1), (42, 7, 'experience', 65, 0.0500, 1),
    -- DIR008 数据分析
    (43, 8, 'interest',     72, 0.2000, 1), (44, 8, 'values',     70, 0.1500, 1),
    (45, 8, 'ability',      72, 0.2500, 1), (46, 8, 'academic',   80, 0.1500, 1),
    (47, 8, 'orientation',  72, 0.2000, 1), (48, 8, 'experience', 65, 0.0500, 1)
ON DUPLICATE KEY UPDATE target_value = VALUES(target_value), weight = VALUES(weight);
