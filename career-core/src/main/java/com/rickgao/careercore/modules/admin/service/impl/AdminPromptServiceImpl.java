package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.PromptVersionRequest;
import com.rickgao.careercore.modules.admin.entity.PromptVersion;
import com.rickgao.careercore.modules.admin.mapper.AdminPromptMapper;
import com.rickgao.careercore.modules.admin.service.AdminPromptService;
import com.rickgao.careercore.modules.admin.vo.PromptVersionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端-提示词版本业务实现。
 */
@Service
public class AdminPromptServiceImpl implements AdminPromptService {

    private static final List<String> DEFAULT_SCENES = List.of(
            "recommendation_explain", "plan_generate", "review_summarize", "career_chat");

    private final AdminPromptMapper mapper;
    private final IdGenerator idGenerator;

    public AdminPromptServiceImpl(AdminPromptMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<String> listScenes() {
        Set<String> scenes = new LinkedHashSet<>(DEFAULT_SCENES);
        for (PromptVersion v : mapper.listByScene(null)) {
            scenes.add(v.getScene());
        }
        return new ArrayList<>(scenes);
    }

    @Override
    public List<PromptVersionVO> listVersions(String scene) {
        return mapper.listByScene(scene).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromptVersionVO createVersion(String adminId, PromptVersionRequest req) {
        boolean exists = mapper.listByScene(req.getScene()).stream()
                .anyMatch(v -> req.getVersion().equals(v.getVersion()));
        if (exists) {
            throw new BizException(ResultCode.STATE_CONFLICT, "该场景下已存在同名版本：" + req.getVersion());
        }
        PromptVersion v = new PromptVersion();
        v.setId(idGenerator.promptVersionId());
        v.setScene(req.getScene());
        v.setVersion(req.getVersion());
        v.setStatus("DRAFT");
        v.setContent(req.getContent());
        v.setCreatedAt(LocalDateTime.now());
        mapper.insert(v);
        return toVO(v);
    }

    @Override
    @Transactional
    public PromptVersionVO publishVersion(String adminId, String promptId) {
        PromptVersion v = mapper.findById(promptId);
        if (v == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "提示词版本不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        mapper.updatePublish(promptId, "PUBLISHED", now, adminId);
        v.setStatus("PUBLISHED");
        v.setPublishedAt(now);
        v.setPublishedBy(adminId);
        return toVO(v);
    }

    private PromptVersionVO toVO(PromptVersion v) {
        PromptVersionVO vo = new PromptVersionVO();
        vo.setId(v.getId());
        vo.setScene(v.getScene());
        vo.setVersion(v.getVersion());
        vo.setStatus(v.getStatus());
        vo.setContent(v.getContent());
        vo.setPublishedAt(v.getPublishedAt());
        vo.setPublishedBy(v.getPublishedBy());
        return vo;
    }
}

