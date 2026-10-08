//package com.rush.ticket.catalog.integration;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.testcontainers.containers.KafkaContainer;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.containers.wait.strategy.Wait;
//import org.testcontainers.utility.DockerImageName;
//
//import java.time.Duration;
//
//@TestConfiguration(proxyBeanMethods = false)
//public class TestContainersConfiguration {
//
//    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(30);
//
//    @Bean
//    @ServiceConnection
//    public PostgreSQLContainer<?> postgresContainer() {
//        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
//                .withStartupTimeout(STARTUP_TIMEOUT);
//    }
//
//    @Bean
//    @ServiceConnection(name = "kafka")
//    public KafkaContainer kafkaContainer() {
//        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
//                .withStartupTimeout(STARTUP_TIMEOUT);
//    }
//}
