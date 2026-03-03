/**
 * Main entry point for the Pulsar Consumer Microservice.
 *
 * <p>This Spring Boot application listens to an Apache Pulsar topic and
 * persists every received message to an H2 in-memory database. It is the
 * consumer half of the Apache Pulsar POC — the producer half being the
 * {@code pulsar-producer} service.</p>
 *
 * <p>Configuration is driven via {@code src/main/resources/application.yml}.</p>
 *
 * <p><b>REST Endpoints:</b></p>
 * <ul>
 *   <li>{@code GET  /api/messages}       – retrieve all persisted messages</li>
 *   <li>{@code GET  /api/messages/{id}}  – retrieve a single message by DB id</li>
 *   <li>{@code GET  /api/messages/health}– liveness check with stored-message count</li>
 *   <li>{@code GET  /h2-console}         – H2 browser console (dev only)</li>
 * </ul>
 *
 * @see com.poc.consumer.service.PulsarConsumerListener
 * @see com.poc.consumer.controller.MessageQueryController
 */
package com.poc.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PulsarConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsarConsumerApplication.class, args);
    }
}
