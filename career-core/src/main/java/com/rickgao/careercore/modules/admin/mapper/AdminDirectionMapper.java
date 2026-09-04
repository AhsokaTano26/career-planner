package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.CareerDirection;
import com.rickgao.careercore.modules.admin.vo.AdminDirectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理端-方向库 Mapper。 */
@Mapper
public interface AdminDirectionMapper {

    List<AdminDirectionVO> selectDirectionPage(@Param("path") String path,
                                               @Param("status") String status,
                                               @Param("keyword") String keyword,
                                               @Param("sortColumn") String sortColumn,
                                               @Param("sortDir") String sortDir,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    long countDirections(@Param("path") String path,
                         @Param("status") String status,
                         @Param("keyword") String keyword);

    CareerDirection findById(@Param("id") String id);

    int insert(CareerDirection direction);

    /** 全量更新内容字段(单行版本语义,服务层先加载合并) */
    int updateContent(CareerDirection direction);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    List<CareerDirection> selectAllPublished();
}
