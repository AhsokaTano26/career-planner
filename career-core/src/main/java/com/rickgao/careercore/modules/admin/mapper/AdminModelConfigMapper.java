package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理端-模型配置 Mapper。 */
@Mapper
public interface AdminModelConfigMapper {

    List<ModelConfig> selectAll();

    ModelConfig findByKey(@Param("configKey") String configKey);

    int insert(ModelConfig config);

    int updateValue(@Param("configKey") String configKey,
                    @Param("configValue") String configValue,
                    @Param("updatedBy") String updatedBy);
}
