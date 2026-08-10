package com.rickgao.careercore.modules.auth.mapper;

import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学号白名单 Mapper。
 */
@Mapper
public interface StudentWhitelistMapper {

    StudentWhitelist findByStudentNo(@Param("studentNo") String studentNo);

    int markUsed(@Param("studentNo") String studentNo);
}
