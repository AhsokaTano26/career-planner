package com.rickgao.careercore.modules.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.query.StudentExportRow;
import com.rickgao.careercore.modules.admin.vo.OperationLogVO;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 导出文件异步生成器(线程池 exportExecutor)。
 * 生成 CSV(UTF-8 带 BOM),成功置 DONE,失败置 FAILED。
 */
@Slf4j
@Component
public class ExportFileGenerator {

    private final AdminExportMapper mapper;
    private final String exportDir;

    public ExportFileGenerator(AdminExportMapper mapper,
                               @Value("${exports.dir:./data/exports}") String exportDir) {
        this.mapper = mapper;
        this.exportDir = exportDir;
    }

    @Async("exportExecutor")
    public void generate(String jobId) {
        try {
            ExportJob job = mapper.findExportJobById(jobId);
            if (job == null) {
                return;
            }
            String csv = buildCsv(job);
            Path target = Paths.get(exportDir, job.getId() + "_" + job.getType() + ".csv");
            Files.createDirectories(target.getParent());
            Files.write(target, ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8));
            mapper.updateExportJobDone(
                    jobId, "/api/v1/admin/exports/" + jobId + "/download", target.toString());
        } catch (Exception e) {
            log.error("导出任务生成失败 jobId={}", jobId, e);
            String message = e.getMessage() == null ? "生成失败" : e.getMessage();
            mapper.updateExportJobFailed(jobId, message.length() > 200 ? message.substring(0, 200) : message);
        }
    }

    private String buildCsv(ExportJob job) throws IOException {
        Map<String, Object> filters = parseFilters(job.getFiltersJson());
        return switch (job.getType()) {
            case "WHITELIST" -> whitelistCsv(filters);
            case "OPERATION_LOG" -> operationLogCsv(filters);
            case "AI_LOG" -> aiLogCsv(filters);
            case "DIRECTION_LIB" -> directionCsv(filters);
            case "STUDENT_DATA" -> studentCsv(filters);
            default -> throw new IllegalStateException("不支持的导出类型: " + job.getType());
        };
    }

    private String whitelistCsv(Map<String, Object> filters) {
        List<String> header = List.of("id", "student_no", "name", "class_name", "verify_code", "used", "created_at");
        List<List<String>> rows = new ArrayList<>();
        for (StudentWhitelist w : mapper.selectWhitelistExport(bool(filters.get("used")))) {
            rows.add(Arrays.asList(
                    w.getId(), w.getStudentNo(), w.getName(), w.getClassName(),
                    w.getVerifyCode(), String.valueOf(w.getUsed()),
                    w.getCreatedAt() == null ? "" : w.getCreatedAt().toString()));
        }
        return toCsv(header, rows);
    }

    private String operationLogCsv(Map<String, Object> filters) {
        List<String> header = List.of("id", "time", "operator", "action", "target", "detail", "ip");
        List<List<String>> rows = new ArrayList<>();
        for (OperationLogVO o : mapper.selectOperationLogExport(
                str(filters.get("action")), str(filters.get("operator")),
                dt(filters.get("from")), dt(filters.get("to")))) {
            rows.add(Arrays.asList(
                    o.getId(), o.getTime() == null ? "" : o.getTime().toString(),
                    o.getOperator(), o.getAction(), o.getTarget(), o.getDetail(), o.getIp()));
        }
        return toCsv(header, rows);
    }

    private String aiLogCsv(Map<String, Object> filters) {
        List<String> header = List.of(
                "id", "time", "user_ref", "scene", "model_name", "prompt_version",
                "duration_ms", "status", "token_estimate", "request_hash");
        List<List<String>> rows = new ArrayList<>();
        for (AiCallLog a : mapper.selectAiLogExport(
                str(filters.get("scene")), str(filters.get("status")),
                dt(filters.get("from")), dt(filters.get("to")))) {
            rows.add(Arrays.asList(
                    a.getId(), a.getCreatedAt() == null ? "" : a.getCreatedAt().toString(),
                    a.getUserRef(), a.getScene(), a.getModelName(), a.getPromptVersion(),
                    a.getDurationMs() == null ? "" : String.valueOf(a.getDurationMs()),
                    a.getStatus(), a.getTokenEstimate() == null ? "" : String.valueOf(a.getTokenEstimate()),
                    a.getRequestHash()));
        }
        return toCsv(header, rows);
    }

    private String directionCsv(Map<String, Object> filters) {
        List<String> header = List.of("id", "name", "path", "status", "sort_order", "updated_at");
        List<List<String>> rows = new ArrayList<>();
        for (CareerDirection d : mapper.selectDirectionExport(
                str(filters.get("path")), str(filters.get("status")))) {
            rows.add(Arrays.asList(
                    d.getId(), d.getName(), d.getPath(), d.getStatus(),
                    d.getSortOrder() == null ? "" : String.valueOf(d.getSortOrder()),
                    d.getUpdatedAt() == null ? "" : d.getUpdatedAt().toString()));
        }
        return toCsv(header, rows);
    }

    private String studentCsv(Map<String, Object> filters) {
        List<String> header = List.of(
                "student_no", "name", "class_name", "grade", "major_category",
                "completeness", "path", "assessed", "primary_goal", "last_review");
        List<List<String>> rows = new ArrayList<>();
        for (StudentExportRow s : mapper.selectStudentExport(
                str(filters.get("className")), str(filters.get("grade")))) {
            rows.add(Arrays.asList(
                    s.getStudentNo(), s.getName(), s.getClassName(), s.getGrade(), s.getMajorCategory(),
                    s.getCompleteness() == null ? "" : String.valueOf(s.getCompleteness()),
                    s.getPath(), String.valueOf(s.getAssessed()), s.getPrimaryGoal(),
                    s.getLastReview() == null ? "" : s.getLastReview().toString()));
        }
        return toCsv(header, rows);
    }

    private String toCsv(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        appendCsvLine(sb, header);
        for (List<String> row : rows) {
            appendCsvLine(sb, row);
        }
        return sb.toString();
    }

    private void appendCsvLine(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = cells.get(i) == null ? "" : cells.get(i);
            if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
                sb.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(value);
            }
        }
        sb.append("\r\n");
    }

    private Map<String, Object> parseFilters(String filtersJson) {
        if (!StringUtils.hasText(filtersJson)) {
            return Map.of();
        }
        try {
            return JsonUtil.parse(filtersJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String str(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return StringUtils.hasText(s) ? s : null;
    }

    private Boolean bool(Object value) {
        return value == null ? null : Boolean.valueOf(String.valueOf(value));
    }

    private LocalDateTime dt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
