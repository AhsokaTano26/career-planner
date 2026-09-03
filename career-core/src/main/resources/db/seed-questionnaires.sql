-- ============================================================
-- 测评问卷种子数据（幂等 INSERT IGNORE,可重复执行）
-- 问卷:能力自评 / 霍兰德兴趣简版
-- 六维:interest(values) ability(academic) tendency(practice)
-- ============================================================

-- ---------- 问卷 ----------
INSERT IGNORE INTO questionnaire (id, type, name, type_name, icon, status, version, minutes, tip) VALUES
('Q-001', 'ability',  '能力自评',   '能力自评',   'ability', 'PUBLISHED', 1, 3, '评估你的基础能力维度'),
('Q-002', 'holland',  '霍兰德兴趣简版', '霍兰德兴趣', 'holland', 'PUBLISHED', 1, 2, '了解你的兴趣倾向');

-- ---------- 问卷版本 ----------
INSERT IGNORE INTO questionnaire_version (id, questionnaire_id, version, status, question_count) VALUES
('QV-001', 'Q-001', 1, 'PUBLISHED', 6),
('QV-002', 'Q-002', 1, 'PUBLISHED', 3);

-- ---------- 题目（能力自评 RATING 1-5，每题映射一个维度） ----------
INSERT IGNORE INTO question (id, questionnaire_version_id, text, type, dim, sort_order) VALUES
('QN-101', 'QV-001', '我对数据分析、编程等动手实践有浓厚兴趣', 'RATING', 'interest', 1),
('QN-102', 'QV-001', '我在团队合作与沟通表达方面表现良好', 'RATING', 'values', 2),
('QN-103', 'QV-001', '我的逻辑思维与问题解决能力较强', 'RATING', 'ability', 3),
('QN-104', 'QV-001', '我的学业成绩与学习效率较高', 'RATING', 'academic', 4),
('QN-105', 'QV-001', '我有明确的升学或就业倾向', 'RATING', 'tendency', 5),
('QN-106', 'QV-001', '我有参加竞赛、实习或项目实践的经验', 'RATING', 'practice', 6);

-- 能力自评 RATING 题无需选项（前端按 1-5 打分）

-- ---------- 题目（霍兰德兴趣 CHOICE） ----------
INSERT IGNORE INTO question (id, questionnaire_version_id, text, type, dim, sort_order) VALUES
('QN-201', 'QV-002', '你更喜欢哪种活动？', 'CHOICE', 'interest', 1),
('QN-202', 'QV-002', '你更看重工作中的哪一点？', 'CHOICE', 'values', 2),
('QN-203', 'QV-002', '未来你更倾向于？', 'CHOICE', 'tendency', 3);

-- ---------- 选项（霍兰德） ----------
INSERT IGNORE INTO question_option (id, question_id, text, scores_json, sort_order) VALUES
('QO-2011', 'QN-201', '动手做实验或项目', '{"interest":0.8,"ability":0.5,"practice":0.7}', 1),
('QO-2012', 'QN-201', '阅读与研究分析', '{"interest":0.6,"academic":0.8,"values":0.4}', 2),
('QO-2013', 'QN-201', '与人交流协作', '{"values":0.8,"interest":0.4,"ability":0.5}', 3),
('QO-2021', 'QN-202', '成就感与挑战', '{"tendency":0.8,"ability":0.7,"interest":0.5}', 1),
('QO-2022', 'QN-202', '稳定与安全感', '{"values":0.8,"academic":0.5,"tendency":0.4}', 2),
('QO-2023', 'QN-202', '帮助他人与社会价值', '{"values":0.9,"interest":0.4,"practice":0.4}', 3),
('QO-2031', 'QN-203', '毕业后直接就业', '{"tendency":0.9,"practice":0.7,"ability":0.6}', 1),
('QO-2032', 'QN-203', '继续深造读研', '{"academic":0.9,"tendency":0.6,"values":0.5}', 2),
('QO-2033', 'QN-203', '出国留学', '{"academic":0.8,"values":0.6,"interest":0.5}', 3);
