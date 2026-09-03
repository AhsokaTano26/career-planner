package com.rickgao.careercore.modules.recommendation.mapper;

import com.rickgao.careercore.modules.recommendation.entity.RecommendationResult;
import com.rickgao.careercore.modules.recommendation.entity.RecommendationRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 推荐模块映射器（批次 + 结果）。
 */
@Mapper
public interface RecommendationMapper {

    int insertRun(RecommendationRun run);

    RecommendationRun findRunById(@Param("id") String id);

    RecommendationRun findLatestRunByStudent(@Param("studentId") String studentId);

    List<RecommendationRun> listRunsByStudent(@Param("studentId") String studentId,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    long countRunsByStudent(@Param("studentId") String studentId);

    int insertResult(RecommendationResult result);

    List<RecommendationResult> listResultsByRun(@Param("runId") String runId);

    RecommendationResult findResultById(@Param("id") String id);

    int updateResultFeedback(@Param("id") String id, @Param("feedbackJson") String feedbackJson);
}
