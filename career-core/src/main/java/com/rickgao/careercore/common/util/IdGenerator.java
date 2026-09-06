package com.rickgao.careercore.common.util;

import com.rickgao.careercore.common.mapper.SequenceMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字符串业务 ID 生成器。
 * 由 id_sequence 表原子递增并格式化为可读前缀,如用户 S1001、经历 EXP-001。
 *
 * <p>稳定性（2026-09）：increment + lastInsertId 必须同一连接，
 * 此前便捷方法经 this.next() 自调用绕过 Spring 代理导致 @Transactional 失效，
 * 并发下可能读到别的连接的 LAST_INSERT_ID。现在全部经自注入代理调用。
 */
@Component
public class IdGenerator {

    private final SequenceMapper sequenceMapper;
    private final IdGenerator self;

    public IdGenerator(SequenceMapper sequenceMapper, @Lazy IdGenerator self) {
        this.sequenceMapper = sequenceMapper;
        this.self = self;
    }

    /** 取下一个序列值并拼接前缀(前缀 + 零填充数字)。需在事务内使用以保证同一连接。 */
    @Transactional
    public String next(String seqName, String prefix, int width) {
        sequenceMapper.increment(seqName);
        Long next = sequenceMapper.lastInsertId();
        return prefix + String.format("%0" + width + "d", next);
    }

    public String userId() {
        return self.next("sys_user", "S", 4);
    }

    public String studentProfileId() {
        return self.next("student_profile", "P", 4);
    }

    public String studentWhitelistId() {
        return self.next("student_whitelist", "WL", 3);
    }

    public String consentDocumentId() {
        return self.next("consent_document", "CD", 3);
    }

    public String consentRecordId() {
        return self.next("consent_record", "CR", 4);
    }

    public String refreshTokenId() {
        return self.next("refresh_token", "RT", 4);
    }

    public String experienceId() {
        return self.next("student_experience", "EXP-", 3);
    }

    public String deletionRequestId() {
        return self.next("deletion_request", "DR", 4);
    }

    public String auditLogId() {
        return self.next("operation_audit_log", "AL", 4);
    }

    public String advisorRelationId() {
        return self.next("advisor_student_relation", "AR-", 3);
    }

    public String advisorCommentId() {
        return self.next("advisor_comment", "GC-", 3);
    }

    public String idempotencyId() {
        return self.next("idempotency_record", "IDEM-", 4);
    }

    public String adminRelationId() {
        return self.next("advisor_student_relation", "REL-", 3);
    }

    public String whitelistId() {
        return self.next("student_whitelist", "WL-", 3);
    }

    public String weightId() {
        return self.next("recommendation_weight", "WGT-", 3);
    }

    public String curriculumJobId() {
        return self.next("curriculum_import_job", "CJ-", 3);
    }

    public String curriculumItemId() {
        return self.next("curriculum_import_item", "IT-", 3);
    }

    public String curriculumVersionId() {
        return self.next("curriculum_version", "CV-", 3);
    }

    public String courseId() {
        return self.next("course", "CRS-", 3);
    }

    public String courseAbilityTagId() {
        return self.next("course_ability_tag", "CAT-", 3);
    }

    public String exportJobId() {
        return self.next("export_job", "EX-", 3);
    }

    public String aiChatMessageId() {
        return self.next("ai_chat_message", "AIM-", 4);
    }

    public String aiChatFeedbackId() {
        return self.next("ai_chat_feedback", "AIF-", 4);
    }

    public String assessmentSessionId() {
        return self.next("assessment_session", "AS-", 4);
    }

    public String profileSnapshotId() {
        return self.next("profile_snapshot", "PS-", 4);
    }

    public String favoriteId() { return self.next("student_favorite", "FAV-", 4); }
    public String goalVersionId() { return self.next("goal_version", "GV-", 4); }
    public String studentGoalId() { return self.next("student_goal", "GOAL-", 4); }
    public String semesterPlanId() { return self.next("semester_plan", "PLAN-", 4); }
    public String planVersionId() { return self.next("plan_version", "PV-", 4); }
    public String planTaskId() { return self.next("plan_task", "TASK-", 4); }
    public String taskCheckinId() { return self.next("task_checkin", "CHK-", 4); }
    public String stageReviewId() { return self.next("stage_review", "REV-", 4); }
    public String reminderId() { return self.next("reminder", "REM-", 4); }
    public String recommendationRunId() { return self.next("recommendation_run", "RUN-", 4); }
    public String recommendationResultId() { return self.next("recommendation_result", "REC-", 4); }
    public String modelConfigId() { return self.next("model_config", "MODEL-", 4); }
    public String promptVersionId() { return self.next("prompt_version", "PROMPT-", 4); }
    public String questionnaireId() { return self.next("questionnaire", "QNR-", 4); }
    public String questionnaireVersionId() { return self.next("questionnaire_version", "QNV-", 4); }
    public String questionId() { return self.next("question", "Q-", 4); }
    public String questionOptionId() { return self.next("question_option", "OPT-", 4); }
}
