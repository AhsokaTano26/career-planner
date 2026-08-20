package com.rickgao.careercore.common.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 写接口幂等服务。
 * 约定(参考《具体实现细节_MVP_V1.0》2.1):以 user_id + endpoint + Idempotency-Key 唯一;
 * 处理中返回 409 STATE_CONFLICT;成功保存首次响应,同 key 再次请求直接重放;记录 24 小时过期可清理。
 */
@Slf4j
@Service
public class IdempotencyService {

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";

    private final IdempotencyRecordMapper mapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordMapper mapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    /**
     * 以幂等键执行一次写操作。
     * 成功请求保存首次响应;相同 key 再次调用直接重放;
     * 仍在处理中返回 409;业务失败清理占位记录,允许同 key 重试。
     */
    public <T> ApiResponse<T> execute(String userId, String endpoint, String key, Class<T> dataType,
                                      IdempotentSupplier<ApiResponse<T>> action) {
        if (!StringUtils.hasText(key)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "缺少 Idempotency-Key 请求头");
        }
        IdempotencyRecord existing = mapper.findByUserEndpointKey(userId, endpoint, key);
        if (existing != null) {
            return handleExisting(existing, dataType);
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId(idGenerator.idempotencyId());
        record.setUserId(userId);
        record.setEndpoint(endpoint);
        record.setRequestKey(key);
        record.setStatus(STATUS_PROCESSING);
        try {
            mapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发竞争:另一请求已插入,回读后按状态处理
            IdempotencyRecord raced = mapper.findByUserEndpointKey(userId, endpoint, key);
            if (raced != null) {
                return handleExisting(raced, dataType);
            }
            throw new BizException(ResultCode.STATE_CONFLICT, "重复请求处理中,请稍后重试");
        }
        try {
            ApiResponse<T> response = action.get();
            mapper.markSuccess(record.getId(), response.getCode(), toJson(response));
            return response;
        } catch (RuntimeException e) {
            // 业务失败:清理占位记录,允许同 key 重试(若外层事务回滚,此删除随事务一并回滚,不影响正确性)
            try {
                mapper.deleteById(record.getId());
            } catch (Exception ex) {
                log.warn("清理幂等占位记录失败, id={}", record.getId(), ex);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> handleExisting(IdempotencyRecord record, Class<T> dataType) {
        if (STATUS_SUCCESS.equals(record.getStatus()) && StringUtils.hasText(record.getResponseBody())) {
            try {
                ApiResponse<?> raw = objectMapper.readValue(record.getResponseBody(), ApiResponse.class);
                Object data = raw.getData();
                if (data != null && !dataType.isInstance(data)) {
                    data = objectMapper.convertValue(data, dataType);
                }
                @SuppressWarnings("unchecked")
                ApiResponse<T> response = new ApiResponse<>(
                        raw.getCode(), raw.getMessage(), (T) data, raw.getTraceId(), raw.getTimestamp());
                return response;
            } catch (IOException e) {
                log.error("幂等响应反序列化失败, id={}", record.getId(), e);
                throw new BizException(ResultCode.INTERNAL_ERROR, "幂等记录异常,请更换 Idempotency-Key 重试");
            }
        }
        throw new BizException(ResultCode.STATE_CONFLICT, "重复请求处理中,请稍后重试");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "响应序列化失败");
        }
    }
}
