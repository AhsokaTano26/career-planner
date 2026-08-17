package com.rickgao.careercore.modules.advisor.service;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvisorScopeServiceTest {

    private final AdvisorStudentRelationMapper relationMapper = mock(AdvisorStudentRelationMapper.class);
    private final AdvisorScopeService service = new AdvisorScopeService(relationMapper);

    @Test
    void assertAssigned_passesWhenRelationExists() {
        when(relationMapper.countByAdvisorAndStudent("A1001", "S1001")).thenReturn(1);
        assertDoesNotThrow(() -> service.assertAssigned("A1001", "S1001"));
    }

    @Test
    void assertAssigned_throwsForbiddenWhenRelationMissing() {
        when(relationMapper.countByAdvisorAndStudent("A1001", "S9999")).thenReturn(0);
        BizException ex = assertThrows(BizException.class, () -> service.assertAssigned("A1001", "S9999"));
        assertEquals(ResultCode.FORBIDDEN, ex.getResultCode());
    }
}
