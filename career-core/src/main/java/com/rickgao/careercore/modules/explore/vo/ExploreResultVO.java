package com.rickgao.careercore.modules.explore.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rickgao.careercore.modules.assessment.vo.DimensionScoreVO;
import com.rickgao.careercore.modules.advisor.vo.ProfileSnapshotVO;
import com.rickgao.careercore.modules.recommendation.vo.RecRunVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生涯探索聚合结果：一次「出结果」返回 计分 + 画像 + 方向推荐。
 *
 * <p>answers 为空（只选路径）时 dimensionScores 为空列表，画像与推荐仍会生成。
 * portraitSource 标明画像来源供前端提示：EXPLORE 本次探索计分 / LEGACY 先前完成的旧问卷测评 /
 * PROFILE 档案估算（无任何测评记录）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExploreResultVO {

    private List<DimensionScoreVO> dimensionScores;
    private ProfileSnapshotVO portrait;
    private RecRunVO recommendation;
    private String portraitSource;
}
