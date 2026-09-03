-- ============================================================
-- 调试全量种子数据(幂等,可重复执行)
-- 执行顺序:seed-debug-users.sql -> seed_create_accounts.py -> 本文件
-- 用户外键通过 session 变量按 username 关联,不依赖生成的业务 ID
-- 不改动 schema.sql/data.sql;本文件不会由 spring.sql.init 自动加载
-- ============================================================

-- 账号必须已存在(由 seed_create_accounts.py 注册)
SET @u1 = (SELECT id FROM sys_user WHERE username = '2026011301');
SET @u2 = (SELECT id FROM sys_user WHERE username = '2026011309');
SET @u3 = (SELECT id FROM sys_user WHERE username = '2026011310');
SET @ua = (SELECT id FROM sys_user WHERE username = 'A2026001');

-- 辅导员注册后是 STUDENT 角色,改为 ADVISOR(辅导员端接口需要)
UPDATE sys_user SET role = 'ADVISOR' WHERE username = 'A2026001';

-- ---------------- 学生档案 ----------------
INSERT IGNORE INTO student_profile (id, user_id, name, class_name, grade, major_category,
    basic_json, academic_json, interest_prefs_json, ability_self_json, values_json,
    development_intention, completeness) VALUES
('SP-001', @u1, '李明', '计科2601', '2026级', '计算机类',
 '{"gender":"男","hometown":"浙江杭州","birthday":"2008-03-12","phone":"13800000001"}',
 '{"math":82,"english":75,"programming":88,"note":"大一上"}',
 '["编程","数据分析"]',
 '{"programming":4,"math":3,"english":3,"communication":3,"organization":2}',
 '["技术成就","稳定发展"]', 'employment', 85),
('SP-002', @u2, '张同学', '计科2601', '2026级', '计算机类',
 '{"gender":"男","hometown":"江苏南京","birthday":"2008-07-21","phone":"13800000002"}',
 '{"math":78,"english":80,"programming":84,"note":"大一上"}',
 '["后端开发","系统架构"]',
 '{"programming":4,"math":3,"english":4,"communication":3,"organization":3}',
 '["挑战","成就感"]', 'employment', 80),
('SP-003', @u3, '王芳', '软工2601', '2026级', '计算机类',
 '{"gender":"女","hometown":"四川成都","birthday":"2008-11-05","phone":"13800000003"}',
 '{"math":90,"english":85,"programming":80,"note":"大一上"}',
 '["算法","科研"]',
 '{"programming":3,"math":5,"english":4,"communication":4,"organization":3}',
 '["探索","学术"]', 'graduate', 88);

-- ---------------- 学生经历 ----------------
INSERT IGNORE INTO student_experience (id, student_id, type, title, start_date, end_date, description) VALUES
('EXP-001', @u1, '竞赛', '蓝桥杯软件赛省赛', '2026-03', '2026-05', '参与蓝桥杯 C/C++ 组,完成算法训练并获省三等奖。'),
('EXP-002', @u1, '项目', '校园二手交易平台', '2026-04', NULL, '独立开发 Spring Boot + Vue 二手交易 demo,支持发布与私信。'),
('EXP-003', @u2, '学生工作', '计科2601 班长', '2026-09', NULL, '负责组织班级活动与信息传达。'),
('EXP-004', @u3, '志愿服务', '迎新志愿者', '2026-09', '2026-09', '参与新生报到引导志愿服务。');

-- ---------------- 测评会话 ----------------
INSERT IGNORE INTO assessment_session (id, student_id, questionnaire_version_id, status,
    total_questions, answered_questions, started_at, finished_at, score_json) VALUES
('AS-001', @u1, 'QV-001', 'COMPLETED', 6, 6, '2026-09-10 10:00:00', '2026-09-10 10:12:00',
 '{"interest":0.8,"values":0.5,"ability":0.7,"academic":0.6,"tendency":0.7,"practice":0.6}'),
('AS-002', @u2, 'QV-001', 'COMPLETED', 6, 6, '2026-09-11 14:00:00', '2026-09-11 14:10:00',
 '{"interest":0.7,"values":0.6,"ability":0.8,"academic":0.6,"tendency":0.8,"practice":0.6}'),
('AS-003', @u3, 'QV-002', 'COMPLETED', 3, 3, '2026-09-12 09:00:00', '2026-09-12 09:05:00',
 '{"interest":0.7,"values":0.5,"ability":0.9,"academic":0.9,"tendency":0.6,"practice":0.5}');

-- ---------------- 画像快照 ----------------
INSERT IGNORE INTO profile_snapshot (id, student_id, source_version, dimension_json, summary,
    strengths_json, explore_json, version_no, completeness) VALUES
('PS-001', @u1, 'AS-001',
 '[{"key":"interest","name":"兴趣","score":80},{"key":"values","name":"价值观","score":50},{"key":"ability","name":"能力","score":70},{"key":"academic","name":"学业","score":60},{"key":"tendency","name":"倾向","score":70},{"key":"practice","name":"实践","score":60}]',
 '实践能力突出,对数据分析与编程感兴趣,数学可加强。',
 '["编程","数据分析"]', '["英语","数学"]', 1, 80),
('PS-002', @u2, 'AS-002',
 '[{"key":"interest","name":"兴趣","score":70},{"key":"values","name":"价值观","score":60},{"key":"ability","name":"能力","score":80},{"key":"academic","name":"学业","score":60},{"key":"tendency","name":"倾向","score":80},{"key":"practice","name":"实践","score":60}]',
 '工程能力强,倾向后端开发,沟通良好。',
 '["编程","沟通"]', '["英语"]', 1, 78),
('PS-003', @u3, 'AS-003',
 '[{"key":"interest","name":"兴趣","score":70},{"key":"values","name":"价值观","score":50},{"key":"ability","name":"能力","score":90},{"key":"academic","name":"学业","score":90},{"key":"tendency","name":"倾向","score":60},{"key":"practice","name":"实践","score":50}]',
 '学业与算法能力强,适合深造,实践待积累。',
 '["数学","算法"]', '["实践"]', 1, 86);

-- ---------------- 推荐批次 + 结果 ----------------
INSERT IGNORE INTO recommendation_run (id, student_id, profile_snapshot_id, rule_version, status, generated_at) VALUES
('RRUN-001', @u1, 'PS-001', 'R1.0', 'SUCCESS', '2026-09-12 12:00:00'),
('RRUN-002', @u2, 'PS-002', 'R1.0', 'SUCCESS', '2026-09-12 12:00:00'),
('RRUN-003', @u3, 'PS-003', 'R1.0', 'SUCCESS', '2026-09-12 12:00:00');

INSERT IGNORE INTO recommendation_result (id, run_id, direction_id, score, `rank`, confidence,
    reasons_json, strengths_json, gaps_json, semester_actions_json) VALUES
('RRES-001', 'RRUN-001', 'data_analysis', 88.0, 1, 'HIGH',
 '["兴趣与能力匹配度高","实践项目丰富"]', '["编程","数据分析"]', '["数学基础"]',
 '["大一上完成 Python 数据分析入门","参与一次 Kaggle 练习"]'),
('RRES-002', 'RRUN-001', 'backend_dev', 80.0, 2, 'MEDIUM',
 '["工程能力较好"]', '["编程"]', '["系统架构经验"]', '["完成一个 Spring Boot 项目"]'),
('RRES-003', 'RRUN-001', 'ai_research', 72.0, 3, 'LOW',
 '["学业尚可但实践不足"]', '["数学"]', '["深度学习实践","科研经历"]', '["补机器学习基础"]'),
('RRES-004', 'RRUN-002', 'backend_dev', 86.0, 1, 'HIGH',
 '["倾向与能力双高"]', '["编程","沟通"]', '["高并发经验"]', '["上线一个后端服务"]'),
('RRES-005', 'RRUN-002', 'software_eng', 78.0, 2, 'MEDIUM',
 '["工程化能力好"]', '["编程"]', '["测试经验"]', '["参与开源项目"]'),
('RRES-006', 'RRUN-002', 'data_analysis', 70.0, 3, 'LOW',
 '["数据分析兴趣一般"]', '["编程"]', '["统计分析"]', '["补 SQL 与统计"]'),
('RRES-007', 'RRUN-003', 'ai_research', 90.0, 1, 'HIGH',
 '["数学与算法突出,适合深造"]', '["数学","算法"]', '["科研产出"]', '["参加科研项目"]'),
('RRES-008', 'RRUN-003', 'backend_dev', 68.0, 2, 'LOW',
 '["工程实践偏弱"]', '["编程"]', '["项目经验"]', '["做一个后端 demo"]'),
('RRES-009', 'RRUN-003', 'software_eng', 74.0, 3, 'MEDIUM',
 '["软件工程基础尚可"]', '["编程"]', '["工程规范"]', '["学习软件工程方法"]');

-- ---------------- 学生目标 + 版本 ----------------
INSERT IGNORE INTO student_goal (id, student_id, goal_type, direction_id, name, status, chosen_at) VALUES
('SG-001', @u1, 'PRIMARY', 'data_analysis', '数据分析师', 'ACTIVE', '2026-09-13 10:00:00'),
('SG-002', @u2, 'PRIMARY', 'backend_dev', '后端开发工程师', 'ACTIVE', '2026-09-13 10:00:00'),
('SG-003', @u3, 'BACKUP', 'ai_research', '算法工程师', 'ACTIVE', '2026-09-13 10:00:00');

INSERT IGNORE INTO goal_version (id, goal_id, version_no, change_reason) VALUES
('GV-001', 'SG-001', 1, '初始选择'),
('GV-002', 'SG-002', 1, '初始选择'),
('GV-003', 'SG-003', 1, '初始选择');

-- ---------------- 学期计划 + 版本 + 任务 + 打卡 ----------------
INSERT IGNORE INTO semester_plan (id, student_id, version_no, status, source, goal_summary,
    semester_goals_json, monthly_tasks_json, confirmed_at) VALUES
('PL-001', @u1, 1, 'CONFIRMED', 'AI', '成为数据分析师:夯实编程与统计基础',
 '[{"title":"掌握 Python 数据分析","abilityTag":"programming_basic"},{"title":"补齐统计基础","abilityTag":"math_basic"}]',
 '[{"month":"2026-09","title":"Python 数据分析入门","taskType":"LEARNING","estimatedHours":20},{"month":"2026-10","title":"SQL 进阶","taskType":"LEARNING","estimatedHours":16}]',
 '2026-09-13 11:00:00'),
('PL-002', @u2, 1, 'DRAFT', 'MANUAL', '成为后端开发:完成 Spring Boot 项目',
 '[{"title":"掌握 Spring Boot","abilityTag":"programming_basic"}]',
 '[{"month":"2026-09","title":"Spring Boot 实战","taskType":"LEARNING","estimatedHours":24}]', NULL),
('PL-003', @u3, 1, 'CONFIRMED', 'AI', '深造算法:强化数学与科研',
 '[{"title":"机器学习基础","abilityTag":"math_basic"},{"title":"科研训练","abilityTag":"algorithm"}]',
 '[{"month":"2026-09","title":"高数复习","taskType":"LEARNING","estimatedHours":20}]',
 '2026-09-13 11:30:00');

INSERT IGNORE INTO plan_version (id, plan_id, version_no, content_json) VALUES
('PV-001', 'PL-001', 1, '{"goalSummary":"成为数据分析师","semesterGoals":[{"title":"掌握 Python 数据分析"}]}'),
('PV-002', 'PL-002', 1, '{"goalSummary":"成为后端开发","semesterGoals":[{"title":"掌握 Spring Boot"}]}'),
('PV-003', 'PL-003', 1, '{"goalSummary":"深造算法","semesterGoals":[{"title":"机器学习基础"}]}');

INSERT IGNORE INTO plan_task (id, plan_id, student_id, month, title, task_type, est_hours,
    status, deadline, ability_tags_json, note) VALUES
('PT-001', 'PL-001', @u1, '2026-09', 'Python 数据分析入门', 'LEARNING', 20, 'PENDING', '2026-09-30', '["programming_basic"]', '完成 pandas 基础教程'),
('PT-002', 'PL-001', @u1, '2026-10', 'SQL 进阶', 'LEARNING', 16, 'DOING', '2026-10-31', '["programming_basic"]', '练习多表联查'),
('PT-003', 'PL-001', @u1, '2026-11', 'Kaggle 入门赛', 'PRACTICE', 24, 'PENDING', '2026-11-30', '["data_analysis"]', '提交首个 notebook'),
('PT-004', 'PL-002', @u2, '2026-09', 'Spring Boot 实战', 'LEARNING', 24, 'DOING', '2026-09-30', '["programming_basic"]', '搭建 RESTful API'),
('PT-005', 'PL-002', @u2, '2026-10', 'MySQL 调优', 'LEARNING', 16, 'PENDING', '2026-10-31', '["programming_basic"]', '索引与慢查询'),
('PT-006', 'PL-002', @u2, '2026-12', '项目上线', 'PRACTICE', 30, 'PENDING', '2026-12-31', '["programming_basic"]', '部署到云服务器'),
('PT-007', 'PL-003', @u3, '2026-09', '高数复习', 'LEARNING', 20, 'DONE', '2026-09-25', '["math_basic"]', '完成三轮复习'),
('PT-008', 'PL-003', @u3, '2026-10', '机器学习导论', 'LEARNING', 24, 'PENDING', '2026-10-31', '["algorithm"]', '吴恩达课程'),
('PT-009', 'PL-003', @u3, '2027-03', '科研训练', 'REVIEW', 40, 'PENDING', '2027-03-31', '["algorithm"]', '加入导师课题组');

INSERT IGNORE INTO task_checkin (id, task_id, done_desc, gains, difficulties, checked_in_at) VALUES
('TC-001', 'PT-002', '完成 Spring Boot 环境搭建与 Hello API', '熟悉依赖管理与启动流程', '配置数据源耗时', '2026-09-20 20:00:00'),
('TC-002', 'PT-004', '复习索引与事务,写出笔记', '理解 B+ 树与隔离级别', '隔离级别易混', '2026-09-22 21:00:00'),
('TC-003', 'PT-007', '完成高数三轮复习', '概念清晰,做题正确率提升', '级数部分仍有盲区', '2026-09-25 19:00:00');

-- ---------------- 阶段复盘 ----------------
INSERT IGNORE INTO stage_review (id, student_id, cycle, status, content_json, ai_summary,
    ai_suggest_json, advisor_requested, advisor_reply, submitted_at) VALUES
('SR-001', @u1, '2026-09', 'SUBMITTED',
 '{"done":["完成 Python 入门","提交作业"],"undone":["未做项目"],"interest":"数据分析","ability":"编程提升","next":"做 Kaggle"}',
 '建议加强项目实践并参与竞赛。', '["参与一次数据分析竞赛"]', 1, NULL, '2026-09-28 10:00:00'),
('SR-002', @u2, '2026-09', 'DRAFT',
 '{"done":["Spring Boot 实战"],"undone":["未上线"],"interest":"后端","ability":"工程能力","next":"项目上线"}',
 NULL, NULL, 0, NULL, NULL),
('SR-003', @u3, '2026-10', 'SUBMITTED',
 '{"done":["高数复习"],"undone":["科研未开始"],"interest":"算法","ability":"数学强","next":"机器学习"}',
 '数学基础扎实,建议尽早进入科研。', '["联系导师加入课题组"]', 1, '建议加强数学基础并参与科研训练。', '2026-10-08 10:00:00');

-- ---------------- 收藏 + 提醒 ----------------
INSERT IGNORE INTO favorite (id, student_id, direction_id) VALUES
('FAV-001', @u1, 'data_analysis'),
('FAV-002', @u1, 'backend_dev'),
('FAV-003', @u2, 'software_eng'),
('FAV-004', @u3, 'ai_research');

INSERT IGNORE INTO reminder (id, student_id, type, title, content, is_read) VALUES
('REM-001', @u1, 'TASK_DEADLINE', '任务即将到期', 'Python 数据分析入门 将于 2026-09-30 截止', 0),
('REM-002', @u2, 'REVIEW_REMIND', '月度复盘提醒', '请提交 2026-09 阶段复盘', 0),
('REM-003', @u3, 'ADVISOR_REPLY', '辅导员已回复', '辅导员对你的复盘给出了建议', 1),
('REM-004', @u1, 'PLAN_UPDATE', '计划已确认', '你的学期计划已确认', 0);

-- ---------------- 辅导员关系 + 指导意见 ----------------
INSERT IGNORE INTO advisor_student_relation (id, advisor_id, student_id) VALUES
('AR-001', @ua, @u1),
('AR-002', @ua, @u2),
('AR-003', @ua, @u3);

INSERT IGNORE INTO advisor_comment (id, student_id, advisor_id, content, advice_type, suggested_task, retest_reason) VALUES
('GC-001', @u1, @ua, '整体表现良好,建议加强项目实践。', 'COMMENT', NULL, NULL),
('GC-002', @u2, @ua, '建议完成后端项目并打卡。', 'SUGGEST_TASK', '部署一个 Spring Boot demo 到云服务器', NULL),
('GC-003', @u3, @ua, '数学基础偏弱,建议重修高数并复测。', 'SUGGEST_RETEST', NULL, '测评显示数学维度较低');

-- ---------------- 管理端参考(让配置/培养方案接口有数据) ----------------
INSERT IGNORE INTO ability_tag (id, name, category) VALUES
('communication', '沟通表达', '能力'),
('organization', '组织协调', '能力'),
('algorithm', '算法', '能力'),
('data_analysis', '数据分析', '能力');

INSERT IGNORE INTO task_template (id, direction_id, name, goal_summary, semester_goals_json, monthly_tasks_json, status) VALUES
('TT-001', 'data_analysis', '数据分析师入门模板', '掌握数据分析基础',
 '[{"title":"Python 数据分析","abilityTag":"programming_basic"}]',
 '[{"month":"2026-09","title":"Python 数据分析入门","taskType":"LEARNING","estimatedHours":20}]', 'ACTIVE'),
('TT-002', 'backend_dev', '后端开发模板', '完成后端项目',
 '[{"title":"Spring Boot","abilityTag":"programming_basic"}]',
 '[{"month":"2026-09","title":"Spring Boot 实战","taskType":"LEARNING","estimatedHours":24}]', 'ACTIVE'),
('TT-003', 'ai_research', '算法深造模板', '进入科研',
 '[{"title":"机器学习","abilityTag":"math_basic"}]',
 '[{"month":"2026-09","title":"机器学习导论","taskType":"LEARNING","estimatedHours":24}]', 'ACTIVE'),
('TT-004', 'software_eng', '软件工程模板', '工程化实践',
 '[{"title":"软件工程","abilityTag":"programming_basic"}]',
 '[{"month":"2026-09","title":"软件工程方法","taskType":"LEARNING","estimatedHours":16}]', 'ACTIVE');

INSERT IGNORE INTO model_config (id, config_key, config_value, updated_by) VALUES
('MC-001', 'llm.provider', 'deepseek', 'admin'),
('MC-002', 'llm.model', 'deepseek-v4-flash', 'admin');

INSERT IGNORE INTO prompt_version (id, scene, version, status, content, published_at, published_by) VALUES
('PR-001', 'recommendation_explain', 'P1', 'PUBLISHED', '根据结构化评分生成自然语言推荐解释。', '2026-08-20 00:00:00', 'admin'),
('PR-002', 'plan_generate', 'P1', 'PUBLISHED', '根据目标与画像生成学期计划。', '2026-08-20 00:00:00', 'admin'),
('PR-003', 'review_summarize', 'P1', 'PUBLISHED', '总结阶段复盘并给出建议。', '2026-08-20 00:00:00', 'admin'),
('PR-004', 'career_chat', 'P1', 'PUBLISHED', '生涯咨询对话提示词。', '2026-08-20 00:00:00', 'admin');

-- 培养方案:版本 + 课程 + 课程能力标签 + 导入任务与待审条目
INSERT IGNORE INTO curriculum_version (id, name, major, course_count, status, published_at, published_by) VALUES
('CV-001', '计算机类培养方案(2026)', '计算机类', 6, 'PUBLISHED', '2026-08-25 00:00:00', 'admin');

INSERT IGNORE INTO course (id, version_id, course_code, course_name, semester, credits, hours, category, module, prerequisites_json) VALUES
('CO-001', 'CV-001', 'CS101', '程序设计基础', '2026-2027-1', 4, 64, '专业基础', '必修', '[]'),
('CO-002', 'CV-001', 'CS201', '数据结构', '2026-2027-2', 4, 64, '专业核心', '必修', '["CS101"]'),
('CO-003', 'CV-001', 'CS202', '算法设计与分析', '2027-2028-1', 3, 48, '专业核心', '必修', '["CS201"]'),
('CO-004', 'CV-001', 'CS301', '数据库系统', '2027-2028-1', 3, 48, '专业核心', '必修', '["CS101"]'),
('CO-005', 'CV-001', 'CS302', '机器学习', '2027-2028-2', 3, 48, '专业选修', '选修', '["CS201","CS202"]'),
('CO-006', 'CV-001', 'CS401', '软件工程', '2028-2029-1', 2, 32, '专业选修', '选修', '["CS101"]');

INSERT IGNORE INTO course_ability_tag (id, course_id, ability_tag) VALUES
('CAT-001', 'CO-001', 'programming_basic'),
('CAT-002', 'CO-002', 'algorithm'),
('CAT-003', 'CO-003', 'algorithm'),
('CAT-004', 'CO-004', 'programming_basic'),
('CAT-005', 'CO-005', 'algorithm'),
('CAT-006', 'CO-006', 'programming_basic');

INSERT IGNORE INTO curriculum_import_job (id, filename, file_type, status, total_items, parsed_items, confidence, created_at) VALUES
('CJ-001', '2026培养方案.pdf', 'PDF', 'REVIEW_REQUIRED', 3, 3, 86.00, '2026-08-26 10:00:00');

INSERT IGNORE INTO curriculum_import_item (id, job_id, course_code, course_name, semester, credits, hours, category, module, prerequisites_json, ability_tags_json, confidence, page_ref, status) VALUES
('IT-001', 'CJ-001', 'CS101', '程序设计基础', '2026-2027-1', 4, 64, '专业基础', '必修', '["CS100"]', '["programming_basic"]', 92.00, '第 12 页', 'PENDING'),
('IT-002', 'CJ-001', 'CS201', '数据结构', '2026-2027-2', 4, 64, '专业核心', '必修', '["CS101"]', '["programming_basic","algorithm"]', 88.00, '第 30 页', 'PENDING'),
('IT-003', 'CJ-001', 'CS202', '数据结构(重修版)', '2026-2027-2', 4, 64, '专业核心', '必修', '["CS101"]', '["algorithm"]', 61.00, '第 31 页', 'PENDING');

-- ============================================================
-- 重置脚本(需要时取消注释执行)
-- DELETE FROM curriculum_import_item WHERE job_id='CJ-001';
-- DELETE FROM curriculum_import_job WHERE id='CJ-001';
-- DELETE FROM course_ability_tag WHERE id LIKE 'CAT-0%';
-- DELETE FROM course WHERE version_id='CV-001';
-- DELETE FROM curriculum_version WHERE id='CV-001';
-- DELETE FROM prompt_version WHERE id LIKE 'PR-0%';
-- DELETE FROM model_config WHERE id LIKE 'MC-0%';
-- DELETE FROM task_template WHERE id LIKE 'TT-0%';
-- DELETE FROM advisor_comment WHERE id LIKE 'GC-0%';
-- DELETE FROM advisor_student_relation WHERE id LIKE 'AR-0%';
-- DELETE FROM reminder WHERE id LIKE 'REM-0%';
-- DELETE FROM favorite WHERE id LIKE 'FAV-0%';
-- DELETE FROM stage_review WHERE id LIKE 'SR-0%';
-- DELETE FROM task_checkin WHERE id LIKE 'TC-0%';
-- DELETE FROM plan_task WHERE id LIKE 'PT-0%';
-- DELETE FROM plan_version WHERE id LIKE 'PV-0%';
-- DELETE FROM semester_plan WHERE id LIKE 'PL-0%';
-- DELETE FROM goal_version WHERE id LIKE 'GV-0%';
-- DELETE FROM student_goal WHERE id LIKE 'SG-0%';
-- DELETE FROM recommendation_result WHERE id LIKE 'RRES-%';
-- DELETE FROM recommendation_run WHERE id LIKE 'RRUN-%';
-- DELETE FROM profile_snapshot WHERE id LIKE 'PS-0%';
-- DELETE FROM assessment_session WHERE id LIKE 'AS-0%';
-- DELETE FROM student_experience WHERE id LIKE 'EXP-0%';
-- DELETE FROM student_profile WHERE id LIKE 'SP-0%';
-- UPDATE sys_user SET role='STUDENT' WHERE username='A2026001';
-- ============================================================
