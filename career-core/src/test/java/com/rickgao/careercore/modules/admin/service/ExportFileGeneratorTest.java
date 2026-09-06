package com.rickgao.careercore.modules.admin.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.admin.query.StudentExportRow;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportFileGeneratorTest {

    private final AdminExportMapper mapper = mock(AdminExportMapper.class);
    private final ExportFileGenerator generator =
            new ExportFileGenerator(mapper, "target/test-exports");

    // 敏感 PII 正则(与 career-ai desensitizer 对齐):手机号/邮箱/18 位身份证
    private static final Pattern PHONE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[\\dXx]");

    @Test
    void generate_whitelist_writesCsvAndMarksDone() throws Exception {
        ExportJob job = new ExportJob();
        job.setId("EX-100");
        job.setType("WHITELIST");
        when(mapper.findExportJobById("EX-100")).thenReturn(job);
        StudentWhitelist row = new StudentWhitelist();
        row.setId("WL001");
        row.setStudentNo("2026011301");
        row.setName("李明");
        row.setClassName("计科2601");
        row.setInitialPassword("202601");
        row.setUsed(false);
        when(mapper.selectWhitelistExport(any())).thenReturn(List.of(row));

        generator.generate("EX-100");

        verify(mapper).updateExportJobDone(eq("EX-100"), anyString(), anyString());
        byte[] bytes = Files.readAllBytes(Paths.get("target/test-exports/EX-100_WHITELIST.csv"));
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("\uFEFF"));
        assertTrue(content.contains("2026011301"));
        assertTrue(content.contains("李明"));
    }

    @Test
    void generate_unsupportedType_marksFailed() {
        ExportJob job = new ExportJob();
        job.setId("EX-101");
        job.setType("UNKNOWN");
        when(mapper.findExportJobById("EX-101")).thenReturn(job);
        generator.generate("EX-101");
        verify(mapper).updateExportJobFailed(eq("EX-101"), anyString());
    }

    // 边界 D:导出内容不得含 phone/email/身份证号(生涯系统导出列不输出这些字段)
    @Test
    void generate_studentData_导出不含敏感PII() throws Exception {
        ExportJob job = new ExportJob();
        job.setId("EX-102");
        job.setType("STUDENT_DATA");
        when(mapper.findExportJobById("EX-102")).thenReturn(job);
        StudentExportRow row = new StudentExportRow();
        row.setStudentNo("2026011301");
        row.setName("王芳");
        row.setClassName("计科2601");
        row.setGrade("2026级");
        row.setMajorCategory("计算机类");
        row.setCompleteness(80);
        when(mapper.selectStudentExport(any(), any())).thenReturn(List.of(row));

        generator.generate("EX-102");

        byte[] bytes = Files.readAllBytes(Paths.get("target/test-exports/EX-102_STUDENT_DATA.csv"));
        String content = new String(bytes, StandardCharsets.UTF_8);
        // 负向控制:确认扫描器自身有效(若未来误加 phone 列会立刻暴露)
        assertTrue(PHONE.matcher("13812345678").find());
        assertFalse(PHONE.matcher(content).find(), "导出含手机号");
        assertFalse(EMAIL.matcher(content).find(), "导出含邮箱");
        assertFalse(ID_CARD.matcher(content).find(), "导出含身份证号");
    }

    // 边界 D:AI 调用日志导出仅含哈希/引用,不得含 phone/email/身份证号
    @Test
    void generate_aiLog_导出仅含哈希无PII() throws Exception {
        ExportJob job = new ExportJob();
        job.setId("EX-104");
        job.setType("AI_LOG");
        when(mapper.findExportJobById("EX-104")).thenReturn(job);
        AiCallLog log = new AiCallLog();
        log.setId("L1");
        log.setUserRef("student_ref_8f3a");
        log.setScene("career_chat");
        log.setModelName("deepseek/m1");
        log.setRequestHash("rhash123");
        when(mapper.selectAiLogExport(any(), any(), any(), any())).thenReturn(List.of(log));

        generator.generate("EX-104");

        byte[] bytes = Files.readAllBytes(Paths.get("target/test-exports/EX-104_AI_LOG.csv"));
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("rhash123"));
        assertFalse(PHONE.matcher(content).find(), "AI 日志导出含手机号");
        assertFalse(EMAIL.matcher(content).find(), "AI 日志导出含邮箱");
        assertFalse(ID_CARD.matcher(content).find(), "AI 日志导出含身份证号");
    }

    // 边界 C:导出失败日志仅记 jobId,不得泄露 PII
    @Test
    void generate_失败日志不含PII() {
        ExportJob job = new ExportJob();
        job.setId("EX-103");
        job.setType("WHITELIST");
        when(mapper.findExportJobById("EX-103")).thenReturn(job);
        when(mapper.selectWhitelistExport(any())).thenThrow(new RuntimeException("db down"));

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            generator.generate("EX-103");
        } finally {
            root.detachAppender(appender);
            appender.stop();
        }

        String logs = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(logs.contains("EX-103"), "失败日志应包含 jobId 便于排查");
        assertFalse(PHONE.matcher(logs).find(), "日志含手机号");
        assertFalse(EMAIL.matcher(logs).find(), "日志含邮箱");
        assertFalse(ID_CARD.matcher(logs).find(), "日志含身份证号");
    }

    private String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
