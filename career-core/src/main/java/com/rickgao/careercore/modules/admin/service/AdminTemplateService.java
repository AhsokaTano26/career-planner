package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.TaskTemplateRequest;
import com.rickgao.careercore.modules.admin.vo.TaskTemplateVO;

/** 管理端-任务模板应用服务。 */
public interface AdminTemplateService {

    PageResult<TaskTemplateVO> listTemplates(String directionId, Integer page, Integer size, String sort);

    TaskTemplateVO createTemplate(String operatorId, String endpoint, String idempotencyKey,
                                  TaskTemplateRequest request);

    TaskTemplateVO updateTemplate(String operatorId, String endpoint, String idempotencyKey,
                                  String templateId, TaskTemplateRequest request);
}
