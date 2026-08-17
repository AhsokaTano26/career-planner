package com.rickgao.careercore.modules.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 单条校核课程请求体。对齐 openapi ImportItemUpdate。 */
@Data
public class ImportItemUpdate {

    @Size(max = 64, message = "课程代码不能超过 64 位")
    private String courseCode;

    @Size(max = 200, message = "课程名称不能超过 200 字")
    private String courseName;

    @Size(max = 50, message = "开课学期不能超过 50 字")
    private String semester;

    private Double credits;
    private Double hours;

    @Size(max = 50, message = "课程类别不能超过 50 字")
    private String category;

    @Size(max = 50, message = "课程模块不能超过 50 字")
    private String module;

    private List<String> prerequisites;
    private List<String> abilityTags;

    /** APPROVED / REJECTED(可选,仅传时变更审核状态) */
    private String status;
}
