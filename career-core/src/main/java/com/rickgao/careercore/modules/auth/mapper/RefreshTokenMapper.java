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

    /** 作废某用户全部刷新令牌(登出/改密/重置密码时使用) */
    int revokeByUserId(@Param("userId") String userId);

    /** 作废单个刷新令牌(刷新轮换) */
    int revoke(@Param("id") String id);
}
