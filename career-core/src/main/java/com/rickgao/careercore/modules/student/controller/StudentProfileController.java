package com.rickgao.careercore.modules.student.controller;

import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.util.IpUtil;
import com.rickgao.careercore.modules.student.dto.DeletionRequestDTO;
import com.rickgao.careercore.modules.student.dto.ExperienceRequest;
import com.rickgao.careercore.modules.student.dto.StudentProfileUpdateDTO;
import com.rickgao.careercore.modules.student.service.StudentProfileService;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO;
import com.rickgao.careercore.modules.student.vo.ExperienceVO;
import com.rickgao.careercore.modules.student.vo.StudentProfileVO;
import com.rickgao.careercore.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生档案模块接口(8 个)。
 * 依据 Apifox 接口文档:/api/v1/students/me/*
 */
@RestController
@RequestMapping("/api/v1/students/me")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    public StudentProfileController(StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    /** 获取我的档案 */
    @GetMapping
    public ApiResponse<StudentProfileVO> getMyProfile() {
        return ApiResponse.ok(studentProfileService.getMyProfile(SecurityUtils.currentUserId()));
    }

    /** 分步保存学生资料 */
    @PatchMapping
    public ApiResponse<StudentProfileVO> updateMyProfile(@Valid @RequestBody StudentProfileUpdateDTO dto) {
        return ApiResponse.ok(studentProfileService.updateMyProfile(SecurityUtils.currentUserId(), dto));
    }

    /** 资料完整度明细 */
    @GetMapping("/completeness")
    public ApiResponse<CompletenessDetailVO> completeness() {
        return ApiResponse.ok(studentProfileService.completeness(SecurityUtils.currentUserId()));
    }

    /** 经历列表(分页) */
    @GetMapping("/experiences")
    public ApiResponse<List<ExperienceVO>> listExperiences(@RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) String sort) {
        return ApiResponse.ok(studentProfileService.listExperiences(SecurityUtils.currentUserId(), page, size, sort));
    }

    /** 新增经历 */
    @PostMapping("/experiences")
    public ApiResponse<ExperienceVO> createExperience(@Valid @RequestBody ExperienceRequest request) {
        return ApiResponse.ok(studentProfileService.createExperience(SecurityUtils.currentUserId(), request));
    }

    /** 修改经历 */
    @PatchMapping("/experiences/{experienceId}")
    public ApiResponse<ExperienceVO> updateExperience(@PathVariable String experienceId,
                                                      @Valid @RequestBody ExperienceRequest request) {
        return ApiResponse.ok(studentProfileService.updateExperience(SecurityUtils.currentUserId(), experienceId, request));
    }

    /** 删除经历(软删除) */
    @DeleteMapping("/experiences/{experienceId}")
    public ApiResponse<Void> deleteExperience(@PathVariable String experienceId) {
        studentProfileService.deleteExperience(SecurityUtils.currentUserId(), experienceId);
        return ApiResponse.ok();
    }

    /** 申请删除本人信息 */
    @PostMapping("/deletion-request")
    public ApiResponse<Void> requestDeletion(@Valid @RequestBody DeletionRequestDTO request, HttpServletRequest httpRequest) {
        studentProfileService.requestDeletion(SecurityUtils.currentUserId(), request, IpUtil.getClientIp(httpRequest));
        return ApiResponse.ok();
    }
}
