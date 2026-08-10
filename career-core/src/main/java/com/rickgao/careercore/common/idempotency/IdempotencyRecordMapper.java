package com.rickgao.careercore.common.idempotency;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 幂等记录 Mapper。
 */
@Mapper
public interface IdempotencyRecordMapper {

    void insert(IdempotencyRecord record);

    IdempotencyRecord findByUserEndpointKey(@Param("userId") String userId,
                                            @Param("endpoint") String endpoint,
                                            @Param("requestKey") String requestKey);

    void markSuccess(@Param("id") String id,
                     @Param("code") String code,
                     @Param("body") String body);

    void deleteById(@Param("id") String id);
}
