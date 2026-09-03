package com.rickgao.careercore.config;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动初始化:确保管理员账号存在。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    // Demo 精简点:管理员初始密码固定;生产环境应从环境变量/密钥管理读取
    private static final String ADMIN_PASSWORD = "Admin@2026";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    public DataInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder, IdGenerator idGenerator) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
    }

    @Override
    public void run(String... args) {
        if (sysUserMapper.findByUsername(ADMIN_USERNAME) == null) {
            SysUser admin = new SysUser();
            admin.setId(idGenerator.userId());
            admin.setUsername(ADMIN_USERNAME);
            admin.setName("系统管理员");
            admin.setRole(CommonConstants.ROLE_ADMIN);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setStatus(CommonConstants.USER_STATUS_ACTIVE);
            admin.setConsentAgreed(true);
            admin.setPasswordChangeRequired(true);
            sysUserMapper.insert(admin);
            log.info("初始化管理员账号: {}", ADMIN_USERNAME);
        }
    }
}
