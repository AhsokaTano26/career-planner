package com.rickgao.careercore.modules.advisor.vo;

import com.rickgao.careercore.modules.student.vo.StudentProfileVO;
import lombok.Data;

import java.util.List;

/**
 * 学生详情总览(只读时间线)。对齐 openapi StudentDetailView。
 */
@Data
public class StudentDetailViewVO {

    private StudentProfileVO profile;
    private ProfileSnapshotVO portrait;
    private RecommendationRunVO recommendation;
    private GoalVO goal;
    private PlanVO plan;
    private List<TaskVO> tasks;
    private List<ReviewVO> reviews;
    private List<GuidanceCommentVO> guidance;
}
