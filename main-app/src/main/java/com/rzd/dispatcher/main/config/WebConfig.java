package com.rzd.dispatcher.main.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    // Аннотация @LoadBalanced включает магию Service Discovery!
    // Теперь RestTemplate будет искать адреса не в DNS (ебаные компсети), а в Eureka.
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}