package com.rickgao.careercore.modules.ai.service;

import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import com.rickgao.careercore.modules.admin.mapper.AiCallLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    private static final Logger log = LoggerFactory.getLogger(AiCallLogWriter.class);

    private final AiCallLogMapper aiCallLogMapper;
    private final IdGenerator idGenerator;

    public AiCallLogWriter(AiCallLogMapper aiCallLogMapper, IdGenerator idGenerator) {
        this.aiCallLogMapper = aiCallLogMapper;
        this.idGenerator = idGenerator;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(String requestId, String scene, String userRef, String promptVersion,
                      String modelName, long durationMs, String status,
                      Integer tokenEstimate, String requestHash, String inputHash) {
        try {
            AiCallLog row = new AiCallLog();
            row.setId(idGenerator.aiCallLogId());
            row.setRequestId(requestId);
            row.setUserRef(userRef);
            row.setScene(scene == null ? "gateway_api" : scene);
            row.setModelName(modelName);
            row.setPromptVersion(promptVersion);
            row.setDurationMs((int) Math.min(Integer.MAX_VALUE, durationMs));
            row.setStatus(status);
            row.setTokenEstimate(tokenEstimate);
            row.setRequestHash(requestHash);
            row.setInputHash(inputHash);
            aiCallLogMapper.insert(row);
        } catch (Exception exc) {
            log.warn("ai_call_log 写入失败（requestId={}，scene={}）：{}", requestId, scene, exc.getMessage());
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
