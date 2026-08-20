package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.RelationRequest;
import com.rickgao.careercore.modules.admin.vo.AdvisorRelationVO;

import java.util.List;

/**
 * 管理端-辅导员学生关系应用服务。
 */
public interface AdminRelationService {

    PageResult<AdvisorRelationVO> listRelations(String advisorId, Integer page, Integer size, String sort);

    List<AdvisorRelationVO> createRelations(String operatorId, String endpoint, String idempotencyKey,
                                            RelationRequest request);

    void deleteRelation(String operatorId, String endpoint, String idempotencyKey, String relationId);
}
