package com.rickgao.careercore.modules.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.mapper.AdminDirectionMapper;
import com.rickgao.careercore.modules.portrait.entity.ProfileSnapshot;
import com.rickgao.careercore.modules.portrait.mapper.ProfileSnapshotMapper;
import com.rickgao.careercore.modules.recommendation.dto.CreateRecommendationRequest;
import com.rickgao.careercore.modules.recommendation.dto.RecommendationFeedbackRequest;
import com.rickgao.careercore.modules.recommendation.entity.RecommendationResult;
import com.rickgao.careercore.modules.recommendation.entity.RecommendationRun;
import com.rickgao.careercore.modules.recommendation.mapper.RecommendationMapper;
import com.rickgao.careercore.modules.recommendation.vo.RecFeedbackVO;
import com.rickgao.careercore.modules.recommendation.vo.RecResultVO;
import com.rickgao.careercore.modules.recommendation.vo.RecRunVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推荐模块：规则引擎评分 + 落库。
 *
 * <p>Demo 精简点 / 后续迭代替换位置：
 *  - 评分用六维加权匹配（画像六维 vs 方向 target_json）；
 *  - 解释（reasons/strengths/gaps/semesterActions/summary）由规则模板生成，未调大模型；
 *  - 无画像时按档案估算或返回空结果。
 */
@Service
public class RecommendationService {

    private static final String[] DIMENSIONS = {"interest", "values", "ability", "academic", "tendency", "practice"};
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "interest", 0.20, "values", 0.15, "ability", 0.25,
            "academic", 0.15, "tendency", 0.20, "practice", 0.05);
    private static final List<String> FEEDBACK_TYPES = List.of("HELPFUL", "NEUTRAL", "MISMATCH", "NOT_INTERESTED");

    private final RecommendationMapper recommendationMapper;
    private final AdminDirectionMapper directionMapper;
    private final ProfileSnapshotMapper snapshotMapper;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public RecommendationService(RecommendationMapper recommendationMapper, AdminDirectionMapper directionMapper,
                                 ProfileSnapshotMapper snapshotMapper, IdGenerator idGenerator, ObjectMapper objectMapper) {
        this.recommendationMapper = recommendationMapper;
        this.directionMapper = directionMapper;
        this.snapshotMapper = snapshotMapper;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RecRunVO createRun(String studentId, CreateRecommendationRequest req) {
        ProfileSnapshot snapshot = snapshotMapper.findLatestByStudent(studentId);
        if (snapshot == null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "请先生成画像（完成测评或刷新画像）");
        }
        Map<String, Double> profile = parseProfile(snapshot.getDimensionJson());

        List<CareerDirection> directions = directionMapper.selectAllPublished();
        if (req.getPathFilter() != null && !req.getPathFilter().isBlank()) {
            directions = directions.stream()
                    .filter(d -> req.getPathFilter().equals(d.getPath()))
                    .collect(Collectors.toList());
        }

        RecommendationRun run = new RecommendationRun();
        run.setId(idGenerator.recommendationRunId());
        run.setStudentId(studentId);
        run.setProfileSnapshotId(snapshot.getId());
        run.setRuleVersion("R1.0");
        run.setStatus("SUCCESS");
        run.setGeneratedAt(LocalDateTime.now());
        recommendationMapper.insertRun(run);

        // 评分排序
        List<Scored> scored = directions.stream()
                .map(d -> new Scored(d, score(profile, parseTarget(d.getTargetJson()))))
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed())
                .collect(Collectors.toList());

        int rank = 1;
        for (Scored s : scored) {
            if (s.score <= 0) {
                continue;
            }
            s.rank = rank++;
            RecommendationResult result = new RecommendationResult();
            result.setId(idGenerator.recommendationResultId());
            result.setRunId(run.getId());
            result.setDirectionId(s.direction.getId());
            result.setScore(BigDecimal.valueOf(s.score));
            result.setRank(s.rank);
            result.setConfidence(s.score >= 70 ? "HIGH" : (s.score >= 50 ? "MEDIUM" : "LOW"));
            result.setReasonsJson(writeJson(reasons(s)));
            result.setStrengthsJson(writeJson(strengths(s)));
            result.setGapsJson(writeJson(gaps(s)));
            result.setSemesterActionsJson(writeJson(semesterActions(s)));
            result.setFeedbackJson(null);
            recommendationMapper.insertResult(result);
        }
        return toRunVO(run, true);
    }

    public RecRunVO getLatest(String studentId) {
        RecommendationRun run = recommendationMapper.findLatestRunByStudent(studentId);
        if (run == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "暂无推荐结果");
        }
        return toRunVO(run, true);
    }

    public List<RecRunVO> listRuns(String studentId, int page, int size) {
        int offset = (page - 1) * size;
        return recommendationMapper.listRunsByStudent(studentId, offset, size).stream()
                .map(r -> toRunVO(r, false))
                .collect(Collectors.toList());
    }

    public long countRuns(String studentId) {
        return recommendationMapper.countRunsByStudent(studentId);
    }

    public RecRunVO getRunDetail(String runId) {
        RecommendationRun run = recommendationMapper.findRunById(runId);
        if (run == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "推荐批次不存在");
        }
        return toRunVO(run, true);
    }

    @Transactional
    public RecResultVO addFeedback(String resultId, RecommendationFeedbackRequest req) {
        validateFeedback(req.getFeedbackType());
        RecommendationResult result = recommendationMapper.findResultById(resultId);
        if (result == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "推荐结果不存在");
        }
        RecFeedbackVO fb = RecFeedbackVO.builder()
                .feedbackType(req.getFeedbackType()).comment(req.getComment()).build();
        recommendationMapper.updateResultFeedback(resultId, writeJson(fb));
        result.setFeedbackJson(writeJson(fb));
        return toResultVO(result);
    }

    // ---------------------------------------------------------------- 评分

    private double score(Map<String, Double> profile, Map<String, Double> target) {
        double total = 0.0;
        for (String d : DIMENSIONS) {
            double p = profile.getOrDefault(d, 0.0);
            double t = target.getOrDefault(d, 0.0);
            double diff = Math.abs(p - t);
            double dimScore = Math.max(0.0, 100.0 - diff);
            total += dimScore * DEFAULT_WEIGHTS.getOrDefault(d, 0.0);
        }
        return Math.round(total * 10.0) / 10.0;
    }

    private Map<String, Double> parseProfile(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (String d : DIMENSIONS) {
            out.put(d, 0.0);
        }
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    out.put(n.path("key").asText(), n.path("score").asDouble());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private Map<String, Double> parseTarget(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (String d : DIMENSIONS) {
            out.put(d, 0.0);
        }
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode obj = objectMapper.readTree(json);
            for (String d : DIMENSIONS) {
                if (obj.has(d)) {
                    out.put(d, obj.get(d).asDouble() * 100.0);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private List<String> reasons(Scored s) {
        List<String> list = new ArrayList<>();
        list.add("该方向与你的画像匹配度为 " + Math.round(s.score) + "%，位列第 " + s.rank + " 名");
        list.add("你的「" + topDimName(s) + "」与该方向目标较为契合");
        return list;
    }

    private List<String> strengths(Scored s) {
        List<String> list = new ArrayList<>();
        list.add(topDimName(s) + "优势明显");
        list.add("具备较好的基础，适合在该方向发展");
        return list;
    }

    private List<String> gaps(Scored s) {
        List<String> list = new ArrayList<>();
        list.add("建议加强「" + lowDimName(s) + "」维度的积累");
        return list;
    }

    private List<String> semesterActions(Scored s) {
        List<String> list = new ArrayList<>();
        list.add("完成一个与" + s.direction.getName() + "相关的小项目");
        list.add("访谈一位该方向的高年级同学");
        return list;
    }

    private String topDimName(Scored s) {
        return s.topDim;
    }

    private String lowDimName(Scored s) {
        return s.lowDim;
    }

    // ---------------------------------------------------------------- VO

    private RecRunVO toRunVO(RecommendationRun run, boolean withResults) {
        Integer profileVersion = null;
        if (run.getProfileSnapshotId() != null) {
            ProfileSnapshot snap = snapshotMapper.findById(run.getProfileSnapshotId());
            profileVersion = snap == null ? null : snap.getVersionNo();
        }
        List<RecResultVO> results = withResults
                ? recommendationMapper.listResultsByRun(run.getId()).stream().map(this::toResultVO).collect(Collectors.toList())
                : List.of();
        return RecRunVO.builder()
                .runId(run.getId()).profileVersion(profileVersion).ruleVersion(run.getRuleVersion())
                .generatedAt(ts(run.getGeneratedAt())).status(run.getStatus()).results(results)
                .build();
    }

    private RecResultVO toResultVO(RecommendationResult r) {
        return RecResultVO.builder()
                .resultId(r.getId()).directionId(r.getDirectionId()).rank(r.getRank())
                .score(r.getScore() == null ? null : r.getScore().doubleValue())
                .confidence(r.getConfidence())
                .reasons(parseStrList(r.getReasonsJson()))
                .strengths(parseStrList(r.getStrengthsJson()))
                .gaps(parseStrList(r.getGapsJson()))
                .semesterActions(parseStrList(r.getSemesterActionsJson()))
                .feedback(parseFeedback(r.getFeedbackJson()))
                .build();
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

    private RecFeedbackVO parseFeedback(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode n = objectMapper.readTree(json);
            return RecFeedbackVO.builder()
                    .feedbackType(n.path("feedbackType").asText())
                    .comment(n.path("comment").asText(""))
                    .build();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void validateFeedback(String type) {
        if (type == null || !FEEDBACK_TYPES.contains(type)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "feedbackType 不合法：" + type);
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception exc) {
            return "[]";
        }
    }

    private String ts(LocalDateTime t) {
        return t == null ? null : t.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** 评分中间结果（方向 + 得分 + 名次）。 */
    private static class Scored {
        final CareerDirection direction;
        final double score;
        final String topDim;
        final String lowDim;
        int rank;

        Scored(CareerDirection direction, double score) {
            this.direction = direction;
            this.score = score;
            // 计算方向目标中的最高/最低维度名（Demo 简化，用方向 id 占位）
            this.topDim = "综合匹配";
            this.lowDim = "综合匹配";
        }
    }
}
