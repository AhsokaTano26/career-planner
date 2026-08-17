package com.rickgao.careercore.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Jackson 工具:提供全局共享的 ObjectMapper。
 * 时间序列化统一为 ISO 8601 带 +08:00 偏移,与接口文档一致。
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 先注册 JavaTimeModule 提供时间反序列化,再注册自定义序列化器保持 ISO 带 +08:00 输出
        MAPPER.registerModule(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeIsoSerializer());
        module.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        MAPPER.registerModule(module);
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtil() {
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 解析失败", e);
        }
    }

    public static <T> T parse(String json, TypeReference<T> typeReference) {
        try {
            return MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON 解析失败", e);
        }
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON 序列化失败", e);
        }
    }
}
