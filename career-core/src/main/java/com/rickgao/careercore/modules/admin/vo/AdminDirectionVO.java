package com.rickgao.careercore.modules.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端-方向库列表项。对齐 openapi AdminDirection。 */
@Data
public class AdminDirectionVO {

    private String id;
    private String name;
    private String path;
    private String status;
    private Integer sortOrder;
    private List<String> applicableMajors;
    private LocalDateTime updatedAt;
}
