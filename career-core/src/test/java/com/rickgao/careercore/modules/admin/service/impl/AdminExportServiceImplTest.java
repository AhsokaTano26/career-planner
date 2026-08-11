package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.ExportRequest;
import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.service.AdminExportService;
import com.rickgao.careercore.modules.admin.service.ExportFileGenerator;
import com.rickgao.careercore.modules.admin.vo.ExportJobVO;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminExportServiceImplTest {

    private final AdminExportMapper exportMapper = mock(AdminExportMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final ExportFileGenerator exportFileGenerator = mock(ExportFileGenerator.class);
    private final AdminExportService service = new AdminExportServiceImpl(
            exportMapper, sysUserMapper, idGenerator, idempotencyService, exportFileGenerator);

    @BeforeEach
    void setUp() {
        when(idGenerator.exportJobId()).thenReturn("EX-100");
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void createExport_invalidType_throwsValidation() {
        ExportRequest request = new ExportRequest();
        request.setType("UNKNOWN");
        BizException ex = assertThrows(BizException.class,
                () -> service.createExport("ADMIN1", "/exports", "k1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createExport_ok_triggersAsyncGeneration() {
        SysUser admin = new SysUser();
        admin.setId("S2001");
        admin.setName("系统管理员");
        when(sysUserMapper.findById("S2001")).thenReturn(admin);
        ExportRequest request = new ExportRequest();
        request.setType("WHITELIST");
        request.setScope("全部白名单");

        ExportJobVO vo = service.createExport("S2001", "/exports", "k1", request);

        ArgumentCaptor<ExportJob> captor = ArgumentCaptor.forClass(ExportJob.class);
        verify(exportMapper).insertExportJob(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals("WHITELIST", captor.getValue().getType());
        verify(exportFileGenerator).generate("EX-100");
        assertEquals("EX-100", vo.getId());
        assertEquals("系统管理员", vo.getOperator());
    }

    @Test
    void listExports_mapsPage() {
        ExportJobVO vo = new ExportJobVO();
        vo.setId("EX-100");
        when(exportMapper.countExportJobs()).thenReturn(2L);
        when(exportMapper.selectExportJobPage("created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));
        PageResult<ExportJobVO> result = service.listExports(1, 20, null);
        assertEquals(2, result.getTotal());
    }

    @Test
    void download_notFound_throws404() {
        when(exportMapper.findExportJobById("EX-999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.download("EX-999"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void download_notDone_throws404() {
        ExportJob job = new ExportJob();
        job.setId("EX-100");
        job.setStatus("PENDING");
        when(exportMapper.findExportJobById("EX-100")).thenReturn(job);
        BizException ex = assertThrows(BizException.class, () -> service.download("EX-100"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void download_expired_throws404() {
        ExportJob job = new ExportJob();
        job.setId("EX-100");
        job.setStatus("DONE");
        job.setFilePath("target/test-exports/x.csv");
        job.setCreatedAt(LocalDateTime.now().minusMinutes(11));
        when(exportMapper.findExportJobById("EX-100")).thenReturn(job);
        BizException ex = assertThrows(BizException.class, () -> service.download("EX-100"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }
}
