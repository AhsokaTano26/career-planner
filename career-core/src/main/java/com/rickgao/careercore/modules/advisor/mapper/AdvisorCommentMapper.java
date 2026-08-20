package com.rickgao.careercore.modules.advisor.mapper;

import com.rickgao.careercore.modules.advisor.entity.AdvisorComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 辅导员指导意见 Mapper。
 */
@Mapper
public interface AdvisorCommentMapper {

    void insert(AdvisorComment comment);

    /** 某学生的全部指导意见(按时间正序,对应历史时间线) */
    List<AdvisorComment> findByStudentId(@Param("studentId") String studentId);
}
