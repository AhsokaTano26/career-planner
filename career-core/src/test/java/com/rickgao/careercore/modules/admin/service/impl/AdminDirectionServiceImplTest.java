package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.DirectionRequest;
import com.rickgao.careercore.modules.admin.dto.DirectionStatusUpdate;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.admin.service.AdminDirectionService;
import com.rickgao.careercore.modules.admin.vo.AdminDirectionVO;
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

class AdminDirectionServiceImplTest {

    private final AdminDirectionMapper directionMapper = mock(AdminDirectionMapper.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminDirectionService service =
            new AdminDirectionServiceImpl(directionMapper, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listDirections_invalidPath_throwsValidation() {
        BizException ex = assertThrows(BizException.class,
                () -> service.listDirections("unknown", null, null, 1, 20, null));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createDirection_duplicate_throws409() {
        when(directionMapper.findById("employment_backend")).thenReturn(new CareerDirection());
        BizException ex = assertThrows(BizException.class,
                () -> service.createDirection("ADMIN1", "/directions", "k1", validRequest()));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void createDirection_missingTarget_throwsValidation() {
        DirectionRequest request = validRequest();
        request.setTarget(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.createDirection("ADMIN1", "/directions", "k1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createDirection_ok_createsDraft() {
        when(directionMapper.findById("employment_backend")).thenReturn(null);
        AdminDirectionVO result = service.createDirection(
                "ADMIN1", "/directions", "k1", validRequest());
        ArgumentCaptor<CareerDirection> captor = ArgumentCaptor.forClass(CareerDirection.class);
        verify(directionMapper).insert(captor.capture());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals("employment_backend", result.getId());
    }

    @Test
    void updateDirection_notFound_throws404() {
        when(directionMapper.findById("x")).thenReturn(null);
        DirectionRequest request = new DirectionRequest();
        request.setName("新方向");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateDirection("ADMIN1", "/directions/{directionId}", "k1", "x", request));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void setStatus_sameStatus_throws409() {
        CareerDirection direction = direction("employment_backend", "PUBLISHED", completeTarget());
        when(directionMapper.findById("employment_backend")).thenReturn(direction);
        DirectionStatusUpdate update = new DirectionStatusUpdate();
        update.setStatus("PUBLISHED");
        BizException ex = assertThrows(BizException.class,
                () -> service.setDirectionStatus("ADMIN1", "/status", "k1", "employment_backend", update));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void setStatus_publishIncomplete_throwsValidation() {
        CareerDirection direction = direction("employment_backend", "DRAFT", null);
        when(directionMapper.findById("employment_backend")).thenReturn(direction);
        DirectionStatusUpdate update = new DirectionStatusUpdate();
        update.setStatus("PUBLISHED");
        BizException ex = assertThrows(BizException.class,
                () -> service.setDirectionStatus("ADMIN1", "/status", "k1", "employment_backend", update));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void setStatus_publishOk() {
        CareerDirection direction = direction("employment_backend", "DRAFT", completeTarget());
        when(directionMapper.findById("employment_backend")).thenReturn(direction);
        DirectionStatusUpdate update = new DirectionStatusUpdate();
        update.setStatus("PUBLISHED");
        AdminDirectionVO result = service.setDirectionStatus(
                "ADMIN1", "/status", "k1", "employment_backend", update);
        verify(directionMapper).updateStatus("employment_backend", "PUBLISHED");
        assertEquals("PUBLISHED", result.getStatus());
    }

    @Test
    void listDirections_mapsPage() {
        AdminDirectionVO vo = new AdminDirectionVO();
        vo.setId("employment_backend");
        vo.setStatus("PUBLISHED");
        when(directionMapper.countDirections("employment", "PUBLISHED", null)).thenReturn(3L);
        when(directionMapper.selectDirectionPage("employment", "PUBLISHED", null, "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));
        PageResult<AdminDirectionVO> result = service.listDirections("employment", "PUBLISHED", null, 1, 20, null);
        assertEquals(3, result.getTotal());
    }

    private DirectionRequest validRequest() {
        DirectionRequest request = new DirectionRequest();
        request.setId("employment_backend");
        request.setName("后端开发工程师");
        request.setPath("employment");
        request.setIntro("面向服务端开发与运维的就业方向。");
        request.setTarget(completeTarget());
        request.setMinAbility(65.0);
        request.setMinAcademic(50.0);
        return request;
    }

    private DirectionRequest.Target completeTarget() {
        DirectionRequest.Target target = new DirectionRequest.Target();
        target.setInterest(70.0);
        target.setValues(70.0);
        target.setAbility(70.0);
        target.setAcademic(70.0);
        target.setTendency(70.0);
        target.setPractice(70.0);
        return target;
    }

    private CareerDirection direction(String id, String status, DirectionRequest.Target target) {
        CareerDirection direction = new CareerDirection();
        direction.setId(id);
        direction.setName("后端开发工程师");
        direction.setPath("employment");
        direction.setStatus(status);
        direction.setIntro(target == null ? null : "简介");
        direction.setTargetJson(target == null ? null : JsonUtil.toJson(target));
        return direction;
    }
}
