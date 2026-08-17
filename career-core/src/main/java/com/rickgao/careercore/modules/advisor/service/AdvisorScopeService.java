package com.rickgao.careercore.modules.advisor.service;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import org.springframework.stereotype.Service;

/**
 * 辅导员数据范围校验。
 * 访问学生资源前必须调用 assertAssigned,关系不存在返回 403 FORBIDDEN。
 */
@Service
public class AdvisorScopeService {

    private final AdvisorStudentRelationMapper relationMapper;

    public AdvisorScopeService(AdvisorStudentRelationMapper relationMapper) {
        this.relationMapper = relationMapper;
    }

    public void assertAssigned(String advisorId, String studentId) {
        if (relationMapper.countByAdvisorAndStudent(advisorId, studentId) <= 0) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问目标学生");
        }
    }
}
