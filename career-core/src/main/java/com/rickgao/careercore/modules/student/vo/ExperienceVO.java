package com.rickgao.careercore.modules.student.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 经历 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceVO {

    private String id;
    private String type;
    private String title;
    private String startDate;
    private String endDate;
    private String description;
    private String attachmentUrl;
}
