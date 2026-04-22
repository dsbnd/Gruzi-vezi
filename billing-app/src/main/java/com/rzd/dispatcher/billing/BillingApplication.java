package com.rzd.dispatcher.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.rzd.dispatcher")
@EnableJpaRepositories(basePackages = "com.rzd.dispatcher.common.repository")
@EntityScan(basePackages = "com.rzd.dispatcher.common.model.entity")
public class BillingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingApplication.class, args);
    }
}