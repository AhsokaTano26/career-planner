package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.dto.AbilityTagRequest;
import com.rickgao.careercore.modules.admin.entity.AbilityTag;
import com.rickgao.careercore.modules.admin.mapper.AdminAbilityMapper;
import com.rickgao.careercore.modules.admin.service.AdminAbilityService;
import com.rickgao.careercore.modules.admin.vo.AbilityTagVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAbilityServiceImplTest {

    private final AdminAbilityMapper abilityMapper = mock(AdminAbilityMapper.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminAbilityService service =
            new AdminAbilityServiceImpl(abilityMapper, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listAbilities_mapsPage() {
        AbilityTagVO vo = new AbilityTagVO();
        vo.setId("programming_basic");
        vo.setName("编程基础");
        when(abilityMapper.countAbilities("能力", "编程")).thenReturn(1L);
        when(abilityMapper.selectAbilityPage("能力", "编程", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));

        PageResult<AbilityTagVO> result = service.listAbilities("能力", "编程", 1, 20, null);

        assertEquals(1, result.getTotal());
        assertEquals("programming_basic", result.getList().get(0).getId());
    }

    @Test
    void createAbility_duplicate_throws409() {
        when(abilityMapper.findById("programming_basic")).thenReturn(new AbilityTag());
        AbilityTagRequest request = new AbilityTagRequest();
        request.setId("programming_basic");
        request.setName("编程基础");
        BizException ex = assertThrows(BizException.class,
                () -> service.createAbility("ADMIN1", "/abilities", "k1", request));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void createAbility_missingId_throwsValidation() {
        AbilityTagRequest request = new AbilityTagRequest();
        request.setName("编程基础");
        BizException ex = assertThrows(BizException.class,
                () -> service.createAbility("ADMIN1", "/abilities", "k1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createAbility_ok_defaultActive() {
        when(abilityMapper.findById("programming_basic")).thenReturn(null);
        AbilityTagRequest request = new AbilityTagRequest();
        request.setId("programming_basic");
        request.setName("编程基础");
        request.setCategory("能力");

        AbilityTagVO result = service.createAbility("ADMIN1", "/abilities", "k1", request);

        ArgumentCaptor<AbilityTag> captor = ArgumentCaptor.forClass(AbilityTag.class);
        verify(abilityMapper).insert(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals("programming_basic", result.getId());
    }

    @Test
    void updateAbility_notFound_throws404() {
        when(abilityMapper.findById("x")).thenReturn(null);
        AbilityTagRequest request = new AbilityTagRequest();
        request.setName("新名字");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateAbility("ADMIN1", "/abilities/{tagId}", "k1", "x", request));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void updateAbility_emptyBody_throwsValidation() {
        when(abilityMapper.findById("programming_basic")).thenReturn(new AbilityTag());
        BizException ex = assertThrows(BizException.class,
                () -> service.updateAbility("ADMIN1", "/abilities/{tagId}", "k1", "programming_basic",
                        new AbilityTagRequest()));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }
}
