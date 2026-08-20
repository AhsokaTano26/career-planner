package com.rickgao.careercore.modules.advisor.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.advisor.dto.StudentListQuery;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStatisticsVO;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStudentVO;
import com.rickgao.careercore.modules.advisor.vo.AttentionStudentVO;
import com.rickgao.careercore.modules.advisor.vo.StudentDetailViewVO;

import java.util.List;

/**
 * 辅导员端学生查询应用服务(列表 / 详情 / 关注 / 统计)。
 */
public interface AdvisorStudentService {

    PageResult<AdvisorStudentVO> listStudents(String advisorId, StudentListQuery query);

    StudentDetailViewVO getDetail(String advisorId, String studentId);

    List<AttentionStudentVO> listAttention(String advisorId);

    AdvisorStatisticsVO getStatistics(String advisorId);
}
