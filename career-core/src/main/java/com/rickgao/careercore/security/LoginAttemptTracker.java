package com.rickgao.careercore.security;

import com.rickgao.careercore.common.constant.CommonConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定:连续失败达阈值后临时锁定账号。
 * Demo 精简点:使用进程内存实现,重启后重置;生产环境建议落库或引入分布式缓存。
 */
@Component
public class LoginAttemptTracker {

    private final Map<String, Attempt> store = new ConcurrentHashMap<>();

    public boolean isLocked(String account) {
        Attempt attempt = store.get(account);
        if (attempt == null || attempt.lockedUntil == null) {
            return false;
        }
        if (attempt.lockedUntil.isAfter(LocalDateTime.now())) {
            return true;
        }
        // 锁定已到期,清除记录
        store.remove(account);
        return false;
    }

    public void recordFailure(String account) {
        Attempt attempt = store.computeIfAbsent(account, k -> new Attempt());
        attempt.failures++;
        if (attempt.failures >= CommonConstants.LOGIN_MAX_FAILURES) {
            attempt.lockedUntil = LocalDateTime.now().plusMinutes(CommonConstants.LOGIN_LOCK_MINUTES);
        }
    }

    public void reset(String account) {
        store.remove(account);
    }

    private static class Attempt {
        int failures;
        LocalDateTime lockedUntil;
    }
}
