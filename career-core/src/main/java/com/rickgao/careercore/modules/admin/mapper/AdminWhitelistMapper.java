package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.vo.WhitelistEntryVO;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理端-白名单 Mapper。
 */
@Mapper
public interface AdminWhitelistMapper {

    List<WhitelistEntryVO> selectWhitelistPage(@Param("used") Boolean used,
                                               @Param("keyword") String keyword,
                                               @Param("sortColumn") String sortColumn,
                                               @Param("sortDir") String sortDir,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    long countWhitelist(@Param("used") Boolean used,
                        @Param("keyword") String keyword);

    StudentWhitelist findById(@Param("id") String id);

    int insert(StudentWhitelist entry);

    int deleteById(@Param("id") String id);
}
