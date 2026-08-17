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
    private String verifyCode;
    private Boolean used;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
