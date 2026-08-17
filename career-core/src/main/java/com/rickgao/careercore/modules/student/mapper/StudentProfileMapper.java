package com.rickgao.careercore.modules.student.mapper;

import com.rickgao.careercore.modules.student.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 学生档案 Mapper。
 */
@Mapper
public interface StudentProfileMapper {

    StudentProfile findByUserId(@Param("userId") String userId);

    int insert(StudentProfile profile);

    int update(StudentProfile profile);
}
