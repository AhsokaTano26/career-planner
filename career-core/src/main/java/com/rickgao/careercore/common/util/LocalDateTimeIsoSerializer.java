package com.rickgao.careercore.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 将 LocalDateTime 序列化为 "2026-08-04T10:30:00+08:00" 形式的 ISO 8601。
 * LocalDateTime 本身不携带时区,应用统一采用东八区,故按无偏移格式输出后拼接 +08:00。
 * 供 JsonUtil 与 Spring 全局 ObjectMapper 共用,保证接口输出时间格式一致。
 */
public class LocalDateTimeIsoSerializer extends JsonSerializer<LocalDateTime> {

    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final String FIXED_OFFSET = "+08:00";

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(FORMATTER.format(value) + FIXED_OFFSET);
    }

    @Override
    public Class<LocalDateTime> handledType() {
        return LocalDateTime.class;
    }
}
