package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求体。对齐 openapi AdminUserUpdate(仅 status / className)。
 */
@Data
public class AdminUserUpdate {

    /** ACTIVE / DISABLED(openapi 含 LOCKED,本批不实现) */
    private String status;

    @Size(max = 50, message = "班级不能超过 50 字")
    private String className;
}
