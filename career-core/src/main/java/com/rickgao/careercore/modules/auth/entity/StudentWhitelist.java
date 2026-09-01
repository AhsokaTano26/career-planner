package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学号白名单实体(student_whitelist)。
 */
@Data
public class StudentWhitelist {

    private String id;
    private String studentNo;
    private String name;
    private String className;
    /** 初始密码(注册时校验,注册后作为用户初始密码) */
    private String initialPassword;
    private Boolean used;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
