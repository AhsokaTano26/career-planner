package com.career.core.modules.recommendation;

import com.career.core.common.BadRequestException;
import com.career.core.common.Constants;
import com.career.core.modules.student.ProfileSnapshot;
import com.career.core.modules.student.StudentProfileDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口2「获取推荐方向列表」服务。
 * Demo 精简逻辑说明：
 *   1. 仅走规则引擎过滤 + 结构化评分，不调用大模型生成解释。
 *   2. 推荐理由使用预置模板拼接（见 RecommendationEngine.buildReason）。
 *   3. 推荐结果不足 3 个时返回实际数量，不强制补齐；相同学生多次调用结果一致（评分确定性）。
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final int TOP_N = 5;

    private final StudentProfileDao studentDao;
    private final RecommendationDao recommendationDao;
    private final RecommendationEngine engine;
    private final ObjectMapper objectMapper;

    public RecommendationService(StudentProfileDao studentDao,
                                 RecommendationDao recommendationDao,
                                 RecommendationEngine engine,
                                 ObjectMapper objectMapper) {
        this.studentDao = studentDao;
        this.recommendationDao = recommendationDao;
        this.engine = engine;
        this.objectMapper = objectMapper;
    }

    /**
     * 为学生生成推荐方向列表（3-5 个）。
     *
     * @param studentId 学生ID（对应 student_profile.user_id）
     * @return 推荐方向列表；学生无画像快照时返回空列表（不报错）
     */
    public List<RecommendationDto> recommend(Long studentId) {
        // 1. 无画像快照 → 无法评分，返回空列表
        ProfileSnapshot snapshot = studentDao.findLatestSnapshot(studentId);
        if (snapshot == null || snapshot.dimensionJson() == null || snapshot.dimensionJson().isBlank()) {
            return List.of();
        }
        Map<String, Double> studentDims = parseDims(snapshot.dimensionJson());

        // 2. 规则过滤：仅启用方向作为候选
        List<CareerDirection> candidates = engine.filterActive(recommendationDao.findAllDirections());

        // 3. 结构化评分（权重/目标值来自配置表 direction_dimension_weight）
        List<RecommendationEngine.ScoredDirection> scoredList = candidates.stream()
                .map(d -> engine.score(d, studentDims, recommendationDao.findWeightsByDirection(d.id(), 1)))
                .toList();

        // 4. 按评分降序取前 5（评分相同时按方向ID升序，保证多次调用结果一致）
        List<RecommendationEngine.ScoredDirection> top = scoredList.stream()
                .sorted(Comparator
                        .comparingDouble((RecommendationEngine.ScoredDirection s) -> s.score())
                        .reversed()
                        .thenComparing(s -> s.direction().id()))
                .limit(TOP_N)
                .toList();

        // 5. 持久化推荐批次与结果（Demo：每次调用生成一批，便于追溯），并组装响应
        long runId = recommendationDao.insertRun(studentId, snapshot.id(), Constants.RULE_VERSION, "DONE");
        AtomicInteger rank = new AtomicInteger(1);
        List<RecommendationDto> dtos = new ArrayList<>();
        for (RecommendationEngine.ScoredDirection s : top) {
            String reason = engine.buildReason(s.direction(), s);
            int r = rank.getAndIncrement();
            recommendationDao.insertResult(runId, s.direction().id(), s.score(), r, toJson(reason));
            // 线上字段 directionId/score/rank/confidence + 增强字段 name/type/reason
            dtos.add(new RecommendationDto(s.direction().id(), s.direction().name(),
                    s.direction().type(), s.score(), r, confidence(s.score()), reason));
        }
        return dtos;
    }

    /** 置信度分级（设计文档枚举 HIGH 等）：>=0.9 HIGH，>=0.75 MEDIUM，其余 LOW */
    private String confidence(double score) {
        if (score >= 0.9) {
            return "HIGH";
        }
        if (score >= 0.75) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 推荐反馈（Demo 最小实现：直接写入 recommendation_feedback，不做内容校验）。
     * 后续迭代可据此做反馈回流调优，或触发 AI 分析。
     */
    public void feedback(Long resultId, String feedbackType, String comment) {
        recommendationDao.insertFeedback(resultId, feedbackType, comment);
    }

    /**
     * 推荐反馈（兼容空 id：自动使用最近一次推荐结果）。
     * Demo 便捷：Apifox 调试时 id 留空也可直接提交；正式环境应要求 id 必填，此兜底可移除。
     */
    public void feedbackLatest(String feedbackType, String comment) {
        Long latestId = recommendationDao.findLatestResultId();
        if (latestId == null) {
            throw new BadRequestException("暂无推荐结果，请先调用 recommendations/run 生成推荐");
        }
        feedback(latestId, feedbackType, comment);
    }

    private Map<String, Double> parseDims(String json) {
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
            log.warn("解析画像快照 dimension_json 失败", e);
            return Collections.emptyMap();
        }
    }

    private String toJson(String reason) {
        try {
            return objectMapper.writeValueAsString(reason);
        } catch (Exception e) {
            return "\"" + reason + "\"";
        }
    }
}
