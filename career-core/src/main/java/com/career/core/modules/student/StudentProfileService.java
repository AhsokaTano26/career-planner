package com.career.core.modules.student;

import com.career.core.common.BadRequestException;
import com.career.core.common.Constants;
import com.career.core.common.NotFoundException;
import com.career.core.modules.profile.ProfileCalculationInput;
import com.career.core.modules.profile.ProfileCalculationResult;
import com.career.core.modules.profile.ProfileCalculator;
import com.career.core.modules.profile.ProfileDimension;
import com.career.core.modules.profile.ProfileEvidence;
import com.career.core.modules.profile.ProfileFeedbackRequest;
import com.career.core.modules.profile.ProfileFeedbackType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生画像服务：公开接口、版本快照与反馈均由 career-core 负责。
 *
 * <p>画像分数只由 {@link ProfileCalculator} 的确定性规则产生，不调用大模型。
 * Demo 尚未实现 assessment_score 表的完整链路，因此 refresh 暂以最近一次快照中的
 * 结构化分数作为重算输入；后续问卷提交应直接调用
 * {@link #calculateAndSave(Long, ProfileCalculationInput, String, String)}。</p>
 */
@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentProfileDao dao;
    private final ObjectMapper objectMapper;
    private final ProfileCalculator calculator;

    public StudentProfileService(
            StudentProfileDao dao,
            ObjectMapper objectMapper,
            ProfileCalculator calculator) {
        this.dao = dao;
        this.objectMapper = objectMapper;
        this.calculator = calculator;
    }

    /** 兼容旧接口名称。 */
    public Map<String, Object> getProfile(Long studentId) {
        return getLatestProfile(studentId);
    }

    /** 查询最新画像；学生不存在或尚无画像时保持 Demo 约定返回空对象。 */
    public Map<String, Object> getLatestProfile(Long studentId) {
        StudentProfile student = dao.findStudentByUserId(studentId);
        if (student == null) {
            return Collections.emptyMap();
        }
        ProfileSnapshot snapshot = dao.findLatestSnapshot(studentId);
        return snapshot == null ? Collections.emptyMap() : toDetail(snapshot, true);
    }

    /** 分页查询历史版本，page 从 1 开始，size 最大 100。 */
    public Map<String, Object> getProfileVersions(Long studentId, int page, int size) {
        validatePage(page, size);
        if (dao.findStudentByUserId(studentId) == null) {
            return Map.of("items", List.of(), "page", page, "size", size, "total", 0);
        }
        int offset = (page - 1) * size;
        List<Map<String, Object>> items = dao.findSnapshotVersions(studentId, offset, size)
                .stream()
                .map(this::toVersionSummary)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", dao.countSnapshots(studentId));
        return result;
    }

    /** 查询指定快照，并校验快照属于当前学生。 */
    public Map<String, Object> getProfileSnapshot(Long snapshotId, Long studentId) {
        ProfileSnapshot snapshot = dao.findSnapshotById(snapshotId, studentId);
        if (snapshot == null) {
            throw new NotFoundException("画像快照不存在");
        }
        return toDetail(snapshot, true);
    }

    /**
     * 根据最近一次可追溯分数重算画像并新增版本。
     *
     * <p>Demo 精简点 / 后续迭代替换位置：assessment 模块完成后，应读取最新
     * assessment_score 与学生资料，而不是把上一快照作为输入。</p>
     */
    @Transactional
    public Map<String, Object> refreshProfile(Long studentId) {
        if (dao.findStudentByUserId(studentId) == null) {
            throw new NotFoundException("学生档案不存在");
        }
        ProfileSnapshot latest = dao.findLatestSnapshot(studentId);
        if (latest == null || latest.dimensionJson() == null || latest.dimensionJson().isBlank()) {
            throw new BadRequestException("暂无可用于生成画像的测评得分");
        }
        Map<String, Object> raw = readRawDimensions(latest.dimensionJson());
        ProfileCalculationInput input = toCalculationInput(raw);
        String personality = raw.get(Constants.PROFILE_PERSONALITY_KEY) == null
                ? null
                : raw.get(Constants.PROFILE_PERSONALITY_KEY).toString();
        ProfileSnapshot created = createSnapshot(
                studentId,
                input,
                ProfileCalculator.RULE_VERSION,
                personality);
        return toDetail(created, true);
    }

    /**
     * assessment 模块的正式接入点：提交并计分成功后，在同一业务流程中新增画像快照。
     */
    @Transactional
    public Map<String, Object> calculateAndSave(
            Long studentId,
            ProfileCalculationInput input,
            String sourceVersion,
            String personality) {
        if (dao.findStudentByUserId(studentId) == null) {
            throw new NotFoundException("学生档案不存在");
        }
        return toDetail(createSnapshot(studentId, input, sourceVersion, personality), true);
    }

    /** 保存画像反馈；反馈不改变任何画像分数。 */
    @Transactional
    public void addFeedback(Long snapshotId, Long studentId, ProfileFeedbackRequest request) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        ProfileSnapshot snapshot = dao.findSnapshotById(snapshotId, studentId);
        if (snapshot == null) {
            throw new NotFoundException("画像快照不存在");
        }
        ProfileFeedbackType feedbackType = ProfileFeedbackType.parse(request.feedbackType());
        String comment = normalizeComment(request.comment());
        dao.insertProfileFeedback(snapshot.id(), studentId, feedbackType.name(), comment);
    }

    private ProfileSnapshot createSnapshot(
            Long studentId,
            ProfileCalculationInput input,
            String sourceVersion,
            String personality) {
        ProfileCalculationResult result = calculator.calculate(input);
        String dimensionJson = writeDimensionJson(result, personality);
        int versionNo = dao.nextSnapshotVersion(studentId);
        long snapshotId = dao.insertSnapshot(
                studentId,
                sourceVersion == null || sourceVersion.isBlank()
                        ? ProfileCalculator.RULE_VERSION
                        : sourceVersion,
                dimensionJson,
                result.summary(),
                versionNo,
                result.completeness());
        ProfileSnapshot snapshot = dao.findSnapshotById(snapshotId, studentId);
        if (snapshot == null) {
            throw new IllegalStateException("画像快照创建成功但无法读取");
        }
        return snapshot;
    }

    private Map<String, Object> toDetail(ProfileSnapshot snapshot, boolean includeExperiences) {
        Map<String, Object> raw = readRawDimensions(snapshot.dimensionJson());
        ProfileCalculationResult normalized = calculator.calculate(
                toCalculationInput(raw));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("snapshotId", snapshot.id());
        data.put("studentId", snapshot.studentId());
        data.put("versionNo", snapshot.versionNo());
        data.put("sourceVersion", snapshot.sourceVersion());
        data.put("dimensions", normalized.dimensions());
        data.put("summary", snapshot.summary() == null || snapshot.summary().isBlank()
                ? normalized.summary()
                : snapshot.summary());
        data.put("strengths", normalized.strengths());
        data.put("explorationQuestions", normalized.explorationQuestions());
        data.put("completeness", snapshot.completeness() == null
                ? normalized.completeness()
                : snapshot.completeness());
        data.put("generatedAt", snapshot.generatedAt());
        Object personality = raw.get(Constants.PROFILE_PERSONALITY_KEY);
        if (personality != null) {
            data.put("personality", personality);
        }
        if (includeExperiences) {
            data.put("experiences", buildExperiences(dao.findExperiences(snapshot.studentId())));
        }
        return data;
    }

    private Map<String, Object> toVersionSummary(ProfileSnapshot snapshot) {
        Map<String, Object> detail = toDetail(snapshot, false);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("snapshotId", detail.get("snapshotId"));
        summary.put("versionNo", detail.get("versionNo"));
        summary.put("sourceVersion", detail.get("sourceVersion"));
        summary.put("summary", detail.get("summary"));
        summary.put("completeness", detail.get("completeness"));
        summary.put("generatedAt", detail.get("generatedAt"));
        return summary;
    }

    private ProfileCalculationInput toCalculationInput(Map<String, Object> raw) {
        EnumMap<ProfileDimension, List<ProfileEvidence>> evidence = new EnumMap<>(ProfileDimension.class);
        int availableCount = 0;
        for (ProfileDimension dimension : ProfileDimension.values()) {
            Double score = findScore(raw, dimension);
            if (score != null) {
                evidence.put(dimension, List.of(
                        new ProfileEvidence("PROFILE_SNAPSHOT", score, 1.0)));
                availableCount++;
            }
        }
        if (availableCount == 0) {
            throw new BadRequestException("画像快照中没有可计算的六维分数");
        }

        return new ProfileCalculationInput(
                evidence,
                availableCount,
                ProfileDimension.values().length);
    }

    private Double findScore(Map<String, Object> raw, ProfileDimension dimension) {
        Object value = raw.get(dimension.canonicalCode());
        if (value == null) {
            value = raw.get(dimension.legacyCode());
        }
        if (value instanceof Number number) {
            return validScore(number.doubleValue());
        }
        if (value instanceof Map<?, ?> map) {
            Object score = map.get("normalizedScore");
            if (!(score instanceof Number)) {
                score = map.get("rawScore");
            }
            if (score instanceof Number number) {
                return validScore(number.doubleValue());
            }
        }
        return null;
    }

    private Double validScore(double score) {
        return Double.isFinite(score) && score >= 0 && score <= 100 ? score : null;
    }

    private Map<String, Object> readRawDimensions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception exception) {
            log.warn("解析画像快照 dimension_json 失败", exception);
            throw new BadRequestException("画像快照数据格式无效");
        }
    }

    private String writeDimensionJson(ProfileCalculationResult result, String personality) {
        Map<String, Object> value = new LinkedHashMap<>(result.dimensions());
        if (personality != null && !personality.isBlank()) {
            value.put(Constants.PROFILE_PERSONALITY_KEY, personality.trim());
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("画像计算结果序列化失败", exception);
        }
    }

    private List<Map<String, Object>> buildExperiences(List<StudentExperience> experiences) {
        return experiences.stream().map(experience -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", experience.type());
            value.put("title", experience.title());
            value.put("startDate", experience.startDate() == null
                    ? null
                    : experience.startDate().toString());
            value.put("description", experience.description());
            return value;
        }).toList();
    }

    private void validatePage(int page, int size) {
        if (page < 1) {
            throw new BadRequestException("page 必须大于等于 1");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("size 必须在 1-100 之间");
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String normalized = comment.trim();
        if (normalized.length() > 500) {
            throw new BadRequestException("comment 最多 500 个字符");
        }
        return normalized;
    }
}
