package com.rickgao.careercore.modules.admin.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhitelistCsvParserTest {

    @Test
    void parse_stripsBomAndSkipsBlankLines() throws Exception {
        String csv = "\uFEFF2026110001,计科2601,abc123\n\n2026110002,软工2601,\n";
        List<WhitelistCsvParser.Row> rows = WhitelistCsvParser.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getRow());
        assertEquals("2026110001", rows.get(0).getStudentNo());
        assertEquals("abc123", rows.get(0).getVerifyCode());
        assertEquals(3, rows.get(1).getRow());
        assertEquals("2026110002", rows.get(1).getStudentNo());
        assertEquals("", rows.get(1).getVerifyCode());
    }

    @Test
    void parse_twoColumnsTreatsSecondAsVerifyCode() throws Exception {
        String csv = "2026110001,abc123\n";
        List<WhitelistCsvParser.Row> rows = WhitelistCsvParser.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, rows.size());
        assertEquals("2026110001", rows.get(0).getStudentNo());
        assertEquals("", rows.get(0).getClassName());
        assertEquals("abc123", rows.get(0).getVerifyCode());
    }
}
