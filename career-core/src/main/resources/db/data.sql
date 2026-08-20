-- ============================================================
-- career_core 种子数据(幂等:INSERT IGNORE / WHERE NOT EXISTS)
-- 管理员账号由 DataInitializer 在应用启动时创建(需要 BCrypt 加密)
-- ============================================================

-- 字符串 ID 序列初始值
INSERT IGNORE INTO id_sequence (seq_name, next_val) VALUES
('sys_user', 1001),
('student_whitelist', 1),
('consent_record', 1),
('consent_document', 1),
('refresh_token', 1),
('token_blacklist', 1),
('student_profile', 1),
('student_experience', 1),
('deletion_request', 1),
('operation_audit_log', 1);

-- 隐私授权文档 v1.0(发布版本;version 与 Apifox 接口的 currentVersion 一致)
INSERT INTO consent_document (id, version, title, content, status, published_at)
SELECT 'CD001', 'v1.0', '隐私告知与 AI 使用说明',
       '本系统为新生生涯规划提供探索与行动计划参考。您授权系统在注册、测评、画像、推荐、计划与复盘过程中采集和使用您的学习相关信息，用于生成个性化发展建议。AI 相关输出由大模型生成，仅供探索参考，不构成职业决策结论；涉及重大决策、心理健康、法律、医疗等问题请咨询专业机构。您可以随时更正或申请删除您的个人信息。',
       'PUBLISHED', '2026-08-01 00:00:00'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consent_document WHERE version = 'v1.0');

-- 白名单测试数据(verify_code 同时作为注册后的初始密码)
INSERT IGNORE INTO student_whitelist (id, student_no, name, class_name, verify_code) VALUES
('WL001', '2026011301', '李明',  '计科2601', '202601'),
('WL002', '2026011309', '张同学', '计科2601', '202609'),
('WL003', '2026011310', '王芳',  '软工2601', '202610');
