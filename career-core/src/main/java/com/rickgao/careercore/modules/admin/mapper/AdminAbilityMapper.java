package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.AbilityTag;
import com.rickgao.careercore.modules.admin.vo.AbilityTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 管理端-能力标签 Mapper。 */
@Mapper
public interface AdminAbilityMapper {

    List<AbilityTagVO> selectAbilityPage(@Param("category") String category,
                                         @Param("keyword") String keyword,
                                         @Param("sortColumn") String sortColumn,
                                         @Param("sortDir") String sortDir,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    long countAbilities(@Param("category") String category,
                        @Param("keyword") String keyword);

    AbilityTag findById(@Param("id") String id);

    int insert(AbilityTag tag);

    int updatePartial(@Param("id") String id,
                      @Param("name") String name,
                      @Param("category") String category,
                      @Param("status") String status);
}
