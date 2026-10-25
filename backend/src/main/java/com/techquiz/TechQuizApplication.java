package com.techquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TechQuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechQuizApplication.class, args);
    }
}
