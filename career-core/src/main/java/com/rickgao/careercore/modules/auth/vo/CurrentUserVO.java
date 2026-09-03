package com.rickgao.careercore.modules.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前用户信息 VO(CurrentUser)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserVO {

    private String id;
    private String username;
    private String name;
    private String role;
    private String studentNo;
    private String grade;
    private String majorCategory;
    private String className;
    private Boolean consentAgreed;
    private Boolean passwordChangeRequired;
}
