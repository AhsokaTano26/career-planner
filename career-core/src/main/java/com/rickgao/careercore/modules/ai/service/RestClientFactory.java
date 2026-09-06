package com.rickgao.careercore.modules.ai.service;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 构造带超时与连接复用的 RestClient 请求工厂。
 *
 * <p>稳定性（2026-09）：由无池的 Simple 工厂换为 JDK HttpClient（自带连接复用，
 * 无新增依赖），高并发调网关不再频繁建连。超时语义保持不变。
 */
final class RestClientFactory {

    /** 进程共享的 HttpClient（连接池复用；超时按请求工厂配置）。 */
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private RestClientFactory() {
    }

    static org.springframework.http.client.ClientHttpRequestFactory factory(int timeoutSeconds) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(SHARED_CLIENT);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }
}
