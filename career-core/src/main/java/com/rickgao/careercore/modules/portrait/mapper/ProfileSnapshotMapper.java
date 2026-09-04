package com.rickgao.careercore.modules.portrait.mapper;

import com.rickgao.careercore.modules.portrait.entity.ProfileSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 画像快照映射器。
 */
@Mapper
public interface ProfileSnapshotMapper {

    int insert(ProfileSnapshot snapshot);

    ProfileSnapshot findById(@Param("id") String id);

    ProfileSnapshot findLatestByStudent(@Param("studentId") String studentId);

    List<ProfileSnapshot> listByStudent(@Param("studentId") String studentId);

    int updateFeedback(@Param("id") String id, @Param("feedbackJson") String feedbackJson);
}

