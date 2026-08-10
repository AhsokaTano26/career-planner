package com.rickgao.careercore.modules.student.dto;

import com.rickgao.careercore.modules.student.model.AbilitySelf;
import com.rickgao.careercore.modules.student.model.AcademicInfo;
import com.rickgao.careercore.modules.student.model.BasicInfo;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 分步保存学生资料请求(PATCH /api/v1/students/me)。
 * 未提交的字段不覆盖。
 */
@Data
public class StudentProfileUpdateDTO {

    private BasicInfo basic;
    private AcademicInfo academic;
    private List<String> interestPrefs;
    private AbilitySelf abilitySelf;
    private List<String> values;

    @Pattern(regexp = "graduate|employment|overseas|undecided", message = "发展意向取值不合法")
    private String developmentIntention;

    private List<String> constraints;
}
