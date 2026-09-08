package com.rickgao.careercore.common.audit;

import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.auth.entity.OperationAuditLog;
import com.rickgao.careercore.modules.auth.mapper.OperationAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作审计日志写入组件(跨模块复用)。
 *
 * <p>稳定性（2026-09）：独立新事务 + 吞错（同 AiCallLogWriter 模式），
 * 审计写失败不再回滚登录/注册等主业务。
 */
@Component
public class AuditLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    private final OperationAuditLogMapper operationAuditLogMapper;
    private final IdGenerator idGenerator;

    public AuditLogWriter(OperationAuditLogMapper operationAuditLogMapper, IdGenerator idGenerator) {
        this.operationAuditLogMapper = operationAuditLogMapper;
        this.idGenerator = idGenerator;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String userId, String targetType, String targetId, String detail, String ip) {
        try {
            OperationAuditLog auditLog = new OperationAuditLog();
            auditLog.setId(idGenerator.auditLogId());
            auditLog.setAction(action);
            auditLog.setUserId(userId);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setDetail(detail);
            auditLog.setIp(ip);
            operationAuditLogMapper.insert(auditLog);
        } catch (Exception exc) {
            // 审计不能阻断主链路；失败仅记日志（DB 抖动/字段超长时安全降级）
            log.warn("审计日志写入失败 action={}：{}", action, exc.getMessage());
        }
    }
}
