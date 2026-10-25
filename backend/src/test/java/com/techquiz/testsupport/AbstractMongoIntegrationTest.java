package com.techquiz.testsupport;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;

/**
 * One MongoDB container for the whole test run, started in a static initializer (the
 * Testcontainers "singleton container" pattern) rather than with {@code @Container}, which would
 * stop it after the first test class that uses it.
 */
public abstract class AbstractMongoIntegrationTest {

    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    static {
        MONGO.start();
    }
}
