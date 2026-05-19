package com.flowboard.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LabelServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelServiceApplication.class, args);
    }
}
