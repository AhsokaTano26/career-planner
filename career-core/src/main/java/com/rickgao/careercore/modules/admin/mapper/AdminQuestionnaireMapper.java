package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.assessment.entity.Question;
import com.rickgao.careercore.modules.assessment.entity.QuestionOption;
import com.rickgao.careercore.modules.assessment.entity.Questionnaire;
import com.rickgao.careercore.modules.assessment.entity.QuestionnaireVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端-问卷管理 Mapper（读全量 + 写）。
 * 复用 assessment 的实体；读侧可与 AssessmentMapper 互补。
 */
@Mapper
public interface AdminQuestionnaireMapper {

    // ---- 问卷 ----
    List<Questionnaire> selectQuestionnairePage(@Param("keyword") String keyword,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    long countQuestionnaires(@Param("keyword") String keyword);

    Questionnaire findQuestionnaireById(@Param("id") String id);

    int insertQuestionnaire(Questionnaire questionnaire);

    int updateQuestionnaireMeta(Questionnaire questionnaire);

    int updateQuestionnaireStatus(@Param("id") String id,
                                  @Param("status") String status);

    int updateQuestionnairePublish(@Param("id") String id,
                                   @Param("status") String status,
                                   @Param("version") Integer version,
                                   @Param("publishedAt") LocalDateTime publishedAt,
                                   @Param("publishedBy") String publishedBy);

    // ---- 版本 ----
    List<QuestionnaireVersion> listVersions(@Param("questionnaireId") String questionnaireId);

    QuestionnaireVersion findVersionById(@Param("id") String id);

    int insertVersion(QuestionnaireVersion version);

    int updateVersionPublish(@Param("id") String id,
                             @Param("status") String status,
                             @Param("publishedAt") LocalDateTime publishedAt,
                             @Param("publishedBy") String publishedBy);

    // ---- 题目 / 选项 ----
    List<Question> listQuestions(@Param("versionId") String versionId);

    List<QuestionOption> listOptions(@Param("questionIds") List<String> questionIds);

    int insertQuestion(Question question);

    int insertOption(QuestionOption option);
}
