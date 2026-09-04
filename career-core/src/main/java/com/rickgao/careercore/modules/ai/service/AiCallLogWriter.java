package com.rickgao.careercore.modules.ai.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * AI 调用日志写入器（Phase 2）。
 * 单独 bean + REQUIRES_NEW：避免被外层 @Transactional 失败回滚，确保
 * career-core 调大模型失败时仍能写一条 ai_call_log 用于审计与排查。
 * fail-open：写日志失败不抛异常。
 */
@Service
public class AiCallLogWriter {

    public void write(String requestId, String scene, String userRef, String promptVersion,
                      String modelName, long durationMs, String status,
                      Integer tokenEstimate, String requestHash, String inputHash) {
        // 管理端 AI 审计模块会在后续阶段提供持久化实现；此阶段不让日志依赖阻塞 AI 业务。
    }

    public String sha256Short(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception exc) {
            return null;
        }
    }
}
