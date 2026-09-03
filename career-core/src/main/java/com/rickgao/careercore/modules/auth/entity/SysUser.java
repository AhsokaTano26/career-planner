package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体(sys_user)。
 */
@Data
public class SysUser {

    private String id;
    /** 学号(学生),教职工可为空 */
    private String studentNo;
    /** 登录名(学号或工号) */
    private String username;
    private String name;
    /** 密码摘要(BCrypt) */
    private String passwordHash;
    /** 角色:STUDENT/ADVISOR/ADMIN */
    private String role;
    /** 状态:ACTIVE/DISABLED */
    private String status;
    private String grade;
    private String majorCategory;
    private String className;
    private Boolean consentAgreed;
    /** 是否必须先修改初始密码后才能使用业务功能 */
    private Boolean passwordChangeRequired;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
}
