package com.rickgao.careercore.modules.admin.service;

import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.mapper.AdminExportMapper;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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

    private String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
