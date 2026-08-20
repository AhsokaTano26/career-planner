package com.rickgao.careercore.modules.advisor.query;

import lombok.Data;

/** 路径分布计数查询行 */
@Data
public class PathCountRow {

    private String path;
    private Long count;
}
