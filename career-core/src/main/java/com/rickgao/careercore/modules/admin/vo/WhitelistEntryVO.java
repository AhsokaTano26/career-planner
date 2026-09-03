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
    private Boolean used;
    private LocalDateTime createdAt;
    /** 仅在服务端自动生成时返回一次；列表查询永不返回。 */
    private String generatedInitialPassword;
}
