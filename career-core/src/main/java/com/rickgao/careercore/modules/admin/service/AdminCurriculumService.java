package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.BatchReviewRequest;
import com.rickgao.careercore.modules.admin.dto.CurriculumPublishRequest;
import com.rickgao.careercore.modules.admin.dto.ImportItemUpdate;
import com.rickgao.careercore.modules.admin.vo.CurriculumImportJobVO;
import com.rickgao.careercore.modules.admin.vo.CurriculumVersionVO;
import com.rickgao.careercore.modules.admin.vo.ImportItemVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 管理端-培养方案应用服务。 */
public interface AdminCurriculumService {

    CurriculumImportJobVO importFile(String operatorId, String endpoint, String idempotencyKey,
                                     MultipartFile file);

    PageResult<CurriculumImportJobVO> listJobs(Integer page, Integer size, String sort);

    CurriculumImportJobVO getJob(String jobId);

    PageResult<ImportItemVO> listItems(String jobId, String status,
                                       Integer page, Integer size, String sort);

    ImportItemVO reviewItem(String operatorId, String endpoint, String idempotencyKey,
                            String itemId, ImportItemUpdate update);

    List<ImportItemVO> batchReview(String operatorId, String endpoint, String idempotencyKey,
                                   BatchReviewRequest request);

    CurriculumVersionVO publish(String operatorId, String endpoint, String idempotencyKey,
                                CurriculumPublishRequest request);

    PageResult<CurriculumVersionVO> listVersions(Integer page, Integer size, String sort);
}
