package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.service.AdminLogService;
import com.rickgao.careercore.modules.admin.vo.AiCallLogVO;
import com.rickgao.careercore.modules.admin.vo.OperationLogVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminLogServiceImplTest {

    private final AdminExportMapper mapper = mock(AdminExportMapper.class);
    private final AdminLogService service = new AdminLogServiceImpl(mapper);

    @Test
    void listAiLogs_invalidStatus_throwsValidation() {
        BizException ex = assertThrows(BizException.class,
                () -> service.listAiLogs(null, "BAD", null, null, 1, 20, null));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void listAiLogs_mapsPage() {
        AiCallLogVO vo = new AiCallLogVO();
        vo.setId("AI-001");
        when(mapper.countAiLogs("career_chat", "SUCCESS", null, null)).thenReturn(1L);
        when(mapper.selectAiLogPage("career_chat", "SUCCESS", null, null, "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));
        PageResult<AiCallLogVO> result = service.listAiLogs("career_chat", "SUCCESS", null, null, 1, 20, null);
        assertEquals(1, result.getTotal());
        assertEquals("AI-001", result.getList().get(0).getId());
    }

    @Test
    void listOperationLogs_setsLevelInfo() {
        OperationLogVO vo = new OperationLogVO();
        vo.setId("LOG-001");
        when(mapper.countOperationLogs(eq("导出"), any(), any(), any())).thenReturn(1L);
        when(mapper.selectOperationLogPage(eq("导出"), any(), any(), any(),
                eq("l.created_at"), eq("DESC"), eq(0), eq(20)))
                .thenReturn(List.of(vo));
        PageResult<OperationLogVO> result = service.listOperationLogs("导出", null, null, null, 1, 20, null);
        assertEquals("info", result.getList().get(0).getLevel());
    }
}
