/**
 * Main entry point for the Pulsar Producer Microservice.
 *
 * <p>This Spring Boot application exposes a REST API for publishing messages
 * to an Apache Pulsar topic. It is one half of the Apache Pulsar POC —
 * the other half being the {@code pulsar-consumer} service which reads
 * messages from the same topic and persists them to an in-memory H2 database.</p>
 *
 * <p>Configuration is driven via {@code src/main/resources/application.yml}.</p>
 *
 * <p><b>REST Endpoints:</b></p>
 * <ul>
 *   <li>{@code POST /api/messages} – publish a new message to Pulsar</li>
 *   <li>{@code GET  /api/messages/health} – liveness check</li>
 * </ul>
 *
 * @see com.poc.producer.controller.MessageController
 * @see com.poc.producer.service.MessageProducerService
 */
package com.poc.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PulsarProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsarProducerApplication.class, args);
    }
}
