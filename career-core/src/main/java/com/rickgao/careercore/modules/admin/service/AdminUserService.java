package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.AdminUserUpdate;
import com.rickgao.careercore.modules.admin.vo.AdminUserVO;

/**
 * 管理端-用户应用服务。
 */
public interface AdminUserService {

    PageResult<AdminUserVO> listUsers(String role, String status, String keyword,
                                      Integer page, Integer size, String sort);

    void updateUser(String operatorId, String endpoint, String idempotencyKey,
                    String userId, AdminUserUpdate dto);
}
