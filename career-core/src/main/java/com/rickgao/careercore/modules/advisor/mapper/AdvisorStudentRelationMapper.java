package com.rickgao.careercore.modules.advisor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 辅导员-学生关系 Mapper。
 */
@Mapper
public interface AdvisorStudentRelationMapper {

    /** 有效关系数(软删除不计),用于数据范围校验 */
    int countByAdvisorAndStudent(@Param("advisorId") String advisorId,
                                 @Param("studentId") String studentId);

    /** 某辅导员名下全部学生 ID(有效关系) */
    List<String> findStudentIdsByAdvisor(@Param("advisorId") String advisorId);
}
