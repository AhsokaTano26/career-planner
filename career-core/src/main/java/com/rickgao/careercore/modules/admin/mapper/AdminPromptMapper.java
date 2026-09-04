package com.rickgao.careercore.modules.admin.mapper;

import com.rickgao.careercore.modules.admin.entity.PromptVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端-提示词版本 Mapper。 */
@Mapper
public interface AdminPromptMapper {

    List<PromptVersion> listByScene(@Param("scene") String scene);

    PromptVersion findById(@Param("id") String id);

    PromptVersion findPublished(@Param("scene") String scene);

    int insert(PromptVersion version);

    int updatePublish(@Param("id") String id,
                      @Param("status") String status,
                      @Param("publishedAt") LocalDateTime publishedAt,
                      @Param("publishedBy") String publishedBy);
}

