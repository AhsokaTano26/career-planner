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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推荐模块服务（按线上 Apifox「方向推荐」5 个接口实现）。
 * <p>
 * Demo 精简逻辑说明：
 *   1. 规则引擎过滤 + 结构化评分（六维加权 + 霍兰德人格契合度软加分，见 RecommendationEngine）。
 *   2. 内部评分保持 0-1（career-ai 按 0-1 解释）；对外 DTO 转百分制（score 0-100）、
 *      confidence 由概率表达改为 HIGH/MEDIUM/LOW 枚举。
 *   3. 推荐理由优先调用 career-ai 大模型生成自然语言解释；失败/超时/未启用时回退规则模板（数组）。
 *   4. 同步生成：落库后 status=SUCCESS 直接返回；线上定义允许 RUNNING 轮询，Demo 阶段不实现异步。
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private static final int TOP_N = 5;

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
     * 生成推荐批次（POST /runs）：评分、落库、返回批次。
     *
     * @param studentId  学生ID（对应 student_profile.user_id）
     * @param pathFilter 发展路径过滤（Demo 阶段忽略，仅保留参数位置）
     * @return 推荐批次
     */
    public RecommendationRunDto run(Long studentId, String pathFilter) {
        ProfileSnapshot snapshot = studentDao.findLatestSnapshot(studentId);
        if (snapshot == null || snapshot.dimensionJson() == null || snapshot.dimensionJson().isBlank()) {
            // 无画像快照无法评分 → 400（避免返回 runId 为 null 的批次，契约中 runId 为必填）
            throw new BadRequestException("学生画像未生成，无法生成推荐");
        }
        Map<String, Double> studentDims = parseDims(snapshot.dimensionJson());
        String personality = parsePersonality(snapshot.dimensionJson());

        List<CareerDirection> candidates = engine.filterActive(recommendationDao.findAllDirections());
        List<RecommendationEngine.ScoredDirection> top = candidates.stream()
                .map(d -> engine.score(d, studentDims,
                        recommendationDao.findWeightsByDirection(d.id(), 1), personality))
                .sorted(Comparator
                        .comparingDouble((RecommendationEngine.ScoredDirection s) -> s.score())
                        .reversed()
                        .thenComparing(s -> s.direction().id()))
                .limit(TOP_N)
                .toList();

        long runId = recommendationDao.insertRun(studentId, snapshot.id(), Constants.RULE_VERSION, "SUCCESS");
        AtomicInteger rank = new AtomicInteger(1);
        List<RecommendationResultDto> results = new ArrayList<>();
        for (RecommendationEngine.ScoredDirection s : top) {
            int r = rank.getAndIncrement();
            String aiReason = explain(s, personality);
            RecommendationResultDto dto = toResultDto(s, r, aiReason);
            // 结构化解释落库（Demo 精简点：explanation_json 由单个 reason 字符串改为结构化对象）
            recommendationDao.insertResult(runId, s.direction().id(), dto.score(), r, toExplanationJson(dto, aiReason));
            results.add(dto);
        }
        return new RecommendationRunDto(String.valueOf(runId), snapshot.id().intValue(),
                Constants.RULE_VERSION, nowIso(), "SUCCESS", results);
    }

    /** 最新推荐结果（GET /latest）：学生无批次时返回 null（由 Controller 转 404） */
    public RecommendationRunDto getLatest(Long studentId) {
        RecommendationDao.RunRow runRow = recommendationDao.findLatestRun(studentId);
        if (runRow == null) {
            return null;
        }
        return toRunDto(runRow);
    }

    /**
     * 推荐批次历史（GET /recommendations）：线上 200 schema 为单个 RecommendationRun。
     * 本地实现取最近一次批次（含结果明细），保证响应结构与线上契约一致。
     */
    public RecommendationRunDto getHistory(Long studentId, int page, int size) {
        List<RecommendationDao.RunRow> runs = recommendationDao.findRunsByStudent(studentId, page, size);
        return runs.isEmpty() ? null : toRunDto(runs.get(0));
    }

    /** 推荐批次详情（GET /recommendation-runs/{runId}）：不存在则返回 null（由 Controller 转 404） */
    public RecommendationRunDto getRunDetail(String runId) {
        long id;
        try {
            id = Long.parseLong(runId);
        } catch (NumberFormatException e) {
            return null;
        }
        RecommendationDao.RunRow runRow = recommendationDao.findRunById(id);
        return runRow == null ? null : toRunDto(runRow);
    }

    /**
     * 推荐反馈（POST /recommendation-results/{resultId}/feedback）。
     * 线上 feedbackType 枚举：HELPFUL / NEUTRAL / MISMATCH / NOT_INTERESTED。
     * 契约中 resultId 为字符串方向编码（如 DIR002）；兼容旧调用方的数字型 resultId。
     * Demo 无登录态，studentId 由 Controller 解析（缺省 1001）。
     */
    public void feedback(String resultRef, String feedbackType, String comment, Long studentId) {
        Long resultId = resolveFeedbackResultId(resultRef, studentId);
        if (resultId == null) {
            throw new BadRequestException("推荐结果不存在：" + resultRef);
        }
        recommendationDao.insertFeedback(resultId, feedbackType, comment);
    }

    /** 解析反馈目标结果ID：数字形式直接使用（兼容旧调用方）；否则按方向编码解析到该学生最新批次的结果。 */
    private Long resolveFeedbackResultId(String resultRef, Long studentId) {
        if (resultRef == null || resultRef.isBlank()) {
            return null;
        }
        // 数字形式：直接作为 recommendation_result.id 使用
        try {
            long num = Long.parseLong(resultRef);
            return recommendationDao.existsResult(num) ? num : null;
        } catch (NumberFormatException ignored) {
            // 非数字，走方向编码解析
        }
        // 方向编码 → 方向ID
        Long dirId = recommendationDao.findAllDirections().stream()
                .filter(d -> resultRef.equals(d.directionCode()))
                .map(CareerDirection::id)
                .findFirst()
                .orElse(null);
        if (dirId == null) {
            return null;
        }
        // 学生最新批次的该方向结果
        RecommendationDao.RunRow run = recommendationDao.findLatestRun(studentId);
        if (run == null) {
            return null;
        }
        return recommendationDao.findResultsByRunId(run.id()).stream()
                .filter(r -> r.directionId() == dirId)
                .map(RecommendationDao.ResultRow::id)
                .findFirst()
                .orElse(null);
    }

    /** 反馈（兼容空 resultId：自动使用最近一次推荐结果；无登录态，studentId 传 null） */
    public void feedbackLatest(String feedbackType, String comment) {
        Long latestId = recommendationDao.findLatestResultId();
        if (latestId == null) {
            throw new BadRequestException("暂无推荐结果，请先调用 /students/me/recommendations/runs 生成推荐");
        }
        feedback(String.valueOf(latestId), feedbackType, comment, null);
    }

    // ---------- 内部工具 ----------

    /** 生成推荐理由：优先 career-ai（LLM）自然语言；失败回退规则模板（数组） */
    private String explain(RecommendationEngine.ScoredDirection scored, String personality) {
        try {
            return aiExplainClient.explain(scored, personality);
        } catch (Exception e) {
            log.warn("career-ai 解释生成失败，回退规则模板理由。directionId={}，原因={}",
                    scored.direction().id(), e.toString());
            return null;
        }
    }

    /** 组装线上 RecommendationResultDto（内部 score 0-1 → 对外百分制 0-100） */
    private RecommendationResultDto toResultDto(RecommendationEngine.ScoredDirection s, int rank, String aiReason) {
        double percent = round1(s.score() * 100.0);
        String confidence = engine.confidenceOf(s.score());
        List<String> reasons = aiReason != null && !aiReason.isBlank()
                ? List.of(aiReason)                       // AI 自然语言解释（单条）
                : engine.buildReasons(s);                 // 模板维度理由（数组）
        List<String> strengths = engine.buildStrengths(s);
        List<String> gaps = engine.buildGaps(s);
        List<String> semesterActions = engine.buildSemesterActions(s.direction());
        return new RecommendationResultDto(s.direction().directionCode(), rank, percent, confidence,
                reasons, strengths, gaps, semesterActions, null);
    }

    /** RunRow → RecommendationRunDto（含结果明细） */
    private RecommendationRunDto toRunDto(RecommendationDao.RunRow runRow) {
        // 方向 id → 方向编码 映射（避免每条结果单独查库）
        Map<Long, CareerDirection> dirById = recommendationDao.findAllDirections().stream()
                .collect(Collectors.toMap(CareerDirection::id, Function.identity()));
        List<RecommendationResultDto> results = recommendationDao.findResultsByRunId(runRow.id()).stream()
                .map(row -> toResultDtoFromRow(row, dirById))
                .toList();
        return new RecommendationRunDto(String.valueOf(runRow.id()), (int) runRow.profileSnapshotId(),
                runRow.ruleVersion(), runRow.createdAt() == null ? null : runRow.createdAt().toString(),
                runRow.status(), results);
    }

    /** 从落库结果行重建 DTO（explanation_json 若为结构化对象则优先使用，否则回退模板重算） */
    private RecommendationResultDto toResultDtoFromRow(RecommendationDao.ResultRow row,
                                                       Map<Long, CareerDirection> dirById) {
        CareerDirection direction = dirById.get(row.directionId());
        String directionCode = direction == null ? String.valueOf(row.directionId()) : direction.directionCode();
        Explanation explanation = parseExplanation(row.explanationJson());
        return new RecommendationResultDto(
                directionCode,
                row.rank(),
                round1(row.score()),
                confidenceOfPercent(row.score()),
                explanation.reasons(),
                explanation.strengths(),
                explanation.gaps(),
                explanation.semesterActions().isEmpty() && direction != null
                        ? engine.buildSemesterActions(direction)
                        : explanation.semesterActions(),
                null);
    }

    /** 结构化解释 JSON 落库：{ reasons: [...], strengths: [...], gaps: [...], semesterActions: [...], aiReason: ... } */
    private String toExplanationJson(RecommendationResultDto dto, String aiReason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reasons", dto.reasons());
        m.put("strengths", dto.strengths());
        m.put("gaps", dto.gaps());
        m.put("semesterActions", dto.semesterActions());
        m.put("aiReason", aiReason);
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 结构化解释（落库/回读） */
    private record Explanation(List<String> reasons, List<String> strengths,
                               List<String> gaps, List<String> semesterActions) {
    }

    /** 从落库 explanation_json 解析结构化解释（reasons/strengths/gaps/semesterActions） */
    private Explanation parseExplanation(String explanationJson) {
        if (explanationJson == null || explanationJson.isBlank()) {
            return new Explanation(List.of(), List.of(), List.of(), List.of());
        }
        try {
            Map<String, Object> m = objectMapper.readValue(explanationJson,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            return new Explanation(
                    stringList(m.get("reasons")),
                    stringList(m.get("strengths")),
                    stringList(m.get("gaps")),
                    stringList(m.get("semesterActions")));
        } catch (Exception e) {
            log.debug("解析 explanation_json 失败，忽略", e);
            return new Explanation(List.of(), List.of(), List.of(), List.of());
        }
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /** 百分制分数 → 置信度枚举（历史/详情回读时用） */
    private String confidenceOfPercent(double percent) {
        if (percent >= 80.0) {
            return "HIGH";
        }
        if (percent >= 60.0) {
            return "MEDIUM";
        }
        return "LOW";
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
            return Map.of();
        }
    }

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

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String nowIso() {
        return java.time.LocalDateTime.now().toString();
    }
}
