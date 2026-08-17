package com.rickgao.careercore.common.constant;

/**
 * 公共常量:角色、状态、审计动作等。
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    // 角色
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_ADVISOR = "ADVISOR";
    public static final String ROLE_ADMIN = "ADMIN";

    // 用户状态
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_DISABLED = "DISABLED";

    // 隐私授权文档状态
    public static final String CONSENT_STATUS_PUBLISHED = "PUBLISHED";

    // 删除申请状态
    public static final String DELETION_STATUS_PENDING = "PENDING";

    // 审计动作
    public static final String AUDIT_REGISTER = "REGISTER";
    public static final String AUDIT_LOGIN = "LOGIN";
    public static final String AUDIT_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String AUDIT_LOGOUT = "LOGOUT";
    public static final String AUDIT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String AUDIT_PASSWORD_RESET = "RESET_PASSWORD";
    public static final String AUDIT_CONSENT = "PRIVACY_CONSENT";
    public static final String AUDIT_DELETION_REQUEST = "DELETION_REQUEST";

    // 令牌类型
    public static final String TOKEN_TYPE_BEARER = "Bearer";

    // 登录锁定阈值(连续失败次数 / 锁定分钟)
    public static final int LOGIN_MAX_FAILURES = 5;
    public static final long LOGIN_LOCK_MINUTES = 15;

    // 经历类别
    public static final String[] EXPERIENCE_TYPES = {"竞赛", "项目", "学生工作", "志愿服务"};
}
