package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.idempotency.IdempotentSupplier;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.JsonUtil;
import com.rickgao.careercore.modules.admin.dto.BatchReviewRequest;
import com.rickgao.careercore.modules.admin.dto.CurriculumPublishRequest;
import com.rickgao.careercore.modules.admin.dto.ImportItemUpdate;
import com.rickgao.careercore.modules.admin.entity.Course;
import com.rickgao.careercore.modules.admin.entity.CurriculumImportItem;
import com.rickgao.careercore.modules.admin.entity.CurriculumImportJob;
import com.rickgao.careercore.modules.admin.entity.CurriculumVersion;
import com.rickgao.careercore.modules.admin.mapper.AdminCurriculumMapper;
import com.rickgao.careercore.modules.admin.service.AdminCurriculumService;
import com.rickgao.careercore.modules.admin.vo.CurriculumImportJobVO;
import com.rickgao.careercore.modules.admin.vo.CurriculumVersionVO;
import com.rickgao.careercore.modules.admin.vo.ImportItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCurriculumServiceImplTest {

    private final AdminCurriculumMapper mapper = mock(AdminCurriculumMapper.class);
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final AdminCurriculumService service =
            new AdminCurriculumServiceImpl(mapper, idGenerator, idempotencyService, "target/test-curricula");

    @BeforeEach
    void setUp() {
        when(idGenerator.curriculumJobId()).thenReturn("CJ-100");
        when(idGenerator.curriculumItemId()).thenReturn("IT-100");
        when(idGenerator.curriculumVersionId()).thenReturn("CV-100");
        when(idGenerator.courseId()).thenReturn("CRS-100");
        when(idGenerator.courseAbilityTagId()).thenReturn("CAT-100");
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    IdempotentSupplier<ApiResponse<?>> supplier = inv.getArgument(4);
                    return supplier.get();
                });
    }

    @Test
    void importFile_invalidExtension_throwsValidation() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));
        BizException ex = assertThrows(BizException.class,
                () -> service.importFile("ADMIN1", "/curricula/import", "k1", file));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void importFile_ok_createsUploadedJob() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "plan.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8));
        CurriculumImportJobVO vo = service.importFile("ADMIN1", "/curricula/import", "k1", file);
        ArgumentCaptor<CurriculumImportJob> captor = ArgumentCaptor.forClass(CurriculumImportJob.class);
        verify(mapper).insertJob(captor.capture());
        assertEquals("UPLOADED", captor.getValue().getStatus());
        assertEquals("PDF", captor.getValue().getFileType());
        assertTrue(captor.getValue().getFilePath().contains("CJ-100_plan.pdf"));
        assertEquals("CJ-100", vo.getId());
    }

    @Test
    void listItems_missingJobId_throwsValidation() {
        BizException ex = assertThrows(BizException.class,
                () -> service.listItems(null, null, 1, 20, null));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void listItems_mapsPage() {
        ImportItemVO vo = new ImportItemVO();
        vo.setId("IT-100");
        when(mapper.countItems("CJ-100", "PENDING")).thenReturn(3L);
        when(mapper.selectItemPage("CJ-100", "PENDING", "created_at", "DESC", 0, 20))
                .thenReturn(List.of(vo));
        PageResult<ImportItemVO> result = service.listItems("CJ-100", "PENDING", 1, 20, null);
        assertEquals(3, result.getTotal());
        assertEquals("IT-100", result.getList().get(0).getId());
    }

    @Test
    void reviewItem_notFound_throws404() {
        when(mapper.findItemById("IT-999")).thenReturn(null);
        BizException ex = assertThrows(BizException.class,
                () -> service.reviewItem("ADMIN1", "/items/{itemId}", "k1", "IT-999", new ImportItemUpdate()));
        assertEquals(ResultCode.RESOURCE_NOT_FOUND, ex.getResultCode());
    }

    @Test
    void reviewItem_notPending_throws409() {
        when(mapper.findItemById("IT-100")).thenReturn(item("IT-100", "APPROVED", "CS101", "程序设计基础"));
        ImportItemUpdate update = new ImportItemUpdate();
        update.setStatus("REJECTED");
        BizException ex = assertThrows(BizException.class,
                () -> service.reviewItem("ADMIN1", "/items/{itemId}", "k1", "IT-100", update));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void reviewItem_approveMissingCourseCode_throwsValidation() {
        when(mapper.findItemById("IT-100")).thenReturn(item("IT-100", "PENDING", null, "无代码课程"));
        ImportItemUpdate update = new ImportItemUpdate();
        update.setStatus("APPROVED");
        BizException ex = assertThrows(BizException.class,
                () -> service.reviewItem("ADMIN1", "/items/{itemId}", "k1", "IT-100", update));
        assertEquals(ResultCode.VALIDATION_ERROR, ex.getResultCode());
    }

    @Test
    void reviewItem_ok_approves() {
        when(mapper.findItemById("IT-100")).thenReturn(item("IT-100", "PENDING", "CS101", "程序设计基础"));
        ImportItemUpdate update = new ImportItemUpdate();
        update.setCourseName("程序设计基础(修订)");
        update.setStatus("APPROVED");
        ImportItemVO result = service.reviewItem("ADMIN1", "/items/{itemId}", "k1", "IT-100", update);
        ArgumentCaptor<CurriculumImportItem> captor = ArgumentCaptor.forClass(CurriculumImportItem.class);
        verify(mapper).updateItemContent(captor.capture());
        assertEquals("APPROVED", captor.getValue().getStatus());
        assertEquals("程序设计基础(修订)", captor.getValue().getCourseName());
        assertEquals("APPROVED", result.getStatus());
    }

    @Test
    void batchReview_oneInvalid_noChangesApplied() {
        CurriculumImportItem pending = item("IT-100", "PENDING", "CS101", "程序设计基础");
        CurriculumImportItem approved = item("IT-101", "APPROVED", "CS201", "数据结构");
        when(mapper.findItemById("IT-100")).thenReturn(pending);
        when(mapper.findItemById("IT-101")).thenReturn(approved);
        BatchReviewRequest request = new BatchReviewRequest();
        BatchReviewRequest.BatchReviewAction a1 = new BatchReviewRequest.BatchReviewAction();
        a1.setItemId("IT-100");
        a1.setAction("APPROVE");
        BatchReviewRequest.BatchReviewAction a2 = new BatchReviewRequest.BatchReviewAction();
        a2.setItemId("IT-101");
        a2.setAction("APPROVE");
        request.setActions(List.of(a1, a2));
        BizException ex = assertThrows(BizException.class,
                () -> service.batchReview("ADMIN1", "/items/batch", "k1", request));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
        verify(mapper, never()).updateItemContent(any(CurriculumImportItem.class));
    }

    @Test
    void batchReview_merge_unionsTagsIntoTarget() {
        CurriculumImportItem source = item("IT-100", "PENDING", "CS201", "数据结构");
        source.setAbilityTagsJson("[\"algorithm\"]");
        CurriculumImportItem target = item("IT-101", "PENDING", "CS201", "数据结构(修订)");
        target.setAbilityTagsJson("[\"programming_basic\"]");
        when(mapper.findItemById("IT-100")).thenReturn(source);
        when(mapper.findItemById("IT-101")).thenReturn(target);
        BatchReviewRequest request = new BatchReviewRequest();
        BatchReviewRequest.BatchReviewAction action = new BatchReviewRequest.BatchReviewAction();
        action.setItemId("IT-100");
        action.setAction("MERGE");
        action.setTargetItemId("IT-101");
        action.setAbilityTags(List.of("data_structure"));
        request.setActions(List.of(action));

        List<ImportItemVO> result = service.batchReview("ADMIN1", "/items/batch", "k1", request);

        ArgumentCaptor<CurriculumImportItem> sourceCaptor = ArgumentCaptor.forClass(CurriculumImportItem.class);
        verify(mapper).updateItemContent(sourceCaptor.capture());
        assertEquals("MERGED", sourceCaptor.getValue().getStatus());
        assertEquals("IT-101", sourceCaptor.getValue().getMergedInto());
        ArgumentCaptor<String> tagsCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateItemTags(org.mockito.ArgumentMatchers.eq("IT-101"), tagsCaptor.capture());
        List<String> tags = JsonUtil.parse(tagsCaptor.getValue(),
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                });
        assertTrue(tags.containsAll(List.of("programming_basic", "algorithm", "data_structure")));
        assertEquals(2, result.size());
    }

    @Test
    void publish_jobNotReviewRequired_throws409() {
        CurriculumImportJob job = new CurriculumImportJob();
        job.setId("CJ-100");
        job.setStatus("UPLOADED");
        when(mapper.findJobById("CJ-100")).thenReturn(job);
        CurriculumPublishRequest request = new CurriculumPublishRequest();
        request.setJobId("CJ-100");
        request.setName("软件工程培养方案 2026 版");
        request.setMajor("软件工程");
        BizException ex = assertThrows(BizException.class,
                () -> service.publish("ADMIN1", "/publish", "k1", request));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void publish_hasPending_throws409() {
        CurriculumImportJob job = new CurriculumImportJob();
        job.setId("CJ-100");
        job.setStatus("REVIEW_REQUIRED");
        when(mapper.findJobById("CJ-100")).thenReturn(job);
        when(mapper.countItems("CJ-100", "PENDING")).thenReturn(1L);
        CurriculumPublishRequest request = new CurriculumPublishRequest();
        request.setJobId("CJ-100");
        request.setName("软件工程培养方案 2026 版");
        request.setMajor("软件工程");
        BizException ex = assertThrows(BizException.class,
                () -> service.publish("ADMIN1", "/publish", "k1", request));
        assertEquals(ResultCode.STATE_CONFLICT, ex.getResultCode());
    }

    @Test
    void publish_ok_createsVersionAndCourses() {
        CurriculumImportJob job = new CurriculumImportJob();
        job.setId("CJ-100");
        job.setStatus("REVIEW_REQUIRED");
        when(mapper.findJobById("CJ-100")).thenReturn(job);
        when(mapper.countItems("CJ-100", "PENDING")).thenReturn(0L);
        when(mapper.countItems("CJ-100", "REJECTED")).thenReturn(0L);
        CurriculumImportItem approved1 = item("IT-100", "APPROVED", "CS101", "程序设计基础");
        approved1.setAbilityTagsJson("[\"programming_basic\"]");
        CurriculumImportItem approved2 = item("IT-101", "APPROVED", "CS201", "数据结构");
        approved2.setAbilityTagsJson("[\"algorithm\"]");
        when(mapper.selectApprovedItems("CJ-100")).thenReturn(List.of(approved1, approved2));
        CurriculumPublishRequest request = new CurriculumPublishRequest();
        request.setJobId("CJ-100");
        request.setName("软件工程培养方案 2026 版");
        request.setMajor("软件工程");

        CurriculumVersionVO vo = service.publish("ADMIN1", "/publish", "k1", request);

        ArgumentCaptor<CurriculumVersion> versionCaptor = ArgumentCaptor.forClass(CurriculumVersion.class);
        verify(mapper).insertVersion(versionCaptor.capture());
        assertEquals(2, versionCaptor.getValue().getCourseCount());
        assertEquals("PUBLISHED", versionCaptor.getValue().getStatus());
        verify(mapper, times(2)).insertCourse(any(Course.class));
        verify(mapper, times(2)).insertCourseAbilityTag(anyString(), anyString(), anyString());
        verify(mapper).updateJobStatus("CJ-100", "PUBLISHED");
        assertEquals("CV-100", vo.getId());
        assertEquals(2, vo.getCourseCount());
    }

    private CurriculumImportItem item(String id, String status, String code, String name) {
        CurriculumImportItem item = new CurriculumImportItem();
        item.setId(id);
        item.setJobId("CJ-100");
        item.setCourseCode(code);
        item.setCourseName(name);
        item.setCredits(BigDecimal.valueOf(4));
        item.setHours(BigDecimal.valueOf(64));
        item.setStatus(status);
        return item;
    }
}
