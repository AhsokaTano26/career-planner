package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.AiCallLog;
import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.entity.ExportJob;
import com.rickgao.careercore.modules.admin.query.StudentExportRow;
import com.rickgao.careercore.modules.admin.vo.AiCallLogVO;
import com.rickgao.careercore.modules.admin.vo.ExportJobVO;
import com.rickgao.careercore.modules.admin.vo.OperationLogVO;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端-日志与导出 Mapper。 */
@Mapper
public interface AdminExportMapper {

    // ---- 导出任务 ----
    int insertExportJob(ExportJob job);

    ExportJob findExportJobById(@Param("id") String id);

    List<ExportJobVO> selectExportJobPage(@Param("sortColumn") String sortColumn,
                                          @Param("sortDir") String sortDir,
                                          @Param("offset") int offset,
                                          @Param("size") int size);

    long countExportJobs();

    int updateExportJobDone(@Param("id") String id,
                            @Param("downloadUrl") String downloadUrl,
                            @Param("filePath") String filePath);

    int updateExportJobFailed(@Param("id") String id, @Param("errorMessage") String errorMessage);

    // ---- 导出数据源 ----
    List<StudentWhitelist> selectWhitelistExport(@Param("used") Boolean used);

    List<OperationLogVO> selectOperationLogExport(@Param("action") String action,
                                                  @Param("operator") String operator,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to);

    List<AiCallLog> selectAiLogExport(@Param("scene") String scene,
                                      @Param("status") String status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    List<CareerDirection> selectDirectionExport(@Param("path") String path,
                                                @Param("status") String status);

    List<StudentExportRow> selectStudentExport(@Param("className") String className,
                                               @Param("grade") String grade);

    // ---- 日志分页 ----
    List<AiCallLogVO> selectAiLogPage(@Param("scene") String scene,
                                      @Param("status") String status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("sortColumn") String sortColumn,
                                      @Param("sortDir") String sortDir,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countAiLogs(@Param("scene") String scene,
                     @Param("status") String status,
                     @Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to);

    List<OperationLogVO> selectOperationLogPage(@Param("action") String action,
                                                @Param("operator") String operator,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("sortColumn") String sortColumn,
                                                @Param("sortDir") String sortDir,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    long countOperationLogs(@Param("action") String action,
                            @Param("operator") String operator,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);
}
