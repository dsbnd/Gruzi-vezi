package com.rzd.dispatcher.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.rzd.dispatcher.common.repository")
@EntityScan(basePackages = "com.rzd.dispatcher.common.model.entity")
public class Main  {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}