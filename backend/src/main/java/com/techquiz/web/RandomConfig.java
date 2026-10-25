package com.techquiz.web;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One injectable source of randomness, so tests can pass a seeded one. */
@Configuration
public class RandomConfig {

    @Bean
    RandomGenerator random() {
        return new SecureRandom();
    }
}
