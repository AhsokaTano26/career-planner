package com.rickgao.careercore.modules.auth.mapper;

import com.rickgao.careercore.modules.auth.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 刷新令牌 Mapper。
 */
@Mapper
public interface RefreshTokenMapper {

    int insert(RefreshToken refreshToken);

    RefreshToken findByToken(@Param("token") String token);

    /** 按哈希精确查询（新路径；双写兼容期内与 findByToken 并存）。 */
    RefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);

    /** 老行回填哈希（双写兼容一轮；下轮切读后删除）。 */
    int backfillTokenHash(@Param("id") String id, @Param("tokenHash") String tokenHash);

    /** 作废某用户全部刷新令牌(登出/改密/重置密码时使用) */
    int revokeByUserId(@Param("userId") String userId);

    /** 作废单个刷新令牌(刷新轮换) */
    int revoke(@Param("id") String id);
}
