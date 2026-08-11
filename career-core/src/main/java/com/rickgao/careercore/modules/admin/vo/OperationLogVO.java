package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 操作审计日志。对齐 openapi OperationLog。 */
@Data
public class OperationLogVO {

    private String id;
    private LocalDateTime time;
    private String operator;
    private String action;
    private String target;
    private String detail;
    private String level;
    private String ip;
}
