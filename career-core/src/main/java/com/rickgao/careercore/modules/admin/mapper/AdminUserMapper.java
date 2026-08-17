package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.vo.AdminUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理端-用户查询/更新 Mapper。
 */
@Mapper
public interface AdminUserMapper {

    List<AdminUserVO> selectUserPage(@Param("keyword") String keyword,
                                     @Param("role") String role,
                                     @Param("status") String status,
                                     @Param("sortColumn") String sortColumn,
                                     @Param("sortDir") String sortDir,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    long countUsers(@Param("keyword") String keyword,
                    @Param("role") String role,
                    @Param("status") String status);

    /** 部分更新:仅更新传入的非空字段 */
    int updateStatusAndClass(@Param("id") String id,
                             @Param("status") String status,
                             @Param("className") String className);
}
