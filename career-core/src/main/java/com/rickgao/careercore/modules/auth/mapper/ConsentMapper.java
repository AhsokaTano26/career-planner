package com.rickgao.careercore.modules.auth.mapper;

import com.rickgao.careercore.modules.auth.entity.ConsentDocument;
import com.rickgao.careercore.modules.auth.entity.ConsentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 隐私授权 Mapper(文档 + 记录)。
 */
@Mapper
public interface ConsentMapper {

    /** 当前已发布的授权文档 */
    ConsentDocument findPublished();

    ConsentDocument findByVersion(@Param("version") String version);

    int insertRecord(ConsentRecord record);

    /** 用户最近一次同意记录 */
    ConsentRecord findLatestByUserId(@Param("userId") String userId);
}
