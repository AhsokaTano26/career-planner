package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.RecommendationWeight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 管理端-推荐权重 Mapper。 */
@Mapper
public interface AdminWeightMapper {

    RecommendationWeight findLatestPublished();

    RecommendationWeight findByVersion(@Param("version") String version);

    int insert(RecommendationWeight weight);
}
