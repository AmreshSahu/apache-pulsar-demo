package com.poc.producer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing an inbound HTTP request
 * to publish a message to the Pulsar topic.
 *
 * <p>
 * Instances of this class are deserialized from the JSON body of
 * {@code POST /api/messages} requests.
 * </p>
 *
 * <p>
 * <b>Example JSON:</b>
 * </p>
 * 
 * <pre>{@code
 * {
 *   "content": "Hello Pulsar!",
 *   "sender":  "TestClient"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    /**
     * The human-readable body of the message to be published.
     * Must not be {@code null} or blank.
     */
    private String content;

    /**
     * An optional identifier for the originator of the message,
     * e.g. a service name or user ID.
     */
    private String sender;
}
