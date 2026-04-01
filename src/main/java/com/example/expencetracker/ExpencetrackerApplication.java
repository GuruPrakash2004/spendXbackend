package com.example.expencetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
 
/**
 * Application entry point.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration       → marks this class as a bean source
 *   - @EnableAutoConfiguration → lets Spring Boot auto-wire dependencies
 *   - @ComponentScan       → scans this package and all sub-packages for beans
 */
@SpringBootApplication
public class ExpencetrackerApplication {
 
    public static void main(String[] args) {
        SpringApplication.run(ExpencetrackerApplication.class, args);
    }
}
