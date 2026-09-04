package com.rickgao.careercore.modules.direction.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 学生收藏方向，对齐 favorite 表。 */
@Data
public class Favorite {

    private String id;
    private String studentId;
    private String directionId;
    private LocalDateTime createdAt;
}

