package com.year2.queryme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueryMeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryMeBackendApplication.class, args);
    }
}
