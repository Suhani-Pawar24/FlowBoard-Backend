package com.flowboard.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ListServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListServiceApplication.class, args);
    }
}
