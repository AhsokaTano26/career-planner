"""SQLite 混合存储 DDL（直接迁移自 localrag-kit storage/schema.py，MIT 协议）。

三张表 + FTS5 虚拟表 + 同步触发器：
  - files：文档元数据 + SHA-256（增量变更检测）
  - chunks：文本 + embedding BLOB + 来源元数据
  - chunks_fts：FTS5 全文索引（porter unicode61 分词，支持 Unicode/中文）
"""

CREATE_TABLES_SQL = """
-- 追踪已索引的文档（增量变更检测用 SHA-256）
CREATE TABLE IF NOT EXISTS files (
    relative_path TEXT PRIMARY KEY,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL,
    file_size_bytes INTEGER NOT NULL,
    sha256 TEXT NOT NULL,
    line_count INTEGER NOT NULL,
    last_modified REAL NOT NULL,
    language TEXT,
    indexed_at REAL NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_files_sha256 ON files(sha256);

-- 存储块文本、坐标、来源与稠密向量嵌入
CREATE TABLE IF NOT EXISTS chunks (
    chunk_id TEXT PRIMARY KEY,
    relative_path TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL,
    start_line INTEGER NOT NULL,
    end_line INTEGER NOT NULL,
    char_count INTEGER NOT NULL,
    estimated_tokens INTEGER NOT NULL,
    section_title TEXT,
    text TEXT NOT NULL,
    embedding BLOB,
    extra_json TEXT,
    FOREIGN KEY(relative_path) REFERENCES files(relative_path) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chunks_relpath ON chunks(relative_path);

-- FTS5 全文索引（BM25 关键词匹配）
CREATE VIRTUAL TABLE IF NOT EXISTS chunks_fts USING fts5(
    chunk_id UNINDEXED,
    relative_path UNINDEXED,
    section_title,
    text,
    tokenize='porter unicode61'
);
"""

# 注：不使用触发器同步 FTS5——中文需要分字后索引，
# 由 SQLiteStore.upsert_chunks / delete_chunks_for_file 手动管理 FTS5 条目。
CREATE_TRIGGERS_SQL = ""
