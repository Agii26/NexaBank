package com.nexabank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NexaBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexaBankApplication.class, args);
    }
}
