# OmniShop — E-Commerce Microservices Platform

Java Full Stack project (ACSD30): Spring Boot microservices + PostgreSQL,
orchestrated with Docker Compose.

## Architecture

```
                    +-----------------------------+
                    |        omnishop-network     |
                    |        (bridge)             |
                    |                             |
  :8081  +----------+-----------+                 |
 ------> | product-service      |                 |
         | (Spring Boot, :8081) |                 |
         +----------+-----------+                 |
                    | JDBC                        |
                    v                             |
         +----------+-----------+    REST         |
         |  postgres-db         |<----------------+
         |  (:5432, internal)   |  product-service:8081
         |  omnishop_products   |
         |  omnishop_orders     |    +-------------+--------+
         |  omnishop_users      |    | order-service       |
         |  omnishop_payments   |    | (Spring Boot, :8082)|
         +----------+-----------+    +------+------+--------+
                    ^                       |      |
                    | JDBC            payment|      | users (Wave 2 auth)
                    |                   v      v
         +----------+-----------+  +-----+  +------+
         |  (one container,     |  | pay |  | user |
            4 logical DBs)      |  |:8084|  |:8083 |
                               |  +-----+  +------+
                                          ^       ^
         frontend :3000 ------------------+-------+
         (React SPA, calls each service directly)
```

Host exposes ONLY: 5432 (postgres-db), 8081 (product), 8082 (order),
8083 (user), 8084 (payment), 3000 (frontend).
Services talk to each other via Docker service names, never localhost.
Payment service is SIMULATED (mock gateway, demo only — no real money moves).
```

## Order saga (simplified SAGA pattern for distributed transactions)

Order placement no longer calls payment-service synchronously. Instead the
flow is choreographed through RabbitMQ (`omnishop.events` topic exchange):

```
Customer            order-service        RabbitMQ            payment-service      product-service      notification-service
   |  POST /orders       |                   |                       |                      |                       |
   | ------------------> | save PENDING      |                       |                      |                       |
   |  201 PENDING        | publish           |                       |                      |                       |
   | <------------------ | order.created --> | --------+------------>|                      |                       |
   |                     |                   |         |             | process (mock ~1.5s) |                       |
   |                     |                   |         |             | publish              |                       |
   |                     |                   |         | payment.completed                |                       |
   |                     | <-----------------+-----------------------+                      |                       |
   |                     | CONFIRMED                                  |                      |                       |
   |  poll → CONFIRMED   |                                            |                      |                       |
```

- `order.created` → payment-service charges (mock) + product-service
  **reserves stock** (optimistic `@Version` locking; `stock.low` if < 10).
- `payment.completed` → order CONFIRMED. `payment.failed` → order
  PAYMENT_FAILED + `order.payment_failed` compensation → product-service
  **restores the reserved stock**, notification-service informs the user.
- Every transition also emits `order.status.changed` for the inbox.
- DB rows are the source of truth; messages only advance state. Duplicate
  deliveries converge (listeners ignore non-PENDING orders).
- RabbitMQ management UI: http://localhost:15672 (guest/guest).

## Cache strategy (product-service + Redis)

- `@Cacheable` on product-by-id and product-list (60s TTL safety net).
- `@CacheEvict` on every mutation: product create/update/delete, review
  add (ratings feed the cached DTO), and saga stock changes.
- Rule: writes evict explicitly; TTL covers any missed invalidation so
  stale data can never live longer than 60 seconds.

## Prerequisites

- Docker Engine 24+
- Docker Compose v2 (`docker compose version`)

## How to run

```bash
cd D:/JFSD_Project
docker compose up --build
```

Start in background:

```bash
docker compose up --build -d
docker compose logs -f
```

Stop / full reset (deletes DB data):

```bash
docker compose down
docker compose down -v   # also removes postgres-data volume
```

## How to verify

```bash
# 1. DB is healthy
docker compose ps
docker exec omnishop-postgres pg_isready -U omnishop_admin

# 2. Both databases exist
docker exec omnishop-postgres psql -U omnishop_admin -d postgres -c "\l" | grep omnishop

# 3. product-service is up — create a product
curl -X POST http://localhost:8081/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Phone","description":"Demo","price":14999,"stockQuantity":20,"category":"Electronics"}'

# 4. product-service is up — list products
curl http://localhost:8081/api/v1/products

# 5. order-service is up — list orders
curl http://localhost:8082/api/v1/orders
```

End-to-end test — order-service calls product-service over the Docker
network (note: `productId` must exist from step 3):

```bash
curl -X POST http://localhost:8082/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"items":[{"productId":1,"quantity":2}]}'
```

Expected: HTTP 201 with `priceAtPurchase` snapshotted from product-service,
`totalAmount` computed via Streams, `status: PENDING`.

## Port reference

| Service         | Host port | Container port | Notes                              |
|-----------------|-----------|----------------|------------------------------------|
| postgres-db     | 5432      | 5432           | Only DB exposed to host            |
| product-service | 8081      | 8081           | REST: `/api/products`              |
| order-service   | 8082      | 8082           | REST: `/api/orders`                |
| frontend        | 3000      | 80             | Phase 4 (currently commented out)  |

Internal traffic: `order-service → http://product-service:8081`
(DB host from inside containers: `postgres-db:5432`).

Config: credentials live in `.env` (gitignored); compose references them
as `${VAR_NAME}` — nothing hardcoded. DB names are logical databases
(`omnishop_products`, `omnishop_orders`) inside ONE Postgres container,
created on first init by `db-init/init-multi-db.sh`.

## Wave 2 — security, observability, safer schema changes

### Centralized auth (JWT everywhere)
- user-service is the only issuer: `POST /api/v1/users/login` returns a **15-minute access token** plus a **7-day refresh token** (opaque, SHA-256 hashed at rest, single-use with rotation — reuse is rejected and revokes the chain).
- product/order/payment validate the same `JWT_SECRET` locally (no per-request call to user-service). No token → `401`; valid token without the role → `403`, both in the standard `{timestamp,status,error,message,path}` envelope.
- Rules: `GET /api/v1/products/**` (+ search, flags, health, swagger) is public; every write needs a login; product CRUD, coupon creation, order status changes, flag toggles and `/api/v1/orders/analytics/summary` need `ADMIN`.
- Frontend: Bearer auto-attached, `/checkout` and `/admin` redirect when unauthenticated, silent refresh 60s before access-token expiry.
- Demo admin: `admin@omnishop.local` / `admin123` (seeded).
- Abuse guards (deliberately in-memory, single-instance scope): login 5 failures / 15 min per email → `429`; order creation and payment posts 30 / min per IP → `429`. Deliberately NOT distributed — a Redis/Bucket4j limiter is the documented next step.

### Correlation IDs + tracing
- Every service accepts/generates `X-Correlation-ID` (response echoes it), logs include it (`[correlationId]` in every log line), order-service forwards it over RestClient, and every saga event carries it — listeners rejoin the trace via MDC.
- Zipkin tracing (Micrometer Brave, 100% sample in demo): UI at http://localhost:9411. HTTP hops trace fully; RabbitMQ hops have no automatic span linking (no Sleuth), so queue legs are followed via the correlationId in the payload/logs instead.

### Flyway (product + order services)
- `ddl-auto=update` replaced with `validate` + versioned migrations (`db/migration/V1__init.sql` mirrors the exact pre-existing schema). Existing databases baseline V1 without replaying (`baseline-on-migrate`); fresh databases build from V1.
- The old `orders_status_check` constraint was dropped on purpose: it rejected new enum values at the DB layer after Java added them. Statuses are validated in Java.
- user/payment/notification still use `ddl-auto` — adopting the same Flyway pattern is tracked follow-up, not done here.

### Analytics + feature flags
- `GET /api/v1/orders/analytics/summary` (ADMIN): totals, 30-day revenue-by-day and status distribution via Streams `groupingBy`. The admin Analytics tab shows this live, keeping the old client-side chart as fallback context.
- Feature flags (`GET /api/v1/flags` public, `PUT /api/v1/flags/{key}` ADMIN; seeded `reviews`, `coupons`): the frontend hides the Reviews tab / coupon field when off, with a 5-min client cache. Admin Flags tab toggles at runtime — no redeploy.

### Verification added
- 20 unit tests (Mockito, no Spring context): coupon math/guards (7), analytics grouping (2), rating aggregation (3), mock-gateway outcomes (3), JWT roundtrip/tamper/expiry (4), login throttle (3). Run in CI (`.github/workflows/ci.yml`: backend `mvn test` matrix + frontend build + compose validate).
- `run-and-test.ps1` now authenticates (customer + admin bootstrap) and asserts the guards: U7 refresh rotation, U8 login throttle 429, SEC1–SEC4 401/403 boundaries, A1 analytics, G1–G3 flags.
- `docs/OmniShop-E2E.postman_collection.json`: full journey collection with admin/customer token variables.

### Deliberately NOT built (Wave 2 scope cuts)
- API Gateway and Config Server: evaluated, rejected for this deployment size — one public surface doesn't justify the hop/latency, and env-var config is sufficient for 5 services. Revisit past ~10 services.
- Notification inbox has no auth (read-only demo feed). Known gap, disclosed.
- No distributed rate limiting, no refresh-token reuse detection beyond chain revocation, no DB-level FK constraints in V1 baselines (integrity enforced by JPA mappings).
- XSS: React escapes all rendered values by default; the only `dangerouslySetInnerHTML` risk would be product descriptions — none used. Admin-entered product/review text is rendered as plain text, never parsed as HTML.

## Port reference (Wave 2 additions)
| Service | Port |
|---|---|
| Zipkin UI | 9411 |

## Wave 3 — relevance, payment retry/receipts, frontend polish, demo kit

### Search relevance (product-service)
- `GET /api/v1/products/search?q=` filters with the existing JPA Specifications,
  then scores with Streams when no explicit `sortBy` is given: name match (3)
  > category (2) > description (1), name tiebreak. Explicit sorts
  (priceAsc/Desc, newest, ratingDesc) bypass scoring untouched.
- Deliberate call: DB-agnostic scoring, no native full-text index at this
  catalog size; revisit past ~10k products. Covered by unit tests
  (name > category > description; explicit sort not reshuffled) and suite
  check S5.

### Payment retry + receipts (payment-service + order history)
- New `GET /api/v1/payments/{id}` receipt (404 envelope when unknown) backed by
  `PaymentNotFoundException`; suite check M3.
- Order history: PAYMENT_FAILED rows get a method picker + **Retry payment**
  (posts a NEW gateway attempt against the same order — the mock declines
  ~15%, so retry usually succeeds, toast confirms either way) and a
  **View receipt** modal listing every attempt with transaction id.
- No idempotency keys on retry by design: each attempt is a distinct recorded
  row, which is exactly what the receipt shows.

### Frontend polish
- Real 404 page (`*` route renders NotFoundPage, no longer Home).
- Toasts everywhere it matters: login/register welcome, coupon
  applied/rejected, order cancelled, cart remove + stock-cap, review added,
  payment retry outcome (cancel/review toasts pre-existed).
- Orders + notifications use the logged-in user's id (guest fallback to the
  demo user); navbar Alerts badge shows inbox size, refreshed on navigation
  (no backend read-state, so it's a total-count badge — labeled as such).
- Optimistic cart: already local-first (reducer updates instantly, persists to
  localStorage) — Wave 3 only added the missing feedback (stock-cap/remove
  toasts) and documented the pattern in code.

### Demo kit
- `docs/DEMO.md`: 5-minute narrated script (story → customer flow → retry →
  admin → Zipkin proof → backup Q&A).
- `docs/OmniShop-E2E.postman_collection.json` (Wave 2) + CI (Wave 2) unchanged.
- Screenshots: not included — the sandboxed docs environment has no route to
  localhost:3000, so canned screenshots would be mockups, not evidence. The
  demo script's checkpoints are the verifiable substitute.

### Verification (Wave 3)
- 26 unit tests green (added: 2 relevance, 2 receipt).
- Suite extended to 65 checks (M3 receipt 404, S5 relevance order).
- Live-verified: relevance order (name hits first), receipt 200 + 404,
  customer review flow re-verified after Wave 2 rule fix.
