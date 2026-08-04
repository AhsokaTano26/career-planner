package com.career.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 生涯规划系统 · 核心业务服务（第一版 Demo）入口。
 * 启动时将自动执行 db/migration 下的建表与种子脚本（幂等）。
 */
@SpringBootApplication
public class CareerCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerCoreApplication.class, args);
    }
}
