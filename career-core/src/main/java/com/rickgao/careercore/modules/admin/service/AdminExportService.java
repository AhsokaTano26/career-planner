package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.ExportRequest;
import com.rickgao.careercore.modules.admin.vo.ExportJobVO;

/** 管理端-数据导出应用服务。 */
public interface AdminExportService {

    ExportJobVO createExport(String operatorId, String endpoint, String idempotencyKey, ExportRequest request);

    PageResult<ExportJobVO> listExports(Integer page, Integer size, String sort);

    ExportJobVO getJob(String jobId);

    DownloadFile download(String jobId);

    record DownloadFile(String path, String filename) {
    }
}
