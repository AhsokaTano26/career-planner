package com.rickgao.careercore.modules.assessment.mapper;

import com.rickgao.careercore.modules.assessment.entity.AssessmentSession;
import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测评模块映射器（问卷 + 会话）。
 */
@Mapper
public interface AssessmentMapper {

    // ---- 问卷 ----
    List<Questionnaire> listPublishedQuestionnaires();

    Questionnaire findQuestionnaireById(@Param("id") String id);

    QuestionnaireVersion findLatestVersion(@Param("questionnaireId") String questionnaireId);

    QuestionnaireVersion findVersionById(@Param("id") String id);

    List<QuestionnaireVersion> listVersions(@Param("questionnaireId") String questionnaireId);

    List<Question> listQuestions(@Param("versionId") String versionId);

    List<QuestionOption> listOptions(@Param("questionIds") List<String> questionIds);

    // ---- 会话 ----
    int insertSession(AssessmentSession session);

    AssessmentSession findSessionById(@Param("id") String id);

    AssessmentSession findActiveSession(@Param("studentId") String studentId);

    AssessmentSession findLatestScoredByStudent(@Param("studentId") String studentId);

    List<AssessmentSession> listSessions(@Param("studentId") String studentId);

    int updateSessionAnswers(@Param("id") String id,
                             @Param("answered") int answered,
                             @Param("status") String status);

    int updateSessionScore(@Param("id") String id,
                           @Param("status") String status,
                           @Param("scoreJson") String scoreJson,
                           @Param("finishedAt") String finishedAt);
}

