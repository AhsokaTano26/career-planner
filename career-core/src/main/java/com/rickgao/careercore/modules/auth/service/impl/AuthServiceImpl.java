package com.rickgao.careercore.modules.auth.service.impl;

import com.rickgao.careercore.common.audit.AuditLogWriter;
import com.rickgao.careercore.common.constant.CommonConstants;
import com.rickgao.careercore.common.exception.BizException;
import com.rickgao.careercore.common.response.ResultCode;
import com.rickgao.careercore.common.util.IdGenerator;
import com.rickgao.careercore.common.util.TraceIdUtil;
import com.rickgao.careercore.modules.auth.dto.ConsentRequest;
import com.rickgao.careercore.modules.auth.dto.CurrentUserUpdateRequest;
import com.rickgao.careercore.modules.auth.dto.LoginRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordChangeRequest;
import com.rickgao.careercore.modules.auth.dto.PasswordResetRequest;
import com.rickgao.careercore.modules.auth.dto.RefreshRequest;
import com.rickgao.careercore.modules.auth.dto.RegisterRequest;
import com.rickgao.careercore.modules.auth.entity.ConsentDocument;
import com.rickgao.careercore.modules.auth.entity.ConsentRecord;
import com.rickgao.careercore.modules.auth.entity.RefreshToken;
import com.rickgao.careercore.modules.auth.entity.StudentWhitelist;
import com.rickgao.careercore.modules.auth.entity.SysUser;
import com.rickgao.careercore.modules.auth.mapper.ConsentMapper;
import com.rickgao.careercore.modules.auth.mapper.RefreshTokenMapper;
import com.rickgao.careercore.modules.auth.mapper.StudentWhitelistMapper;
import com.rickgao.careercore.modules.auth.mapper.SysUserMapper;
import com.rickgao.careercore.modules.auth.mapper.TokenBlacklistMapper;
import com.rickgao.careercore.modules.auth.service.AuthService;
import com.rickgao.careercore.modules.auth.vo.ConsentStatusVO;
import com.rickgao.careercore.modules.auth.vo.CurrentUserVO;
import com.rickgao.careercore.modules.auth.vo.TokenVO;
import com.rickgao.careercore.security.JwtUtil;
import com.rickgao.careercore.security.LoginAttemptTracker;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 认证与账号应用服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final StudentWhitelistMapper studentWhitelistMapper;
    private final ConsentMapper consentMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final AuditLogWriter auditLogWriter;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IdGenerator idGenerator;
    private final LoginAttemptTracker loginAttemptTracker;
    private final com.rickgao.careercore.security.AuthUserCache authUserCache;
    private final AuthServiceImpl self;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           StudentWhitelistMapper studentWhitelistMapper,
                           ConsentMapper consentMapper,
                           RefreshTokenMapper refreshTokenMapper,
                           TokenBlacklistMapper tokenBlacklistMapper,
                           AuditLogWriter auditLogWriter,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           IdGenerator idGenerator,
                           LoginAttemptTracker loginAttemptTracker,
                           com.rickgao.careercore.security.AuthUserCache authUserCache,
                           @org.springframework.context.annotation.Lazy AuthServiceImpl self) {
        this.sysUserMapper = sysUserMapper;
        this.studentWhitelistMapper = studentWhitelistMapper;
        this.consentMapper = consentMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
        this.auditLogWriter = auditLogWriter;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.idGenerator = idGenerator;
        this.loginAttemptTracker = loginAttemptTracker;
        this.authUserCache = authUserCache;
        this.self = self;
    }

    @Override
    @Transactional
    public TokenVO register(RegisterRequest request, String ip) {
        StudentWhitelist whitelist = studentWhitelistMapper.findByStudentNo(request.getStudentNo());
        if (whitelist == null) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "学号不在白名单,无法注册");
        }
        if (!java.util.Objects.equals(whitelist.getInitialPassword(), request.getInitialPassword())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "初始密码不匹配");
        }
        if (Boolean.TRUE.equals(whitelist.getUsed()) || sysUserMapper.findByUsername(request.getStudentNo()) != null) {
            throw new BizException(ResultCode.STATE_CONFLICT, "该学号已注册,请直接登录");
        }

        SysUser user = new SysUser();
        user.setId(idGenerator.userId());
        user.setStudentNo(request.getStudentNo());
        user.setUsername(request.getStudentNo());
        user.setName(request.getName());
        // 注册时使用管理员分配的初始密码；首次进入后必须修改。
        user.setPasswordHash(passwordEncoder.encode(request.getInitialPassword()));
        user.setRole(CommonConstants.ROLE_STUDENT);
        user.setStatus(CommonConstants.USER_STATUS_ACTIVE);
        // Demo 精简点:白名单未包含专业大类;年级由学号前 4 位推导
        user.setGrade(deriveGrade(request.getStudentNo()));
        user.setClassName(StringUtils.hasText(request.getClassName()) ? request.getClassName() : whitelist.getClassName());
        user.setConsentAgreed(false);
        user.setPasswordChangeRequired(true);
        sysUserMapper.insert(user);
        studentWhitelistMapper.markUsed(request.getStudentNo());

        recordAudit(CommonConstants.AUDIT_REGISTER, user.getId(), "sys_user", user.getId(), "学号注册", ip);
        return createTokenPair(user);
    }

    @Override
    @Transactional
    public TokenVO login(LoginRequest request, String ip) {
        String account = request.getAccount();
        if (loginAttemptTracker.isLocked(account)) {
            throw new BizException(ResultCode.STATE_CONFLICT, "连续失败次数过多,账号已临时锁定,请稍后再试");
        }
        SysUser user = sysUserMapper.findByUsername(account);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptTracker.recordFailure(account);
            recordAudit(CommonConstants.AUDIT_LOGIN_FAIL, null, "sys_user", account, "登录失败", ip);
            throw new BizException(ResultCode.VALIDATION_ERROR, "账号或密码错误");
        }
        if (!CommonConstants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被停用");
        }
        if (StringUtils.hasText(request.getRole()) && !request.getRole().equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号角色与请求不一致");
        }

        loginAttemptTracker.reset(account);
        sysUserMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
        recordAudit(CommonConstants.AUDIT_LOGIN, user.getId(), "sys_user", user.getId(), "登录成功", ip);
        return createTokenPair(user);
    }

    @Override
    @Transactional
    public TokenVO refresh(RefreshRequest request, String ip) {
        String presented = request.getRefreshToken();
        // 新路径优先：按哈希查；双写兼容一轮：老行无哈希时回退明文并回填（下轮删除该分支）
        RefreshToken stored = null;
        if (org.springframework.util.StringUtils.hasText(presented)) {
            stored = refreshTokenMapper.findByTokenHash(sha256Hex(presented));
        }
        if (stored == null && org.springframework.util.StringUtils.hasText(presented)) {
            RefreshToken legacy = refreshTokenMapper.findByToken(presented);
            if (legacy != null && !org.springframework.util.StringUtils.hasText(legacy.getTokenHash())) {
                refreshTokenMapper.backfillTokenHash(legacy.getId(), sha256Hex(presented));
                legacy.setTokenHash(sha256Hex(presented));
            }
            stored = legacy;
        }
        if (stored == null
                || stored.getExpiresAt() == null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.AUTH_REQUIRED, "刷新令牌无效或已过期");
        }
        if (Boolean.TRUE.equals(stored.getRevoked())) {
            // 复用检测（RFC 9700 轮换语义）：已作废的 refresh 再次出现视为疑似盗用，
            // 作废该用户全部 refresh，强制重登。注意：网络重试导致的“旧 token 重放”
            // 在正常轮换后同样触发，此为安全优先的取舍（客户端应只保留最新 refresh）。
            // 必须经 REQUIRES_NEW 独立提交——外层随后抛异常，若同事务会被回滚导致吊销落空。
            self.revokeAllUserRefreshTokens(stored.getUserId());
            recordAudit(CommonConstants.AUDIT_LOGOUT, stored.getUserId(), "refresh_token", stored.getId(),
                    "刷新令牌复用,全部作废", ip);
            throw new BizException(ResultCode.AUTH_REQUIRED, "刷新令牌已轮换,请重新登录");
        }
        SysUser user = sysUserMapper.findById(stored.getUserId());
        if (user == null || !CommonConstants.USER_STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BizException(ResultCode.AUTH_REQUIRED, "账号不可用");
        }
        // 安全：首改密码未完成前不允许经 refresh 续期，防止无限推迟强制改密
        if (Boolean.TRUE.equals(user.getPasswordChangeRequired())) {
            throw new BizException(ResultCode.FORBIDDEN, "首次登录请先修改初始密码");
        }
        // 刷新轮换:旧刷新令牌随即作废
        refreshTokenMapper.revoke(stored.getId());
        return createTokenPair(user);
    }

    @Override
    @Transactional
    public void logout(String userId, String jti, LocalDateTime tokenExpiresAt, String ip) {
        // 访问令牌加入黑名单,刷新令牌全部作废
        if (StringUtils.hasText(jti)) {
            tokenBlacklistMapper.insert(jti, userId, tokenExpiresAt);
        }
        refreshTokenMapper.revokeByUserId(userId);
        recordAudit(CommonConstants.AUDIT_LOGOUT, userId, null, null, "退出登录", ip);
    }

    @Override
    public CurrentUserVO me(String userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.AUTH_REQUIRED, "用户不存在");
        }
        return toCurrentUserVO(user);
    }

    @Override
    @Transactional
    public CurrentUserVO updateMe(String userId, CurrentUserUpdateRequest request, String ip) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.AUTH_REQUIRED, "用户不存在");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "姓名不能为空");
        }
        String name = request.getName().trim();
        sysUserMapper.updateOwnName(userId, name);
        user.setName(name);
        recordAudit("UPDATE_ACCOUNT", userId, "sys_user", userId, "更新账户姓名", ip);
        return toCurrentUserVO(user);
    }

    @Override
    @Transactional
    public void changePassword(String userId, PasswordChangeRequest request, String ip) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.AUTH_REQUIRED, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "原密码错误");
        }
        validatePasswordStrength(request.getNewPassword());
        sysUserMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()), false);
        // 使其他端会话失效：刷新令牌作废 + 令牌版本递增（旧 accessToken 即刻失效）
        sysUserMapper.incrementTokenVersion(userId);
        authUserCache.evict(userId);
        refreshTokenMapper.revokeByUserId(userId);
        recordAudit(CommonConstants.AUDIT_PASSWORD_CHANGE, userId, "sys_user", userId, "修改密码", ip);
    }

    @Override
    @Transactional
    public void resetPassword(String operatorId, PasswordResetRequest request, String ip) {
        SysUser operator = sysUserMapper.findById(operatorId);
        if (operator == null || !CommonConstants.ROLE_ADMIN.equals(operator.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可重置密码");
        }
        SysUser target = sysUserMapper.findByStudentNo(request.getStudentNo());
        if (target == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "未找到该学号的用户");
        }
        sysUserMapper.updatePassword(target.getId(), passwordEncoder.encode(request.getNewPassword()), true);
        sysUserMapper.incrementTokenVersion(target.getId());
        authUserCache.evict(target.getId());
        refreshTokenMapper.revokeByUserId(target.getId());
        String reason = StringUtils.hasText(request.getReason()) ? request.getReason() : "";
        recordAudit(CommonConstants.AUDIT_PASSWORD_RESET, operatorId, "sys_user", target.getId(), "重置密码,原因:" + reason, ip);
    }

    @Override
    @Transactional
    public ConsentStatusVO consent(String userId, ConsentRequest request, String ip) {
        ConsentDocument published = getPublishedConsent();
        if (!published.getVersion().equals(request.getVersion())) {
            throw new BizException(ResultCode.STATE_CONFLICT, "授权文本已更新,请阅读最新版本后重新同意");
        }
        ConsentRecord record = new ConsentRecord();
        record.setId(idGenerator.consentRecordId());
        record.setUserId(userId);
        record.setVersion(request.getVersion());
        record.setAgreedAt(LocalDateTime.now());
        record.setIp(ip);
        consentMapper.insertRecord(record);
        sysUserMapper.updateConsentAgreed(userId, true);
        recordAudit(CommonConstants.AUDIT_CONSENT, userId, "consent", request.getVersion(), "同意隐私授权", ip);
        return consentStatus(userId);
    }

    @Override
    public ConsentStatusVO consentStatus(String userId) {
        ConsentDocument published = getPublishedConsent();
        ConsentRecord record = consentMapper.findLatestByUserId(userId);
        return ConsentStatusVO.builder()
                .agreed(record != null)
                .version(record != null ? record.getVersion() : null)
                .agreedAt(record != null ? record.getAgreedAt() : null)
                .currentVersion(published.getVersion())
                .currentVersionPublishedAt(published.getPublishedAt())
                .content(published.getContent())
                .build();
    }

    private ConsentDocument getPublishedConsent() {
        ConsentDocument document = consentMapper.findPublished();
        if (document == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "隐私授权文本尚未发布");
        }
        return document;
    }

    /** 生成访问 + 刷新令牌对,并持久化刷新令牌（双写兼容：明文 + 哈希并存）。 */
    private TokenVO createTokenPair(SysUser user) {
        int tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);
        String refreshTokenValue = generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(idGenerator.refreshTokenId());
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setTokenHash(sha256Hex(refreshTokenValue));
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenTtlSeconds()));
        refreshToken.setRevoked(false);
        refreshTokenMapper.insert(refreshToken);

        return TokenVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtUtil.getAccessTokenTtlSeconds())
                .tokenType(CommonConstants.TOKEN_TYPE_BEARER)
                .firstLogin(!Boolean.TRUE.equals(user.getConsentAgreed()))
                .user(toCurrentUserVO(user))
                .build();
    }

    private String generateRefreshToken() {
        return "rt_" + TraceIdUtil.generate() + UUID.randomUUID().toString().replace("-", "");
    }

    /** 复用吊销独立事务（见 refresh）：外层抛异常时仍保证提交。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllUserRefreshTokens(String userId) {
        refreshTokenMapper.revokeByUserId(userId);
    }

    /** 刷新令牌 SHA-256（hex 小写）；失败抛内部错（fail-closed，不降级明文比对）。 */
    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "哈希算法不可用");
        }
    }

    private CurrentUserVO toCurrentUserVO(SysUser user) {
        return CurrentUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .studentNo(user.getStudentNo())
                .grade(user.getGrade())
                .majorCategory(user.getMajorCategory())
                .className(user.getClassName())
                .consentAgreed(user.getConsentAgreed())
                .passwordChangeRequired(user.getPasswordChangeRequired())
                .build();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "新密码长度至少 6 位");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!(hasLetter && hasDigit)) {
            throw new BizException(ResultCode.VALIDATION_ERROR, "新密码需同时包含字母和数字");
        }
    }

    private String deriveGrade(String studentNo) {
        if (studentNo == null || studentNo.length() < 4) {
            return null;
        }
        return studentNo.substring(0, 4) + "级";
    }

    private void recordAudit(String action, String userId, String targetType, String targetId, String detail, String ip) {
        auditLogWriter.record(action, userId, targetType, targetId, detail, ip);
    }
}
