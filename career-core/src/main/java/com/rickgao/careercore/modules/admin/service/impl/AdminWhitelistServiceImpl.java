package com.rickgao.careercore.modules.admin.service.impl;

import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.idempotency.IdempotencyService;
import com.rickgao.careercore.common.page.PageResult;
import com.rickgao.careercore.common.response.ApiResponse;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.dto.WhitelistCreate;
import com.rickgao.careercore.modules.admin.mapper.AdminWhitelistMapper;
import com.rickgao.careercore.modules.admin.service.AdminWhitelistService;
import com.rickgao.careercore.modules.admin.service.WhitelistCsvParser;
import com.rickgao.careercore.modules.admin.vo.WhitelistEntryVO;
import com.rickgao.careercore.modules.admin.vo.WhitelistImportResultVO;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import com.rickgao.careercore.modules.auth.mapper.StudentWhitelistMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 管理端-白名单服务实现。
 * CSV 约定:UTF-8(兼容 BOM)、无表头、列=学号,班级,校验码;总行数上限 700;重复学号该行失败。
 */
@Service
public class AdminWhitelistServiceImpl implements AdminWhitelistService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_IMPORT_ROWS = 700;
    private static final long MAX_IMPORT_BYTES = 1024 * 1024;
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "studentNo", "className");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminWhitelistMapper adminWhitelistMapper;
    private final StudentWhitelistMapper studentWhitelistMapper;
    private final IdGenerator idGenerator;
    private final IdempotencyService idempotencyService;

    public AdminWhitelistServiceImpl(AdminWhitelistMapper adminWhitelistMapper,
                                     StudentWhitelistMapper studentWhitelistMapper,
                                     IdGenerator idGenerator,
                                     IdempotencyService idempotencyService) {
        this.adminWhitelistMapper = adminWhitelistMapper;
        this.studentWhitelistMapper = studentWhitelistMapper;
        this.idGenerator = idGenerator;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public PageResult<WhitelistEntryVO> listWhitelist(Boolean used, String keyword,
                                                      Integer page, Integer size, String sort) {
        int currentPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int currentSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String[] sortPair = resolveSort(sort);
        long total = adminWhitelistMapper.countWhitelist(used, keyword);
        List<WhitelistEntryVO> list = adminWhitelistMapper.selectWhitelistPage(
                used, keyword, sortPair[0], sortPair[1],
                (currentPage - 1) * currentSize, currentSize);
        return PageResult.of(list, total, currentPage, currentSize);
    }

    @Override
    @Transactional
    public WhitelistEntryVO createWhitelist(String operatorId, String endpoint, String idempotencyKey,
                                            WhitelistCreate dto) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, WhitelistEntryVO.class,
                () -> ApiResponse.ok(doCreateWhitelist(dto))).getData();
    }

    @Override
    @Transactional
    public WhitelistImportResultVO importWhitelist(String operatorId, String endpoint, String idempotencyKey,
                                                   MultipartFile file) {
        return idempotencyService.execute(operatorId, endpoint, idempotencyKey, WhitelistImportResultVO.class,
                () -> ApiResponse.ok(doImportWhitelist(file))).getData();
    }

    @Override
    @Transactional
    public void deleteWhitelist(String operatorId, String endpoint, String idempotencyKey, String whitelistId) {
        idempotencyService.execute(operatorId, endpoint, idempotencyKey, Void.class,
                () -> {
                    StudentWhitelist entry = adminWhitelistMapper.findById(whitelistId);
                    if (entry == null) {
                        throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "白名单条目不存在");
                    }
                    if (Boolean.TRUE.equals(entry.getUsed())) {
                        throw new BizException(ResultCode.STATE_CONFLICT, "该学号已注册使用,禁止删除");
                    }
                    adminWhitelistMapper.deleteById(whitelistId);
                    return ApiResponse.ok();
                });
    }

    private WhitelistEntryVO doCreateWhitelist(WhitelistCreate dto) {
        String studentNo = dto.getStudentNo().trim();
        if (studentWhitelistMapper.findByStudentNo(studentNo) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "学号已存在");
        }
        StudentWhitelist entry = new StudentWhitelist();
        entry.setId(idGenerator.whitelistId());
        entry.setStudentNo(studentNo);
        entry.setClassName(StringUtils.hasText(dto.getClassName()) ? dto.getClassName().trim() : null);
        entry.setVerifyCode(StringUtils.hasText(dto.getVerifyCode())
                ? dto.getVerifyCode().trim()
                : generateVerifyCode());
        adminWhitelistMapper.insert(entry);
        return toVO(entry);
    }

    private WhitelistImportResultVO doImportWhitelist(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "请上传 CSV 文件");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "CSV 文件不能超过 1MB");
        }
        List<WhitelistCsvParser.Row> rows;
        try {
            rows = WhitelistCsvParser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "CSV 解析失败,请确认文件为 UTF-8 编码");
        }
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "导入行数超过上限 " + MAX_IMPORT_ROWS);
        }
        WhitelistImportResultVO result = new WhitelistImportResultVO();
        List<WhitelistImportResultVO.Failure> failures = new ArrayList<>();
        List<WhitelistCsvParser.Row> validRows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (WhitelistCsvParser.Row row : rows) {
            String studentNo = row.getStudentNo() == null ? "" : row.getStudentNo().trim();
            if (!StringUtils.hasText(studentNo)) {
                failures.add(failure(row.getRow(), "", "学号为空"));
                continue;
            }
            if (!seen.add(studentNo)) {
                failures.add(failure(row.getRow(), studentNo, "重复学号"));
                continue;
            }
            if (studentWhitelistMapper.findByStudentNo(studentNo) != null) {
                failures.add(failure(row.getRow(), studentNo, "重复学号"));
                continue;
            }
            row.setStudentNo(studentNo);
            validRows.add(row);
        }
        for (WhitelistCsvParser.Row row : validRows) {
            StudentWhitelist entry = new StudentWhitelist();
            entry.setId(idGenerator.whitelistId());
            entry.setStudentNo(row.getStudentNo());
            entry.setClassName(StringUtils.hasText(row.getClassName()) ? row.getClassName().trim() : null);
            entry.setVerifyCode(StringUtils.hasText(row.getVerifyCode())
                    ? row.getVerifyCode().trim()
                    : generateVerifyCode());
            adminWhitelistMapper.insert(entry);
        }
        result.setSuccessCount(validRows.size());
        result.setFailCount(failures.size());
        result.setFailures(failures);
        return result;
    }

    private WhitelistImportResultVO.Failure failure(int row, String studentNo, String reason) {
        WhitelistImportResultVO.Failure f = new WhitelistImportResultVO.Failure();
        f.setRow(row);
        f.setStudentNo(studentNo);
        f.setReason(reason);
        return f;
    }

    private WhitelistEntryVO toVO(StudentWhitelist entry) {
        WhitelistEntryVO vo = new WhitelistEntryVO();
        vo.setId(entry.getId());
        vo.setStudentNo(entry.getStudentNo());
        vo.setClassName(entry.getClassName());
        vo.setUsed(entry.getUsed());
        vo.setCreatedAt(entry.getCreatedAt());
        return vo;
    }

    private String generateVerifyCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
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
        String column = switch (field) {
            case "studentNo" -> "student_no";
            case "className" -> "class_name";
            default -> "created_at";
        };
        return new String[]{column, dir};
    }
}
