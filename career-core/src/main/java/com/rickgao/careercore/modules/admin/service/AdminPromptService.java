package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.modules.admin.dto.PromptVersionRequest;
import com.rickgao.careercore.modules.admin.vo.PromptVersionVO;

import java.util.List;

/** 管理端-提示词版本业务。 */
public interface AdminPromptService {

    List<String> listScenes();

    List<PromptVersionVO> listVersions(String scene);

    PromptVersionVO createVersion(String adminId, PromptVersionRequest req);

    PromptVersionVO publishVersion(String adminId, String promptId);
}
