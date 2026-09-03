package com.rickgao.careercore.modules.direction.mapper;

import com.rickgao.careercore.modules.direction.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 学生收藏方向 Mapper。 */
@Mapper
public interface FavoriteMapper {

    List<Favorite> selectByStudent(@Param("studentId") String studentId);

    Favorite selectByStudentAndDirection(@Param("studentId") String studentId,
                                         @Param("directionId") String directionId);

    int insert(Favorite favorite);

    int deleteByStudentAndDirection(@Param("studentId") String studentId,
                                    @Param("directionId") String directionId);
}
