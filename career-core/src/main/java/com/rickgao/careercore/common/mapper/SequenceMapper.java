package com.rickgao.careercore.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字符串 ID 序列映射器。
 * 采用 MySQL LAST_INSERT_ID 技巧原子递增(需在事务内调用,保证同一连接)。
 */
@Mapper
public interface SequenceMapper {

    void increment(@Param("seqName") String seqName);

    Long lastInsertId();
}
