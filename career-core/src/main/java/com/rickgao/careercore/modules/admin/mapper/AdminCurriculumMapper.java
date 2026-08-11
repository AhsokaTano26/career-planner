package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.Course;
import com.rickgao.careercore.modules.admin.entity.CurriculumImportItem;
import com.rickgao.careercore.modules.admin.entity.CurriculumImportJob;
import com.rickgao.careercore.modules.admin.entity.CurriculumVersion;
import com.rickgao.careercore.modules.admin.vo.CurriculumImportJobVO;
import com.rickgao.careercore.modules.admin.vo.CurriculumVersionVO;
import com.rickgao.careercore.modules.admin.vo.ImportItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理端-培养方案 Mapper。 */
@Mapper
public interface AdminCurriculumMapper {

    // ---- 任务 ----
    int insertJob(CurriculumImportJob job);

    CurriculumImportJob findJobById(@Param("id") String id);

    List<CurriculumImportJobVO> selectJobPage(@Param("sortColumn") String sortColumn,
                                              @Param("sortDir") String sortDir,
                                              @Param("offset") int offset,
                                              @Param("size") int size);

    long countJobs();

    int updateJobStatus(@Param("id") String id, @Param("status") String status);

    int updateJobParseResult(@Param("id") String id,
                             @Param("status") String status,
                             @Param("totalItems") Integer totalItems,
                             @Param("parsedItems") Integer parsedItems,
                             @Param("confidence") Double confidence,
                             @Param("errorMessage") String errorMessage);

    // ---- 条目 ----
    int insertItem(CurriculumImportItem item);

    CurriculumImportItem findItemById(@Param("id") String id);

    List<ImportItemVO> selectItemPage(@Param("jobId") String jobId,
                                      @Param("status") String status,
                                      @Param("sortColumn") String sortColumn,
                                      @Param("sortDir") String sortDir,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    long countItems(@Param("jobId") String jobId,
                    @Param("status") String status);

    int updateItemContent(CurriculumImportItem item);

    int updateItemTags(@Param("id") String id, @Param("abilityTagsJson") String abilityTagsJson);

    int updateItemStatus(@Param("id") String id, @Param("status") String status);

    List<CurriculumImportItem> selectApprovedItems(@Param("jobId") String jobId);

    // ---- 版本 ----
    int insertVersion(CurriculumVersion version);

    List<CurriculumVersionVO> selectVersionPage(@Param("sortColumn") String sortColumn,
                                                @Param("sortDir") String sortDir,
                                                @Param("offset") int offset,
                                                @Param("size") int size);

    long countVersions();

    // ---- 正式课程 ----
    int insertCourse(Course course);

    int insertCourseAbilityTag(@Param("id") String id,
                               @Param("courseId") String courseId,
                               @Param("abilityTag") String abilityTag);
}
