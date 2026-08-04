package com.career.core.modules.recommendation;

import com.career.core.common.BadRequestException;
import com.career.core.common.Constants;
import com.career.core.integration.ai.AiExplainClient;
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
 *   1. 规则引擎过滤 + 结构化评分；评分叠加霍兰德人格契合度软加分（见 RecommendationEngine）。
 *   2. 推荐理由优先调用 career-ai（FastAPI + 大模型）生成自然语言解释；
 *      AI 失败/超时/未启用时回退规则模板（RecommendationEngine.buildReason）。
 *   3. confidence 改用归一化概率表达（softmax，0-1，结果集内求和约等于 1）。
 *   4. 推荐结果不足 3 个时返回实际数量，不强制补齐；相同学生多次调用结果一致（评分确定性）。
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final int TOP_N = 5;

    /** 置信度归一化概率的 softmax 温度（Demo 常量；越小概率区分度越大） */
    private static final double CONFIDENCE_TEMPERATURE = 0.1;

    private final StudentProfileDao studentDao;
    private final RecommendationDao recommendationDao;
    private final RecommendationEngine engine;
    private final AiExplainClient aiExplainClient;
    private final ObjectMapper objectMapper;

    public RecommendationService(StudentProfileDao studentDao,
                                 RecommendationDao recommendationDao,
                                 RecommendationEngine engine,
                                 AiExplainClient aiExplainClient,
                                 ObjectMapper objectMapper) {
        this.studentDao = studentDao;
        this.recommendationDao = recommendationDao;
        this.engine = engine;
        this.aiExplainClient = aiExplainClient;
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
        String personality = parsePersonality(snapshot.dimensionJson());

        // 2. 规则过滤：仅启用方向作为候选
        List<CareerDirection> candidates = engine.filterActive(recommendationDao.findAllDirections());

        // 3. 结构化评分（权重/目标值来自配置表 direction_dimension_weight；叠加霍兰德人格契合度软加分）
        List<RecommendationEngine.ScoredDirection> scoredList = candidates.stream()
                .map(d -> engine.score(d, studentDims, recommendationDao.findWeightsByDirection(d.id(), 1), personality))
                .toList();

        // 4. 按评分降序取前 5（评分相同时按方向ID升序，保证多次调用结果一致）
        List<RecommendationEngine.ScoredDirection> top = scoredList.stream()
                .sorted(Comparator
                        .comparingDouble((RecommendationEngine.ScoredDirection s) -> s.score())
                        .reversed()
                        .thenComparing(s -> s.direction().id()))
                .limit(TOP_N)
                .toList();

        // 5. confidence 归一化概率表达：对 Top 结果评分做 softmax，结果集内求和约等于 1
        List<Double> confidences = normalizedConfidence(top);

        // 6. 持久化推荐批次与结果（Demo：每次调用生成一批，便于追溯），并组装响应
        long runId = recommendationDao.insertRun(studentId, snapshot.id(), Constants.RULE_VERSION, "DONE");
        AtomicInteger rank = new AtomicInteger(1);
        List<RecommendationDto> dtos = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            RecommendationEngine.ScoredDirection s = top.get(i);
            // 替换点：优先 career-ai 大模型生成解释，失败回退引擎模板
            String reason = explain(s, personality);
            int r = rank.getAndIncrement();
            recommendationDao.insertResult(runId, s.direction().id(), s.score(), r, toJson(reason));
            // 线上字段 directionId/score/rank/confidence + 增强字段 name/type/reason
            dtos.add(new RecommendationDto(s.direction().id(), s.direction().name(),
                    s.direction().type(), s.score(), r, confidences.get(i), reason));
        }
        return dtos;
    }

    /**
     * 生成推荐理由：优先调用 career-ai（LLM）生成自然语言解释；失败/超时/未启用时回退规则模板。
     * 后续迭代：可在此叠加降级策略（如仅 Top1 走 AI、批量调用等）。
     */
    private String explain(RecommendationEngine.ScoredDirection scored, String personality) {
        try {
            return aiExplainClient.explain(scored, personality);
        } catch (Exception e) {
            // 降级路径：AI 不可用属预期情况，WARN 仅记录原因，不打印完整堆栈（调试时改 DEBUG 即可）
            log.warn("career-ai 解释生成失败，回退规则模板理由。directionId={}，原因={}",
                    scored.direction().id(), e.toString());
            return engine.buildReason(scored.direction(), scored);
        }
    }

    /** 置信度归一化概率（softmax over 评分，温度 CONFIDENCE_TEMPERATURE），结果集内求和约等于 1 */
    private List<Double> normalizedConfidence(List<RecommendationEngine.ScoredDirection> top) {
        double[] exps = new double[top.size()];
        double sum = 0.0;
        for (int i = 0; i < top.size(); i++) {
            exps[i] = Math.exp(top.get(i).score() / CONFIDENCE_TEMPERATURE);
            sum += exps[i];
        }
        List<Double> confidences = new ArrayList<>(top.size());
        for (int i = 0; i < top.size(); i++) {
            confidences.add(round4(sum > 0 ? exps[i] / sum : 0.0));
        }
        return confidences;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
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

    /** 提取画像中的霍兰德人格类型（RIASEC 编码串），无则返回 null */
    private String parsePersonality(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Object v = raw.get(Constants.PROFILE_PERSONALITY_KEY);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            log.warn("解析画像快照霍兰德人格失败", e);
            return null;
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
