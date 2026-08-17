package com.rickgao.careercore.modules.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 隐私授权状态 VO(ConsentStatus)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentStatusVO {

    private Boolean agreed;
    /** 已同意的版本(未同意则为空) */
    private String version;
    private LocalDateTime agreedAt;
    /** 当前发布版本 */
    private String currentVersion;
    private LocalDateTime currentVersionPublishedAt;
    /** 当前版本文本摘要 */
    private String content;
}
