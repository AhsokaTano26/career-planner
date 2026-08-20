-- ============================================================
-- 【仅本地联调用】模拟 AI 解析结果(生产环境勿执行)
-- career-ai 未接入前,先用本脚本把上传的任务推进到 REVIEW_REQUIRED
-- 并生成种子条目,便于验收"校核/批量/发布"流程。
-- 用法:1) 先通过 POST /api/v1/admin/curricula/import 上传一份 PDF,
--        得到任务 ID(如 CJ-100);
--      2) 把下面的 CJ-100 替换为实际任务 ID 后执行本脚本。
-- ============================================================

UPDATE curriculum_import_job
SET status = 'REVIEW_REQUIRED', total_items = 3, parsed_items = 3, confidence = 86
WHERE id = 'CJ-100';

INSERT IGNORE INTO curriculum_import_item
    (id, job_id, course_code, course_name, semester, credits, hours, category, module,
     prerequisites_json, ability_tags_json, confidence, page_ref, status)
VALUES
    ('IT-100', 'CJ-100', 'CS101', '程序设计基础', '2026-2027-1', 4, 64, '专业基础', '必修',
     '["CS100"]', '["programming_basic"]', 92, '第 12 页', 'PENDING'),
    ('IT-101', 'CJ-100', 'CS201', '数据结构', '2026-2027-2', 4, 64, '专业核心', '必修',
     '["CS101"]', '["programming_basic","algorithm"]', 88, '第 30 页', 'PENDING'),
    ('IT-102', 'CJ-100', 'CS202', '数据结构(重修版)', '2026-2027-2', 4, 64, '专业核心', '必修',
     '["CS101"]', '["algorithm"]', 61, '第 31 页', 'PENDING');
