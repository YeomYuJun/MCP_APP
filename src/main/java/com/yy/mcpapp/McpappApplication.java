package com.yy.mcpapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class McpappApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpappApplication.class, args);
    }

}
