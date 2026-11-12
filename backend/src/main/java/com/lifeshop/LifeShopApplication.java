package com.lifeshop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 悦家生活 · 生活用品电商系统 启动类
 */
@SpringBootApplication
@MapperScan("com.lifeshop.mapper")
@EnableScheduling
public class LifeShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeShopApplication.class, args);
        System.out.println("\n==============================");
        System.out.println("  悦家生活电商系统启动成功");
        System.out.println("  接口地址: http://localhost:8080/api");
        System.out.println("==============================\n");
    }
}
