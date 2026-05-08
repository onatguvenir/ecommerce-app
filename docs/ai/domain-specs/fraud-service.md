---
type: ai-context
scope: domain-spec
service: fraud-service
port-http: 8088
port-grpc: none
db: none (KafkaStreams state store)
status: SKELETON — not production ready
last-updated: 2026-04-27
---

# fraud-service Domain Spec

## Responsibility
Real-time fraud detection on payment events using Kafka Streams. Should publish `UserAccountSuspendedEvent` when fraud detected.

## Status: Skeleton
Implementation is minimal. `FraudDetectionTopology` is a stub. Not integrated with user-service account suspension.

## Key Classes

| Class | Path | Role |
|---|---|---|
| `FraudDetectionTopology` | `application/stream/` | KafkaStreams DSL topology (stub) |
| `KafkaStreamsConfig` | `infrastructure/config/` | Streams config |

## Intended Flow
```
payment.completed → KafkaStreams topology
                  → detect anomalies (velocity, amount, geography)
                  → fraud detected → publish UserAccountSuspendedEvent
                  → user-service consumes → suspends account
```

## Event Models Defined (but not fully wired)
- `UserAccountSuspendedEvent` (in event-models module)

## What's Missing
1. Actual fraud detection logic in `FraudDetectionTopology`
2. Consumer in user-service for `UserAccountSuspendedEvent`
3. Account suspension endpoint in user-service
4. State store for tracking payment velocity per user

## When Implementing
- Use Kafka Streams DSL (not Processor API) for simplicity
- State store: windowed KeyValueStore for per-user payment count/amount
- Fraud rules: > N payments in window OR > X amount in window → suspicious
- Publish to `fraud.user-suspended` topic via Kafka Streams output
