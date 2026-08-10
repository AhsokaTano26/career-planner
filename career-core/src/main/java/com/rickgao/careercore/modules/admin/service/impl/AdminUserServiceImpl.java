package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.dto.AdminUserUpdate;
import com.rickgao.careercore.modules.admin.mapper.AdminUserMapper;
import com.rickgao.careercore.modules.admin.service.AdminUserService;
import com.rickgao.careercore.modules.admin.vo.AdminUserVO;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 管理端-用户服务实现。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ROLE_ENUM = Set.of(
            CommonConstants.ROLE_STUDENT, CommonConstants.ROLE_ADVISOR, CommonConstants.ROLE_ADMIN);
    private static final Set<String> STATUS_ENUM = Set.of(
            CommonConstants.USER_STATUS_ACTIVE, CommonConstants.USER_STATUS_DISABLED);
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "username", "name", "lastLoginAt");

    private final AdminUserMapper adminUserMapper;
    private final SysUserMapper sysUserMapper;
    private final IdempotencyService idempotencyService;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper, SysUserMapper sysUserMapper,
                                IdempotencyService idempotencyService) {
        this.adminUserMapper = adminUserMapper;
        this.sysUserMapper = sysUserMapper;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<AdminUserVO> listUsers(String role, String status, String keyword,
                                             Integer page, Integer size, String sort) {
        if (StringUtils.hasText(role) && !ROLE_ENUM.contains(role)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "role 仅支持 STUDENT/ADVISOR/ADMIN");
        }
        if (StringUtils.hasText(status) && !STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "status 仅支持 ACTIVE/DISABLED(本版不支持 LOCKED)");
        }
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = adminUserMapper.countUsers(keyword, role, status);
        List<AdminUserVO> list = adminUserMapper.selectUserPage(
                keyword, role, status, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public void updateUser(String operatorId, String endpoint, String idempotencyKey,
                           String userId, AdminUserUpdate dto) {
        idempotencyService.execute(operatorId, endpoint, idempotencyKey, Void.class,
                () -> {
                    doUpdateUser(operatorId, userId, dto);
                    return ApiResponse.ok();
                });
    }

    private void doUpdateUser(String operatorId, String userId, AdminUserUpdate dto) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : null;
        String className = StringUtils.hasText(dto.getClassName()) ? dto.getClassName().trim() : null;
        if (status == null && className == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 与 className 至少提供一个");
        }
        if (status != null && !STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "status 仅支持 ACTIVE/DISABLED(本版不支持 LOCKED)");
        }
        if (CommonConstants.USER_STATUS_DISABLED.equals(status) && operatorId.equals(userId)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "不能停用当前登录账号");
        }
        adminUserMapper.updateStatusAndClass(userId, status, className);
        // TODO(审计): 高风险操作(更新用户状态/班级)需写入审计日志,由审计模块队友接入
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
            case "username" -> "username";
            case "name" -> "name";
            case "lastLoginAt" -> "last_login_at";
            default -> "created_at";
        };
        return new String[]{column, dir};
    }
}
