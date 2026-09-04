package com.rickgao.careercore.modules.ai.service;

import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import com.rickgao.careercore.modules.admin.mapper.AiCallLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * AI 调用日志写入器（Phase 2）。
 * 单独 bean + REQUIRES_NEW：避免被外层 @Transactional 失败回滚，确保
 * career-core 调大模型失败时仍能写一条 ai_call_log 用于审计与排查。
 * fail-open：写日志失败不抛异常。
 */
@Service
public class AiCallLogWriter {

    private final AiCallLogMapper aiCallLogMapper;

    public AiCallLogWriter(AiCallLogMapper aiCallLogMapper) {
        this.aiCallLogMapper = aiCallLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String requestId, String scene, String userRef, String promptVersion,
                      String modelName, long durationMs, String status,
                      Integer tokenEstimate, String requestHash, String inputHash) {
        try {
            AiCallLog log = new AiCallLog();
            log.setId("AIL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28));
            log.setRequestId(requestId);
            log.setScene(scene);
            log.setUserRef(userRef);
            log.setPromptVersion(promptVersion);
            log.setModelName(modelName);
            log.setDurationMs((int) Math.min(Integer.MAX_VALUE, Math.max(0, durationMs)));
            log.setStatus(status);
            log.setTokenEstimate(tokenEstimate);
            log.setRequestHash(requestHash);
            log.setInputHash(inputHash);
            aiCallLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计日志不能阻断主链路；重复 requestId 或数据库暂不可用时安全降级。
        }
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
