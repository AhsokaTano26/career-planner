package com.rickgao.careercore.modules.admin.service;

import lombok.Data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 白名单 CSV 解析器。
 * 约定:UTF-8(兼容 BOM)、无表头、每行 3 列(学号,班级,校验码);
 * 校验码可空(由服务层生成);空行跳过;行号从 1 计(数据行)。
 */
public final class WhitelistCsvParser {

    private static final String UTF8_BOM = "\uFEFF";

    private WhitelistCsvParser() {
    }

    public static List<Row> parse(InputStream inputStream) throws IOException {
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String content = lineNo == 1 && line.startsWith(UTF8_BOM)
                        ? line.substring(1)
                        : line;
                if (content.trim().isEmpty()) {
                    continue;
                }
                String[] parts = content.split(",", -1);
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                Row row = new Row();
                row.setRow(lineNo);
                if (parts.length >= 3) {
                    row.setStudentNo(parts[0]);
                    row.setClassName(parts[1]);
                    row.setVerifyCode(parts[2]);
                } else if (parts.length == 2) {
                    // 两列视为 学号,校验码(班级留空)
                    row.setStudentNo(parts[0]);
                    row.setClassName("");
                    row.setVerifyCode(parts[1]);
                } else {
                    row.setStudentNo(parts[0]);
                    row.setClassName("");
                    row.setVerifyCode("");
                }
                rows.add(row);
            }
        }
        return rows;
    }

    @Data
    public static class Row {
        private int row;
        private String studentNo;
        private String className;
        private String verifyCode;
    }
}
