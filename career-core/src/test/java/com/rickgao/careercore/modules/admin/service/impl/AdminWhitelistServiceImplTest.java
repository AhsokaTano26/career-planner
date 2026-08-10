package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.WhitelistCreate;
import com.rickgao.careercore.modules.admin.mapper.AdminWhitelistMapper;
import com.rickgao.careercore.modules.admin.service.AdminWhitelistService;
import com.rickgao.careercore.modules.admin.vo.WhitelistEntryVO;
import com.rickgao.careercore.modules.admin.vo.WhitelistImportResultVO;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import com.rickgao.careercore.modules.auth.mapper.StudentWhitelistMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWhitelistServiceImplTest {

    private final AdminWhitelistMapper adminWhitelistMapper = mock(AdminWhitelistMapper.class);
    private final StudentWhitelistMapper studentWhitelistMapper = mock(StudentWhitelistMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminWhitelistService service = new AdminWhitelistServiceImpl(
            adminWhitelistMapper, studentWhitelistMapper, idGenerator, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idGenerator.whitelistId()).thenReturn("WL-100");
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listWhitelist_mapsPage() {
        WhitelistEntryVO vo = new WhitelistEntryVO();
        vo.setId("WL001");
        vo.setStudentNo("2026011301");
        when(adminWhitelistMapper.countWhitelist(false, "2026")).thenReturn(3L);
        when(adminWhitelistMapper.selectWhitelistPage(false, "2026", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));

        PageResult<WhitelistEntryVO> result = service.listWhitelist(false, "2026", 1, 20, null);

        assertEquals(3, result.getTotal());
        assertEquals("WL001", result.getList().get(0).getId());
    }

    @Test
    void createWhitelist_duplicate_throws409() {
        when(studentWhitelistMapper.findByStudentNo("2026011301"))
                .thenReturn(new StudentWhitelist());
        WhitelistCreate dto = new WhitelistCreate();
        dto.setStudentNo("2026011301");
        BizException ex = assertThrows(BizException.class,
                () -> service.createWhitelist("ADMIN1", "/whitelist", "k1", dto));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void createWhitelist_generatesVerifyCodeWhenBlank() {
        when(studentWhitelistMapper.findByStudentNo("2026110001")).thenReturn(null);
        WhitelistCreate dto = new WhitelistCreate();
        dto.setStudentNo("2026110001");
        dto.setClassName("计科2601");

        WhitelistEntryVO result = service.createWhitelist("ADMIN1", "/whitelist", "k1", dto);

        ArgumentCaptor<StudentWhitelist> captor = ArgumentCaptor.forClass(StudentWhitelist.class);
        verify(adminWhitelistMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getVerifyCode());
        assertTrue(captor.getValue().getVerifyCode().matches("\\d{6}"));
        assertEquals("WL-100", result.getId());
    }

    @Test
    void importWhitelist_overLimit_throwsValidation() {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 701; i++) {
            csv.append("2026").append(String.format("%06d", i)).append(",计科2601,code\n");
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "w.csv", "text/csv", csv.toString().getBytes(StandardCharsets.UTF_8));

        BizException ex = assertThrows(BizException.class,
                () -> service.importWhitelist("ADMIN1", "/whitelist/import", "k1", file));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void importWhitelist_duplicateInFile_failsThatRow() {
        String csv = "2026110001,计科2601,aaa111\n2026110001,软工2601,bbb222\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "w.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        WhitelistImportResultVO result = service.importWhitelist("ADMIN1", "/whitelist/import", "k1", file);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(2, result.getFailures().get(0).getRow());
        assertEquals("重复学号", result.getFailures().get(0).getReason());
    }

    @Test
    void importWhitelist_existingInDb_failsThatRow() {
        when(studentWhitelistMapper.findByStudentNo("2026011301")).thenReturn(new StudentWhitelist());
        String csv = "2026011301,计科2601,aaa111\n2026110002,软工2601,\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "w.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        WhitelistImportResultVO result = service.importWhitelist("ADMIN1", "/whitelist/import", "k1", file);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(1, result.getFailures().get(0).getRow());
        ArgumentCaptor<StudentWhitelist> captor = ArgumentCaptor.forClass(StudentWhitelist.class);
        verify(adminWhitelistMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getVerifyCode());
    }

    @Test
    void deleteWhitelist_notFound_throws404() {
        when(adminWhitelistMapper.findById("WL-999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.deleteWhitelist("ADMIN1", "/whitelist/{whitelistId}", "k1", "WL-999"));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void deleteWhitelist_used_throws409() {
        StudentWhitelist entry = new StudentWhitelist();
        entry.setId("WL001");
        entry.setUsed(true);
        when(adminWhitelistMapper.findById("WL001")).thenReturn(entry);
        BizException ex = assertThrows(BizException.class,
                () -> service.deleteWhitelist("ADMIN1", "/whitelist/{whitelistId}", "k1", "WL001"));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
        verify(adminWhitelistMapper, never()).deleteById(anyString());
    }

    @Test
    void deleteWhitelist_ok_deletes() {
        StudentWhitelist entry = new StudentWhitelist();
        entry.setId("WL-100");
        entry.setUsed(false);
        when(adminWhitelistMapper.findById("WL-100")).thenReturn(entry);
        service.deleteWhitelist("ADMIN1", "/whitelist/{whitelistId}", "k1", "WL-100");
        verify(adminWhitelistMapper).deleteById("WL-100");
    }
}
