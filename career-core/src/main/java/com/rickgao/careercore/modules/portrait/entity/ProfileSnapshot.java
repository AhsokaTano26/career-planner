package com.rickgao.careercore.modules.portrait.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画像快照。
 */
@Data
public class ProfileSnapshot {

    private String id;
    private String studentId;
    private String sourceVersion;
    private String dimensionJson;
    private String summary;
    private String strengthsJson;
    private String exploreJson;
    private String feedbackJson;
    private Integer versionNo;
    private Integer completeness;
    private LocalDateTime createdAt;
}
