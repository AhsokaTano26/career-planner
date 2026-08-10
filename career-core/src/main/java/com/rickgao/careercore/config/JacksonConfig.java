package com.rickgao.careercore.config;

import com.rickgao.careercore.common.util.LocalDateTimeIsoSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 Jackson 定制:统一时间输出为 ISO 8601(带 +08:00 偏移)。
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.serializers(new LocalDateTimeIsoSerializer());
    }
}
