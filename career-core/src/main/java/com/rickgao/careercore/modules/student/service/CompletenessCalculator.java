package com.rickgao.careercore.modules.student.service;

import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.model.AbilitySelf;
import com.rickgao.careercore.modules.student.model.AcademicInfo;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO.Dimension;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO.MissingField;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资料完整度计算器。
 * 必填字段规则依据《需求规格说明书》6.3 / 附录A 定义:
 * 基本信息、学业基础(数学/英语/编程自评与备注)、兴趣偏好、能力自评、职业价值观、发展意向为必填;
 * 现实约束/经历为选填。如需调整字段规则,仅需修改本类。
 */
@Component
public class CompletenessCalculator {

    /** 必填字段 key -> 展示名 */
    private static final Map<String, String> REQUIRED_FIELDS = new LinkedHashMap<>();

    static {
        REQUIRED_FIELDS.put("basic.gender", "性别");
        REQUIRED_FIELDS.put("basic.birthday", "出生日期");
        REQUIRED_FIELDS.put("academic.math", "数学基础");
        REQUIRED_FIELDS.put("academic.english", "英语基础");
        REQUIRED_FIELDS.put("academic.programming", "编程基础");
        REQUIRED_FIELDS.put("academic.note", "学业备注");
        REQUIRED_FIELDS.put("interestPrefs", "兴趣偏好");
        REQUIRED_FIELDS.put("abilitySelf.programming", "编程能力");
        REQUIRED_FIELDS.put("abilitySelf.math", "数学能力");
        REQUIRED_FIELDS.put("abilitySelf.english", "英语能力");
        REQUIRED_FIELDS.put("abilitySelf.communication", "沟通表达");
        REQUIRED_FIELDS.put("abilitySelf.organization", "组织执行");
        REQUIRED_FIELDS.put("values", "职业价值观");
        REQUIRED_FIELDS.put("developmentIntention", "发展意向");
    }

    public CompletenessDetailVO calculate(StudentProfile profile) {
        List<MissingField> missing = new ArrayList<>();
        int total = REQUIRED_FIELDS.size();
        int filled = 0;
        for (Map.Entry<String, String> entry : REQUIRED_FIELDS.entrySet()) {
            if (isFilled(profile, entry.getKey())) {
                filled++;
            } else {
                missing.add(MissingField.builder().key(entry.getKey()).name(entry.getValue()).build());
            }
        }
        int score = total == 0 ? 0 : (int) Math.round(filled * 100.0 / total);

        List<Dimension> dimensions = List.of(
                Dimension.builder().key("interest").name("兴趣").filled(!isEmpty(profile.getInterestPrefs())).required(true).build(),
                Dimension.builder().key("values").name("职业价值观").filled(!isEmpty(profile.getValues())).required(true).build(),
                Dimension.builder().key("ability").name("能力").filled(abilityFilled(profile)).required(true).build(),
                Dimension.builder().key("academic").name("学业基础").filled(academicFilled(profile)).required(true).build(),
                Dimension.builder().key("tendency").name("发展意向").filled(StringUtils.hasText(profile.getDevelopmentIntention())).required(true).build(),
                Dimension.builder().key("practice").name("实践经历").filled(!isEmpty(profile.getConstraints())).required(false).build());

        return CompletenessDetailVO.builder()
                .score(score)
                .total(total)
                .filled(filled)
                .missing(missing)
                .dimensions(dimensions)
                .build();
    }

    private boolean isFilled(StudentProfile p, String key) {
        return switch (key) {
            case "basic.gender" -> p.getBasic() != null && StringUtils.hasText(p.getBasic().getGender());
            case "basic.birthday" -> p.getBasic() != null && StringUtils.hasText(p.getBasic().getBirthday());
            case "academic.math" -> p.getAcademic() != null && p.getAcademic().getMath() != null;
            case "academic.english" -> p.getAcademic() != null && p.getAcademic().getEnglish() != null;
            case "academic.programming" -> p.getAcademic() != null && p.getAcademic().getProgramming() != null;
            case "academic.note" -> p.getAcademic() != null && StringUtils.hasText(p.getAcademic().getNote());
            case "interestPrefs" -> !isEmpty(p.getInterestPrefs());
            case "abilitySelf.programming" -> p.getAbilitySelf() != null && p.getAbilitySelf().getProgramming() != null;
            case "abilitySelf.math" -> p.getAbilitySelf() != null && p.getAbilitySelf().getMath() != null;
            case "abilitySelf.english" -> p.getAbilitySelf() != null && p.getAbilitySelf().getEnglish() != null;
            case "abilitySelf.communication" -> p.getAbilitySelf() != null && p.getAbilitySelf().getCommunication() != null;
            case "abilitySelf.organization" -> p.getAbilitySelf() != null && p.getAbilitySelf().getOrganization() != null;
            case "values" -> !isEmpty(p.getValues());
            case "developmentIntention" -> StringUtils.hasText(p.getDevelopmentIntention());
            default -> false;
        };
    }

    private boolean abilityFilled(StudentProfile p) {
        AbilitySelf a = p.getAbilitySelf();
        return a != null && a.getProgramming() != null && a.getMath() != null && a.getEnglish() != null
                && a.getCommunication() != null && a.getOrganization() != null;
    }

    private boolean academicFilled(StudentProfile p) {
        AcademicInfo a = p.getAcademic();
        return a != null && a.getMath() != null && a.getEnglish() != null && a.getProgramming() != null
                && StringUtils.hasText(a.getNote());
    }

    private boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
