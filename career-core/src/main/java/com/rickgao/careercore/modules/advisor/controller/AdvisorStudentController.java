package com.rickgao.careercore.modules.advisor.controller;

import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.modules.advisor.dto.StudentListQuery;
import com.rickgao.careercore.modules.advisor.service.AdvisorStudentService;
import com.rickgao.careercore.modules.advisor.vo.AdvisorStudentVO;
import com.rickgao.careercore.modules.advisor.vo.AttentionStudentVO;
import com.rickgao.careercore.modules.advisor.vo.StudentDetailViewVO;
import com.rickgao.careercore.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 辅导员端-学生查询接口。
 * openapi:GET /api/v1/advisor/students、/students/{studentId}、/attention。
 */
@RestController
@RequestMapping("/api/v1/advisor")
@PreAuthorize("hasRole('ADVISOR')")
public class AdvisorStudentController {

    private final AdvisorStudentService advisorStudentService;

    public AdvisorStudentController(AdvisorStudentService advisorStudentService) {
        this.advisorStudentService = advisorStudentService;
    }

    /** 所带学生列表(组合筛选 + 分页 + 排序) */
    @GetMapping("/students")
    public ApiResponse<PageResult<AdvisorStudentVO>> listStudents(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String directionId,
            @RequestParam(required = false) String goalStatus,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) Boolean guidanceRequested,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        StudentListQuery query = new StudentListQuery();
        query.setPath(path);
        query.setDirectionId(directionId);
        query.setGoalStatus(goalStatus);
        query.setReviewStatus(reviewStatus);
        query.setGuidanceRequested(guidanceRequested);
        query.setKeyword(keyword);
        query.setPage(page);
        query.setSize(size);
        query.setSort(sort);
        return ApiResponse.ok(advisorStudentService.listStudents(SecurityUtils.currentUserId(), query));
    }

    /** 学生详情总览(只读时间线) */
    @GetMapping("/students/{studentId}")
    public ApiResponse<StudentDetailViewVO> getDetail(@PathVariable String studentId) {
        return ApiResponse.ok(advisorStudentService.getDetail(SecurityUtils.currentUserId(), studentId));
    }

    /** 需关注学生(申请指导 / 长期未复盘 / 多次调整目标) */
    @GetMapping("/attention")
    public ApiResponse<List<AttentionStudentVO>> listAttention() {
        return ApiResponse.ok(advisorStudentService.listAttention(SecurityUtils.currentUserId()));
    }
}
