package com.rickgao.careercore.modules.ai.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 构造带超时的 RestClient 请求工厂（Demo 精简点：用 Simple 工厂，无连接池；
 * 后续迭代替换位置：接入 JdkClientHttpRequestFactory 或 WebClient 做连接复用与限流）。
 */
final class RestClientFactory {

    private RestClientFactory() {
    }

    static org.springframework.http.client.ClientHttpRequestFactory factory(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return factory;
    }
}
