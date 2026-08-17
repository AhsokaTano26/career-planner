package com.rickgao.careercore.modules.student.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 申请删除本人信息请求。
 */
@Data
public class DeletionRequestDTO {

    @Size(max = 255, message = "原因长度不能超过 255")
    private String reason;
}
