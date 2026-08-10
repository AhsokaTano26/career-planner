package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.TaskTemplate;
import com.rickgao.careercore.modules.admin.vo.TaskTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理端-任务模板 Mapper。 */
@Mapper
public interface AdminTemplateMapper {

    List<TaskTemplateVO> selectTemplatePage(@Param("directionId") String directionId,
                                            @Param("sortColumn") String sortColumn,
                                            @Param("sortDir") String sortDir,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    long countTemplates(@Param("directionId") String directionId);

    TaskTemplate findById(@Param("id") String id);

    int insert(TaskTemplate template);

    int updateContent(TaskTemplate template);

    int updateStatus(@Param("id") String id, @Param("status") String status);
}
