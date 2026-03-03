package com.poc.consumer.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Spring configuration class for the Apache Pulsar client in the consumer
 * service.
 *
 * <p>
 * Creates and exposes a singleton {@link PulsarClient} bean used by
 * {@link com.poc.consumer.service.PulsarConsumerListener} to subscribe to
 * the Pulsar topic. The broker URL is provided via {@code application.yml}.
 * </p>
 *
 * <p>
 * <b>Configuration properties used:</b>
 * </p>
 * <ul>
 * <li>{@code pulsar.broker-url} – e.g. {@code pulsar://localhost:6650}</li>
 * </ul>
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
     * @throws PulsarClientException if the client cannot establish a connection
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
