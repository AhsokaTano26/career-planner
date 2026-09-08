package com.rickgao.careercore.security;

import com.rickgao.careercore.modules.auth.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证用户行短 TTL 缓存（2026-09 复审 Batch4）。
 *
 * <p>JwtAuthFilter 每请求查一次 sys_user；DB 抖动即整站 401。在此缓存用户行 60 秒，
 * 黑名单 {@code exists(jti)} 仍每请求实时查（登出即刻生效）。正确性论证：
 * <ul>
 *   <li>tokenVersion 命中缓存比较——版本已递增的老 token 必然 mismatch；</li>
 *   <li>mismatch 时回源重读一次自愈（防变更路径漏驱逐）；</li>
 *   <li>改密/重置/停用三处递增版本处同步驱逐（纵深）。</li>
 * </ul>
 * Demo 精简点 / 后续迭代替换位置：多实例换 Redis（key=auth:user:{id}，TTL 60s）。
 */
@Component
public class AuthUserCache {

    static final long TTL_MS = 60_000;

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public SysUser get(String userId) {
        if (userId == null) {
            return null;
        }
        Entry entry = cache.get(userId);
        if (entry == null || System.currentTimeMillis() > entry.expireAt) {
            if (entry != null) {
                cache.remove(userId, entry);
            }
            return null;
        }
        return entry.user;
    }

    public void put(String userId, SysUser user) {
        if (userId == null || user == null) {
            return;
        }
        cache.put(userId, new Entry(user, System.currentTimeMillis() + TTL_MS));
    }

    public void evict(String userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private static final class Entry {
        final SysUser user;
        final long expireAt;

        Entry(SysUser user, long expireAt) {
            this.user = user;
            this.expireAt = expireAt;
        }
    }
}
