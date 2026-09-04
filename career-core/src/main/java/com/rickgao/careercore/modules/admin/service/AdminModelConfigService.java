package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.modules.admin.dto.ModelConfigUpdateRequest;
import com.rickgao.careercore.modules.admin.vo.ModelConfigVO;

import java.util.List;

/** 管理端-模型配置业务。 */
public interface AdminModelConfigService {

    List<ModelConfigVO> listConfigs();

    ModelConfigVO updateConfig(String adminId, String configKey, ModelConfigUpdateRequest req);
}

