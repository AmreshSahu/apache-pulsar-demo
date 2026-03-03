# Apache Pulsar POC — Walkthrough

## What Was Built

A complete two-microservice POC demonstrating async messaging with Apache Pulsar.

```
/Users/amresh/ag/
├── docker-compose.yml             # Pulsar standalone (port 6650 broker, 8085 admin)
├── pulsar-producer/               # Spring Boot :8080 — publishes messages
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── src/main/java/com/poc/producer/
│       ├── PulsarProducerApplication.java
│       ├── config/PulsarConfig.java
│       ├── dto/{MessageRequest, MessageEvent}.java
│       ├── service/MessageProducerService.java
│       └── controller/MessageController.java
└── pulsar-consumer/               # Spring Boot :8081 — consumes & persists to H2
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/main/java/com/poc/consumer/
        ├── PulsarConsumerApplication.java
        ├── config/PulsarConfig.java
        ├── dto/MessageEvent.java
        ├── entity/Message.java
        ├── repository/MessageRepository.java
        ├── service/PulsarConsumerListener.java
        └── controller/MessageQueryController.java
```

## Build Validation

Both projects compiled successfully with zero errors:

```
✅ pulsar-producer  → ./mvnw compile  [BUILD SUCCESS]
✅ pulsar-consumer  → ./mvnw compile  [BUILD SUCCESS]
```

---

## How to Run

### Step 1 — Start Apache Pulsar (Docker required)

```bash
cd /Users/amresh/ag
docker-compose up -d
```

Wait ~20s for Pulsar to boot. Verify:
```bash
docker-compose logs pulsar | grep "messaging service is ready"
```

> Pulsar Admin UI: http://localhost:8085

```
# Is Pulsar healthy?
curl http://localhost:8085/admin/v2/brokers/healthcheck

# List topics in the default namespace
curl http://localhost:8085/admin/v2/persistent/public/default

# After sending a message — topic stats
curl http://localhost:8085/admin/v2/persistent/public/default/order-topic/stats

```

### Step 2 — Start the Producer (Terminal 1)

```bash
cd /Users/amresh/ag/pulsar-producer
./mvnw spring-boot:run
```

Expected: `Started PulsarProducerApplication on port 8080`

### Step 3 — Start the Consumer (Terminal 2)

```bash
cd /Users/amresh/ag/pulsar-consumer
./mvnw spring-boot:run
```

Expected: `Started PulsarConsumerApplication on port 8081`

---

## Test the Flow

### Publish a Message

```bash
curl -X POST http://localhost:8080/api/messages \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello Pulsar!", "sender": "TestClient"}'
```

**Expected Response (202 Accepted):**
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "content": "Hello Pulsar!",
  "sender": "TestClient",
  "timestamp": "2026-03-03T12:02:00"
}
```

### Verify Consumer Persisted the Message

```bash
curl http://localhost:8081/api/messages
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "messageId": "550e8400-e29b-41d4-a716-446655440000",
    "content": "Hello Pulsar!",
    "sender": "TestClient",
    "producedAt": "2026-03-03T12:02:00",
    "receivedAt": "2026-03-03T12:02:01"
  }
]
```

### H2 Console (optional)

Open: http://localhost:8081/h2-console  
- JDBC URL: `jdbc:h2:mem:orderdb`  
- Username: [sa](file:///Users/amresh/ag/pulsar-consumer/src/main/java/com/poc/consumer/entity/Message.java#11-36) | Password: *(blank)*

### Health Checks

```bash
curl http://localhost:8080/api/messages/health  # producer
curl http://localhost:8081/api/messages/health  # consumer (shows stored count)
```

---

## Key Design Decisions

| Concern | Decision |
|---|---|
| Serialization | JSON via Jackson (`ObjectMapper`) |
| Deduplication | Consumer checks [existsByMessageId()](file:///Users/amresh/ag/pulsar-consumer/src/main/java/com/poc/consumer/repository/MessageRepository.java#10-11) before saving |
| Consumer threading | Dedicated `ExecutorService` single thread, graceful shutdown via `@PreDestroy` |
| Topic type | `persistent://public/default/order-topic` — durable, survives restarts |
| Subscription type | `Shared` — allows multiple consumer instances |
