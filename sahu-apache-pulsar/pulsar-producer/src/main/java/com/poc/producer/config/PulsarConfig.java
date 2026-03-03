package com.poc.producer.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration class for Apache Pulsar client.
 *
 * <p>
 * Creates and exposes a singleton {@link PulsarClient} bean that is shared
 * across the application. The client connects to the Pulsar broker whose URL
 * is specified in {@code application.yml} under the {@code pulsar.broker-url}
 * key.
 * </p>
 *
 * <p>
 * <b>Configuration properties used:</b>
 * </p>
 * <ul>
 * <li>{@code pulsar.broker-url} – e.g. {@code pulsar://localhost:6650}</li>
 * </ul>
 *
 * <p>
 * The client is configured with explicit connection and operation timeouts
 * to fail fast in development environments when Pulsar is not reachable.
 * </p>
 */
@Configuration
public class PulsarConfig {

    /** Pulsar broker URL injected from application.yml. */
    @Value("${pulsar.broker-url}")
    private String brokerUrl;

    /**
     * Creates a {@link PulsarClient} connected to the configured broker.
     *
     * @return a fully initialised Pulsar client
     * @throws PulsarClientException if the client cannot connect to the broker
     */
    @Bean
    public PulsarClient pulsarClient() throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(brokerUrl)
                .connectionTimeout(30, TimeUnit.SECONDS)
                .operationTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
