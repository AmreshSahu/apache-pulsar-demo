package com.poc.producer.controller;

import com.poc.producer.dto.MessageEvent;
import com.poc.producer.dto.MessageRequest;
import com.poc.producer.service.MessageProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes endpoints for publishing messages to Apache
 * Pulsar.
 *
 * <p>
 * Base path: {@code /api/messages}
 * </p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 * <li>{@code POST /api/messages} – accepts a JSON {@link MessageRequest} body,
 * delegates to {@link MessageProducerService}, and returns the published
 * {@link MessageEvent} with HTTP 202 Accepted.</li>
 * <li>{@code GET  /api/messages/health} – lightweight liveness probe returning
 * service name and status.</li>
 * </ul>
 *
 * <p>
 * All errors during publishing are caught here and returned as a
 * {@code 500 Internal Server Error} with a JSON {@code error} field,
 * so callers always receive a structured response.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProducerService producerService;

    /**
     * Publishes a new message to the configured Pulsar topic.
     *
     * <p>
     * <b>Request body (JSON):</b>
     * </p>
     * 
     * <pre>{@code
     * { "content": "Hello Pulsar!", "sender": "TestClient" }
     * }</pre>
     *
     * <p>
     * <b>Success response (202 Accepted):</b>
     * </p>
     * 
     * <pre>{@code
     * {
     *   "messageId": "<uuid>",
     *   "content":   "Hello Pulsar!",
     *   "sender":    "TestClient",
     *   "timestamp": "2026-03-03T12:02:00"
     * }
     * }</pre>
     *
     * @param request the message payload deserialized from the HTTP request body
     * @return {@code 202 Accepted} with the published {@link MessageEvent},
     *         or {@code 500 Internal Server Error} on failure
     */
    @PostMapping
    public ResponseEntity<?> publishMessage(@RequestBody MessageRequest request) {
        try {
            MessageEvent event = producerService.publishMessage(request);
            log.info("Message published successfully with id: {}", event.getMessageId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(event);
        } catch (Exception e) {
            log.error("Failed to publish message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Simple health-check endpoint for liveness probes.
     *
     * @return {@code 200 OK} with {@code { "status": "UP", "service":
     *         "pulsar-producer" }}
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "pulsar-producer"));
    }
}
