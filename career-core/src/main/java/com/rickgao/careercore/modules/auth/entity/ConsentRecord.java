package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐私授权记录(consent_record)。
 */
@Data
public class ConsentRecord {

    private String id;
    private String userId;
    /** 同意的版本号 */
    private String version;
    private LocalDateTime agreedAt;
    private String ip;
    private LocalDateTime createdAt;
}
