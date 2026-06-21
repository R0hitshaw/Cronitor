package com.cronitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CronitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CronitorApplication.class, args);
    }
}
