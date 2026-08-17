package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

import java.time.LocalDateTime;

/** 推荐批次查询行 */
@Data
public class RunRow {

    private String id;
    private String studentId;
    private String profileSnapshotId;
    private String ruleVersion;
    private String status;
    private LocalDateTime generatedAt;
    private Integer profileVersion;
}
