# Real-Time Fraud Detection Microservice

A Spring Boot microservice that detects UPI payment fraud in real time using Apache Kafka Streams, Redis, and PostgreSQL.

## What it does

Consumes UPI transaction events from Kafka, runs them through a stateful sliding window aggregation, evaluates configurable fraud rules, and routes decisions — blocked transactions to a fraud-alerts topic, allowed transactions to the payment pipeline.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Streaming | Apache Kafka + Kafka Streams |
| Cache | Redis 7 |
| Database | PostgreSQL 15 |
| Resilience | Resilience4j |
| Metrics | Micrometer + Prometheus |
| Build | Maven (wrapper included) |
| Container | Docker Compose |

## Architecture

```
MockUpiProducer
  → transactions-raw (Kafka)
  → FraudDetectionTopology (Kafka Streams, sliding 60s window per user)
  → windowed-transactions (Kafka)
  → FraudDecisionService (idempotency check → load rules → evaluate)
  → ALLOW → allowed-transactions
  → BLOCK → fraud-alerts
```

## Prerequisites

- Java 17+
- Docker Desktop

No Maven install needed — wrapper (`mvnw`) is included.

## Run Locally

```bash
# 1. Start all infrastructure
docker-compose up -d

# 2. Create Kafka topics
docker exec fraud-kafka kafka-topics.sh --create --bootstrap-server localhost:9092 --topic transactions-raw --partitions 12 --replication-factor 1
docker exec fraud-kafka kafka-topics.sh --create --bootstrap-server localhost:9092 --topic windowed-transactions --partitions 12 --replication-factor 1
docker exec fraud-kafka kafka-topics.sh --create --bootstrap-server localhost:9092 --topic fraud-alerts --partitions 6 --replication-factor 1
docker exec fraud-kafka kafka-topics.sh --create --bootstrap-server localhost:9092 --topic allowed-transactions --partitions 12 --replication-factor 1

# 3. Start the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Verify It Works

| URL | What you see |
|---|---|
| `http://localhost:8090` | Kafka UI — live messages in topics |
| `http://localhost:8080/actuator/health` | App health (UP) |
| `http://localhost:8080/api/rules` | 4 active fraud rules |
| `http://localhost:8080/api/fraud/stats/today` | Live fraud counts |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics |

## Fraud Patterns (Auto-injected by MockUpiProducer)

| Pattern | User | Trigger | Every |
|---|---|---|---|
| Velocity abuse | user-003 | 6 transactions in 30s | 30s |
| Large amount + new payee | user-007 | ₹75,000 to unknown payee | 60s |
| New device + high amount | user-005 | New device, ₹15,000 transfer | 90s |

## REST Endpoints

```
GET    /api/rules                         — list active fraud rules
POST   /api/rules                         — create new rule
PUT    /api/rules/{id}                    — update rule threshold
DELETE /api/rules/{id}/deactivate         — soft-disable a rule

GET    /api/fraud/alerts                  — query fraud history (filter by userId, date, rule)
GET    /api/fraud/alerts/{txnId}          — single transaction decision
PUT    /api/fraud/alerts/{txnId}/override — mark false positive
GET    /api/fraud/stats/today             — total / flagged / allowed counts
GET    /api/fraud/stats/rules             — per-rule hit count (last 7 days)
```

## Toggle a Rule (Hot Reload — no restart needed)

```bash
# Disable velocity rule
curl -X DELETE http://localhost:8080/api/rules/1/deactivate

# Re-enable
curl -X PUT http://localhost:8080/api/rules/1/activate
```

Change takes effect within 30 seconds.
