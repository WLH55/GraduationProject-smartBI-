package com.wlh.smartbi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@MapperScan("com.wlh.smartbi.mapper")
@EnableScheduling
@EnableWebMvc
@EnableRetry
public class SmartBiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartBiApplication.class, args);
    }

}
