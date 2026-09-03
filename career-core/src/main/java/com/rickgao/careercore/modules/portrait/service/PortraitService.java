package com.rickgao.careercore.modules.portrait.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.advisor.vo.ProfileSnapshotVO;
import com.rickgao.careercore.modules.assessment.entity.AssessmentSession;
import com.rickgao.careercore.modules.assessment.mapper.AssessmentMapper;
import com.rickgao.careercore.modules.portrait.dto.ProfileFeedbackRequest;
import com.rickgao.careercore.modules.portrait.entity.ProfileSnapshot;
import com.rickgao.careercore.modules.portrait.mapper.ProfileSnapshotMapper;
import com.rickgao.careercore.modules.student.entity.StudentProfile;
import com.rickgao.careercore.modules.student.mapper.StudentProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 画像模块：基于测评得分与档案生成画像快照。
 *
 * <p>复用 advisor 的 ProfileSnapshotVO（保证学生端画像与辅导员端详情结构一致）。
 * Demo 精简点 / 后续迭代替换位置：
 *  - 画像由规则生成（最高/最低维度 → strengths/explore/summary），未调大模型；
 *  - 无测评时用档案基础信息估算六维。
 */
@Service
public class PortraitService {

    private static final String[] DIMENSIONS = {"interest", "values", "ability", "academic", "tendency", "practice"};
    private static final Map<String, String> DIM_NAMES = Map.of(
            "interest", "兴趣", "values", "价值观", "ability", "能力",
            "academic", "学业", "tendency", "倾向", "practice", "实践");

    private final ProfileSnapshotMapper snapshotMapper;
    private final AssessmentMapper assessmentMapper;
    private final StudentProfileMapper profileMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public PortraitService(ProfileSnapshotMapper snapshotMapper, AssessmentMapper assessmentMapper,
                           StudentProfileMapper profileMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.snapshotMapper = snapshotMapper;
        this.assessmentMapper = assessmentMapper;
        this.profileMapper = profileMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProfileSnapshotVO refresh(String studentId) {
        AssessmentSession scored = assessmentMapper.findLatestScoredByStudent(studentId);
        Map<String, Double> dims = new LinkedHashMap<>();
        if (scored != null && scored.getScoreJson() != null) {
            dims = parseDimensions(scored.getScoreJson());
        } else {
            dims = estimateFromProfile(studentId);
        }
        // 统一画像维度到 0-100（测评原始为 1-5 分制，×20 归一化；档案估算已为 0-100）
        dims.replaceAll((k, v) -> v != null && v <= 5.0 ? Math.min(100.0, Math.round(v * 20.0 * 10.0) / 10.0) : v);

        ProfileSnapshot latest = snapshotMapper.findLatestByStudent(studentId);
        int nextVersion = (latest == null ? 0 : (latest.getVersionNo() == null ? 0 : latest.getVersionNo())) + 1;

        List<ProfileSnapshotVO.DimensionValue> dimVOs = dims.entrySet().stream().map(e -> {
            ProfileSnapshotVO.DimensionValue v = new ProfileSnapshotVO.DimensionValue();
            v.setKey(e.getKey());
            v.setName(DIM_NAMES.getOrDefault(e.getKey(), e.getKey()));
            v.setScore(e.getValue());
            return v;
        }).collect(Collectors.toList());
        List<String> strengths = topDims(dims, 2, true);
        List<String> explore = topDims(dims, 2, false);
        String summary = buildSummary(dims);

        ProfileSnapshot snap = new ProfileSnapshot();
        snap.setId(idGenerator.profileSnapshotId());
        snap.setStudentId(studentId);
        snap.setSourceVersion(scored == null ? "profile" : "assessment:" + scored.getId());
        snap.setDimensionJson(writeJson(dimVOs));
        snap.setSummary(summary);
        snap.setStrengthsJson(writeJson(strengths));
        snap.setExploreJson(writeJson(explore));
        snap.setFeedbackJson(null);
        snap.setVersionNo(nextVersion);
        snap.setCompleteness(computeCompleteness(studentId));
        snap.setCreatedAt(java.time.LocalDateTime.now());
        snapshotMapper.insert(snap);
        return toVO(snap);
    }

    public ProfileSnapshotVO getLatest(String studentId) {
        ProfileSnapshot snap = snapshotMapper.findLatestByStudent(studentId);
        if (snap == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "暂无画像，请先完成测评或刷新生成");
        }
        return toVO(snap);
    }

    public List<ProfileSnapshotVO> listVersions(String studentId) {
        return snapshotMapper.listByStudent(studentId).stream().map(this::toVO).collect(Collectors.toList());
    }

    public ProfileSnapshotVO getSnapshot(String snapshotId) {
        ProfileSnapshot snap = snapshotMapper.findById(snapshotId);
        if (snap == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "画像快照不存在");
        }
        return toVO(snap);
    }

    @Transactional
    public ProfileSnapshotVO addFeedback(String snapshotId, ProfileFeedbackRequest req) {
        ProfileSnapshot snap = snapshotMapper.findById(snapshotId);
        if (snap == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "画像快照不存在");
        }
        ProfileSnapshotVO.ProfileFeedback fb = new ProfileSnapshotVO.ProfileFeedback();
        fb.setFeedbackType(req.getFeedbackType());
        fb.setComment(req.getComment());
        snapshotMapper.updateFeedback(snapshotId, writeJson(fb));
        snap.setFeedbackJson(writeJson(fb));
        return toVO(snap);
    }

    // ---------------------------------------------------------------- 内部

    private ProfileSnapshotVO toVO(ProfileSnapshot snap) {
        ProfileSnapshotVO vo = new ProfileSnapshotVO();
        vo.setId(snap.getId());
        vo.setVersion(snap.getVersionNo());
        vo.setGeneratedAt(snap.getCreatedAt());
        vo.setSourceVersion(snap.getSourceVersion());
        vo.setCompleteness(snap.getCompleteness());
        vo.setDimensions(parseDimVOs(snap.getDimensionJson()));
        vo.setSummary(snap.getSummary());
        vo.setStrengths(parseStrList(snap.getStrengthsJson()));
        vo.setExplore(parseStrList(snap.getExploreJson()));
        vo.setFeedback(parseFeedback(snap.getFeedbackJson()));
        return vo;
    }

    private Map<String, Double> parseDimensions(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.put(n.path("dimensionCode").asText(), n.path("score").asDouble());
                }
            }
        } catch (Exception ignored) {
        }
        for (String d : DIMENSIONS) {
            out.putIfAbsent(d, 0.0);
        }
        return out;
    }

    private List<ProfileSnapshotVO.DimensionValue> parseDimVOs(String json) {
        List<ProfileSnapshotVO.DimensionValue> out = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    ProfileSnapshotVO.DimensionValue v = new ProfileSnapshotVO.DimensionValue();
                    v.setKey(n.path("key").asText());
                    v.setName(n.path("name").asText());
                    v.setScore(n.path("score").asDouble());
                    out.add(v);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private List<String> parseStrList(String json) {
        List<String> out = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.add(n.asText());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private ProfileSnapshotVO.ProfileFeedback parseFeedback(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            ProfileSnapshotVO.ProfileFeedback fb = new ProfileSnapshotVO.ProfileFeedback();
            fb.setFeedbackType(n.path("feedbackType").asText());
            fb.setComment(n.path("comment").asText(""));
            return fb;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> topDims(Map<String, Double> dims, int n, boolean high) {
        return dims.entrySet().stream()
                .sorted(high
                        ? Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed()
                        : Comparator.comparingDouble(Map.Entry::getValue))
                .limit(n)
                .map(e -> DIM_NAMES.getOrDefault(e.getKey(), e.getKey()) + "(" + Math.round(e.getValue()) + ")")
                .collect(Collectors.toList());
    }

    private String buildSummary(Map<String, Double> dims) {
        Map.Entry<String, Double> top = dims.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue)).orElse(null);
        Map.Entry<String, Double> low = dims.entrySet().stream()
                .min(Comparator.comparingDouble(Map.Entry::getValue)).orElse(null);
        if (top == null) {
            return "画像尚未完善，建议完成测评。";
        }
        return "你的优势维度在「" + DIM_NAMES.getOrDefault(top.getKey(), top.getKey())
                + "」（" + Math.round(top.getValue()) + "），可在「"
                + DIM_NAMES.getOrDefault(low.getKey(), low.getKey()) + "」方向继续探索提升。";
    }

    private Map<String, Double> estimateFromProfile(String studentId) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (String d : DIMENSIONS) {
            out.put(d, 0.0);
        }
        StudentProfile p = profileMapper.findByUserId(studentId);
        if (p == null) {
            return out;
        }
        // Demo 精简点：从档案基础能力/学业估算（无测评时）
        if (p.getAbilitySelf() != null && p.getAbilitySelf().getProgramming() != null) {
            out.put("ability", toScore(p.getAbilitySelf().getProgramming()));
        }
        if (p.getAcademic() != null && p.getAcademic().getMath() != null) {
            out.put("academic", toScore(p.getAcademic().getMath()));
        }
        if (p.getInterestPrefs() != null && !p.getInterestPrefs().isEmpty()) {
            out.put("interest", 60.0);
        }
        return out;
    }

    private Double toScore(Integer o) {
        if (o == null) {
            return 0.0;
        }
        double v = o.doubleValue();
        return Math.min(100.0, v * 20.0);
    }

    private int computeCompleteness(String studentId) {
        StudentProfile p = profileMapper.findByUserId(studentId);
        return p == null ? 0 : (p.getCompleteness() == null ? 0 : p.getCompleteness());
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception exc) {
            return "[]";
        }
    }
}
