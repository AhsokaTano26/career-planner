package com.rickgao.careercore.modules.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.DirectionRequest;
import com.rickgao.careercore.modules.admin.dto.DirectionStatusUpdate;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.admin.service.AdminDirectionService;
import com.rickgao.careercore.modules.admin.vo.AdminDirectionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 管理端-方向库服务实现(单行 + updatedAt 版本语义)。 */
@Service
public class AdminDirectionServiceImpl implements AdminDirectionService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> PATH_ENUM = Set.of("graduate", "employment", "overseas");
    private static final Set<String> STATUS_ENUM = Set.of("PUBLISHED", "DISABLED", "DRAFT");
    private static final Set<String> STATUS_TRANSITION_ENUM = Set.of("PUBLISHED", "DISABLED");
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "name", "sortOrder");

    private final AdminDirectionMapper directionMapper;
    private final IdempotencyService idempotencyService;

    public AdminDirectionServiceImpl(AdminDirectionMapper directionMapper,
                                     IdempotencyService idempotencyService) {
        this.directionMapper = directionMapper;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<AdminDirectionVO> listDirections(String path, String status, String keyword,
                                                       Integer page, Integer size, String sort) {
        if (StringUtils.hasText(path) && !PATH_ENUM.contains(path)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "path 仅支持 graduate/employment/overseas");
        }
        if (StringUtils.hasText(status) && !STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 PUBLISHED/DISABLED/DRAFT");
        }
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = directionMapper.countDirections(path, status, keyword);
        List<AdminDirectionVO> list = directionMapper.selectDirectionPage(
                path, status, keyword, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public AdminDirectionVO createDirection(String operatorId, String endpoint, String idempotencyKey,
                                            DirectionRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, AdminDirectionVO.class,
                () -> ApiResponse.ok(doCreateDirection(request))).getData();
    }

    @Override
    @Transactional
    public AdminDirectionVO updateDirection(String operatorId, String endpoint, String idempotencyKey,
                                            String directionId, DirectionRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, AdminDirectionVO.class,
                () -> ApiResponse.ok(doUpdateDirection(directionId, request))).getData();
    }

    @Override
    @Transactional
    public AdminDirectionVO setDirectionStatus(String operatorId, String endpoint, String idempotencyKey,
                                               String directionId, DirectionStatusUpdate update) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, AdminDirectionVO.class,
                () -> ApiResponse.ok(doSetStatus(directionId, update.getStatus()))).getData();
    }

    private AdminDirectionVO doCreateDirection(DirectionRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向编码不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向名称不能为空");
        }
        validatePath(request.getPath());
        if (!StringUtils.hasText(request.getIntro())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向简介不能为空");
        }
        validateTarget(request.getTarget());
        if (directionMapper.findById(request.getId().trim()) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "方向编码已存在");
        }
        CareerDirection direction = new CareerDirection();
        direction.setId(request.getId().trim());
        direction.setName(request.getName().trim());
        direction.setPath(request.getPath());
        direction.setIcon(trimToNull(request.getIcon()));
        direction.setIntro(request.getIntro().trim());
        direction.setTargetJson(JsonUtil.toJson(request.getTarget()));
        direction.setMinAbility(toBigDecimal(request.getMinAbility()));
        direction.setMinAcademic(toBigDecimal(request.getMinAcademic()));
        direction.setLearningJson(toJsonOrNull(request.getLearning()));
        direction.setAbilitiesJson(toJsonOrNull(request.getAbilities()));
        direction.setCoursesJson(toJsonOrNull(request.getCourses()));
        direction.setActivitiesJson(toJsonOrNull(request.getActivities()));
        direction.setPathDescJson(toJsonOrNull(request.getPathDesc()));
        direction.setMisconceptionsJson(toJsonOrNull(request.getMisconceptions()));
        direction.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        direction.setApplicableMajorsJson(toJsonOrNull(request.getApplicableMajors()));
        direction.setStatus("DRAFT");
        direction.setUpdated(LocalDate.now());
        directionMapper.insert(direction);
        return toVO(direction);
    }

    private AdminDirectionVO doUpdateDirection(String directionId, DirectionRequest request) {
        CareerDirection direction = directionMapper.findById(directionId);
        if (direction == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "方向不存在");
        }
        if (StringUtils.hasText(request.getName())) {
            direction.setName(request.getName().trim());
        }
        if (StringUtils.hasText(request.getPath())) {
            validatePath(request.getPath());
            direction.setPath(request.getPath());
        }
        if (request.getIcon() != null) {
            direction.setIcon(trimToNull(request.getIcon()));
        }
        if (StringUtils.hasText(request.getIntro())) {
            direction.setIntro(request.getIntro().trim());
        }
        if (request.getTarget() != null) {
            validateTarget(request.getTarget());
            direction.setTargetJson(JsonUtil.toJson(request.getTarget()));
        }
        if (request.getMinAbility() != null) {
            validateRange(request.getMinAbility(), "minAbility");
            direction.setMinAbility(toBigDecimal(request.getMinAbility()));
        }
        if (request.getMinAcademic() != null) {
            validateRange(request.getMinAcademic(), "minAcademic");
            direction.setMinAcademic(toBigDecimal(request.getMinAcademic()));
        }
        if (request.getLearning() != null) {
            direction.setLearningJson(toJsonOrNull(request.getLearning()));
        }
        if (request.getAbilities() != null) {
            direction.setAbilitiesJson(toJsonOrNull(request.getAbilities()));
        }
        if (request.getCourses() != null) {
            direction.setCoursesJson(toJsonOrNull(request.getCourses()));
        }
        if (request.getActivities() != null) {
            direction.setActivitiesJson(toJsonOrNull(request.getActivities()));
        }
        if (request.getPathDesc() != null) {
            direction.setPathDescJson(toJsonOrNull(request.getPathDesc()));
        }
        if (request.getMisconceptions() != null) {
            direction.setMisconceptionsJson(toJsonOrNull(request.getMisconceptions()));
        }
        if (request.getSortOrder() != null) {
            direction.setSortOrder(request.getSortOrder());
        }
        if (request.getApplicableMajors() != null) {
            direction.setApplicableMajorsJson(toJsonOrNull(request.getApplicableMajors()));
        }
        direction.setUpdated(LocalDate.now());
        directionMapper.updateContent(direction);
        return toVO(direction);
    }

    private AdminDirectionVO doSetStatus(String directionId, String status) {
        CareerDirection direction = directionMapper.findById(directionId);
        if (direction == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "方向不存在");
        }
        if (!STATUS_TRANSITION_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 PUBLISHED/DISABLED");
        }
        if (status.equals(direction.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "方向已处于目标状态");
        }
        if ("PUBLISHED".equals(status) && !isContentComplete(direction)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向内容不完整,无法发布(intro 与六维目标值必填)");
        }
        directionMapper.updateStatus(directionId, status);
        direction.setStatus(status);
        return toVO(direction);
    }

    private boolean isContentComplete(CareerDirection direction) {
        if (!StringUtils.hasText(direction.getIntro()) || !StringUtils.hasText(direction.getTargetJson())) {
            return false;
        }
        try {
            DirectionRequest.Target target = JsonUtil.parse(direction.getTargetJson(), DirectionRequest.Target.class);
            return target != null
                    && target.getInterest() != null && target.getValues() != null
                    && target.getAbility() != null && target.getAcademic() != null
                    && target.getTendency() != null && target.getPractice() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateTarget(DirectionRequest.Target target) {
        if (target == null || target.getInterest() == null || target.getValues() == null
                || target.getAbility() == null || target.getAcademic() == null
                || target.getTendency() == null || target.getPractice() == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "六维目标值(interest/values/ability/academic/tendency/practice)必填");
        }
        validateRange(target.getInterest(), "target.interest");
        validateRange(target.getValues(), "target.values");
        validateRange(target.getAbility(), "target.ability");
        validateRange(target.getAcademic(), "target.academic");
        validateRange(target.getTendency(), "target.tendency");
        validateRange(target.getPractice(), "target.practice");
    }

    private void validateRange(Double value, String field) {
        if (value == null || value < 0 || value > 100) {
            throw new BizException(ResultCode.VALIDATION_ERROR, field + " 必须在 0-100 之间");
        }
    }

    private void validatePath(String path) {
        if (!PATH_ENUM.contains(path)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "path 仅支持 graduate/employment/overseas");
        }
    }

    private AdminDirectionVO toVO(CareerDirection direction) {
        AdminDirectionVO vo = new AdminDirectionVO();
        vo.setId(direction.getId());
        vo.setName(direction.getName());
        vo.setPath(direction.getPath());
        vo.setStatus(direction.getStatus());
        vo.setSortOrder(direction.getSortOrder());
        vo.setApplicableMajors(parseStringList(direction.getApplicableMajorsJson()));
        vo.setUpdatedAt(direction.getUpdatedAt());
        return vo;
    }

    private List<String> parseStringList(String json) {
        return StringUtils.hasText(json)
                ? JsonUtil.parse(json, new TypeReference<List<String>>() {
        })
                : new ArrayList<>();
    }

    private String toJsonOrNull(Object value) {
        return value == null ? null : JsonUtil.toJson(value);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
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
            case "sortOrder" -> "sort_order";
            default -> "created_at";
        };
        return new String[]{column, dir};
    }
}
