package com.rickgao.careercore.modules.student.service;

import com.rickgao.careercore.modules.student.dto.DeletionRequestDTO;
import com.rickgao.careercore.modules.student.dto.ExperienceRequest;
import com.rickgao.careercore.modules.student.dto.StudentProfileUpdateDTO;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO;
import com.rickgao.careercore.modules.student.vo.ExperienceVO;
import com.rickgao.careercore.modules.student.vo.StudentProfileVO;

import java.util.List;

/**
 * 学生档案应用服务接口。
 */
public interface StudentProfileService {

    StudentProfileVO getMyProfile(String userId);

    StudentProfileVO updateMyProfile(String userId, StudentProfileUpdateDTO dto);

    CompletenessDetailVO completeness(String userId);

    List<ExperienceVO> listExperiences(String userId, Integer page, Integer size, String sort);

    ExperienceVO createExperience(String userId, ExperienceRequest request);

    ExperienceVO updateExperience(String userId, String experienceId, ExperienceRequest request);

    void deleteExperience(String userId, String experienceId);

    void requestDeletion(String userId, DeletionRequestDTO dto, String ip);
}
