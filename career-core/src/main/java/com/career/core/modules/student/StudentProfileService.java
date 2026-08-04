package com.career.core.modules.student;

import com.career.core.common.Constants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 接口1「获取学生画像」服务。
 * Demo 精简逻辑说明：
 *   1. 直接查库返回，不做缓存（后续可加 Redis/本地缓存）。
 *   2. 学生不存在或画像未生成 → 返回空对象而非报错。
 *   3. 数据完整度 = 非空字段数 / 总字段数（六维），简单计算。
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
     * 查询学生六维画像及数据完整度。
     *
     * @param studentId 学生ID（对应 student_profile.user_id）
     * @return 画像数据 Map；学生不存在或画像未生成时返回空 Map
     */
    public Map<String, Object> getProfile(Long studentId) {
        // 1. 学生不存在 → 空对象
        StudentProfile student = dao.findStudentByUserId(studentId);
        if (student == null) {
            return Collections.emptyMap();
        }

        // 2. 画像未生成（无快照或快照无维度数据）→ 空对象
        ProfileSnapshot snapshot = dao.findLatestSnapshot(studentId);
        if (snapshot == null || snapshot.dimensionJson() == null || snapshot.dimensionJson().isBlank()) {
            return Collections.emptyMap();
        }

        // 3. 解析六维画像 JSON
        Map<String, Object> dimensions = parseDimensions(snapshot.dimensionJson());

        // 4. 完整度 = 非空字段数 / 总字段数（Demo 简单算法，后续可替换为加权完整度）
        int nonNullCount = 0;
        for (String dim : Constants.ALL_DIMENSIONS) {
            if (dimensions.get(dim) != null) {
                nonNullCount++;
            }
        }
        double completeness = BigDecimal.valueOf(nonNullCount * 100.0 / Constants.ALL_DIMENSIONS.size())
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        // 5. 组装返回结构（线上结构为主 + 增强字段）
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimensions", dimensions);          // 线上字段
        data.put("summary", snapshot.summary());      // 线上字段
        data.put("version", snapshot.sourceVersion()); // 线上字段
        // 以下为增强字段（线上未定义，保留原验收能力）：
        data.put("completeness", completeness);
        data.put("studentId", studentId);
        data.put("experiences", buildExperiences(dao.findExperiences(studentId)));
        return data;
    }

    private Map<String, Object> parseDimensions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("解析画像快照 dimension_json 失败，studentId 见调用方", e);
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> buildExperiences(List<StudentExperience> experiences) {
        return experiences.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.type());
            m.put("title", e.title());
            m.put("startDate", e.startDate() == null ? null : e.startDate().toString());
            m.put("description", e.description());
            return m;
        }).toList();
    }
}
