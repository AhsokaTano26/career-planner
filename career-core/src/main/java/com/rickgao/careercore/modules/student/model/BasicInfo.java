package com.rickgao.careercore.modules.student.model;

import lombok.Data;

/**
 * 基本信息(请求/响应/JSON 列共用)。
 */
@Data
public class BasicInfo {

    private String gender;
    /** 籍贯(选填) */
    private String hometown;
    /** 出生日期,如 2008-05-14 */
    private String birthday;
    /** 手机号(脱敏展示) */
    private String phone;
}
