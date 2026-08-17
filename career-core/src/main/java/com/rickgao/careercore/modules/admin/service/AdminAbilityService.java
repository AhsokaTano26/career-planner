package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.AbilityTagRequest;
import com.rickgao.careercore.modules.admin.vo.AbilityTagVO;

/** 管理端-能力标签应用服务。 */
public interface AdminAbilityService {

    PageResult<AbilityTagVO> listAbilities(String category, String keyword,
                                           Integer page, Integer size, String sort);

    AbilityTagVO createAbility(String operatorId, String endpoint, String idempotencyKey,
                               AbilityTagRequest request);

    AbilityTagVO updateAbility(String operatorId, String endpoint, String idempotencyKey,
                               String tagId, AbilityTagRequest request);
}
