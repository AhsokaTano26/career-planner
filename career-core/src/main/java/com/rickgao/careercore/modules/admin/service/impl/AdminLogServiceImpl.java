package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.service.AdminLogService;
import com.rickgao.careercore.modules.admin.vo.AiCallLogVO;
import com.rickgao.careercore.modules.admin.vo.OperationLogVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 管理端-日志查询服务实现。 */
@Service
public class AdminLogServiceImpl implements AdminLogService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> AI_STATUS_ENUM = Set.of("SUCCESS", "FAILED", "TIMEOUT", "DEGRADED");

    private final AdminExportMapper exportMapper;

    public AdminLogServiceImpl(AdminExportMapper exportMapper) {
        this.exportMapper = exportMapper;
    }

    @Override
    public PageResult<AiCallLogVO> listAiLogs(String scene, String status,
                                              LocalDateTime from, LocalDateTime to,
                                              Integer page, Integer size, String sort) {
        if (StringUtils.hasText(status) && !AI_STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "status 仅支持 SUCCESS/FAILED/TIMEOUT/DEGRADED");
        }
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean desc = !StringUtils.hasText(sort) || sort.startsWith("-");
        long total = exportMapper.countAiLogs(scene, status, from, to);
        List<AiCallLogVO> list = exportMapper.selectAiLogPage(
                scene, status, from, to, "created_at", desc ? "DESC" : "ASC",
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    public PageResult<OperationLogVO> listOperationLogs(String action, String operator,
                                                        LocalDateTime from, LocalDateTime to,
                                                        Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean desc = !StringUtils.hasText(sort) || sort.startsWith("-");
        long total = exportMapper.countOperationLogs(action, operator, from, to);
        List<OperationLogVO> list = exportMapper.selectOperationLogPage(
                action, operator, from, to, "l.created_at", desc ? "DESC" : "ASC",
                (currentPage - 1) * currentSize, currentSize);
        list.forEach(vo -> vo.setLevel("info"));
        return PageResult.of(list, total, currentPage, currentSize);
    }
}
