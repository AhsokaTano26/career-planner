package com.rickgao.careercore.modules.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 访问令牌黑名单 Mapper。
 */
@Mapper
public interface TokenBlacklistMapper {

    int insert(@Param("jti") String jti,
               @Param("userId") String userId,
               @Param("expiresAt") LocalDateTime expiresAt);

    /** 返回 0 表示不在黑名单 */
    int exists(@Param("jti") String jti);
}
