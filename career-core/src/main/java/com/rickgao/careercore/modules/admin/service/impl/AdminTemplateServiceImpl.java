package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.TaskTemplateRequest;
import com.rickgao.careercore.modules.admin.entity.TaskTemplate;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.admin.mapper.AdminTemplateMapper;
import com.rickgao.careercore.modules.admin.service.AdminTemplateService;
import com.rickgao.careercore.modules.admin.vo.TaskTemplateVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/** 管理端-任务模板服务实现。 */
@Service
public class AdminTemplateServiceImpl implements AdminTemplateService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> STATUS_ENUM = Set.of(
            CommonConstants.USER_STATUS_ACTIVE, CommonConstants.USER_STATUS_DISABLED);
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "name");

    private final AdminTemplateMapper templateMapper;
    private final AdminDirectionMapper directionMapper;
    private final IdempotencyService idempotencyService;

    public AdminTemplateServiceImpl(AdminTemplateMapper templateMapper,
                                    AdminDirectionMapper directionMapper,
                                    IdempotencyService idempotencyService) {
        this.templateMapper = templateMapper;
        this.directionMapper = directionMapper;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<TaskTemplateVO> listTemplates(String directionId, Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = templateMapper.countTemplates(directionId);
        List<TaskTemplateVO> list = templateMapper.selectTemplatePage(
                directionId, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public TaskTemplateVO createTemplate(String operatorId, String endpoint, String idempotencyKey,
                                         TaskTemplateRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, TaskTemplateVO.class,
                () -> ApiResponse.ok(doCreateTemplate(request))).getData();
    }

    @Override
    @Transactional
    public TaskTemplateVO updateTemplate(String operatorId, String endpoint, String idempotencyKey,
                                         String templateId, TaskTemplateRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, TaskTemplateVO.class,
                () -> ApiResponse.ok(doUpdateTemplate(templateId, request))).getData();
    }

    private TaskTemplateVO doCreateTemplate(TaskTemplateRequest request) {
        if (!StringUtils.hasText(request.getId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "模板 ID 不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "模板名称不能为空");
        }
        requireDirection(request.getDirectionId());
        if (templateMapper.findById(request.getId().trim()) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "模板 ID 已存在");
        }
        TaskTemplate template = new TaskTemplate();
        template.setId(request.getId().trim());
        template.setDirectionId(request.getDirectionId().trim());
        template.setName(request.getName().trim());
        template.setGoalSummary(trimToNull(request.getGoalSummary()));
        template.setSemesterGoalsJson(request.getSemesterGoals() == null ? null : JsonUtil.toJson(request.getSemesterGoals()));
        template.setMonthlyTasksJson(request.getMonthlyTasks() == null ? null : JsonUtil.toJson(request.getMonthlyTasks()));
        template.setStatus(CommonConstants.USER_STATUS_ACTIVE);
        templateMapper.insert(template);
        return toVO(template);
    }

    private TaskTemplateVO doUpdateTemplate(String templateId, TaskTemplateRequest request) {
        TaskTemplate template = templateMapper.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "任务模板不存在");
        }
        if (StringUtils.hasText(request.getDirectionId())) {
            requireDirection(request.getDirectionId());
            template.setDirectionId(request.getDirectionId().trim());
        }
        if (StringUtils.hasText(request.getName())) {
            template.setName(request.getName().trim());
        }
        if (request.getGoalSummary() != null) {
            template.setGoalSummary(trimToNull(request.getGoalSummary()));
        }
        if (request.getSemesterGoals() != null) {
            template.setSemesterGoalsJson(JsonUtil.toJson(request.getSemesterGoals()));
        }
        if (request.getMonthlyTasks() != null) {
            template.setMonthlyTasksJson(JsonUtil.toJson(request.getMonthlyTasks()));
        }
        templateMapper.updateContent(template);
        if (StringUtils.hasText(request.getStatus())) {
            if (!STATUS_ENUM.contains(request.getStatus())) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 ACTIVE/DISABLED");
            }
            templateMapper.updateStatus(templateId, request.getStatus());
            template.setStatus(request.getStatus());
        }
        return toVO(template);
    }

    private void requireDirection(String directionId) {
        if (!StringUtils.hasText(directionId)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向编码不能为空");
        }
        if (directionMapper.findById(directionId.trim()) == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "方向不存在: " + directionId);
        }
    }

    private TaskTemplateVO toVO(TaskTemplate template) {
        TaskTemplateVO vo = new TaskTemplateVO();
        vo.setId(template.getId());
        vo.setDirectionId(template.getDirectionId());
        vo.setName(template.getName());
        vo.setGoalSummary(template.getGoalSummary());
        vo.setSemesterGoals(parseList(template.getSemesterGoalsJson(), TaskTemplateVO.SemesterGoal.class));
        vo.setMonthlyTasks(parseList(template.getMonthlyTasksJson(), TaskTemplateVO.MonthlyTask.class));
        vo.setStatus(template.getStatus());
        return vo;
    }

    private <T> List<T> parseList(String json, Class<T> elementType) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return JsonUtil.getMapper().readValue(
                    json,
                    JsonUtil.getMapper().getTypeFactory()
                            .constructCollectionType(List.class, elementType));
        } catch (Exception e) {
            return List.of();
        }
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
        return new String[]{field.equals("name") ? "name" : "created_at", dir};
    }
}
