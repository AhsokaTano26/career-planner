package com.rickgao.careercore.modules.student.service.impl;

import com.rickgao.careercore.common.audit.AuditLogWriter;
import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import com.rickgao.careercore.modules.student.dto.DeletionRequestDTO;
import com.rickgao.careercore.modules.student.dto.ExperienceRequest;
import com.rickgao.careercore.modules.student.dto.StudentProfileUpdateDTO;
import com.rickgao.careercore.modules.student.entity.DeletionRequest;
import com.rickgao.careercore.modules.student.entity.StudentExperience;
import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.mapper.DeletionRequestMapper;
import com.rickgao.careercore.modules.student.mapper.StudentExperienceMapper;
import com.rickgao.careercore.modules.student.mapper.StudentProfileMapper;
import com.rickgao.careercore.modules.student.service.CompletenessCalculator;
import com.rickgao.careercore.modules.student.service.StudentProfileService;
import com.rickgao.careercore.modules.student.vo.CompletenessDetailVO;
import com.rickgao.careercore.modules.student.vo.ExperienceVO;
import com.rickgao.careercore.modules.student.vo.StudentProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 学生档案应用服务实现。
 */
@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private static final String SORT_COLUMN = "created_at";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final StudentProfileMapper studentProfileMapper;
    private final StudentExperienceMapper studentExperienceMapper;
    private final DeletionRequestMapper deletionRequestMapper;
    private final SysUserMapper sysUserMapper;
    private final CompletenessCalculator completenessCalculator;
    private final IdGenerator idGenerator;
    private final AuditLogWriter auditLogWriter;

    public StudentProfileServiceImpl(StudentProfileMapper studentProfileMapper,
                                     StudentExperienceMapper studentExperienceMapper,
                                     DeletionRequestMapper deletionRequestMapper,
                                     SysUserMapper sysUserMapper,
                                     CompletenessCalculator completenessCalculator,
                                     IdGenerator idGenerator,
                                     AuditLogWriter auditLogWriter) {
        this.studentProfileMapper = studentProfileMapper;
        this.studentExperienceMapper = studentExperienceMapper;
        this.deletionRequestMapper = deletionRequestMapper;
        this.sysUserMapper = sysUserMapper;
        this.completenessCalculator = completenessCalculator;
        this.idGenerator = idGenerator;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public StudentProfileVO getMyProfile(String userId) {
        StudentProfile profile = getOrCreateProfile(userId);
        List<ExperienceVO> experiences = studentExperienceMapper.findAllByStudentId(userId).stream()
                .map(this::toExperienceVO)
                .toList();
        return toVO(profile, experiences);
    }

    @Override
    @Transactional
    public StudentProfileVO updateMyProfile(String userId, StudentProfileUpdateDTO dto) {
        StudentProfile profile = getOrCreateProfile(userId);
        // 分步保存:仅覆盖提交的字段,未提交的不覆盖
        if (dto.getBasic() != null) {
            profile.setBasic(dto.getBasic());
        }
        if (dto.getAcademic() != null) {
            profile.setAcademic(dto.getAcademic());
        }
        if (dto.getInterestPrefs() != null) {
            profile.setInterestPrefs(dto.getInterestPrefs());
        }
        if (dto.getAbilitySelf() != null) {
            profile.setAbilitySelf(dto.getAbilitySelf());
        }
        if (dto.getValues() != null) {
            profile.setValues(dto.getValues());
        }
        if (dto.getDevelopmentIntention() != null) {
            profile.setDevelopmentIntention(dto.getDevelopmentIntention());
        }
        if (dto.getConstraints() != null) {
            profile.setConstraints(dto.getConstraints());
        }
        profile.setCompleteness(completenessCalculator.calculate(profile).getScore());
        // Demo 精简点:直接以当前时间近似返回 updatedAt(数据库 ON UPDATE 时间未回填实体)
        profile.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.update(profile);
        return toVO(profile, studentExperienceMapper.findAllByStudentId(userId).stream()
                .map(this::toExperienceVO)
                .toList());
    }

    @Override
    public CompletenessDetailVO completeness(String userId) {
        return completenessCalculator.calculate(getOrCreateProfile(userId));
    }

    @Override
    public List<ExperienceVO> listExperiences(String userId, Integer page, Integer size, String sort) {
        int safePage = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        // sort 形如 -createdAt(desc) / createdAt(asc);仅支持 created_at,白名单化防注入
        String dir = "DESC";
        if (StringUtils.hasText(sort) && !sort.startsWith("-")) {
            dir = "ASC";
        }
        int offset = (safePage - 1) * safeSize;
        return studentExperienceMapper.findByStudentIdPaged(userId, SORT_COLUMN, dir, offset, safeSize).stream()
                .map(this::toExperienceVO)
                .toList();
    }

    @Override
    @Transactional
    public ExperienceVO createExperience(String userId, ExperienceRequest request) {
        validateExperienceType(request.getType());
        StudentExperience experience = new StudentExperience();
        experience.setId(idGenerator.experienceId());
        experience.setStudentId(userId);
        experience.setType(request.getType());
        experience.setTitle(request.getTitle());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());
        // Demo 精简点:附件上传接口暂未提供,attachment 直接作为 URL/ID 存储
        experience.setAttachmentUrl(request.getAttachment());
        studentExperienceMapper.insert(experience);
        return toExperienceVO(experience);
    }

    @Override
    @Transactional
    public ExperienceVO updateExperience(String userId, String experienceId, ExperienceRequest request) {
        validateExperienceType(request.getType());
        StudentExperience experience = studentExperienceMapper.findByIdAndStudent(experienceId, userId);
        if (experience == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "经历不存在或无权修改");
        }
        experience.setType(request.getType());
        experience.setTitle(request.getTitle());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());
        experience.setAttachmentUrl(request.getAttachment());
        studentExperienceMapper.update(experience);
        return toExperienceVO(experience);
    }

    @Override
    @Transactional
    public void deleteExperience(String userId, String experienceId) {
        int rows = studentExperienceMapper.softDelete(experienceId, userId);
        if (rows == 0) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "经历不存在或无权删除");
        }
    }

    @Override
    @Transactional
    public void requestDeletion(String userId, DeletionRequestDTO dto, String ip) {
        DeletionRequest request = new DeletionRequest();
        request.setId(idGenerator.deletionRequestId());
        request.setUserId(userId);
        request.setReason(dto.getReason());
        request.setStatus(CommonConstants.DELETION_STATUS_PENDING);
        deletionRequestMapper.insert(request);
        String reason = StringUtils.hasText(dto.getReason()) ? dto.getReason() : "";
        auditLogWriter.record(CommonConstants.AUDIT_DELETION_REQUEST, userId, "deletion_request", request.getId(),
                "申请删除本人信息,原因:" + reason, ip);
    }

    /** 获取档案,不存在则按用户信息创建空档案。 */
    private StudentProfile getOrCreateProfile(String userId) {
        StudentProfile profile = studentProfileMapper.findByUserId(userId);
        if (profile != null) {
            return profile;
        }
        SysUser user = sysUserMapper.findById(userId);
        StudentProfile created = new StudentProfile();
        created.setId(idGenerator.studentProfileId());
        created.setUserId(userId);
        created.setName(user != null ? user.getName() : null);
        created.setClassName(user != null ? user.getClassName() : null);
        created.setGrade(user != null ? user.getGrade() : null);
        created.setMajorCategory(user != null ? user.getMajorCategory() : null);
        created.setCompleteness(0);
        studentProfileMapper.insert(created);
        return created;
    }

    private StudentProfileVO toVO(StudentProfile profile, List<ExperienceVO> experiences) {
        return StudentProfileVO.builder()
                .userId(profile.getUserId())
                .name(profile.getName())
                .className(profile.getClassName())
                .grade(profile.getGrade())
                .majorCategory(profile.getMajorCategory())
                .basic(profile.getBasic())
                .academic(profile.getAcademic())
                .interestPrefs(profile.getInterestPrefs())
                .abilitySelf(profile.getAbilitySelf())
                .values(profile.getValues())
                .experiences(experiences)
                .developmentIntention(profile.getDevelopmentIntention())
                .constraints(profile.getConstraints())
                .completeness(completenessCalculator.calculate(profile).getScore())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private ExperienceVO toExperienceVO(StudentExperience experience) {
        return ExperienceVO.builder()
                .id(experience.getId())
                .type(experience.getType())
                .title(experience.getTitle())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .description(experience.getDescription())
                .attachmentUrl(experience.getAttachmentUrl())
                .build();
    }

    private void validateExperienceType(String type) {
        if (!Arrays.asList(CommonConstants.EXPERIENCE_TYPES).contains(type)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "经历类别不合法,应为:竞赛/项目/学生工作/志愿服务");
        }
    }
}
