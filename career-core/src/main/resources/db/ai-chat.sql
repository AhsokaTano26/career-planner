-- ============================================================
-- AI 对话与反馈持久化（2026-09 Phase 1）。
-- 演示精简点：原 AiService 用 ConcurrentHashMap 存进程内存，重启即清空；
-- 此处落库实现数据持久、跨设备可同步、支持分页历史。
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id VARCHAR(32) PRIMARY KEY,
  session_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(32) NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'user / assistant',
  content TEXT NOT NULL,
  needs_human_support TINYINT(1) NOT NULL DEFAULT 0,
  support_reason VARCHAR(500) NOT NULL DEFAULT '',
  message_group VARCHAR(64) NOT NULL DEFAULT '' COMMENT '同一次对话共享 message_group（即原始 messageId）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ai_chat_message_user_created (user_id, created_at),
  KEY idx_ai_chat_message_session (session_id, created_at),
  KEY idx_ai_chat_message_group (message_group)
);

CREATE TABLE IF NOT EXISTS ai_chat_feedback (
  id VARCHAR(32) PRIMARY KEY,
  message_group VARCHAR(64) NOT NULL COMMENT '对应 ai_chat_message.message_group（即原始 messageId）',
  user_id VARCHAR(32) NOT NULL,
  feedback_type VARCHAR(20) NOT NULL COMMENT 'HELPFUL / NEUTRAL / MISMATCH / NOT_INTERESTED',
  comment VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_message_group (message_group),
  KEY idx_ai_chat_feedback_user_created (user_id, created_at)
);
