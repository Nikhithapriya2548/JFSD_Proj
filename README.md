# OmniShop — E-Commerce Microservices Platform

**Stack:** Java 17, Spring Boot 3.2, Spring Data JPA + Hibernate, PostgreSQL, RabbitMQ, Redis, React + Tailwind, Docker Compose.
**Status:** All services healthy, robust E2E test coverage.

OmniShop is a comprehensive, production-ready e-commerce platform built on a microservices architecture. It supports a full customer journey: catalog browsing, reviews, coupon application, order checkout through a simulated payment gateway, and order tracking. For administrators, it provides full control over products, coupons, order statuses, feature flags, and sales analytics.

The system is designed with scalability and resilience in mind, split into five independent Spring Boot microservices that communicate asynchronously via RabbitMQ and synchronously via REST. The storefront is a modern React application, and the entire stack can be orchestrated locally with a single Docker Compose command.

## Contents

1. [Architecture](#1-architecture)
2. [Quick Start Setup](#2-quick-start-setup)
3. [Service Modules Overview](#3-service-modules-overview)
4. [API Reference](#4-api-reference)
5. [Testing & Verification](#5-testing--verification)
6. [Architecture Decisions](#6-architecture-decisions)
7. [Troubleshooting](#7-troubleshooting)

## 1. Architecture

```
Browser (:3000, React SPA)
 ├── product-service   :8081 ──► omnishop_products DB ──► Redis (cache)
 ├── order-service     :8082 ──► omnishop_orders DB
 ├── user-service      :8083 ──► omnishop_users DB
 ├── payment-service   :8084 ──► omnishop_payments DB
 ├── notification-svc  :8085 ──► omnishop_notifications DB
 ├── RabbitMQ :5672 (order.created → stock/payment/notify fan-out)
 ├── Zipkin   :9411 (distributed tracing)
 └── Postgres :5432 (isolated logical databases in one container for local dev)
```

Each service owns its domain data and can be deployed and scaled independently. Synchronous calls (e.g., frontend interactions, real-time price checks) utilize REST. Asynchronous side-effects (e.g., stock deduction, payment processing, notification dispatch) leverage RabbitMQ events, ensuring that a slow or unavailable consumer does not impact critical paths like checkout.

## 2. Quick Start Setup

### Prerequisites

- Docker Desktop (Compose v2 included) with at least 8 GB RAM allocated.
- A shell environment (PowerShell, bash, zsh) with the `docker` CLI available.
- Available ports: 3000, 5432, 5672, 6379, 8081–8085, 9411.
- *Note:* No local Java or Node.js installation is required; all builds occur within Docker.

### Step 1 — Start the Infrastructure

```bash
docker compose up -d --build
```

The initial boot will download Maven dependencies and build the images (typically 5–10 minutes). Subsequent startups will use cached layers.

You can verify the backend health status:
```bash
# PowerShell example
foreach ($p in 8081,8082,8083,8084,8085) {
  Write-Host "$p :" (Invoke-RestMethod "http://localhost:$p/actuator/health").status
}
```

### Step 2 — Seed Initial Data

Populate the database with an initial product catalog:

```bash
python scripts/seed_products.py
```
This script populates products, assigns stock, attaches placeholder images, and provisions the default administrator account.

### Step 3 — Access the Application

| Interface | URL |
|---|---|
| Storefront | http://localhost:3000 |
| Admin Dashboard | http://localhost:3000/admin/login |
| Distributed Tracing (Zipkin) | http://localhost:9411 |
| RabbitMQ Management | http://localhost:15672 (guest/guest) |
| API Docs (Swagger/OpenAPI) | http://localhost:8081/swagger-ui.html (available on ports 8081-8085) |

**Default Credentials:** 
- Admin: `admin@omnishop.local` / `admin123`
- Customers can self-register via the storefront UI.

### Configuration

Base configuration resides in `docker-compose.yml`. Sensitive data and secrets are managed via an environment file (`.env`):

| Variable | Description |
|---|---|
| `JWT_SECRET` | Shared signing key for JWT validation across services. |
| `DB_USER` / `DB_PASSWORD` | PostgreSQL credentials. |
| `VITE_*_API_URL` | Frontend service mapping (baked during the build phase). |

*Note: Changes to `VITE_*` variables require a frontend rebuild (`docker compose build frontend`).*

## 3. Service Modules Overview

### Product Service (:8081) — Catalog, Search, and Reviews
- Manages `Product` and `Review` entities, along with system-wide `FeatureFlag`s.
- Implements Redis caching for catalog reads with explicit eviction on writes.
- Advanced search capabilities using JPA Specifications and stream-based relevance scoring.
- Event-driven stock management: listens to `order.created` and `order.payment_failed` to adjust inventory.

### Order Service (:8082) — Checkout, Saga Management, and Analytics
- Orchestrates the order lifecycle using the Saga pattern. Orders begin as `PENDING` and are finalized based on asynchronous payment events.
- Performs server-side price validation to ensure integrity.
- Handles coupon validation (expiry, usage limits, minimum spend) and discount calculation.
- Exposes administrative endpoints for sales analytics and revenue aggregation.

### User Service (:8083) — Identity and Authentication
- Centralized token issuer implementing short-lived JWT access tokens and rotatable refresh tokens.
- Secure password storage using BCrypt.
- Implements robust rate-limiting to prevent brute-force attacks.

### Payment Service (:8084) — Payment Processing Gateway
- Acts as an internal gateway simulator, mimicking external providers (e.g., Stripe, Razorpay).
- Injects realistic latency and configurable success/failure rates for robust system testing.
- Maintains transaction records and provides receipt endpoints.

### Notification Service (:8085) — Event Delivery
- Consumes domain events across the platform to generate user-facing notifications.
- Provides a unified inbox experience for end-users, decoupling communication logic from core business transactions.

### Frontend (:3000) — React Storefront
- Responsive, mobile-first design built with Tailwind CSS.
- Features an optimistic, local-first shopping cart and a streamlined multi-step checkout.
- Comprehensive admin dashboard for managing the catalog, orders, and system configuration.
- Resilient authentication layer with silent token refresh and automatic retry mechanisms.

## 4. API Reference

Base URLs correspond to the respective service ports. Authentication requirements: `–` (Public), `U` (Authenticated User), `A` (Administrator).

| Endpoint | Auth | Description |
|---|---|---|
| `GET /api/v1/products` | – | Retrieve catalog listings |
| `GET /api/v1/products/search` | – | Search with relevance sorting |
| `POST/PUT/DELETE /api/v1/products` | A | Catalog administration |
| `POST /api/v1/users/register` | – | User registration |
| `POST /api/v1/orders` | U | Initialize checkout and begin saga |
| `GET /api/v1/orders/analytics/summary` | A | Retrieve sales and revenue metrics |
| `POST /api/v1/payments` | U | Process simulated payment |
| `GET /api/v1/notifications/user/{uid}` | U | Fetch user notification inbox |

*Refer to the Swagger UI on each service for complete endpoint documentation.*

## 5. Testing & Verification

The project includes an extensive test suite ensuring reliability across all layers:

- **End-to-End Suite (`run-and-test.ps1`):** A comprehensive PowerShell script that orchestrates environment setup, data seeding, and executes 65 automated checks covering auth, checkout sagas, rate limiting, and RBAC guards.
- **Unit Testing:** 60+ isolated tests utilizing JUnit 5, Mockito, and AssertJ, focusing heavily on domain logic, token validation, and rate-limiting algorithms.
- **Continuous Integration:** GitHub Actions (`.github/workflows/ci.yml`) pipeline enforces build success and unit test passing on all commits.
- **Observability:** Integrated distributed tracing via Zipkin. All log statements across services inject an `X-Correlation-ID` for seamless request tracking.

## 6. Architecture Decisions

- **Stateless Authentication:** Transitioned from session-based auth to stateless JWTs to support seamless horizontal scaling.
- **Choreography-based Sagas:** Opted for event-driven choreography over centralized orchestration for the checkout flow, improving service decoupling and fault tolerance.
- **API Gateway Omission:** For this specific local topology, direct service routing was chosen to reduce latency and architectural complexity; an API gateway would be introduced for external, multi-region deployments.

## 7. Troubleshooting

| Issue | Resolution |
|---|---|
| **Port Conflicts** | Run `docker compose down` to clear stale containers, then `up -d`. |
| **Authentication Errors post-login** | If tokens expire during development, log out and back in to re-initialize the rotation cycle. |
| **Empty Catalog** | Ensure the seeding script (`python scripts/seed_products.py`) has been executed successfully. |
| **Frontend Connection Issues** | If environment variables (`VITE_*`) were modified, rebuild the frontend image: `docker compose build frontend`. |
