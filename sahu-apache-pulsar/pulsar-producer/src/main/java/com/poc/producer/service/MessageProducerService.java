package com.poc.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poc.producer.dto.MessageEvent;
import com.poc.producer.dto.MessageRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for publishing messages to an Apache Pulsar topic.
 *
 * <p>
 * On application startup ({@link PostConstruct}), this service creates a
 * Pulsar {@link Producer} bound to the topic defined by {@code pulsar.topic}
 * in {@code application.yml}. The producer remains open for the lifetime of
 * the application and is gracefully closed on shutdown ({@link PreDestroy}).
 * </p>
 *
 * <h3>Message flow</h3>
 * <ol>
 * <li>Caller invokes {@link #publishMessage(MessageRequest)}.</li>
 * <li>A {@link MessageEvent} is built, enriched with a UUID and timestamp.</li>
 * <li>The event is serialized to JSON via Jackson.</li>
 * <li>The JSON bytes are sent synchronously to the Pulsar broker.</li>
 * </ol>
 *
 * <p>
 * The Pulsar client ({@link PulsarClient}) is injected as a Spring bean
 * created by {@link com.poc.producer.config.PulsarConfig}.
 * </p>
 */
@Slf4j
@Service
public class MessageProducerService {

    private final PulsarClient pulsarClient;
    private final ObjectMapper objectMapper;
    private Producer<byte[]> producer;

    /** Pulsar topic name, e.g. {@code persistent://public/default/order-topic}. */
    @Value("${pulsar.topic}")
    private String topic;

    /**
     * Constructs the service with the shared Pulsar client and configures
     * the Jackson {@link ObjectMapper} with Java 8 date/time support.
     *
     * @param pulsarClient the shared Pulsar client bean
     */
    public MessageProducerService(PulsarClient pulsarClient) {
        this.pulsarClient = pulsarClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Initialises the Pulsar {@link Producer} after the Spring context is ready.
     *
     * <p>
     * The producer is given a fixed name ({@code order-producer}) so that
     * Pulsar can track sequences per named producer, aiding deduplication.
     * </p>
     *
     * @throws PulsarClientException if the producer cannot be created
     */
    @PostConstruct
    public void init() throws PulsarClientException {
        producer = pulsarClient.newProducer()
                .topic(topic)
                .producerName("sahu-event-producer")
                .sendTimeout(10, TimeUnit.SECONDS)
                .create();
        log.info("Pulsar Producer created for topic: {}", topic);
    }

    /**
     * Builds a {@link MessageEvent} from the incoming request, serializes it
     * to JSON, and sends it to the Pulsar topic synchronously.
     *
     * @param request the inbound message request containing {@code content} and
     *                {@code sender}
     * @return the enriched {@link MessageEvent} that was published (includes
     *         generated
     *         {@code messageId} and {@code timestamp})
     * @throws Exception if JSON serialization fails or the Pulsar send operation
     *                   fails
     */
    public MessageEvent publishMessage(MessageRequest request) throws Exception {
        MessageEvent event = MessageEvent.builder()
                .messageId(UUID.randomUUID().toString())
                .content(request.getContent())
                .sender(request.getSender())
                .timestamp(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(event);
        producer.send(json.getBytes());

        log.info("Message published to topic [{}]: {}", topic, json);
        return event;
    }

    /**
     * Closes the Pulsar {@link Producer} when the Spring context is destroyed,
     * ensuring all in-flight messages are flushed before shutdown.
     *
     * @throws PulsarClientException if closing the producer fails
     */
    @PreDestroy
    public void cleanup() throws PulsarClientException {
        if (producer != null) {
            producer.close();
            log.info("Pulsar Producer closed.");
        }
    }
}
