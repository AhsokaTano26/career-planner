package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.dto.AdminUserUpdate;
import com.rickgao.careercore.modules.admin.mapper.AdminUserMapper;
import com.rickgao.careercore.modules.admin.service.AdminUserService;
import com.rickgao.careercore.modules.admin.vo.AdminUserVO;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceImplTest {

    private final AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminUserService service =
            new AdminUserServiceImpl(adminUserMapper, sysUserMapper, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<Void>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listUsers_passesFiltersAndPaginates() {
        AdminUserVO vo = new AdminUserVO();
        vo.setId("S1001");
        vo.setUsername("2026011301");
        vo.setName("李明");
        vo.setRole("STUDENT");
        vo.setStatus("ACTIVE");
        when(adminUserMapper.countUsers("李", "STUDENT", "ACTIVE")).thenReturn(1L);
        when(adminUserMapper.selectUserPage("李", "STUDENT", "ACTIVE", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));

        PageResult<AdminUserVO> result = service.listUsers("STUDENT", "ACTIVE", "李", 1, 20, null);

        assertEquals(1, result.getTotal());
        assertEquals("S1001", result.getList().get(0).getId());
    }

    @Test
    void listUsers_invalidRole_throwsValidation() {
        BizException ex = assertThrows(BizException.class,
                () -> service.listUsers("TEACHER", null, null, 1, 20, null));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void listUsers_lockedStatus_rejected() {
        BizException ex = assertThrows(BizException.class,
                () -> service.listUsers(null, "LOCKED", null, 1, 20, null));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void updateUser_notFound_throws404() {
        when(sysUserMapper.findById("S9999")).thenReturn(null);
        AdminUserUpdate dto = new AdminUserUpdate();
        dto.setStatus("DISABLED");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateUser("A1", "/users/{userId}", "k1", "S9999", dto));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void updateUser_disableSelf_throwsValidation() {
        SysUser self = user("S2001", "ADMIN");
        when(sysUserMapper.findById("S2001")).thenReturn(self);
        AdminUserUpdate dto = new AdminUserUpdate();
        dto.setStatus("DISABLED");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateUser("S2001", "/users/{userId}", "k1", "S2001", dto));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
        verify(adminUserMapper, never()).updateStatusAndClass(anyString(), anyString(), anyString());
    }

    @Test
    void updateUser_lockedStatus_rejected() {
        when(sysUserMapper.findById("S1001")).thenReturn(user("S1001", "STUDENT"));
        AdminUserUpdate dto = new AdminUserUpdate();
        dto.setStatus("LOCKED");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateUser("S2001", "/users/{userId}", "k1", "S1001", dto));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void updateUser_bothEmpty_throwsValidation() {
        when(sysUserMapper.findById("S1001")).thenReturn(user("S1001", "STUDENT"));
        BizException ex = assertThrows(BizException.class,
                () -> service.updateUser("S2001", "/users/{userId}", "k1", "S1001", new AdminUserUpdate()));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void updateUser_partialStatus_updates() {
        when(sysUserMapper.findById("S1001")).thenReturn(user("S1001", "STUDENT"));
        AdminUserUpdate dto = new AdminUserUpdate();
        dto.setStatus("DISABLED");
        service.updateUser("S2001", "/users/{userId}", "k1", "S1001", dto);
        verify(adminUserMapper).updateStatusAndClass("S1001", "DISABLED", null);
    }

    private SysUser user(String id, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
