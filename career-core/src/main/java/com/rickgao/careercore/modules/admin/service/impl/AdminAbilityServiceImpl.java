package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.dto.AbilityTagRequest;
import com.rickgao.careercore.modules.admin.entity.AbilityTag;
import com.rickgao.careercore.modules.admin.mapper.AdminAbilityMapper;
import com.rickgao.careercore.modules.admin.service.AdminAbilityService;
import com.rickgao.careercore.modules.admin.vo.AbilityTagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/** 管理端-能力标签服务实现。 */
@Service
public class AdminAbilityServiceImpl implements AdminAbilityService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> STATUS_ENUM = Set.of(
            CommonConstants.USER_STATUS_ACTIVE, CommonConstants.USER_STATUS_DISABLED);
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "name", "category");

    private final AdminAbilityMapper abilityMapper;
    private final IdempotencyService idempotencyService;

    public AdminAbilityServiceImpl(AdminAbilityMapper abilityMapper, IdempotencyService idempotencyService) {
        this.abilityMapper = abilityMapper;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<AbilityTagVO> listAbilities(String category, String keyword,
                                                  Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = abilityMapper.countAbilities(category, keyword);
        List<AbilityTagVO> list = abilityMapper.selectAbilityPage(
                category, keyword, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public AbilityTagVO createAbility(String operatorId, String endpoint, String idempotencyKey,
                                      AbilityTagRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, AbilityTagVO.class,
                () -> ApiResponse.ok(doCreateAbility(request))).getData();
    }

    @Override
    @Transactional
    public AbilityTagVO updateAbility(String operatorId, String endpoint, String idempotencyKey,
                                      String tagId, AbilityTagRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, AbilityTagVO.class,
                () -> ApiResponse.ok(doUpdateAbility(tagId, request))).getData();
    }

    private AbilityTagVO doCreateAbility(AbilityTagRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "标签编码不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "标签名称不能为空");
        }
        if (abilityMapper.findById(request.getId().trim()) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "标签编码已存在");
        }
        AbilityTag tag = new AbilityTag();
        tag.setId(request.getId().trim());
        tag.setName(request.getName().trim());
        tag.setCategory(trimToNull(request.getCategory()));
        tag.setStatus(validateStatus(request.getStatus(), CommonConstants.USER_STATUS_ACTIVE));
        abilityMapper.insert(tag);
        return toVO(tag);
    }

    private AbilityTagVO doUpdateAbility(String tagId, AbilityTagRequest request) {
        AbilityTag tag = abilityMapper.findById(tagId);
        if (tag == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "能力标签不存在");
        }
        String name = StringUtils.hasText(request.getName()) ? request.getName().trim() : null;
        String category = StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : null;
        String status = StringUtils.hasText(request.getStatus())
                ? validateStatus(request.getStatus(), null)
                : null;
        if (name == null && category == null && status == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "name/category/status 至少提供一个");
        }
        abilityMapper.updatePartial(tagId, name, category, status);
        tag.setName(name != null ? name : tag.getName());
        tag.setCategory(category != null ? category : tag.getCategory());
        tag.setStatus(status != null ? status : tag.getStatus());
        return toVO(tag);
    }

    private String validateStatus(String status, String defaultStatus) {
        if (!StringUtils.hasText(status)) {
            return defaultStatus;
        }
        if (!STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 ACTIVE/DISABLED");
        }
        return status;
    }

    private AbilityTagVO toVO(AbilityTag tag) {
        AbilityTagVO vo = new AbilityTagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCategory(tag.getCategory());
        vo.setStatus(tag.getStatus());
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String[] resolveSort(String sort) {
        String field = "createdAt";
        String dir = "DESC";
        if (StringUtils.hasText(sort)) {
            String raw = sort.startsWith("-") ? sort.substring(1) : sort;
            if (SORTABLE_FIELDS.contains(raw)) {
                field = raw;
                dir = sort.startsWith("-") ? "DESC" : "ASC";
            }
        }
        String column = switch (field) {
            case "name" -> "name";
            case "category" -> "category";
            default -> "created_at";
        };
        return new String[]{column, dir};
    }
}
