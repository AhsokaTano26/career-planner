package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.vo.AiCallLogVO;
import com.rickgao.careercore.modules.admin.vo.OperationLogVO;

import java.time.LocalDateTime;

/** 管理端-日志查询应用服务。 */
public interface AdminLogService {

    PageResult<AiCallLogVO> listAiLogs(String scene, String status,
                                       LocalDateTime from, LocalDateTime to,
                                       Integer page, Integer size, String sort);

    PageResult<OperationLogVO> listOperationLogs(String action, String operator,
                                                 LocalDateTime from, LocalDateTime to,
                                                 Integer page, Integer size, String sort);
}
