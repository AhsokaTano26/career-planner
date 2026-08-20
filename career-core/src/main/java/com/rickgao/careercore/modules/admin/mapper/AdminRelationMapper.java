package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.advisor.entity.AdvisorStudentRelation;
import com.rickgao.careercore.modules.admin.vo.AdvisorRelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理端-辅导员学生关系 Mapper。
 */
@Mapper
public interface AdminRelationMapper {

    List<AdvisorRelationVO> selectRelationPage(@Param("advisorId") String advisorId,
                                               @Param("sortColumn") String sortColumn,
                                               @Param("sortDir") String sortDir,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    long countRelations(@Param("advisorId") String advisorId);

    AdvisorStudentRelation findById(@Param("id") String id);

    /** 含软删记录查询(用于解除后重新建立时恢复) */
    AdvisorStudentRelation findByAdvisorAndStudentIncludingDeleted(@Param("advisorId") String advisorId,
                                                                  @Param("studentId") String studentId);

    int insert(AdvisorStudentRelation relation);

    int softDeleteById(@Param("id") String id);

    /** 恢复已软删关系 */
    int restoreById(@Param("id") String id);
}
