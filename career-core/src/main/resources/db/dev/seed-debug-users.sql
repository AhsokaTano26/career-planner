-- 调试前置:插入辅导员白名单(供 seed_create_accounts.py 注册辅导员账号)
-- 仅插入一行,幂等;执行顺序:本文件 → seed_create_accounts.py → seed-debug.sql
INSERT IGNORE INTO student_whitelist (id, student_no, name, class_name, verify_code) VALUES
('WL900', 'A2026001', '陈辅导员', '辅导员办公室', 'Adv@2026');
