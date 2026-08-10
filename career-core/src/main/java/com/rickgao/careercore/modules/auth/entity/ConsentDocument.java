package com.rickgao.careercore.modules.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 隐私授权文本文档(consent_document,版本化管理)。
 */
@Data
public class ConsentDocument {

    private String id;
    private String version;
    private String title;
    private String content;
    /** 状态:DRAFT/PUBLISHED */
    private String status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
