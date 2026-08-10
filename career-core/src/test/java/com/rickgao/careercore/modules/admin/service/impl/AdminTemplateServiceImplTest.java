package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.dto.TaskTemplateRequest;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.entity.TaskTemplate;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.admin.mapper.AdminTemplateMapper;
import com.rickgao.careercore.modules.admin.service.AdminTemplateService;
import com.rickgao.careercore.modules.admin.vo.TaskTemplateVO;
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

class AdminTemplateServiceImplTest {

    private final AdminTemplateMapper templateMapper = mock(AdminTemplateMapper.class);
    private final AdminDirectionMapper directionMapper = mock(AdminDirectionMapper.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminTemplateService service =
            new AdminTemplateServiceImpl(templateMapper, directionMapper, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void createTemplate_directionMissing_throwsValidation() {
        when(directionMapper.findById("employment_backend")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.createTemplate("ADMIN1", "/templates", "k1", validRequest()));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createTemplate_duplicate_throws409() {
        when(directionMapper.findById("employment_backend")).thenReturn(new CareerDirection());
        when(templateMapper.findById("TPL-backend")).thenReturn(new TaskTemplate());
        BizException ex = assertThrows(BizException.class,
                () -> service.createTemplate("ADMIN1", "/templates", "k1", validRequest()));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void createTemplate_ok_defaultActive() {
        when(directionMapper.findById("employment_backend")).thenReturn(new CareerDirection());
        when(templateMapper.findById("TPL-backend")).thenReturn(null);
        TaskTemplateVO result = service.createTemplate("ADMIN1", "/templates", "k1", validRequest());
        ArgumentCaptor<TaskTemplate> captor = ArgumentCaptor.forClass(TaskTemplate.class);
        verify(templateMapper).insert(captor.capture());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals("TPL-backend", result.getId());
    }

    @Test
    void updateTemplate_notFound_throws404() {
        when(templateMapper.findById("x")).thenReturn(null);
        TaskTemplateRequest request = new TaskTemplateRequest();
        request.setName("新模板");
        BizException ex = assertThrows(BizException.class,
                () -> service.updateTemplate("ADMIN1", "/templates/{templateId}", "k1", "x", request));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void listTemplates_mapsPage() {
        TaskTemplateVO vo = new TaskTemplateVO();
        vo.setId("TPL-backend");
        when(templateMapper.countTemplates("employment_backend")).thenReturn(2L);
        when(templateMapper.selectTemplatePage("employment_backend", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));
        assertEquals(2, service.listTemplates("employment_backend", 1, 20, null).getTotal());
    }

    private TaskTemplateRequest validRequest() {
        TaskTemplateRequest request = new TaskTemplateRequest();
        request.setId("TPL-backend");
        request.setDirectionId("employment_backend");
        request.setName("后端开发方向任务模板");
        TaskTemplateRequest.SemesterGoal goal = new TaskTemplateRequest.SemesterGoal();
        goal.setTitle("掌握 Java 基础");
        goal.setAbilityTag("programming_basic");
        request.setSemesterGoals(List.of(goal));
        return request;
    }
}
