package com.rickgao.careercore.modules.student.mapper;

import com.rickgao.careercore.modules.student.entity.DeletionRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 删除本人信息申请 Mapper。
 */
@Mapper
public interface DeletionRequestMapper {

    int insert(DeletionRequest request);
}
