package com.rickgao.careercore.modules.student.entity;

import com.rickgao.careercore.modules.student.model.AbilitySelf;
import com.rickgao.careercore.modules.student.model.AcademicInfo;
import com.rickgao.careercore.modules.student.model.BasicInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生档案实体(student_profile)。
 * 嵌套结构存储于 JSON 列,由 MyBatis TypeHandler 双向映射。
 */
@Data
public class StudentProfile {

    private String id;
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
    private String developmentIntention;
    private List<String> constraints;
    private Integer completeness;
    private LocalDateTime updatedAt;
}
