package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.ModelConfigUpdateRequest;
import com.rickgao.careercore.modules.admin.entity.ModelConfig;
import com.rickgao.careercore.modules.admin.mapper.AdminModelConfigMapper;
import com.rickgao.careercore.modules.admin.service.AdminModelConfigService;
import com.rickgao.careercore.modules.admin.vo.ModelConfigVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 管理端-模型配置业务实现。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：敏感值（key/secret/token）在列表接口掩码展示，更新时由管理员输入新值覆盖。
 */
@Service
public class AdminModelConfigServiceImpl implements AdminModelConfigService {

    private final AdminModelConfigMapper mapper;
    private final IdGenerator idGenerator;

    public AdminModelConfigServiceImpl(AdminModelConfigMapper mapper, IdGenerator idGenerator) {
        this.mapper = mapper;
        this.idGenerator = idGenerator;
    }

    @Override
    public List<ModelConfigVO> listConfigs() {
        return mapper.selectAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ModelConfigVO updateConfig(String adminId, String configKey, ModelConfigUpdateRequest req) {
        ModelConfig existing = mapper.findByKey(configKey);
        if (existing == null) {
            existing = new ModelConfig();
            existing.setId(idGenerator.modelConfigId());
            existing.setConfigKey(configKey);
            existing.setConfigValue(req.getConfigValue());
            existing.setUpdatedBy(adminId);
            mapper.insert(existing);
        } else {
            mapper.updateValue(configKey, req.getConfigValue(), adminId);
            existing.setConfigValue(req.getConfigValue());
            existing.setUpdatedBy(adminId);
        }
        return toVO(existing);
    }

    private ModelConfigVO toVO(ModelConfig c) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setConfigKey(c.getConfigKey());
        vo.setConfigValue(maskIfSensitive(c.getConfigKey(), c.getConfigValue()));
        vo.setMasked(isSensitive(c.getConfigKey()));
        vo.setUpdatedBy(c.getUpdatedBy());
        vo.setUpdatedAt(c.getUpdatedAt());
        return vo;
    }

    private boolean isSensitive(String key) {
        String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return k.contains("key") || k.contains("secret") || k.contains("token") || k.contains("password");
    }

    private String maskIfSensitive(String key, String value) {
        if (!isSensitive(key) || value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
