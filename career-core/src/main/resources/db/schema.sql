-- ============================================================
-- career_core 建表脚本(MySQL 8, utf8mb4)
-- 依据《开发设计说明书》第 8 章数据库设计与 Apifox 接口文档
-- 幂等:应用启动时由 spring.sql.init 执行,使用 CREATE TABLE IF NOT EXISTS
-- 约定:snake_case 命名;主键为字符串业务 ID(如 S1001 / EXP-001),由 id_sequence 生成
-- ============================================================

-- 字符串主键 ID 生成序列
CREATE TABLE IF NOT EXISTS id_sequence (
    seq_name VARCHAR(64) NOT NULL COMMENT '序列名(对应表/业务)',
    next_val BIGINT      NOT NULL DEFAULT 1 COMMENT '下一个待分配值',
    PRIMARY KEY (seq_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字符串主键ID生成序列';

-- 用户表(学生 / 辅导员 / 管理员)
CREATE TABLE IF NOT EXISTS sys_user (
    id             VARCHAR(32)  NOT NULL COMMENT '用户ID,如 S1001',
    student_no     VARCHAR(32)  DEFAULT NULL COMMENT '学号(学生),教职工可为空',
    username       VARCHAR(64)  NOT NULL COMMENT '登录名(学号或工号)',
    name           VARCHAR(50)  NOT NULL COMMENT '姓名',
    password_hash  VARCHAR(100) NOT NULL COMMENT '密码摘要(BCrypt)',
    role           VARCHAR(20)  NOT NULL COMMENT '角色:STUDENT/ADVISOR/ADMIN',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/DISABLED',
    grade          VARCHAR(20)  DEFAULT NULL COMMENT '年级(学生),如 2026级',
    major_category VARCHAR(50)  DEFAULT NULL COMMENT '专业大类(学生),如 计算机类',
    class_name     VARCHAR(50)  DEFAULT NULL COMMENT '班级(学生),如 计科2601',
    consent_agreed TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已同意隐私授权',
    last_login_at  DATETIME     DEFAULT NULL COMMENT '最近登录时间',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 学号白名单
CREATE TABLE IF NOT EXISTS student_whitelist (
    id          VARCHAR(32) NOT NULL COMMENT '主键',
    student_no  VARCHAR(32) NOT NULL COMMENT '可注册学号',
    name        VARCHAR(50) DEFAULT NULL COMMENT '预填姓名',
    class_name  VARCHAR(50) DEFAULT NULL COMMENT '班级',
    verify_code VARCHAR(32) NOT NULL COMMENT '初始校验码(注册时校验,作为初始密码)',
    used        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已注册使用',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学号白名单';

-- 隐私授权文本文档(版本化管理)
CREATE TABLE IF NOT EXISTS consent_document (
    id           VARCHAR(32) NOT NULL,
    version      VARCHAR(20) NOT NULL COMMENT '版本号,如 v1.0',
    title        VARCHAR(100) DEFAULT NULL COMMENT '标题',
    content      TEXT COMMENT '授权文本',
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态:DRAFT/PUBLISHED',
    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐私授权文本文档';

-- 隐私授权记录
CREATE TABLE IF NOT EXISTS consent_record (
    id         VARCHAR(32) NOT NULL,
    user_id    VARCHAR(32) NOT NULL COMMENT '用户ID',
    version    VARCHAR(20) NOT NULL COMMENT '同意的版本号',
    agreed_at  DATETIME NOT NULL COMMENT '同意时间',
    ip         VARCHAR(64) DEFAULT NULL COMMENT '同意时的客户端IP',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐私授权记录';

-- 刷新令牌
CREATE TABLE IF NOT EXISTS refresh_token (
    id         VARCHAR(32) NOT NULL,
    user_id    VARCHAR(32) NOT NULL,
    token      VARCHAR(128) NOT NULL COMMENT '刷新令牌(随机串)',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    revoked    TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已作废(登出/刷新轮换)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌';

-- 访问令牌黑名单(登出后使 JWT 失效)
CREATE TABLE IF NOT EXISTS token_blacklist (
    jti        VARCHAR(64) NOT NULL COMMENT 'JWT 的 jti',
    user_id    VARCHAR(32) NOT NULL,
    expires_at DATETIME NOT NULL COMMENT '令牌过期时间(用于清理)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问令牌黑名单';

-- 学生档案(嵌套结构使用 JSON 列;分步保存时合并)
CREATE TABLE IF NOT EXISTS student_profile (
    id                    VARCHAR(32) NOT NULL,
    user_id               VARCHAR(32) NOT NULL COMMENT '所属用户ID',
    name                  VARCHAR(50) DEFAULT NULL,
    class_name            VARCHAR(50) DEFAULT NULL,
    grade                 VARCHAR(20) DEFAULT NULL,
    major_category        VARCHAR(50) DEFAULT NULL,
    basic_json            JSON DEFAULT NULL COMMENT '基本信息 {gender,hometown,birthday,phone}',
    academic_json         JSON DEFAULT NULL COMMENT '学业基础 {math,english,programming,note}',
    interest_prefs_json   JSON DEFAULT NULL COMMENT '兴趣偏好标签数组',
    ability_self_json     JSON DEFAULT NULL COMMENT '能力自评 {programming,math,english,communication,organization}',
    values_json           JSON DEFAULT NULL COMMENT '职业价值观标签数组',
    development_intention VARCHAR(20) DEFAULT NULL COMMENT '发展意向:graduate/employment/overseas/undecided',
    constraints_json      JSON DEFAULT NULL COMMENT '现实约束数组(选填)',
    completeness          INT NOT NULL DEFAULT 0 COMMENT '资料完整度(0-100)',
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生档案';

-- 学生经历
CREATE TABLE IF NOT EXISTS student_experience (
    id             VARCHAR(32) NOT NULL COMMENT '经历ID,如 EXP-001',
    student_id     VARCHAR(32) NOT NULL COMMENT '所属学生用户ID',
    type           VARCHAR(20) NOT NULL COMMENT '类别:竞赛/项目/学生工作/志愿服务',
    title          VARCHAR(100) NOT NULL COMMENT '经历名称',
    start_date     VARCHAR(7) NOT NULL COMMENT '开始时间(YYYY-MM)',
    end_date       VARCHAR(7) DEFAULT NULL COMMENT '结束时间(YYYY-MM,选填)',
    description    TEXT COMMENT '经历描述',
    attachment_url VARCHAR(255) DEFAULT NULL COMMENT '附件URL(可选)',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除',
    PRIMARY KEY (id),
    KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生经历';

-- 删除本人信息申请
CREATE TABLE IF NOT EXISTS deletion_request (
    id           VARCHAR(32) NOT NULL,
    user_id      VARCHAR(32) NOT NULL,
    reason       VARCHAR(255) DEFAULT NULL COMMENT '申请原因(选填)',
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/PROCESSED/REJECTED',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME DEFAULT NULL,
    processed_by VARCHAR(32) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='删除本人信息申请';

-- 操作审计日志
CREATE TABLE IF NOT EXISTS operation_audit_log (
    id          VARCHAR(32) NOT NULL,
    user_id     VARCHAR(32) DEFAULT NULL COMMENT '操作者用户ID',
    action      VARCHAR(50) NOT NULL COMMENT '动作:LOGIN/LOGIN_FAIL/RESET_PASSWORD/DELETION_REQUEST 等',
    target_type VARCHAR(50) DEFAULT NULL COMMENT '目标对象类型',
    target_id   VARCHAR(64) DEFAULT NULL COMMENT '目标对象ID',
    detail      VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
    ip          VARCHAR(64) DEFAULT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';
