package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireRequest;
import com.rickgao.careercore.modules.admin.dto.QuestionnaireStatusUpdate;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireDetailVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVersionVO;
import com.rickgao.careercore.modules.assessment.vo.QuestionnaireVO;

import java.util.List;

/**
 * 管理端-问卷管理业务。
 */
public interface AdminQuestionnaireService {

    PageResult<QuestionnaireVO> listQuestionnaires(String keyword, int page, int size);

    QuestionnaireVO createQuestionnaire(String adminId, QuestionnaireRequest req);

    QuestionnaireVO updateQuestionnaire(String adminId, String questionnaireId, QuestionnaireRequest req);

    QuestionnaireVO setStatus(String adminId, String questionnaireId, QuestionnaireStatusUpdate req);

    List<QuestionnaireVersionVO> listVersions(String questionnaireId);

    QuestionnaireDetailVO getVersionDetail(String questionnaireId, String versionId);

    QuestionnaireVersionVO createVersion(String adminId, String questionnaireId, QuestionnaireRequest req);

    QuestionnaireVersionVO publishVersion(String adminId, String questionnaireId, String versionId);
}
