package com.rickgao.careercore.modules.student.vo;

import com.rickgao.careercore.modules.student.model.AbilitySelf;
import com.rickgao.careercore.modules.student.model.AcademicInfo;
import com.rickgao.careercore.modules.student.model.BasicInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生档案 VO(StudentProfile)。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileVO {

    private String userId;
    private String name;
    private String className;
    private String grade;
    private String majorCategory;
    private BasicInfo basic;
    private AcademicInfo academic;
    private List<String> interestPrefs;
    private AbilitySelf abilitySelf;
    private List<String> values;
    private List<ExperienceVO> experiences;
    private String developmentIntention;
    private List<String> constraints;
    private Integer completeness;
    private LocalDateTime updatedAt;
}
