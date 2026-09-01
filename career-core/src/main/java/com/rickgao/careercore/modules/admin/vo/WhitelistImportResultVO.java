package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.util.List;

/**
 * 白名单 CSV 导入结果。对齐 openapi WhitelistImportResult。
 */
@Data
public class WhitelistImportResultVO {

    private Integer successCount;
    private Integer failCount;
    private List<Failure> failures;
    /** CSV 中未填写初始密码时，服务端自动生成的密码清单，仅在本次响应中返回。 */
    private List<GeneratedInitialPassword> generatedInitialPasswords;

    @Data
    public static class Failure {
        private Integer row;
        private String studentNo;
        private String reason;
    }

    @Data
    public static class GeneratedInitialPassword {
        private String studentNo;
        private String initialPassword;
    }
}
