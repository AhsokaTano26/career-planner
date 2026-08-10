package com.rickgao.careercore.modules.student.mapper;

import com.rickgao.careercore.modules.student.entity.StudentExperience;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生经历 Mapper。
 */
@Mapper
public interface StudentExperienceMapper {

    List<StudentExperience> findAllByStudentId(@Param("studentId") String studentId);

    List<StudentExperience> findByStudentIdPaged(@Param("studentId") String studentId,
                                                 @Param("sortColumn") String sortColumn,
                                                 @Param("sortDir") String sortDir,
                                                 @Param("offset") int offset,
                                                 @Param("size") int size);

    StudentExperience findByIdAndStudent(@Param("id") String id, @Param("studentId") String studentId);

    int insert(StudentExperience experience);

    int update(StudentExperience experience);

    int softDelete(@Param("id") String id, @Param("studentId") String studentId);
}
