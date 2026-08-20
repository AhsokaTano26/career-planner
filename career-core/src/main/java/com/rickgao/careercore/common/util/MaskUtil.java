package com.rickgao.careercore.common.util;

/**
 * 脱敏工具。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * 手机号脱敏:保留前 3 位与后 4 位,如 138****6721。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
