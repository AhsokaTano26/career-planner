package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.RelationRequest;
import com.rickgao.careercore.modules.admin.mapper.AdminRelationMapper;
import com.rickgao.careercore.modules.admin.service.AdminRelationService;
import com.rickgao.careercore.modules.admin.vo.AdvisorRelationVO;
import com.rickgao.careercore.modules.advisor.entity.AdvisorStudentRelation;
import com.rickgao.careercore.modules.advisor.mapper.AdvisorStudentRelationMapper;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端-关系服务实现。
 * 批量语义:校验角色、去重、跳过已存在关系、返回新建列表;解除关系为软删除。
 */
@Service
public class AdminRelationServiceImpl implements AdminRelationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminRelationMapper adminRelationMapper;
    private final AdvisorStudentRelationMapper advisorRelationMapper;
    private final SysUserMapper sysUserMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;

    public AdminRelationServiceImpl(AdminRelationMapper adminRelationMapper,
                                    AdvisorStudentRelationMapper advisorRelationMapper,
                                    SysUserMapper sysUserMapper,
                                    IdGenerator idGenerator,
                                    IdempotencyService idempotencyService) {
        this.adminRelationMapper = adminRelationMapper;
        this.advisorRelationMapper = advisorRelationMapper;
        this.sysUserMapper = sysUserMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<AdvisorRelationVO> listRelations(String advisorId, Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean desc = !StringUtils.hasText(sort) || sort.startsWith("-");
        long total = adminRelationMapper.countRelations(advisorId);
        List<AdvisorRelationVO> list = adminRelationMapper.selectRelationPage(
                advisorId, "created_at", desc ? "DESC" : "ASC",
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public List<AdvisorRelationVO> createRelations(String operatorId, String endpoint, String idempotencyKey,
                                                   RelationRequest request) {
        return idempotencyService.execute(
                operatorId, endpoint, idempotencyKey, List.class,
                () -> ApiResponse.ok(doCreateRelations(request))).getData();
    }

    @Override
    @Transactional
    public void deleteRelation(String operatorId, String endpoint, String idempotencyKey, String relationId) {
        idempotencyService.execute(operatorId, endpoint, idempotencyKey, Void.class,
                () -> {
                    AdvisorStudentRelation relation = adminRelationMapper.findById(relationId);
                    if (relation == null) {
                        throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "关系不存在");
                    }
                    adminRelationMapper.softDeleteById(relationId);
                    return ApiResponse.ok();
                });
    }

    private List<AdvisorRelationVO> doCreateRelations(RelationRequest request) {
        if (request.getStudentIds() != null && request.getStudentIds().size() > 100) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "单次批量最多 100 名学生");
        }
        SysUser advisor = sysUserMapper.findById(request.getAdvisorId());
        if (advisor == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "辅导员不存在");
        }
        if (!CommonConstants.ROLE_ADVISOR.equals(advisor.getRole())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "指定用户不是辅导员");
        }
        List<String> studentIds = new ArrayList<>(new LinkedHashSet<>(request.getStudentIds()));
        Set<String> existing = Set.copyOf(advisorRelationMapper.findStudentIdsByAdvisor(advisor.getId()));
        List<AdvisorRelationVO> created = new ArrayList<>();
        for (String studentId : studentIds) {
            if (existing.contains(studentId)) {
                continue;
            }
            SysUser student = sysUserMapper.findById(studentId);
            if (student == null || !CommonConstants.ROLE_STUDENT.equals(student.getRole())) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "学生不存在或非学生角色: " + studentId);
            }
            AdvisorStudentRelation relation = adminRelationMapper
                    .findByAdvisorAndStudentIncludingDeleted(advisor.getId(), studentId);
            if (relation != null) {
                // 曾解除过:恢复旧关系,保持唯一键约束
                adminRelationMapper.restoreById(relation.getId());
            } else {
                relation = new AdvisorStudentRelation();
                relation.setId(idGenerator.adminRelationId());
                relation.setAdvisorId(advisor.getId());
                relation.setStudentId(studentId);
                adminRelationMapper.insert(relation);
            }

            AdvisorRelationVO vo = new AdvisorRelationVO();
            vo.setId(relation.getId());
            vo.setAdvisorId(advisor.getId());
            vo.setAdvisorName(advisor.getName());
            vo.setStudentId(studentId);
            vo.setStudentName(student.getName());
            vo.setCreatedAt(LocalDateTime.now());
            created.add(vo);
        }
        return created;
    }
}
