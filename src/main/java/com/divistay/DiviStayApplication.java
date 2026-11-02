package com.divistay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DiviStayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiviStayApplication.class, args);
    }
}
