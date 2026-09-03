-- ============================================================
-- 方向库种子数据（幂等 INSERT IGNORE）
-- 供推荐引擎/方向探索使用（PUBLISHED 状态）
-- target_json 为六维目标 {interest,values,ability,academic,tendency,practice}，0-1
-- ============================================================

INSERT IGNORE INTO career_direction (id, name, path, icon, intro, target_json, min_ability, min_academic,
                                     learning_json, abilities_json, courses_json, activities_json,
                                     path_desc_json, misconceptions_json, sort_order, applicable_majors_json,
                                     status, updated) VALUES
('data_analysis', '数据分析师', 'employment', 'data',
 '面向数据采集、清洗、分析与可视化表达的就业方向。',
 '{"interest":0.8,"values":0.4,"ability":0.7,"academic":0.7,"tendency":0.7,"practice":0.6}',
 60, 60,
 '["Python","SQL","统计学","数据可视化"]',
 '["programming_basic","math_basic"]',
 '["Python数据分析","SQL数据库","统计学基础"]',
 '["数据分析实战项目","Kaggle练习"]',
 '["基础学习","项目实战","求职准备"]',
 '["仅需会Excel","不需要数学基础"]',
 1, '["计算机类"]', 'PUBLISHED', '2026-08-21'),

('backend_dev', '后端开发工程师', 'employment', 'backend',
 '面向 Web 服务端、接口与系统架构的就业方向。',
 '{"interest":0.7,"values":0.4,"ability":0.8,"academic":0.7,"tendency":0.8,"practice":0.6}',
 65, 60,
 '["Java","Spring Boot","数据库","Linux"]',
 '["programming_basic"]',
 '["Java基础","Spring Boot","MySQL","计算机网络"]',
 '["后端项目开发","系统设计"]',
 '["基础学习","框架实践","项目上线"]',
 '["只会增删改查就行"]',
 2, '["计算机类"]', 'PUBLISHED', '2026-08-21'),

('ai_research', '算法工程师', 'graduate', 'algorithm',
 '面向机器学习、深度学习与算法研究的深造方向。',
 '{"interest":0.7,"values":0.5,"ability":0.8,"academic":0.9,"tendency":0.6,"practice":0.5}',
 70, 80,
 '["Python","机器学习","深度学习","数学"]',
 '["programming_basic","math_basic"]',
 '["高等数学","机器学习","深度学习","论文写作"]',
 '["算法竞赛","科研项目"]',
 '["基础夯实","科研训练","读研深造"]',
 '["本科就能进大厂算法岗"]',
 3, '["计算机类","数学类"]', 'PUBLISHED', '2026-08-21'),

('software_eng', '软件工程师', 'employment', 'software',
 '面向软件开发、测试与工程化实践的就业方向。',
 '{"interest":0.7,"values":0.5,"ability":0.7,"academic":0.6,"tendency":0.7,"practice":0.7}',
 55, 55,
 '["Java","Python","软件工程","测试"]',
 '["programming_basic"]',
 '["程序设计","软件工程","数据结构"]',
 '["软件项目开发","开源贡献"]',
 '["基础学习","项目实践","求职准备"]',
 '["编程能力强就够"]',
 4, '["计算机类"]', 'PUBLISHED', '2026-08-21');
