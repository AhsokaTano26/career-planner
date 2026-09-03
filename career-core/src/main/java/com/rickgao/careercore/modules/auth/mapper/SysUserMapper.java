package com.rickgao.careercore.modules.auth.mapper;

import com.rickgao.careercore.modules.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface SysUserMapper {

    SysUser findById(@Param("id") String id);

    SysUser findByUsername(@Param("username") String username);

    SysUser findByStudentNo(@Param("studentNo") String studentNo);

    int insert(SysUser user);

    /** 更新基础字段(姓名/年级/专业大类/班级/状态) */
    int updateBasic(SysUser user);

    /** 当前用户仅可更新自己的展示姓名，登录名、角色和状态由系统管理。 */
    int updateOwnName(@Param("id") String id, @Param("name") String name);

    int updatePassword(@Param("id") String id, @Param("passwordHash") String passwordHash,
                       @Param("passwordChangeRequired") boolean passwordChangeRequired);

    int updateConsentAgreed(@Param("id") String id, @Param("consentAgreed") boolean consentAgreed);

    int updateLastLoginAt(@Param("id") String id, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);
}
