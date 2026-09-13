package com.rickgao.careercore.modules.explore.dto;

import com.rickgao.careercore.modules.assessment.dto.AnswerItem;
import lombok.Data;

import java.util.List;

/**
 * 生涯探索提交请求（点选采集层）。
 *
 * <p>path：graduate/employment/overseas/undecided，可选；
 * answers：最多 3 个兴趣标签答案（EXPLORE 问卷 TAG 区题目）。
 */
@Data
public class ExploreRequest {

    private String path;

    private List<AnswerItem> answers;
}
