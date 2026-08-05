package com.career.core.modules.profile;

import java.util.Locale;
import java.util.Optional;

/**
 * 六维画像的稳定编码。
 *
 * <p>canonicalCode 用于新版画像快照；legacyCode 用于兼容第一版 Demo
 * 的 interest/values/academic/ability/orientation/experience 数据。</p>
 */
public enum ProfileDimension {
    INTEREST("INTEREST", "interest", "兴趣"),
    WORK_VALUES("WORK_VALUES", "values", "职业价值观"),
    ACADEMIC_FOUNDATION("ACADEMIC_FOUNDATION", "academic", "学业基础"),
    ABILITY("ABILITY", "ability", "能力"),
    DEVELOPMENT_TENDENCY("DEVELOPMENT_TENDENCY", "orientation", "发展倾向"),
    PRACTICAL_EXPERIENCE("PRACTICAL_EXPERIENCE", "experience", "实践经历");

    private final String canonicalCode;
    private final String legacyCode;
    private final String displayName;

    ProfileDimension(String canonicalCode, String legacyCode, String displayName) {
        this.canonicalCode = canonicalCode;
        this.legacyCode = legacyCode;
        this.displayName = displayName;
    }

    public String canonicalCode() {
        return canonicalCode;
    }

    public String legacyCode() {
        return legacyCode;
    }

    public String displayName() {
        return displayName;
    }

    /** 兼容新版编码、旧版编码和常见 camelCase 写法。 */
    public static Optional<ProfileDimension> fromCode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);
        for (ProfileDimension dimension : values()) {
            if (dimension.canonicalCode.equals(normalized)
                    || dimension.legacyCode.toUpperCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(dimension);
            }
        }
        return switch (normalized) {
            case "WORKVALUES" -> Optional.of(WORK_VALUES);
            case "ACADEMICFOUNDATION" -> Optional.of(ACADEMIC_FOUNDATION);
            case "DEVELOPMENTTENDENCY" -> Optional.of(DEVELOPMENT_TENDENCY);
            case "PRACTICALEXPERIENCE" -> Optional.of(PRACTICAL_EXPERIENCE);
            default -> Optional.empty();
        };
    }
}
