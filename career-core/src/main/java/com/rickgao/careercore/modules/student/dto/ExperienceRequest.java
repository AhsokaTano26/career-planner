package com.rickgao.careercore.modules.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/修改经历请求。
 */
@Data
public class ExperienceRequest {

    @NotBlank(message = "经历类别不能为空")
    @Size(max = 20, message = "经历类别长度不能超过 20")
    private String type;

    @NotBlank(message = "经历名称不能为空")
    @Size(max = 100, message = "经历名称长度不能超过 100")
    private String title;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "开始时间格式应为 YYYY-MM")
    private String startDate;

    @Pattern(regexp = "\\d{4}-\\d{2}", message = "结束时间格式应为 YYYY-MM")
    private String endDate;

    @Size(max = 2000, message = "经历描述长度不能超过 2000")
    private String description;

    /** 可选附件(multipart 上传的临时文件 ID;首版简化为直接存储) */
    @Size(max = 255, message = "附件长度不能超过 255")
    private String attachment;
}
