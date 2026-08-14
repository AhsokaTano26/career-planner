package com.career.core.modules.student;

import com.career.core.common.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生画像服务（按线上 Apifox「学生画像」模块 ProfileSnapshot 结构实现）。
 * <p>
 * Demo 精简逻辑说明：
 *   1. 直接查库返回，不做缓存（后续可加 Redis/本地缓存）。
 *   2. 学生不存在或画像未生成 → 返回 null（由 Controller 返回空对象），而非报错。
 *   3. version 复用 profile_snapshot.id 作为画像版本号（Demo 精简点：线上为独立版本号列，此处无该列）。
 *   4. strengths/explore 由六维得分简单推导（>=80 优势 / <70 待探索），后续可替换为大模型生成。
 */
@Service
public class StudentProfileService {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentProfileDao dao;
    private final ObjectMapper objectMapper;

    public StudentProfileService(StudentProfileDao dao, ObjectMapper objectMapper) {
        this.dao = dao;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询学生最新画像快照。
     *
     * @param studentId 学生ID（对应 student_profile.user_id）
     * @return ProfileSnapshotDto；学生不存在或画像未生成时返回 null
     */
    public ProfileSnapshotDto getLatestProfile(Long studentId) {
        StudentProfile student = dao.findStudentByUserId(studentId);
        if (student == null) {
            return null;
        }
        ProfileSnapshot snapshot = dao.findLatestSnapshot(studentId);
        if (snapshot == null || snapshot.dimensionJson() == null || snapshot.dimensionJson().isBlank()) {
            return null;
        }
        return toDto(snapshot);
    }

    /** 画像版本列表（历史快照，分页） */
    public List<ProfileSnapshotDto> getVersions(Long studentId, int page, int size) {
        return dao.findSnapshots(studentId, page, size).stream()
                .filter(s -> s.dimensionJson() != null && !s.dimensionJson().isBlank())
                .map(this::toDto)
                .toList();
    }

    private ProfileSnapshotDto toDto(ProfileSnapshot snapshot) {
        Map<String, Double> dims = parseDimensions(snapshot.dimensionJson());
        List<ProfileSnapshotDto.DimensionValueDto> dimensions = buildDimensionValues(dims);
        int nonNullCount = 0;
        for (String dim : Constants.ALL_DIMENSIONS) {
            if (dims.get(dim) != null) {
                nonNullCount++;
            }
        }
        int completeness = (int) Math.round(nonNullCount * 100.0 / Constants.ALL_DIMENSIONS.size());
        return new ProfileSnapshotDto(
                snapshot.id() == null ? null : "PS-" + snapshot.id(),
                snapshot.id() == null ? 0 : snapshot.id().intValue(),
                snapshot.createdAt() == null ? null : snapshot.createdAt().toString(),
                snapshot.sourceVersion(),
                completeness,
                dimensions,
                snapshot.summary(),
                buildStrengths(dims),
                buildExplore(dims),
                null);
    }

    /** 六维得分对象 → 数组（线上 DimensionValue[]：key/name/score） */
    private List<ProfileSnapshotDto.DimensionValueDto> buildDimensionValues(Map<String, Double> dims) {
        List<ProfileSnapshotDto.DimensionValueDto> list = new ArrayList<>();
        for (String dim : Constants.ALL_DIMENSIONS) {
            Double score = dims.get(dim);
            if (score != null) {
                list.add(new ProfileSnapshotDto.DimensionValueDto(
                        toOnlineKey(dim),
                        Constants.DIMENSION_NAMES.getOrDefault(dim, dim),
                        score));
            }
        }
        return list;
    }

    /**
     * 内部维度编码 → 线上 DimensionValue.key 枚举。
     * 线上 key 枚举为 interest/values/ability/academic/tendency/practice；
     * 内部沿用 orientation（发展倾向）/experience（实践经历），对外映射为 tendency/practice。
     */
    private String toOnlineKey(String dim) {
        return switch (dim) {
            case Constants.DIM_ORIENTATION -> "tendency";
            case Constants.DIM_EXPERIENCE -> "practice";
            default -> dim;
        };
    }

    /** 优势（Demo 精简点）：得分 >= 80 的维度，按得分降序 */
    private List<String> buildStrengths(Map<String, Double> dims) {
        return dims.entrySet().stream()
                .filter(e -> e.getValue() >= 80.0)
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey()) + "表现较好")
                .toList();
    }

    /** 待探索（Demo 精简点）：得分 < 70 的维度，按得分升序 */
    private List<String> buildExplore(Map<String, Double> dims) {
        return dims.entrySet().stream()
                .filter(e -> e.getValue() < 70.0)
                .sorted((a, b) -> Double.compare(a.getValue(), b.getValue()))
                .map(e -> Constants.DIMENSION_NAMES.getOrDefault(e.getKey(), e.getKey()) + "有待积累")
                .toList();
    }

    private Map<String, Double> parseDimensions(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Map<String, Double> dims = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if (e.getValue() instanceof Number n) {
                    dims.put(e.getKey(), n.doubleValue());
                }
            }
            return dims;
        } catch (Exception e) {
            log.warn("解析画像快照 dimension_json 失败，studentId 见调用方", e);
            return Collections.emptyMap();
        }
    }
}
