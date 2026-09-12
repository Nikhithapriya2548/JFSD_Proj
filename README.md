# OmniShop — E-Commerce Microservices Platform

**Course:** ACSD30 Java Full Stack Development, V Semester, Institute of
Aeronautical Engineering (Autonomous), Hyderabad.
**Stack:** Java 17, Spring Boot 3.2, Spring Data JPA + Hibernate, PostgreSQL,
RabbitMQ, Redis, React + Tailwind, Docker Compose.
**Status:** all services healthy, 65/65 end-to-end checks green, 60/60 unit
tests green.

OmniShop is a complete online store: customers browse a catalog, read and
write reviews, apply coupons, check out through a simulated payment gateway
and track orders; admins manage products, coupons, order statuses, feature
flags and sales analytics. The system is split into five independent Spring
Boot services that communicate over REST and RabbitMQ events, with a React
storefront and everything runnable in Docker with one command.

## Contents

1. [Architecture](#1-architecture)
2. [Full setup](#2-full-setup)
3. [How each module works](#3-how-each-module-works)
4. [Syllabus mapping (ACSD30 Modules I–V)](#4-syllabus-mapping-acsd30-modules-iv)
5. [API reference](#5-api-reference)
6. [Verification and testing](#6-verification-and-testing)
7. [Demo script](#7-demo-script)
8. [Deliberately not built](#8-deliberately-not-built)
9. [Troubleshooting](#9-troubleshooting)

## 1. Architecture

```
Browser (:3000, React SPA)
 ├── product-service   :8081 ──► omnishop_products DB ──► Redis (cache)
 ├── order-service     :8082 ──► omnishop_orders DB
 ├── user-service      :8083 ──► omnishop_users DB
 ├── payment-service   :8084 ──► omnishop_payments DB
 ├── notification-svc  :8085 ──► omnishop_notifications DB
 ├── RabbitMQ :5672 (order.created → stock/payment/notify fan-out)
 ├── Zipkin   :9411 (distributed traces, optional)
 └── Postgres :5432 (four logical databases, one container)
```

Each service owns its database and can be built, deployed and scaled
independently. Synchronous calls (order → product/price check, frontend →
any service) use REST; asynchronous side-effects (stock deduction, payment
verdict, notifications) use RabbitMQ events so a slow or down consumer never
blocks checkout.

## 2. Full setup

### Prerequisites

- Docker Desktop (Compose v2 included), 8 GB RAM free
- PowerShell on Windows (commands below) or any shell with `docker`
- Ports free: 3000, 5432, 5672, 6379, 8081–8085, 9411
- No Java/Node needed on the host — everything builds inside Docker

### Step 1 — start everything

```powershell
cd D:\JFSD_Project
docker compose up -d --build
```

First boot takes 5–10 minutes (Maven downloads dependencies once, cached
afterwards). Wait until all backends answer:

```powershell
foreach ($p in 8081,8082,8083,8084,8085) {
  Write-Host "$p :" (Invoke-RestMethod "http://localhost:$p/actuator/health").status
}
```

### Step 2 — seed the catalog (~78 products)

```powershell
python scripts/seed_products.py
```

This scrapes the legal scraping sandbox (webscraper.io test site), assigns
stock, attaches images and POSTs each product as the admin user. Answer `y`
if it asks about existing products.

### Step 3 — verify (65 automated checks)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File run-and-test.ps1
```

Expected: `Total: 65 Passed: 65 Failed: 0`. The script rebuilds, waits for
health, then exercises register → login → catalog → reviews → search →
order saga → payment → coupons → notifications → admin guards (summary table
printed at the end).

### Step 4 — open the app

| Page | URL |
|---|---|
| Storefront | http://localhost:3000 |
| Admin login | http://localhost:3000/admin/login |
| Zipkin traces | http://localhost:9411 |
| RabbitMQ dashboard | http://localhost:15672 (guest/guest) |
| Swagger (each service) | http://localhost:8081/swagger-ui.html (…8082–8085 likewise) |

**Accounts:** admin `admin@omnishop.local` / `admin123` (seeded); customers
self-register in the UI in seconds.

### Configuration

Copy-free defaults live in `docker-compose.yml`; secrets live in `.env`
(gitignored, never committed):

| Variable | Meaning | Default |
|---|---|---|
| `JWT_SECRET` | Shared signing key for all services | dev key (override in production) |
| `DB_USER` / `DB_PASSWORD` | Postgres credentials | see `.env` |
| `VITE_*_API_URL` | Browser-side service URLs (baked at frontend build) | `http://localhost:80xx` |

Frontend `VITE_*` values are baked at **build** time, so after changing them
rebuild the frontend (`docker compose build frontend`).

## 3. How each module works

### product-service (:8081) — catalog, reviews, search, flags

- `Product` entity with price, stock, category, image; `Review` entity with
  1–5 rating; `FeatureFlag` entity (`reviews`, `coupons` kill-switches).
- Reads are public; writes need login; product CRUD and flag toggles need
  ADMIN (Spring Security + JWT filter, first-match rule order).
- Catalog reads are cached in Redis (60 s TTL, explicit eviction on writes).
- Search filters with JPA Specifications, then scores relevance with Streams
  (name 3 > category 2 > description 1) unless an explicit sort is given.
- Listens for `order.created` (decrements stock, emits `stock.low` under
  threshold) and `order.payment_failed` (restores reserved stock).

### order-service (:8082) — ordering, coupons, saga, analytics

- Order flow is a choreography saga: persist PENDING → publish
  `order.created` → return immediately. The payment verdict arrives later via
  `payment.completed` / `payment.failed` events (CONFIRMED or PAYMENT_FAILED).
- Live prices are re-read from product-service per item; client-sent totals
  are never trusted. Coupons validate (expiry, usage limit, minimum order)
  and discount the Streams-computed subtotal.
- Only PENDING orders can be cancelled (guard message otherwise); status
  changes publish `order.status.changed` for the inbox.
- `GET /api/v1/orders/analytics/summary` (ADMIN) aggregates totals, 30-day
  revenue-by-day and status split with `groupingBy`.

### user-service (:8083) — identity only

- Sole token issuer: 15-minute JWT access tokens + 7-day opaque refresh
  tokens (SHA-256 hashed at rest, single-use rotation — reuse revokes the
  chain). Passwords are BCrypt-hashed; login is throttled (5 failures /
  15 min per email → 429).

### payment-service (:8084) — honest simulator

- No real money moves: fixed delay, ~85% success, `MOCK-TXN-*` ids standing
  in for a Razorpay/Stripe gateway. Every attempt is a recorded row;
  `GET /api/v1/payments/{id}` returns the receipt. Direct re-POST against the
  same order is the retry mechanism the UI exposes.

### notification-service (:8085) — event inbox

- Pure consumer: every saga event becomes a persisted human-readable record
  (`order.created`, `payment.completed/failed`, `stock.low`, …), served
  newest-first per user. No real email/SMS provider by design.

### frontend (:3000) — React storefront

- Catalog with search/category/sort, product detail with reviews and rating
  distribution, optimistic local-first cart (instant updates, localStorage
  persistence, stock-cap feedback), 3-step checkout with inline validation
  and coupon field, calm payment-retry screen, order history with status
  stepper + receipt modal, notifications inbox with navbar badge, admin
  dashboard (products, orders, coupons, analytics, reviews, feature flags).
- JWT auto-attached; silent refresh 60 s before expiry plus a 401-rotation
  interceptor, so sessions survive past the 15-minute access token.

## 4. Syllabus mapping (ACSD30 Modules I–V)

### Module I — Collections, Streams, exception handling, Java 8

| Syllabus topic | Where it is implemented | How it is used |
|---|---|---|
| List / Set / Map | `OrderServiceImpl`, `CouponService`, `ProductServiceImpl`, analytics | Item lists, event payload maps, flag maps, category sets |
| Streams (`filter/map/reduce/sorted/collect`) | Order subtotal (`reduce`), revenue-by-day and status split (`groupingBy`), relevance scoring (`sorted`), in-stock filter, rating average | All totals, analytics and search ordering areStreams pipelines with no manual loops |
| `Collectors` | `groupingBy`, `toMap` (flags), `toList`, `reducing` | Analytics endpoint and flag controller |
| Lambda + functional interfaces | Comparators, `orElseThrow` suppliers, `thenAnswer` in tests, React-side array methods mirroring the same style | Sorting, lazy exceptions, test stubs |
| `Optional` | Repository lookups (`findById`, `findByCodeIgnoreCase`) | Every "get by id/code" path returns `Optional` and throws a named exception when empty |
| Date/Time API | `LocalDateTime` order dates, coupon expiry, token expiry, 15-min/7-day windows | Ordering, expiry checks, session lifetimes |
| Exception handling | `GlobalExceptionHandler` per service + named exceptions (`OrderNotFoundException`, `InsufficientStockException`, `InvalidCouponException`, `PaymentNotFoundException`, `TooManyAttemptsException`) | Every error returns the same `{timestamp,status,error,message,path}` envelope; 401/403/404/409/429 each have a dedicated path |
| I/O streams | Logback file/console appenders in all five services; resource loading (Flyway SQL, seeder file handling) | Startup, request and saga logging with correlation ids |
| Mini-project (Student Management) | Superset: the same List/Map/Streams/exception skills drive the catalog, coupon and analytics features above | CO1 evidence: 60 unit tests assert the stream logic directly |

### Module II — Session management, Hibernate ORM

| Syllabus topic | Where it is implemented | How it is used |
|---|---|---|
| Cookies / client session | Browser `localStorage` (`omnishop-token`, `omnishop-refresh`, `omnishop-user`) + silent refresh | Login persists across reloads; `RequireAuth`/`RequireAdmin` route guards replace server session checks |
| HttpSession (server side) | Stateless JWT sessions (the clustered successor): services are `STATELESS`, no server session state | Horizontal scaling without sticky sessions; logout = client discards tokens, refresh rotation revoked server-side |
| URL rewriting | Not used — stateless Bearer headers replace session-id-in-URL entirely | Documented as intentionally superseded |
| `@Entity`, `@Table`, `@Id` | `Product`, `Review`, `Order`, `OrderItem`, `Coupon`, `User`, `RefreshToken`, `Payment`, `Notification`, `FeatureFlag` | Every table is an annotated class; schema managed by Flyway baselines + `validate` (product/order) |
| One-to-Many | `Order` → `OrderItem` (`@OneToMany`), `Product` → `Review` (query-side) | Order totals, stock checks and rating aggregation traverse these relationships |
| Mini-project (Library Management) | Superset: books→products, users→customers, issue/return→order saga with PENDING/CONFIRMED states | CO3 evidence: full CRUD + sessions + relationships across five databases |

### Module III — Spring Framework (IoC, DI, MVC)

| Syllabus topic | Where it is implemented | How it is used |
|---|---|---|
| IoC container / beans | `@Service`, `@Component`, `@Configuration`, `@RestControllerAdvice` in every service | All wiring done by Spring; no `new` for dependencies |
| Dependency injection | Constructor injection via Lombok `@RequiredArgsConstructor` | Services declare `final` dependencies; tests inject Mockito mocks the same way |
| Bean lifecycle | `CommandLineRunner` seeders (`AdminSeeder`, `FlagSeeder`) | Admin user and flag keys seeded once at startup, idempotently |
| DispatcherServlet / Controllers | `@RestController` + `@GetMapping/@PostMapping/...` (Boot auto-configures the servlet) | ~40 endpoints; OpenAPI docs generated from annotations |
| Model–View | DTOs (`ProductResponseDTO`, `OrderResponseDTO`, …) separate entities from API shapes | Entities never leak to JSON; mapping is explicit (`toDTO`) |
| Mini-project (Employee CRUD) | Superset: admin dashboard performs product/coupon CRUD through the same controller→service→repository layers | CO4 evidence |

### Module IV — Microservices with Spring Boot, RWD, Spring Data JPA

| Syllabus topic | Where it is implemented | How it is used |
|---|---|---|
| Auto-configuration | `@SpringBootApplication` + starters (web, data-jpa, amqp, validation, actuator) | Zero XML; `application.yml` only carries DB URLs, ports and feature toggles |
| Independent microservices | Five deployables, four databases, one `docker-compose.yml` | Each has its own Dockerfile, health check and Swagger UI |
| REST APIs | Resource URLs (`/api/v1/products/{id}`, `/api/v1/orders`, …) with correct status codes (200/201/400/401/403/404/409/429) | Tested by Postman collection, 65-check PowerShell suite and 60 unit tests |
| Spring Data JPA | `JpaRepository` + `JpaSpecificationExecutor` per aggregate; derived queries (`findByCodeIgnoreCase`, `findByOrderId`) | No handwritten SQL except versioned Flyway migrations |
| Responsive UI | Tailwind breakpoints in every page (`grid-cols-1 sm:… lg:…`, drawer, stepper) | Phone-to-desktop layouts; mobile-first cart and checkout |
| Mini-project (Product Catalog) | This project *is* the catalog microservice, plus four siblings | CO5 evidence |

### Module V — Postman, Docker

| Syllabus topic | Where it is implemented | How it is used |
|---|---|---|
| Postman | `docs/OmniShop-E2E.postman_collection.json` + root `OmniShop-Postman-Collection.json` | Full journey (register → coupon → saga → retry → admin) with `{{customerToken}}` / `{{adminToken}}` variables |
| Images vs containers | One Dockerfile per service (multi-stage Maven build → slim JRE runtime) | `docker compose build` produces 6 images; `up` runs 10 containers |
| Dockerfile | Each service: `maven:3.9` build stage, `eclipse-temurin:17-jre` runtime, `curl` for health checks | Mirrors the syllabus Dockerfile pattern per service |
| `docker build/run/ps/stop`, port mapping | `docker-compose.yml` (ports, healthchecks, networks, named volumes) | `up -d`, `ps`, `logs <service>`, `down -v` (wipe + fresh start) |
| Mini-project (Dockerized Boot app) | Six-fold: every backend plus the Nginx frontend is containerized | CO6 evidence; CI (`.github/workflows/ci.yml`) builds and unit-tests on every push |

## 5. API reference

Base URLs are the service roots (`:8081` product, `:8082` order, `:8083`
user, `:8084` payment, `:8085` notification). Auth column: `–` public,
`U` any login, `A` ADMIN.

| Method & path | Auth | Purpose |
|---|---|---|
| `GET /api/v1/products`, `/{id}`, `/in-stock` | – | Catalog reads |
| `GET /api/v1/products/search?…` | – | Filter + relevance/sort search |
| `POST /api/v1/products` `PUT /{id}` `DELETE /{id}` | A | Catalog management |
| `GET /api/v1/products/{id}/reviews` | – | Review list |
| `POST /api/v1/products/{id}/reviews` | U | Write a review |
| `GET /api/v1/flags` / `PUT /api/v1/flags/{key}` | – / A | Feature flags |
| `POST /api/v1/users/register` `/login` | – | Auth (login returns access + refresh JWT) |
| `POST /api/v1/users/refresh-token` | – | Rotate access token (single-use) |
| `GET /api/v1/users/{id}` `PUT /{id}` | – | Profile (demo-open) |
| `POST /api/v1/orders` | U | Place order → PENDING + saga start |
| `GET /api/v1/orders/{id}` `/user/{uid}` `/high-value` | U | Order reads |
| `PUT /api/v1/orders/{id}/status` | A | Status transition |
| `PUT /api/v1/orders/{id}/cancel` | U | Cancel (PENDING only) |
| `POST /api/v1/coupons` | A | Create coupon |
| `GET /api/v1/coupons/validate?…` | U | Validate without consuming |
| `GET /api/v1/orders/analytics/summary` | A | Revenue + status analytics |
| `POST /api/v1/payments` | U | Simulated charge (SUCCESS/FAILED) |
| `GET /api/v1/payments/order/{oid}` `GET /api/v1/payments/{id}` | U | Attempts / receipt |
| `GET /api/v1/notifications/user/{uid}` | – | Event inbox (demo-open) |
| `GET /actuator/health` | – | Liveness (all services) |

## 6. Verification and testing

- `run-and-test.ps1` — 65 checks: rebuild, health, auth, reviews, search,
  saga settlement, payment, coupons, notifications, guards (401/403/429),
  flags, analytics, frontend routes, product update/delete roundtrip.
- Unit tests — 60 total, no Spring context (JUnit 5 + Mockito + AssertJ):
  16 user (service, JWT, throttle), 24 order (service, coupons, analytics,
  JWT filter, rate limit), 8 product (service, flags), 7 payment (service,
  rate limit), 5 notification (listener rendering).
- CI — `.github/workflows/ci.yml`: backend `mvn test` matrix, frontend
  build, compose validation on every push.
- Tracing — Zipkin UI shows cross-service spans; every log line carries the
  `X-Correlation-ID`.

## 7. Demo script

`docs/DEMO.md` is a 5-minute narrated script: customer flow → failed-payment
retry → order receipt → admin analytics/flags → Zipkin trace proof, with
backup viva answers.

## 8. Deliberately not built

- API Gateway and Config Server: evaluated, rejected — one public surface
  does not justify the extra hop; env-var config suffices for five services.
  Revisit past ~10 services.
- Real payments (PCI-DSS, webhooks, refunds), real email/SMS providers.
- Distributed rate limiting (in-memory guards are single-instance by
  design), DB-level FK constraints in Flyway baselines (integrity via JPA).

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `My Orders: Authentication required` right after login | Access token older than 15 min with no refresh stored (pre-fix session) | Log out and back in once; rotation handles it thereafter |
| Port already in use | Stale containers | `docker compose down` then `up -d` |
| Empty catalog | `run-and-test.ps1` starts with `down -v` (fresh DB by design) | `python scripts/seed_products.py` |
| Suite `S5` fails on custom data | Assertion assumes the wiped single-product catalog | Re-run the unmodified suite |
| Frontend calls wrong host | `VITE_*` baked at build | Rebuild frontend after env changes |
| `401` on all guarded calls after env edit | `JWT_SECRET` must match in every service block | Keep the single `.env` value; all services read it |
