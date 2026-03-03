package com.poc.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poc.consumer.dto.MessageEvent;
import com.poc.consumer.entity.Message;
import com.poc.consumer.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Apache Pulsar message consumer that listens to a topic and persists
 * every received message to the H2 in-memory database.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li><b>Startup</b> ({@link PostConstruct}): Creates a Pulsar {@link Consumer}
 *       with a {@code Shared} subscription and launches a single-threaded polling
 *       loop on a background {@link ExecutorService}.</li>
 *   <li><b>Processing</b>: For each message received from the broker, the raw
 *       JSON bytes are deserialized into a {@link MessageEvent}. An idempotency
 *       check is performed via {@link MessageRepository#existsByMessageId(String)};
 *       if the message is new it is saved as a {@link Message} entity. The message
 *       is then acknowledged so Pulsar removes it from the subscription backlog.</li>
 *   <li><b>Shutdown</b> ({@link PreDestroy}): The polling loop is stopped via a
 *       volatile flag, the executor is interrupted, and the consumer is closed
 *       cleanly.</li>
 * </ol>
 *
 * <h3>Reliability notes</h3>
 * <ul>
 *   <li>Uses a {@code Shared} subscription type so multiple instances of the
 *       consumer service can run in parallel without one consumer starving others.</li>
 *   <li>Acknowledgement happens <em>after</em> a successful database save, ensuring
 *       at-least-once processing (though the deduplication check prevents double-saves).</li>
 *   <li>If processing fails, the exception is logged and the loop continues;
 *       the message will be re-delivered by Pulsar after the ack timeout.</li>
 * </ul>
 *
 * <p><b>Configuration properties used:</b></p>
 * <ul>
 *   <li>{@code pulsar.topic}        – e.g. {@code persistent://public/default/order-topic}</li>
 *   <li>{@code pulsar.subscription} – e.g. {@code order-subscription}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PulsarConsumerListener {

    private final PulsarClient pulsarClient;
    private final MessageRepository messageRepository;

    /** Pulsar topic to subscribe to, resolved from {@code application.yml}. */
    @Value("${pulsar.topic}")
    private String topic;

    /** Pulsar subscription name, resolved from {@code application.yml}. */
    @Value("${pulsar.subscription}")
    private String subscription;

    private Consumer<byte[]> consumer;

    /** Single background thread that blocks on {@code consumer.receive()}. */
    private final ExecutorService listenerThread = Executors.newSingleThreadExecutor();

    /**
     * Guards the polling loop. Set to {@code false} during {@link #shutdown()}
     * to allow a clean exit without waiting for the next message.
     */
    private volatile boolean running = true;

    /** Jackson mapper configured with JSR-310 (Java 8 date/time) support. */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Creates the Pulsar {@link Consumer} and starts the message polling loop
     * on a dedicated background thread after the Spring context is ready.
     *
     * @throws PulsarClientException if the subscription cannot be created
     */
    @PostConstruct
    public void startListening() throws PulsarClientException {
        consumer = pulsarClient.newConsumer()
                .topic(topic)
                .subscriptionName(subscription)
                .subscriptionType(SubscriptionType.Shared)
                .subscribe();

        log.info("Pulsar Consumer subscribed to topic [{}] with subscription [{}]", topic, subscription);
        listenerThread.submit(this::pollMessages);
    }

    /**
     * Blocking poll loop executed on the {@link #listenerThread}.
     *
     * <p>Calls {@code consumer.receive()} which blocks until a message is available.
     * On receipt, the payload is deserialized, deduplicated, persisted, and
     * acknowledged. Exceptions are logged and the loop continues.</p>
     */
    private void pollMessages() {
        while (running) {
            try {
                org.apache.pulsar.client.api.Message<byte[]> pulsarMsg = consumer.receive();
                String payload = new String(pulsarMsg.getData());
                log.info("Received message from Pulsar: {}", payload);

                MessageEvent event = objectMapper.readValue(payload, MessageEvent.class);

                // Idempotency check — prevents duplicate DB rows on Pulsar re-delivery
                if (!messageRepository.existsByMessageId(event.getMessageId())) {
                    Message message = Message.builder()
                            .messageId(event.getMessageId())
                            .content(event.getContent())
                            .sender(event.getSender())
                            .producedAt(event.getTimestamp())
                            .receivedAt(LocalDateTime.now())
                            .build();

                    messageRepository.save(message);
                    log.info("Message persisted to H2 DB with messageId: {}", event.getMessageId());
                } else {
                    log.warn("Duplicate message detected, skipping messageId: {}", event.getMessageId());
                }

                // Acknowledge only after successful processing
                consumer.acknowledge(pulsarMsg);

            } catch (Exception e) {
                if (running) {
                    log.error("Error processing Pulsar message", e);
                }
            }
        }
    }

    /**
     * Gracefully shuts down the consumer when the Spring context is destroyed.
     *
     * <p>Sets {@link #running} to {@code false}, interrupts the listener thread,
     * and closes the Pulsar consumer to release broker-side resources.</p>
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        listenerThread.shutdownNow();
        try {
            if (consumer != null) consumer.close();
            log.info("Pulsar Consumer closed.");
        } catch (PulsarClientException e) {
            log.error("Error closing Pulsar consumer", e);
        }
    }
}
