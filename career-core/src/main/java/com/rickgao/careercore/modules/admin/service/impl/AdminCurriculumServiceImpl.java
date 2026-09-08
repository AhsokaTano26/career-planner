package com.rickgao.careercore.modules.admin.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 管理端-培养方案服务实现。 */
@Service
public class AdminCurriculumServiceImpl implements AdminCurriculumService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ITEM_STATUS_ENUM = Set.of("PENDING", "APPROVED", "REJECTED", "MERGED");
    private static final Set<String> REVIEW_ACTION_ENUM = Set.of("APPROVE", "REJECT", "MERGE");
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt");

    private final AdminCurriculumMapper curriculumMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;
    private final String uploadDir;

    public AdminCurriculumServiceImpl(AdminCurriculumMapper curriculumMapper,
                                      IdGenerator idGenerator,
                                      IdempotencyService idempotencyService,
                                      @Value("${curricula.upload-dir:./data/curricula}") String uploadDir) {
        this.curriculumMapper = curriculumMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
        this.uploadDir = uploadDir;
    }

    @Override
    @Transactional
    public CurriculumImportJobVO importFile(String operatorId, String endpoint, String idempotencyKey,
                                            MultipartFile file) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, CurriculumImportJobVO.class,
                () -> ApiResponse.ok(doImportFile(operatorId, file))).getData();
    }

    @Override
    public PageResult<CurriculumImportJobVO> listJobs(Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = curriculumMapper.countJobs();
        List<CurriculumImportJobVO> list = curriculumMapper.selectJobPage(
                sortPair[0], sortPair[1], (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    public CurriculumImportJobVO getJob(String jobId) {
        CurriculumImportJob job = curriculumMapper.findJobById(jobId);
        if (job == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导入任务不存在");
        }
        return toJobVO(job);
    }

    @Override
    public PageResult<ImportItemVO> listItems(String jobId, String status,
                                              Integer page, Integer size, String sort) {
        if (!StringUtils.hasText(jobId)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "jobId 不能为空");
        }
        if (StringUtils.hasText(status) && !ITEM_STATUS_ENUM.contains(status)) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "status 仅支持 PENDING/APPROVED/REJECTED/MERGED");
        }
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        // Demo 增强点：前端/Apifox 常以空串传 status，归一为 null 以免 MyBatis 空串当成过滤条件
        String statusFilter = StringUtils.hasText(status) ? status : null;
        long total = curriculumMapper.countItems(jobId, statusFilter);
        List<ImportItemVO> list = curriculumMapper.selectItemPage(
                jobId, statusFilter, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public ImportItemVO reviewItem(String operatorId, String endpoint, String idempotencyKey,
                                   String itemId, ImportItemUpdate update) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, ImportItemVO.class,
                () -> ApiResponse.ok(doReviewItem(itemId, update))).getData();
    }

    @Override
    @Transactional
    public List<ImportItemVO> batchReview(String operatorId, String endpoint, String idempotencyKey,
                                          BatchReviewRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, List.class,
                () -> ApiResponse.ok(doBatchReview(request))).getData();
    }

    @Override
    @Transactional
    public CurriculumVersionVO publish(String operatorId, String endpoint, String idempotencyKey,
                                       CurriculumPublishRequest request) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, CurriculumVersionVO.class,
                () -> ApiResponse.ok(doPublish(operatorId, request))).getData();
    }

    @Override
    public PageResult<CurriculumVersionVO> listVersions(Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = curriculumMapper.countVersions();
        List<CurriculumVersionVO> list = curriculumMapper.selectVersionPage(
                sortPair[0], sortPair[1], (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    // ---------- 导入 ----------

    private CurriculumImportJobVO doImportFile(String operatorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "请上传培养方案文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "文件不能超过 20MB");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "仅支持 PDF / Word(.doc/.docx) 文档");
        }
        String jobId = idGenerator.curriculumJobId();
        String safeName = sanitize(filename);
        String storedName = jobId + "_" + safeName;
        Path target = Paths.get(uploadDir, storedName);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "文件保存失败");
        }
        CurriculumImportJob job = new CurriculumImportJob();
        job.setId(jobId);
        job.setFilename(filename);
        job.setFilePath(target.toString());
        job.setFileType("pdf".equals(ext) ? "PDF" : "WORD");
        job.setStatus("UPLOADED");
        job.setCreatedBy(operatorId);
        curriculumMapper.insertJob(job);
        // TODO(AI): 解析触发点 —— 由 AI 组接入 career-ai /pdf/parse 异步解析,
        // 完成后调用 updateJobParseResult 推进 UPLOADED -> PARSING -> REVIEW_REQUIRED/FAILED
        return toJobVO(job);
    }

    // ---------- 单条校核 ----------

    private ImportItemVO doReviewItem(String itemId, ImportItemUpdate update) {
        CurriculumImportItem item = curriculumMapper.findItemById(itemId);
        if (item == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "课程条目不存在");
        }
        if (!"PENDING".equals(item.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "仅 PENDING 条目可校核,当前状态: " + item.getStatus());
        }
        applyFieldUpdates(item, update);
        String targetStatus = StringUtils.hasText(update.getStatus()) ? update.getStatus() : null;
        if (targetStatus != null) {
            if (!"APPROVED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "status 仅支持 APPROVED/REJECTED");
            }
            if ("APPROVED".equals(targetStatus)
                    && (!StringUtils.hasText(item.getCourseCode()) || !StringUtils.hasText(item.getCourseName()))) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "审核通过前必须填写课程代码与课程名称");
            }
            item.setStatus(targetStatus);
        }
        curriculumMapper.updateItemContent(item);
        return toItemVO(item);
    }

    // ---------- 批量校核 ----------

    private List<ImportItemVO> doBatchReview(BatchReviewRequest request) {
        if (request.getActions() == null || request.getActions().isEmpty()) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "actions 不能为空");
        }
        // 第一阶段:整体校验,任一失败则不执行
        for (BatchReviewRequest.BatchReviewAction action : request.getActions()) {
            validateAction(action);
        }
        // 第二阶段:执行
        Set<String> affected = new LinkedHashSet<>();
        for (BatchReviewRequest.BatchReviewAction action : request.getActions()) {
            CurriculumImportItem item = curriculumMapper.findItemById(action.getItemId());
            switch (action.getAction()) {
                case "APPROVE" -> {
                    item.setStatus("APPROVED");
                    item.setAbilityTagsJson(unionTags(item.getAbilityTagsJson(), action.getAbilityTags()));
                    curriculumMapper.updateItemContent(item);
                    affected.add(item.getId());
                }
                case "REJECT" -> {
                    curriculumMapper.updateItemStatus(item.getId(), "REJECTED");
                    affected.add(item.getId());
                }
                case "MERGE" -> {
                    CurriculumImportItem target = curriculumMapper.findItemById(action.getTargetItemId());
                    item.setStatus("MERGED");
                    item.setMergedInto(target.getId());
                    curriculumMapper.updateItemContent(item);
                    target.setAbilityTagsJson(unionTags(
                            unionTags(target.getAbilityTagsJson(), parseStringList(item.getAbilityTagsJson())),
                            action.getAbilityTags()));
                    curriculumMapper.updateItemTags(target.getId(), target.getAbilityTagsJson());
                    affected.add(item.getId());
                    affected.add(target.getId());
                }
                default -> throw new BizException(ResultCode.VALIDATION_ERROR, "非法操作类型");
            }
        }
        List<ImportItemVO> result = new ArrayList<>();
        for (String id : affected) {
            result.add(toItemVO(curriculumMapper.findItemById(id)));
        }
        return result;
    }

    private void validateAction(BatchReviewRequest.BatchReviewAction action) {
        if (!StringUtils.hasText(action.getItemId())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "action.itemId 不能为空");
        }
        if (!StringUtils.hasText(action.getAction()) || !REVIEW_ACTION_ENUM.contains(action.getAction())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "action 仅支持 APPROVE/REJECT/MERGE");
        }
        CurriculumImportItem item = curriculumMapper.findItemById(action.getItemId());
        if (item == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "课程条目不存在: " + action.getItemId());
        }
        if (!"PENDING".equals(item.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT,
                    "仅 PENDING 条目可校核: " + action.getItemId() + " 当前状态 " + item.getStatus());
        }
        if ("APPROVE".equals(action.getAction())
                && (!StringUtils.hasText(item.getCourseCode()) || !StringUtils.hasText(item.getCourseName()))) {
            throw new BizException(ResultCode.VALIDATION_ERROR,
                    "审核通过前必须填写课程代码与课程名称: " + action.getItemId());
        }
        if ("MERGE".equals(action.getAction())) {
            if (!StringUtils.hasText(action.getTargetItemId()) || action.getTargetItemId().equals(action.getItemId())) {
                throw new BizException(ResultCode.VALIDATION_ERROR, "MERGE 必须指定不同的 targetItemId");
            }
            CurriculumImportItem target = curriculumMapper.findItemById(action.getTargetItemId());
            if (target == null) {
                throw new BizException(ResultCode.RESOURCE_NOT_FOUND,
                        "合并目标条目不存在: " + action.getTargetItemId());
            }
            if (!"PENDING".equals(target.getStatus())) {
                throw new BizException(ResultCode.STATE_CONFLICT,
                        "合并目标条目须为 PENDING: " + action.getTargetItemId());
            }
        }
    }

    // ---------- 发布 ----------

    private CurriculumVersionVO doPublish(String operatorId, CurriculumPublishRequest request) {
        CurriculumImportJob job = curriculumMapper.findJobById(request.getJobId());
        if (job == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "导入任务不存在");
        }
        if (!"REVIEW_REQUIRED".equals(job.getStatus())) {
            throw new BizException(ResultCode.STATE_CONFLICT,
                    "任务状态不允许发布,当前状态: " + job.getStatus());
        }
        long pending = curriculumMapper.countItems(job.getId(), "PENDING");
        long rejected = curriculumMapper.countItems(job.getId(), "REJECTED");
        if (pending > 0 || rejected > 0) {
            throw new BizException(ResultCode.STATE_CONFLICT, "存在未审核条目,无法发布");
        }
        List<CurriculumImportItem> approved = curriculumMapper.selectApprovedItems(job.getId());
        CurriculumVersion version = new CurriculumVersion();
        version.setId(idGenerator.curriculumVersionId());
        version.setName(request.getName().trim());
        version.setMajor(request.getMajor().trim());
        version.setCourseCount(approved.size());
        version.setStatus("PUBLISHED");
        version.setSourceJobId(job.getId());
        version.setPublishedAt(LocalDateTime.now());
        version.setPublishedBy(operatorId);
        curriculumMapper.insertVersion(version);
        for (CurriculumImportItem item : approved) {
            Course course = new Course();
            course.setId(idGenerator.courseId());
            course.setVersionId(version.getId());
            course.setCourseCode(item.getCourseCode());
            course.setCourseName(item.getCourseName());
            course.setSemester(item.getSemester());
            course.setCredits(item.getCredits());
            course.setHours(item.getHours());
            course.setCategory(item.getCategory());
            course.setModule(item.getModule());
            course.setPrerequisitesJson(item.getPrerequisitesJson());
            course.setSourceItemId(item.getId());
            curriculumMapper.insertCourse(course);
            for (String tag : parseStringList(item.getAbilityTagsJson())) {
                curriculumMapper.insertCourseAbilityTag(
                        idGenerator.courseAbilityTagId(), course.getId(), tag);
            }
        }
        curriculumMapper.updateJobStatus(job.getId(), "PUBLISHED");
        return toVersionVO(version);
    }

    // ---------- 工具 ----------

    private void applyFieldUpdates(CurriculumImportItem item, ImportItemUpdate update) {
        if (StringUtils.hasText(update.getCourseCode())) {
            item.setCourseCode(update.getCourseCode().trim());
        }
        if (StringUtils.hasText(update.getCourseName())) {
            item.setCourseName(update.getCourseName().trim());
        }
        if (StringUtils.hasText(update.getSemester())) {
            item.setSemester(update.getSemester().trim());
        }
        if (update.getCredits() != null) {
            item.setCredits(BigDecimal.valueOf(update.getCredits()));
        }
        if (update.getHours() != null) {
            item.setHours(BigDecimal.valueOf(update.getHours()));
        }
        if (StringUtils.hasText(update.getCategory())) {
            item.setCategory(update.getCategory().trim());
        }
        if (StringUtils.hasText(update.getModule())) {
            item.setModule(update.getModule().trim());
        }
        if (update.getPrerequisites() != null) {
            item.setPrerequisitesJson(JsonUtil.toJson(update.getPrerequisites()));
        }
        if (update.getAbilityTags() != null) {
            item.setAbilityTagsJson(JsonUtil.toJson(update.getAbilityTags()));
        }
    }

    private String unionTags(String existingJson, List<String> extra) {
        LinkedHashSet<String> tags = new LinkedHashSet<>(parseStringList(existingJson));
        if (extra != null) {
            tags.addAll(extra);
        }
        return JsonUtil.toJson(new ArrayList<>(tags));
    }

    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return JsonUtil.parse(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitize(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private CurriculumImportJobVO toJobVO(CurriculumImportJob job) {
        CurriculumImportJobVO vo = new CurriculumImportJobVO();
        vo.setId(job.getId());
        vo.setFilename(job.getFilename());
        vo.setStatus(job.getStatus());
        vo.setTotalItems(job.getTotalItems() == null ? 0 : job.getTotalItems());
        vo.setParsedItems(job.getParsedItems() == null ? 0 : job.getParsedItems());
        vo.setConfidence(job.getConfidence() == null ? null : job.getConfidence().doubleValue());
        vo.setCreatedAt(job.getCreatedAt());
        return vo;
    }

    private ImportItemVO toItemVO(CurriculumImportItem item) {
        ImportItemVO vo = new ImportItemVO();
        vo.setId(item.getId());
        vo.setJobId(item.getJobId());
        vo.setCourseCode(item.getCourseCode());
        vo.setCourseName(item.getCourseName());
        vo.setSemester(item.getSemester());
        vo.setCredits(item.getCredits() == null ? null : item.getCredits().doubleValue());
        vo.setHours(item.getHours() == null ? null : item.getHours().doubleValue());
        vo.setCategory(item.getCategory());
        vo.setModule(item.getModule());
        vo.setPrerequisites(parseStringList(item.getPrerequisitesJson()));
        vo.setAbilityTags(parseStringList(item.getAbilityTagsJson()));
        vo.setConfidence(item.getConfidence() == null ? null : item.getConfidence().doubleValue());
        vo.setPageRef(item.getPageRef());
        vo.setStatus(item.getStatus());
        return vo;
    }

    private CurriculumVersionVO toVersionVO(CurriculumVersion version) {
        CurriculumVersionVO vo = new CurriculumVersionVO();
        vo.setId(version.getId());
        vo.setName(version.getName());
        vo.setMajor(version.getMajor());
        vo.setCourseCount(version.getCourseCount());
        vo.setStatus(version.getStatus());
        vo.setPublishedAt(version.getPublishedAt());
        vo.setPublishedBy(version.getPublishedBy());
        return vo;
    }

    private String[] resolveSort(String sort) {
        String field = "createdAt";
        String dir = "DESC";
        if (StringUtils.hasText(sort)) {
            String raw = sort.startsWith("-") ? sort.substring(1) : sort;
            if (SORTABLE_FIELDS.contains(raw)) {
                field = raw;
                dir = sort.startsWith("-") ? "DESC" : "ASC";
            }
        }
        return new String[]{"created_at", dir};
    }
}
