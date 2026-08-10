package com.rickgao.careercore.common.audit;

import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.auth.entity.OperationAuditLog;
import com.rickgao.careercore.modules.auth.mapper.OperationAuditLogMapper;
import org.springframework.stereotype.Component;

/**
 * 操作审计日志写入组件(跨模块复用)。
 */
@Component
public class AuditLogWriter {

    private final OperationAuditLogMapper operationAuditLogMapper;
    private final IdGenerator idGenerator;

    public AuditLogWriter(OperationAuditLogMapper operationAuditLogMapper, IdGenerator idGenerator) {
        this.operationAuditLogMapper = operationAuditLogMapper;
        this.idGenerator = idGenerator;
    }

    public void record(String action, String userId, String targetType, String targetId, String detail, String ip) {
        OperationAuditLog log = new OperationAuditLog();
        log.setId(idGenerator.auditLogId());
        log.setAction(action);
        log.setUserId(userId);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(ip);
        operationAuditLogMapper.insert(log);
    }
}
