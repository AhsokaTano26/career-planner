package com.rickgao.careercore.modules.advisor.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.advisor.dto.GuidanceCommentRequest;
import com.rickgao.careercore.modules.advisor.entity.AdvisorComment;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorCommentMapper;
import com.rickgao.careercore.modules.advisor.service.AdvisorGuidanceService;
import com.rickgao.careercore.modules.advisor.service.AdvisorScopeService;
import com.rickgao.careercore.modules.advisor.vo.GuidanceCommentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdvisorGuidanceServiceImplTest {

    private final AdvisorScopeService scopeService = mock(AdvisorScopeService.class);
    private final AdvisorCommentMapper commentMapper = mock(AdvisorCommentMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdvisorGuidanceService service =
            new AdvisorGuidanceServiceImpl(scopeService, commentMapper, idGenerator, idempotencyService);

    @BeforeEach
    void setUp() {
        when(idGenerator.advisorCommentId()).thenReturn("GC-100");
        // 幂等服务桩:直接执行回调,幂等内部逻辑由 IdempotencyServiceTest 覆盖
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<GuidanceCommentVO>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void listGuidance_returnsMappedVosInOrder() {
        AdvisorComment c1 = comment("GC-001", "第一条", "COMMENT");
        AdvisorComment c2 = comment("GC-002", "第二条", "SUGGEST_RETEST");
        when(commentMapper.findByStudentId("S1001")).thenReturn(List.of(c1, c2));

        List<GuidanceCommentVO> result = service.listGuidance("A1001", "S1001");

        verify(scopeService).assertAssigned("A1001", "S1001");
        assertEquals(2, result.size());
        assertEquals("GC-001", result.get(0).getId());
        assertEquals("SUGGEST_RETEST", result.get(1).getAdviceType());
    }

    @Test
    void writeGuidance_comment_savesTrimmedContent() {
        GuidanceCommentRequest request = new GuidanceCommentRequest();
        request.setContent("  建议 10 月聚焦数据结构主线。  ");
        request.setAdviceType("COMMENT");

        GuidanceCommentVO result = service.writeGuidance("A1001", "S1001", "/guidance", "key-1", request);

        ArgumentCaptor<AdvisorComment> captor = ArgumentCaptor.forClass(AdvisorComment.class);
        verify(commentMapper).insert(captor.capture());
        AdvisorComment saved = captor.getValue();
        assertEquals("建议 10 月聚焦数据结构主线。", saved.getContent());
        assertEquals("COMMENT", saved.getAdviceType());
        assertEquals("A1001", saved.getAdvisorId());
        assertEquals("S1001", saved.getStudentId());
        assertNull(saved.getSuggestedTask());
        assertEquals("GC-100", result.getId());
    }

    @Test
    void writeGuidance_suggestTaskWithoutTask_throwsValidation() {
        GuidanceCommentRequest request = new GuidanceCommentRequest();
        request.setContent("建议任务");
        request.setAdviceType("SUGGEST_TASK");

        BizException ex = assertThrows(BizException.class,
                () -> service.writeGuidance("A1001", "S1001", "/guidance", "key-1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
        verify(commentMapper, never()).insert(any(AdvisorComment.class));
    }

    @Test
    void writeGuidance_suggestRetestWithoutReason_throwsValidation() {
        GuidanceCommentRequest request = new GuidanceCommentRequest();
        request.setContent("建议复测");
        request.setAdviceType("SUGGEST_RETEST");

        BizException ex = assertThrows(BizException.class,
                () -> service.writeGuidance("A1001", "S1001", "/guidance", "key-1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void writeGuidance_invalidAdviceType_throwsValidation() {
        GuidanceCommentRequest request = new GuidanceCommentRequest();
        request.setContent("x");
        request.setAdviceType("UNKNOWN");

        BizException ex = assertThrows(BizException.class,
                () -> service.writeGuidance("A1001", "S1001", "/guidance", "key-1", request));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    private AdvisorComment comment(String id, String content, String type) {
        AdvisorComment comment = new AdvisorComment();
        comment.setId(id);
        comment.setStudentId("S1001");
        comment.setAdvisorId("A1001");
        comment.setContent(content);
        comment.setAdviceType(type);
        comment.setCreatedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        return comment;
    }
}
