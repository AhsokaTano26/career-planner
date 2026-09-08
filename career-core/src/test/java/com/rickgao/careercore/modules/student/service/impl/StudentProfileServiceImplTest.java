package com.rickgao.careercore.modules.student.service.impl;

import com.rickgao.careercore.common.audit.AuditLogWriter;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.MaskUtil;
import com.rickgao.careercore.modules.student.dto.StudentProfileUpdateDTO;
import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.mapper.DeletionRequestMapper;
import com.rickgao.careercore.modules.student.mapper.StudentExperienceMapper;
import com.rickgao.careercore.modules.student.mapper.StudentProfileMapper;
import com.rickgao.careercore.modules.student.model.BasicInfo;
import com.rickgao.careercore.modules.student.service.CompletenessCalculator;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO;
import com.rickgao.careercore.modules.student.vo.StudentProfileVO;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentProfileServiceImplTest {

    private final StudentProfileMapper studentProfileMapper = mock(StudentProfileMapper.class);
    private final StudentExperienceMapper studentExperienceMapper = mock(StudentExperienceMapper.class);
    private final DeletionRequestMapper deletionRequestMapper = mock(DeletionRequestMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final CompletenessCalculator completenessCalculator = mock(CompletenessCalculator.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final AuditLogWriter auditLogWriter = mock(AuditLogWriter.class);

    private final StudentProfileServiceImpl service = new StudentProfileServiceImpl(
            studentProfileMapper, studentExperienceMapper, deletionRequestMapper,
            sysUserMapper, completenessCalculator, idGenerator, auditLogWriter);

    private static StudentProfile profileWithPhone(String phone) {
        StudentProfile profile = new StudentProfile();
        profile.setUserId("S1001");
        profile.setName("张三");
        BasicInfo basic = new BasicInfo();
        basic.setPhone(phone);
        profile.setBasic(basic);
        return profile;
    }

    @Test
    void getMyProfile_手机号脱敏展示() {
        when(studentProfileMapper.findByUserId("S1001")).thenReturn(profileWithPhone("13812346721"));
        when(studentExperienceMapper.findAllByStudentId("S1001")).thenReturn(List.of());
        CompletenessDetailVO completeness = mock(CompletenessDetailVO.class);
        when(completeness.getScore()).thenReturn(80);
        when(completenessCalculator.calculate(any())).thenReturn(completeness);

        StudentProfileVO vo = service.getMyProfile("S1001");

        assertEquals(MaskUtil.maskPhone("13812346721"), vo.getBasic().getPhone());
        assertEquals("138****6721", vo.getBasic().getPhone());
    }

    @Test
    void updateMyProfile_返回脱敏手机号() {
        when(studentProfileMapper.findByUserId("S1001")).thenReturn(profileWithPhone("13812346721"));
        when(studentExperienceMapper.findAllByStudentId("S1001")).thenReturn(List.of());
        CompletenessDetailVO completeness = mock(CompletenessDetailVO.class);
        when(completeness.getScore()).thenReturn(80);
        when(completenessCalculator.calculate(any())).thenReturn(completeness);

        StudentProfileUpdateDTO dto = new StudentProfileUpdateDTO();
        BasicInfo basic = new BasicInfo();
        basic.setPhone("13812346721");
        dto.setBasic(basic);

        StudentProfileVO vo = service.updateMyProfile("S1001", dto);

        assertEquals("138****6721", vo.getBasic().getPhone());
    }

    @Test
    void getMyProfile_手机号为空不报错() {
        StudentProfile profile = profileWithPhone(null);
        when(studentProfileMapper.findByUserId("S1001")).thenReturn(profile);
        when(studentExperienceMapper.findAllByStudentId("S1001")).thenReturn(List.of());
        CompletenessDetailVO completeness = mock(CompletenessDetailVO.class);
        when(completeness.getScore()).thenReturn(0);
        when(completenessCalculator.calculate(any())).thenReturn(completeness);

        StudentProfileVO vo = service.getMyProfile("S1001");

        assertEquals(null, vo.getBasic().getPhone());
    }
}
