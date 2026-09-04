package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 调用日志 Mapper（Phase 2：career-core AI 模块也写）。
 */
@Mapper
public interface AiCallLogMapper {

    void insert(AiCallLog log);
}

