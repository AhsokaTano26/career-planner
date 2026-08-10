package com.rickgao.careercore.common.idempotency;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private final IdempotencyRecordMapper mapper = mock(IdempotencyRecordMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService service = new IdempotencyService(mapper, idGenerator, JsonUtil.getMapper());

    @BeforeEach
    void setUp() {
        when(idGenerator.idempotencyId()).thenReturn("IDEM-0001");
    }

    @Test
    void execute_missingKey_throwsValidationError() {
        BizException ex = assertThrows(BizException.class,
                () -> service.execute("A1", "/guidance", null, String.class, ApiResponse::ok));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void execute_newRequest_runsActionAndMarksSuccess() {
        when(mapper.findByUserEndpointKey(anyString(), anyString(), anyString())).thenReturn(null);
        ApiResponse<String> result = service.execute("A1", "/guidance", "key-1", String.class,
                () -> ApiResponse.ok("ok"));
        assertEquals("ok", result.getData());
        verify(mapper).insert(any(IdempotencyRecord.class));
        verify(mapper).markSuccess(eqId("IDEM-0001"), eqCode("OK"), anyString());
    }

    @Test
    void execute_existingSuccess_replaysWithoutRunningAction() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId("IDEM-0001");
        record.setStatus(IdempotencyService.STATUS_SUCCESS);
        record.setResponseBody(JsonUtil.toJson(ApiResponse.ok("replayed")));
        when(mapper.findByUserEndpointKey(anyString(), anyString(), anyString())).thenReturn(record);

        AtomicInteger calls = new AtomicInteger();
        ApiResponse<String> result = service.execute("A1", "/guidance", "key-1", String.class,
                () -> {
                    calls.incrementAndGet();
                    return ApiResponse.ok("never");
                });
        assertEquals("replayed", result.getData());
        assertEquals(0, calls.get());
        verify(mapper, never()).insert(any(IdempotencyRecord.class));
    }

    @Test
    void execute_existingProcessing_throwsConflict() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId("IDEM-0001");
        record.setStatus(IdempotencyService.STATUS_PROCESSING);
        when(mapper.findByUserEndpointKey(anyString(), anyString(), anyString())).thenReturn(record);

        BizException ex = assertThrows(BizException.class,
                () -> service.execute("A1", "/guidance", "key-1", String.class, ApiResponse::ok));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void execute_actionFailure_deletesRecordAndRethrows() {
        when(mapper.findByUserEndpointKey(anyString(), anyString(), anyString())).thenReturn(null);
        IllegalStateException boom = new IllegalStateException("boom");
        assertThrows(IllegalStateException.class,
                () -> service.execute("A1", "/guidance", "key-1", String.class,
                        () -> {
                            throw boom;
                        }));
        verify(mapper).deleteById("IDEM-0001");
        verify(mapper, never()).markSuccess(anyString(), anyString(), anyString());
    }

    @Test
    void execute_duplicateInsert_readsExistingAndReplays() {
        when(mapper.findByUserEndpointKey(anyString(), anyString(), anyString()))
                .thenReturn(null)
                .thenReturn(successRecord("duplicated"));
        doThrow(new DuplicateKeyException("dup"))
                .when(mapper).insert(any(IdempotencyRecord.class));

        ApiResponse<String> result = service.execute("A1", "/guidance", "key-1", String.class,
                () -> ApiResponse.ok("never"));
        assertEquals("duplicated", result.getData());
    }

    private IdempotencyRecord successRecord(String data) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId("IDEM-0001");
        record.setStatus(IdempotencyService.STATUS_SUCCESS);
        record.setResponseBody(JsonUtil.toJson(ApiResponse.ok(data)));
        return record;
    }

    private String eqId(String id) {
        return org.mockito.ArgumentMatchers.eq(id);
    }

    private String eqCode(String code) {
        return org.mockito.ArgumentMatchers.eq(code);
    }
}
