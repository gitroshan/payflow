# PayFlow — Distributed Payment Processing Platform

A production-shaped, event-driven payment platform built as a set of independent
Spring Boot microservices. It models the real money-movement lifecycle: authorize
→ capture → settle, with a double-entry ledger, refunds & disputes, merchant
payouts, and settlement reconciliation.

The goal of this repository is to demonstrate the **architecture and patterns**
that make payment systems correct and resilient — idempotency, the transactional
outbox, saga orchestration, event-carried state, and idempotent consumers — with
enough working code to run the core flow end to end.

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8080 | Single entry point: routing + JWT authentication, forwards trusted `X-Merchant-Id` |
| `payment-orchestrator-service` | 8081 | Payment API + saga: create/authorize/capture, idempotency, outbox |
| `ledger-service` | 8082 | Immutable double-entry ledger, projected from events |
| `refund-dispute-service` | 8083 | Refunds and chargeback/dispute lifecycle |
| `payout-reconciliation-service` | 8084 | Merchant payouts + provider settlement reconciliation |
| `gateway-adapter-service` | 8085 | Anti-corruption layer fronting external PSPs (mock included) |
| `payflow-common` | — | Shared value objects (`Money`), domain events, topic names |

Two things are named "gateway" and it's worth keeping them straight: **`api-gateway`**
is the edge (auth + routing for clients), while **`gateway-adapter-service`** is the
outbound anti-corruption layer in front of external payment providers.

Backbone infrastructure (via `infra/docker-compose.yml`): **Kafka** for events,
**PostgreSQL** (one database per service), and **Redis** for idempotency keys.

---

## Requirements

- **JDK 17+** (the project targets Java 17; it was written against Spring Boot 3.2)
- **Maven 3.9+**
- **Docker + Docker Compose** (for Kafka, Postgres, Redis)

---

## Quick start

```bash
# 1. Start the backbone (Kafka, Postgres with per-service DBs, Redis)
cd infra
docker compose up -d
cd ..

# 2. Build everything
mvn clean install

# 3. Run each service in its own terminal (or background them)
mvn -pl gateway-adapter-service         spring-boot:run   # :8085
mvn -pl payment-orchestrator-service    spring-boot:run   # :8081
mvn -pl ledger-service                  spring-boot:run   # :8082
mvn -pl refund-dispute-service          spring-boot:run   # :8083
mvn -pl payout-reconciliation-service   spring-boot:run   # :8084
mvn -pl api-gateway                     spring-boot:run   # :8080
```

Then exercise the platform (calls the services directly):

```bash
./scripts/smoke-test.sh
```

Or go through the authenticated gateway:

```bash
./scripts/auth-demo.sh
```

## Authentication (via the gateway)

Every request through the gateway (port 8080) needs a JWT. Get one with the demo
client credentials, then call the API with the `Authorization` header:

```bash
# 1. Exchange client credentials for a token (public endpoint)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"merchant_acme_client","clientSecret":"s3cr3t"}' | jq -r .accessToken)

# 2. Call the payment API through the gateway
curl -s -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-2001' \
  -d '{"merchantId":"merchant_acme","amount":49.99,"currency":"USD",
       "paymentMethodToken":"tok_visa","captureImmediately":true}'
```

The gateway verifies the HS256 signature, then forwards the merchant identity to
downstream services as a trusted `X-Merchant-Id` header. Requests without a valid
token get `401`. Demo clients and the signing secret live in
`api-gateway/src/main/resources/application.yml` (override the secret in
production via the `PAYFLOW_AUTH_SECRET` environment variable).

Kafka UI is available at http://localhost:8090 to watch events flow.

---

## The core flow, by example

Create and immediately capture a payment (a "sale"):

```bash
curl -s -X POST http://localhost:8081/api/v1/payments \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-1001' \
  -d '{
        "merchantId": "merchant_acme",
        "amount": 49.99,
        "currency": "USD",
        "paymentMethodToken": "tok_visa",
        "captureImmediately": true
      }'
```

What happens:

1. The **orchestrator** reserves the idempotency key (Redis), creates the payment,
   and calls the **gateway adapter** to authorize, then capture.
2. In the same DB transaction as each state change, it writes a domain event to
   its **outbox** table. A poller relays those to Kafka (`payment.events`).
3. The **ledger** consumes `PaymentCaptured` and posts a balanced double-entry
   journal (debit provider-clearing, credit merchant-payable).
4. The **refund** and **payout** services consume the same event to build their
   own local read models of captured payments.

Inspect the results:

```bash
# Ledger balances for the merchant
curl -s http://localhost:8082/api/v1/ledger/accounts/merchant_acme | jq

# Refund it (partial refunds supported)
curl -s -X POST http://localhost:8083/api/v1/refunds \
  -H 'Content-Type: application/json' \
  -d '{"paymentId":"<id>","amount":10.00,"reason":"customer_request"}' | jq

# Pay the merchant out
curl -s -X POST http://localhost:8084/api/v1/payouts \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"merchant_acme","amount":39.99,"currency":"USD"}' | jq
```

### Triggering declines (mock PSP rules)

The mock gateway is deterministic, so you can demo failure paths:

- `paymentMethodToken: "tok_declined"` → `card_declined`
- `paymentMethodToken: "tok_insufficient"` → `insufficient_funds`
- any amount ending in `.13` (e.g. `10.13`) → `do_not_honor`

---

## Key design patterns

- **Idempotency** — clients send an `Idempotency-Key`; the orchestrator reserves
  it in Redis and de-duplicates retries so a network retry never double-charges.
- **Transactional outbox** — events are written to a DB table in the same
  transaction as the state change, then relayed to Kafka. This removes the
  dual-write problem (state and events can never diverge).
- **Idempotent consumers** — every consumer records processed `eventId`s, so
  at-least-once delivery becomes effectively-once.
- **Dead-letter + retry** — each consumer retries a failing record with a fixed
  back-off (3× / 2s) and, once exhausted, routes it to a `<topic>.DLT` topic so a
  single poison message never stalls the partition.
- **Gateway auth** — the edge gateway authenticates every request (HS256 JWT) and
  forwards a trusted `X-Merchant-Id` downstream, keeping services free of auth code.
- **Saga orchestration** — the orchestrator drives authorize → capture and
  compensates (marks `FAILED`, emits `PaymentFailed`) on any step failure.
- **Event-carried state / read models** — downstream services never call the
  orchestrator synchronously; they build their own projections from events.
- **Double-entry ledger** — every money movement is a balanced journal entry;
  the debits==credits invariant is enforced before any balance is touched.
- **Money value object** — a single immutable `Money` type enforces currency
  matching and consistent rounding across every service.

See `docs/ARCHITECTURE.md` for the full write-up and diagrams.

---

## Testing

```bash
mvn test                                   # everything
mvn -pl payflow-common test                # Money value-object tests (no infra)
mvn -pl ledger-service test                # double-entry + idempotency (Mockito, no infra)
mvn -pl payment-orchestrator-service test  # full flow via Testcontainers (needs Docker)
```

- `MoneyTest` — currency-matching, rounding, comparisons. Pure unit test.
- `LedgerServiceTest` — asserts every posting is balanced (debits == credits),
  that balances move the right way, and that replays are ignored. Pure unit test.
- `PaymentFlowIntegrationTest` — spins up **real** Postgres, Redis and Kafka with
  Testcontainers, stubs the PSP, and verifies create+capture, idempotent replay,
  and the declined-payment path end to end. Requires a running Docker daemon.

---

## Notes & next steps

This is a reference implementation focused on architecture. Still worth adding for
a real deployment: a real PSP adapter behind the existing interface, PCI-compliant
tokenization/vaulting, CDC (Debezium) instead of the polling outbox relay,
distributed tracing with correlation IDs, multi-currency FX and per-currency
rounding, and three-way reconciliation against bank statements. Authentication,
dead-letter handling, and a test harness are already included.
