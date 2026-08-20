package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.modules.admin.dto.WhitelistCreate;
import com.rickgao.careercore.modules.admin.vo.WhitelistEntryVO;
import com.rickgao.careercore.modules.admin.vo.WhitelistImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端-白名单应用服务。
 */
public interface AdminWhitelistService {

    PageResult<WhitelistEntryVO> listWhitelist(Boolean used, String keyword,
                                               Integer page, Integer size, String sort);

    WhitelistEntryVO createWhitelist(String operatorId, String endpoint, String idempotencyKey, WhitelistCreate dto);

    WhitelistImportResultVO importWhitelist(String operatorId, String endpoint, String idempotencyKey,
                                            MultipartFile file);

    void deleteWhitelist(String operatorId, String endpoint, String idempotencyKey, String whitelistId);
}
