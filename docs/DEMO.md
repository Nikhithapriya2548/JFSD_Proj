# OmniShop — 5-Minute Demo Script

Audience: course evaluator / interviewer. Total: ~5 minutes.
Prereqs: `docker compose up -d --build`, then `powershell -NoProfile -ExecutionPolicy Bypass -File run-and-test.ps1` (63 checks, all green).

## 0:00 — The story (30s)

"OmniShop is a 5-service e-commerce platform: Spring Boot microservices,
React storefront, RabbitMQ event choreography, Redis cache. No real money —
the payment gateway is an honest simulator with a documented 85% success rate."

Open: **http://localhost:3000** (storefront) and **http://localhost:9411** (Zipkin).

## 0:30 — Shop as a customer (90s)

1. Search "phone" in the navbar — name matches rank above category/description
   (Streams relevance scoring, no full-text index at this size).
2. Open a product → Reviews tab, star distribution, add a review (needs login —
   register in 10s, get a "Welcome" toast; JWT + silent refresh under the hood).
3. Add to cart (toast), cart badge increments, cart page caps quantity at stock
   with a toast.
4. Checkout → shipping form validates inline → payment step → apply `SAVE10`
   coupon (toast shows savings) → Pay.
5. If the mock gateway declines (~15%): the calm retry screen explains no money
   moved → "Try a different method". If it succeeds: confirmation page.

## 2:00 — My Orders: retry + receipt (60s)

1. **My Orders** → status stepper animates PENDING → CONFIRMED → …
2. For a PAYMENT_FAILED order: pick a method, **Retry payment** — a NEW gateway
   attempt posts against the same order, toast confirms.
3. **View receipt** — every attempt with transaction id, method, amount, status.

## 3:00 — Admin (60s)

Login at **/admin/login** (`admin@omnishop.local` / `admin123`):

1. **Analytics** — server-computed summary (revenue-by-day, status split) live
   from `/api/v1/orders/analytics/summary`, plus client-side charts.
2. **Flags** — toggle `reviews` off → open the storefront in another tab: the
   Reviews tab is gone. Toggle back on. No redeploy.
3. Change an order status → customer inbox gets `order.status.changed`.

## 4:00 — Architecture proof (60s)

1. **Zipkin** — click a trace: user → order → product + payment spans, one
   `X-Correlation-ID` across logs and saga events.
2. `docker compose logs product-service | grep correlationId` — same id on the
   HTTP request and the RabbitMQ stock update.
3. Guards: `POST /api/v1/products` without a token → 401 envelope; as a
   customer → 403; `PUT /api/v1/flags/reviews` as anonymous → 401.

## 4:50 — Close (10s)

"20+ unit tests, CI on every push, one-command verify. Deliberately NOT built:
API gateway and config server — documented in the README with the revisit
threshold. Questions?"

## Backup answers

- **Why RabbitMQ over REST for the saga?** Stock + payment + notifications fan
  out from one order event; queues survive a consumer being down.
- **Why no gateway?** One public surface (the SPA talks to services directly);
  a gateway hop buys little before ~10 services.
- **Real payments?** Out of scope by design — PCI-DSS, webhooks and refunds are
  named in the README as the production follow-up.
