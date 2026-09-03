package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.ExportRequest;
import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.service.AdminExportService;
import com.rickgao.careercore.modules.admin.service.ExportFileGenerator;
import com.rickgao.careercore.modules.admin.vo.ExportJobVO;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 管理端-数据导出服务实现(异步生成文件,10 分钟失效)。 */
@Service
public class AdminExportServiceImpl implements AdminExportService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long EXPIRE_MINUTES = 30;
    private static final Set<String> TYPE_ENUM =
            Set.of("STUDENT_DATA", "WHITELIST", "OPERATION_LOG", "AI_LOG", "DIRECTION_LIB");

    private final AdminExportMapper exportMapper;
    private final SysUserMapper sysUserMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;
    private final ExportFileGenerator exportFileGenerator;

    public AdminExportServiceImpl(AdminExportMapper exportMapper,
                                  SysUserMapper sysUserMapper,
                                  IdGenerator idGenerator,
                                  IdempotencyService idempotencyService,
                                  ExportFileGenerator exportFileGenerator) {
        this.exportMapper = exportMapper;
        this.sysUserMapper = sysUserMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
        this.exportFileGenerator = exportFileGenerator;
    }

    @Override
    public ExportJobVO createExport(String operatorId, String endpoint, String idempotencyKey,
                                    ExportRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, ExportJobVO.class,
                () -> ApiResponse.ok(doCreateExport(operatorId, request))).getData();
    }

    @Override
    public PageResult<ExportJobVO> listExports(Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        boolean desc = !StringUtils.hasText(sort) || sort.startsWith("-");
        long total = exportMapper.countExportJobs();
        List<ExportJobVO> list = exportMapper.selectExportJobPage(
                "created_at", desc ? "DESC" : "ASC",
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    public ExportJobVO getJob(String jobId) {
        ExportJob job = exportMapper.findExportJobById(jobId);
        if (job == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导出任务不存在");
        }
        SysUser operator = sysUserMapper.findById(job.getOperatorId());
        ExportJobVO vo = new ExportJobVO();
        vo.setId(job.getId());
        vo.setType(job.getType());
        vo.setScope(job.getScope());
        vo.setStatus(job.getStatus());
        vo.setDownloadUrl(job.getDownloadUrl());
        vo.setCreatedAt(job.getCreatedAt());
        vo.setOperator(operator == null ? job.getOperatorId() : operator.getName());
        return vo;
    }

    @Override
    public DownloadFile download(String jobId) {
        ExportJob job = exportMapper.findExportJobById(jobId);
        if (job == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导出任务不存在");
        }
        if (!"DONE".equals(job.getStatus()) || !StringUtils.hasText(job.getFilePath())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导出尚未完成或已失败");
        }
        if (job.getUpdatedAt() != null
                && job.getUpdatedAt().plusMinutes(EXPIRE_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导出文件已过期(30 分钟有效)");
        }
        if (!Files.exists(Paths.get(job.getFilePath()))) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导出文件不存在");
        }
        // TODO(审计): 下载访问记入审计日志,由审计模块队友接入
        return new DownloadFile(job.getFilePath(), job.getId() + "_" + job.getType() + ".csv");
    }

    private ExportJobVO doCreateExport(String operatorId, ExportRequest request) {
        if (!TYPE_ENUM.contains(request.getType())) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "type 仅支持 STUDENT_DATA/WHITELIST/OPERATION_LOG/AI_LOG/DIRECTION_LIB");
        }
        ExportJob job = new ExportJob();
        job.setId(idGenerator.exportJobId());
        job.setType(request.getType());
        job.setScope(StringUtils.hasText(request.getScope()) ? request.getScope().trim() : null);
        job.setFiltersJson(request.getFilters() == null ? null : JsonUtil.toJson(request.getFilters()));
        job.setStatus("PENDING");
        job.setOperatorId(operatorId);
        exportMapper.insertExportJob(job);
        exportFileGenerator.generate(job.getId());
        // TODO(审计): 导出范围/操作者/时间写入审计日志,由审计模块队友接入
        SysUser operator = sysUserMapper.findById(operatorId);
        ExportJobVO vo = new ExportJobVO();
        vo.setId(job.getId());
        vo.setType(job.getType());
        vo.setScope(job.getScope());
        vo.setStatus("PENDING");
        vo.setCreatedAt(job.getCreatedAt());
        vo.setOperator(operator == null ? operatorId : operator.getName());
        return vo;
    }
}
