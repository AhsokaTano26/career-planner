package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端-用户列表项。对齐 openapi AdminUser。
 */
@Data
public class AdminUserVO {

    private String id;
    private String username;
    private String name;
    private String role;
    private String className;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
