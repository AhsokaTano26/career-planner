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

    @Data
    public static class Failure {
        private Integer row;
        private String studentNo;
        private String reason;
    }
}
