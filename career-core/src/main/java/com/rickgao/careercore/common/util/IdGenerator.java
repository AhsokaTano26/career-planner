package com.rickgao.careercore.common.util;

import com.rickgao.careercore.common.mapper.SequenceMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字符串业务 ID 生成器。
 * 由 id_sequence 表原子递增并格式化为可读前缀,如用户 S1001、经历 EXP-001。
 */
@Component
public class IdGenerator {

    private final SequenceMapper sequenceMapper;

    public IdGenerator(SequenceMapper sequenceMapper) {
        this.sequenceMapper = sequenceMapper;
    }

    /** 取下一个序列值并拼接前缀(前缀 + 零填充数字)。需在事务内使用以保证同一连接。 */
    @Transactional
    public String next(String seqName, String prefix, int width) {
        sequenceMapper.increment(seqName);
        Long next = sequenceMapper.lastInsertId();
        return prefix + String.format("%0" + width + "d", next);
    }

    public String userId() {
        return next("sys_user", "S", 4);
    }

    public String studentProfileId() {
        return next("student_profile", "P", 4);
    }

    public String studentWhitelistId() {
        return next("student_whitelist", "WL", 3);
    }

    public String consentDocumentId() {
        return next("consent_document", "CD", 3);
    }

    public String consentRecordId() {
        return next("consent_record", "CR", 4);
    }

    public String refreshTokenId() {
        return next("refresh_token", "RT", 4);
    }

    public String experienceId() {
        return next("student_experience", "EXP-", 3);
    }

    public String deletionRequestId() {
        return next("deletion_request", "DR", 4);
    }

    public String auditLogId() {
        return next("operation_audit_log", "AL", 4);
    }

    public String advisorRelationId() {
        return next("advisor_student_relation", "AR-", 3);
    }

    public String advisorCommentId() {
        return next("advisor_comment", "GC-", 3);
    }

    public String idempotencyId() {
        return next("idempotency_record", "IDEM-", 4);
    }

    public String adminRelationId() {
        return next("advisor_student_relation", "REL-", 3);
    }

    public String whitelistId() {
        return next("student_whitelist", "WL-", 3);
    }

    public String weightId() {
        return next("recommendation_weight", "WGT-", 3);
    }

    public String curriculumJobId() {
        return next("curriculum_import_job", "CJ-", 3);
    }

    public String curriculumItemId() {
        return next("curriculum_import_item", "IT-", 3);
    }

    public String curriculumVersionId() {
        return next("curriculum_version", "CV-", 3);
    }

    public String courseId() {
        return next("course", "CRS-", 3);
    }

    public String courseAbilityTagId() {
        return next("course_ability_tag", "CAT-", 3);
    }
}
