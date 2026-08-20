package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.DirectionRequest;
import com.rickgao.careercore.modules.admin.dto.DirectionStatusUpdate;
import com.rickgao.careercore.modules.admin.vo.AdminDirectionVO;

/** 管理端-方向库应用服务。 */
public interface AdminDirectionService {

    PageResult<AdminDirectionVO> listDirections(String path, String status, String keyword,
                                                Integer page, Integer size, String sort);

    AdminDirectionVO createDirection(String operatorId, String endpoint, String idempotencyKey,
                                     DirectionRequest request);

    AdminDirectionVO updateDirection(String operatorId, String endpoint, String idempotencyKey,
                                     String directionId, DirectionRequest request);

    AdminDirectionVO setDirectionStatus(String operatorId, String endpoint, String idempotencyKey,
                                        String directionId, DirectionStatusUpdate update);
}
