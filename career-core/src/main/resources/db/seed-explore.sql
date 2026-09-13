-- ============================================================
-- 生涯探索问卷（EXPLORE）：兴趣标签（TAG 区）
-- 只换采集层：点选答案走既有 AssessmentService 计分 → 画像 → 推荐
-- 幂等:由 spring.sql.init 执行,固定 ID + INSERT IGNORE
-- 口径：每题唯一"符合"选项，scoresJson 为六维贡献(0~0.6/维)；
--       任意 3 个标签同维合计 ≤1.8，供 M2 PROBE 深挖继续补足到 ≤5（×20 归一语义）
-- ============================================================

INSERT IGNORE INTO questionnaire (id, type, name, type_name, icon, status, version, minutes, tip)
VALUES ('Q-EXPLORE', 'EXPLORE', '生涯探索', '生涯探索·兴趣定位', 'explore', 'PUBLISHED', 1, 3,
        '选择 1-3 个感兴趣的领域，完成探索并生成个性化方向推荐。');

INSERT IGNORE INTO questionnaire_version (id, questionnaire_id, version, status, question_count, change_note, published_at, published_by)
VALUES ('QV-EXPLORE-1', 'Q-EXPLORE', 1, 'PUBLISHED', 18, '探索快速版：兴趣标签区 + AI 深挖区', NOW(), 'system');

-- 兴趣标签（TAG 区，dim=EXPLORE_TAG 供聚合服务识别；CHOICE 计分不读 dim）
INSERT IGNORE INTO question (id, questionnaire_version_id, text, type, dim, sort_order) VALUES
('EQT-001', 'QV-EXPLORE-1', '编程与软件开发', 'CHOICE', 'EXPLORE_TAG', 1),
('EQT-002', 'QV-EXPLORE-1', '数据分析与算法', 'CHOICE', 'EXPLORE_TAG', 2),
('EQT-003', 'QV-EXPLORE-1', '产品与设计', 'CHOICE', 'EXPLORE_TAG', 3),
('EQT-004', 'QV-EXPLORE-1', '写作与内容创作', 'CHOICE', 'EXPLORE_TAG', 4),
('EQT-005', 'QV-EXPLORE-1', '人际沟通与协作', 'CHOICE', 'EXPLORE_TAG', 5),
('EQT-006', 'QV-EXPLORE-1', '组织与领导', 'CHOICE', 'EXPLORE_TAG', 6),
('EQT-007', 'QV-EXPLORE-1', '商业与创业', 'CHOICE', 'EXPLORE_TAG', 7),
('EQT-008', 'QV-EXPLORE-1', '科研与深造', 'CHOICE', 'EXPLORE_TAG', 8),
('EQT-009', 'QV-EXPLORE-1', '教学与分享', 'CHOICE', 'EXPLORE_TAG', 9),
('EQT-010', 'QV-EXPLORE-1', '公益与社会服务', 'CHOICE', 'EXPLORE_TAG', 10),
('EQT-011', 'QV-EXPLORE-1', '动手实践与制造', 'CHOICE', 'EXPLORE_TAG', 11),
('EQT-012', 'QV-EXPLORE-1', '艺术与设计表达', 'CHOICE', 'EXPLORE_TAG', 12);

INSERT IGNORE INTO question_option (id, question_id, text, scores_json, sort_order) VALUES
('EQO-001', 'EQT-001', '符合', '{"ability":0.6,"interest":0.5}', 0),
('EQO-002', 'EQT-002', '符合', '{"academic":0.5,"ability":0.4,"interest":0.3}', 0),
('EQO-003', 'EQT-003', '符合', '{"values":0.4,"interest":0.4,"ability":0.3}', 0),
('EQO-004', 'EQT-004', '符合', '{"tendency":0.5,"interest":0.3,"values":0.2}', 0),
('EQO-005', 'EQT-005', '符合', '{"tendency":0.5,"values":0.3,"interest":0.2}', 0),
('EQO-006', 'EQT-006', '符合', '{"tendency":0.4,"practice":0.4,"values":0.3}', 0),
('EQO-007', 'EQT-007', '符合', '{"practice":0.5,"values":0.4,"tendency":0.2}', 0),
('EQO-008', 'EQT-008', '符合', '{"academic":0.6,"interest":0.4}', 0),
('EQO-009', 'EQT-009', '符合', '{"tendency":0.5,"practice":0.3,"values":0.2}', 0),
('EQO-010', 'EQT-010', '符合', '{"values":0.6,"tendency":0.3}', 0),
('EQO-011', 'EQT-011', '符合', '{"practice":0.6,"ability":0.3}', 0),
('EQO-012', 'EQT-012', '符合', '{"interest":0.4,"values":0.3,"practice":0.2}', 0);

-- AI 深挖（PROBE 区，dim=EXPLORE_PROBE；M2 由 explore_chat 场景选题，学生以选项作答折算六维）
-- 预算：每单选贡献 ≤0.6/维，标签(TAG)+深挖合计使六维总和 ≤5（×20 归一语义）
INSERT IGNORE INTO question (id, questionnaire_version_id, text, type, dim, sort_order) VALUES
('EQP-001', 'QV-EXPLORE-1', '课余时间更愿意投入在哪类事情上？', 'CHOICE', 'EXPLORE_PROBE', 101),
('EQP-002', 'QV-EXPLORE-1', '面对有挑战的任务，你通常的做法是？', 'CHOICE', 'EXPLORE_PROBE', 102),
('EQP-003', 'QV-EXPLORE-1', '你更看重工作的哪一面？', 'CHOICE', 'EXPLORE_PROBE', 103),
('EQP-004', 'QV-EXPLORE-1', '学习新知识时你更习惯？', 'CHOICE', 'EXPLORE_PROBE', 104),
('EQP-005', 'QV-EXPLORE-1', '团队合作中你更倾向的角色是？', 'CHOICE', 'EXPLORE_PROBE', 105),
('EQP-006', 'QV-EXPLORE-1', '毕业后的首选节奏是？', 'CHOICE', 'EXPLORE_PROBE', 106);

INSERT IGNORE INTO question_option (id, question_id, text, scores_json, sort_order) VALUES
('EQPO-001A', 'EQP-001', '做项目、写代码、搞实践', '{"practice":0.6,"ability":0.3}', 0),
('EQPO-001B', 'EQP-001', '读书、看课程、做研究', '{"academic":0.6,"interest":0.3}', 1),
('EQPO-002A', 'EQP-002', '拆解目标、主动推进', '{"ability":0.5,"tendency":0.4}', 0),
('EQPO-002B', 'EQP-002', '先请教他人、结伴完成', '{"tendency":0.5,"values":0.3}', 1),
('EQPO-003A', 'EQP-003', '收入与发展空间', '{"values":0.6,"tendency":0.2}', 0),
('EQPO-003B', 'EQP-003', '意义感与兴趣契合', '{"interest":0.5,"values":0.4}', 1),
('EQPO-004A', 'EQP-004', '动手试错、快速迭代', '{"practice":0.5,"ability":0.4}', 0),
('EQPO-004B', 'EQP-004', '系统学习、打牢基础', '{"academic":0.5,"ability":0.3}', 1),
('EQPO-005A', 'EQP-005', '组织分工、带队推进', '{"tendency":0.5,"practice":0.3}', 0),
('EQPO-005B', 'EQP-005', '专注自己的模块、交付靠谱', '{"ability":0.5,"values":0.2}', 1),
('EQPO-006A', 'EQP-006', '尽早工作、积累经验', '{"practice":0.5,"tendency":0.4}', 0),
('EQPO-006B', 'EQP-006', '继续深造、提升学历', '{"academic":0.5,"interest":0.3}', 1);
