package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端-白名单条目。对齐 openapi WhitelistEntry。
 */
@Data
public class WhitelistEntryVO {

    private String id;
    private String studentNo;
    private String className;
    private String verifyCode;
    private Boolean used;
    private LocalDateTime createdAt;
}
