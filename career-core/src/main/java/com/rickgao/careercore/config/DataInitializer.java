package com.rickgao.careercore.config;

import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.modules.admin.mapper.AdminRelationMapper;
import com.rickgao.careercore.modules.advisor.entity.AdvisorStudentRelation;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用启动初始化:确保管理员 + 辅导员账号存在,并绑定学生关系。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_USERNAME = "admin";
    // Demo 精简点:管理员初始密码固定;生产环境应从环境变量/密钥管理读取
    private static final String ADMIN_PASSWORD = "Admin@2026";

    private static final String ADVISOR_USERNAME = "A2026001";
    private static final String ADVISOR_PASSWORD = "Adv@2026";
    private static final String ADVISOR_NAME = "辅导员张老师";
    private static final List<String> ADVISOR_STUDENT_IDS = List.of("S1003", "S1004");

    private final SysUserMapper sysUserMapper;
    private final AdminRelationMapper adminRelationMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    public DataInitializer(SysUserMapper sysUserMapper,
                           AdminRelationMapper adminRelationMapper,
                           PasswordEncoder passwordEncoder,
                           IdGenerator idGenerator) {
        this.sysUserMapper = sysUserMapper;
        this.adminRelationMapper = adminRelationMapper;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
    }

    @Override
    public void run(String... args) {
        seedAdminIfAbsent();
        seedAdvisorIfAbsent();
    }

    private void seedAdminIfAbsent() {
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

    private void seedAdvisorIfAbsent() {
        SysUser advisor = sysUserMapper.findByUsername(ADVISOR_USERNAME);
        if (advisor == null) {
            advisor = new SysUser();
            advisor.setId(idGenerator.userId());
            advisor.setUsername(ADVISOR_USERNAME);
            advisor.setName(ADVISOR_NAME);
            advisor.setRole(CommonConstants.ROLE_ADVISOR);
            advisor.setPasswordHash(passwordEncoder.encode(ADVISOR_PASSWORD));
            advisor.setStatus(CommonConstants.USER_STATUS_ACTIVE);
            advisor.setConsentAgreed(true);
            sysUserMapper.insert(advisor);
            log.info("初始化辅导员账号: {}", ADVISOR_USERNAME);
        }
        // Demo 精简点:启动时为该辅导员补齐学生关系(若不存在,含软删)
        for (String studentId : ADVISOR_STUDENT_IDS) {
            AdvisorStudentRelation existing = adminRelationMapper.findByAdvisorAndStudentIncludingDeleted(advisor.getId(), studentId);
            if (existing == null) {
                AdvisorStudentRelation rel = new AdvisorStudentRelation();
                rel.setId(idGenerator.advisorRelationId());
                rel.setAdvisorId(advisor.getId());
                rel.setStudentId(studentId);
                rel.setCreatedAt(LocalDateTime.now());
                rel.setUpdatedAt(LocalDateTime.now());
                rel.setDeleted(0);
                adminRelationMapper.insert(rel);
            }
        }
    }
}
