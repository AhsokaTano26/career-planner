package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.WeightConfigRequest;
import com.rickgao.careercore.modules.admin.entity.RecommendationWeight;
import com.rickgao.careercore.modules.admin.mapper.AdminWeightMapper;
import com.rickgao.careercore.modules.admin.service.AdminWeightService;
import com.rickgao.careercore.modules.admin.vo.WeightConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWeightServiceImplTest {

    private final AdminWeightMapper weightMapper = mock(AdminWeightMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminWeightService service =
            new AdminWeightServiceImpl(weightMapper, idGenerator, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idGenerator.weightId()).thenReturn("WGT-100");
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void getWeights_returnsPublished() {
        RecommendationWeight weight = new RecommendationWeight();
        weight.setVersion("R1.0");
        weight.setWeightsJson("{\"interest\":0.2,\"values\":0.15,\"ability\":0.25,\"academic\":0.15,\"tendency\":0.2,\"practice\":0.05}");
        weight.setMinConfidence(BigDecimal.ZERO);
        weight.setTopN(5);
        weight.setStatus("PUBLISHED");
        when(weightMapper.findLatestPublished()).thenReturn(weight);

        WeightConfigVO vo = service.getWeights();

        assertEquals("R1.0", vo.getVersion());
        assertEquals(0.25, vo.getWeights().getAbility());
        assertEquals("PUBLISHED", vo.getStatus());
    }

    @Test
    void getWeights_none_returnsNull() {
        when(weightMapper.findLatestPublished()).thenReturn(null);
        assertNull(service.getWeights());
    }

    @Test
    void createWeights_sumNotOne_throwsValidation() {
        WeightConfigRequest request = validRequest();
        request.getWeights().setInterest(0.5);
        BizException ex = assertThrows(BizException.class,
                () -> service.createWeights("ADMIN1", "/weights", "k1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createWeights_topNOutOfRange_throwsValidation() {
        WeightConfigRequest request = validRequest();
        request.setTopN(10);
        BizException ex = assertThrows(BizException.class,
                () -> service.createWeights("ADMIN1", "/weights", "k1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void createWeights_duplicateVersion_throws409() {
        when(weightMapper.findByVersion("R2.0")).thenReturn(new RecommendationWeight());
        BizException ex = assertThrows(BizException.class,
                () -> service.createWeights("ADMIN1", "/weights", "k1", validRequest()));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void createWeights_ok_createsDraft() {
        when(weightMapper.findByVersion("R2.0")).thenReturn(null);
        WeightConfigVO result = service.createWeights("ADMIN1", "/weights", "k1", validRequest());
        ArgumentCaptor<RecommendationWeight> captor = ArgumentCaptor.forClass(RecommendationWeight.class);
        verify(weightMapper).insert(captor.capture());
        assertEquals("DRAFT", captor.getValue().getStatus());
        assertEquals("WGT-100", captor.getValue().getId());
        assertEquals("R2.0", result.getVersion());
    }

    private WeightConfigRequest validRequest() {
        WeightConfigRequest request = new WeightConfigRequest();
        request.setVersion("R2.0");
        WeightConfigRequest.Weights weights = new WeightConfigRequest.Weights();
        weights.setInterest(0.2);
        weights.setValues(0.15);
        weights.setAbility(0.25);
        weights.setAcademic(0.15);
        weights.setTendency(0.2);
        weights.setPractice(0.05);
        request.setWeights(weights);
        request.setMinConfidence(0.0);
        request.setTopN(5);
        return request;
    }
}
