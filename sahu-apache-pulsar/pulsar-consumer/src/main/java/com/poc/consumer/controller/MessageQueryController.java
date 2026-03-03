package com.poc.consumer.controller;

import com.poc.consumer.entity.Message;
import com.poc.consumer.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for querying messages that have been consumed from Apache
 * Pulsar
 * and persisted to the H2 in-memory database.
 *
 * <p>
 * Base path: {@code /api/messages}
 * </p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 * <li>{@code GET /api/messages} – returns all stored messages as a JSON
 * array</li>
 * <li>{@code GET /api/messages/{id}} – returns a single message by its H2 DB
 * primary key;
 * responds with {@code 404 Not Found} if absent</li>
 * <li>{@code GET /api/messages/health}– liveness probe that also reports the
 * current
 * total message count stored in H2</li>
 * </ul>
 *
 * <p>
 * This controller is read-only; message ingestion is handled entirely by
 * {@link com.poc.consumer.service.PulsarConsumerListener} in the background.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageQueryController {

    private final MessageRepository messageRepository;

    /**
     * Returns all messages currently stored in the H2 database.
     *
     * <p>
     * An empty JSON array ({@code []}) is returned when no messages have been
     * consumed yet — this is a valid response, not an error.
     * </p>
     *
     * @return {@code 200 OK} with a list of {@link Message} entities serialized to
     *         JSON
     */
    @GetMapping
    public ResponseEntity<List<Message>> getAllMessages() {
        List<Message> messages = messageRepository.findAll();
        log.info("Returning {} messages from H2", messages.size());
        return ResponseEntity.ok(messages);
    }

    /**
     * Returns a single message by its auto-generated H2 primary key ({@code id}).
     *
     * @param id the surrogate primary key of the {@link Message} entity
     * @return {@code 200 OK} with the matching {@link Message},
     *         or {@code 404 Not Found} if no record with that ID exists
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        return messageRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lightweight health-check endpoint that reports the service status and
     * the total number of messages stored in H2.
     *
     * <p>
     * <b>Example response:</b>
     * </p>
     * 
     * <pre>{@code
     * { "status": "UP", "service": "pulsar-consumer", "storedMessages": "5" }
     * }</pre>
     *
     * @return {@code 200 OK} with a JSON map containing service metadata
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        long count = messageRepository.count();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "pulsar-consumer",
                "storedMessages", String.valueOf(count)));
    }
}
