# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Running the Application

```bash
# Start required services (PostgreSQL, Redis, Garage S3)
cd docker && docker-compose up

# Initialize Garage S3 storage (first time only, after compose startup)
./docker/garage-init.sh

# Build frontend CSS
npm run build:css          # one-time build
npm run watch:css          # watch mode during development

# Run the application
./mvnw spring-boot:run

# Run with Docker services (uses application-docker.yaml profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

The app runs on http://localhost:8000. It requires PostgreSQL, Redis, and Garage S3 to start.

### Build & Migrations

```bash
./mvnw clean package                                            # build JAR
./mvnw clean flyway:migrate -Dflyway.configFiles=flyway.conf   # run migrations manually
```

### Full Docker Stack

```bash
docker-compose -f docker/docker-compose.yml -f docker/app-docker-compose.yml up
```

## Architecture

This is a Spring Boot 4.1.0 / Java 25 e-commerce application with a **dual-interface design**: REST API (`/api/*`) for AJAX/programmatic access, and server-rendered Thymeleaf views for traditional page navigation. Both share the same service layer.

### Key Patterns

**Exception hierarchy:** All domain exceptions extend `BusinessException` (which carries an HTTP status). `ApiExceptionHandler` returns JSON for REST endpoints; `WebExceptionHandler` renders HTML error pages for views.

**Caching:** Redis-backed via Spring Cache annotations. Each cache has its own TTL (users: 1h, products: 30m, categories: 2h, orders: 15m). Cache eviction is triggered by `@CacheEvict` on mutating service methods.

**Security:** Stateless JWT authentication stored in httpOnly cookies. The `JwtAuthenticationFilter` validates tokens on every request. Route protection is layered: Spring Security handles coarse-grained route rules (`/admin/**` → ADMIN/MANAGER), and `@PreAuthorize` handles method-level checks.

**Rate limiting:** Redis sliding-window counters applied per endpoint/per-user. Configured in `RateLimitConfig` with stricter limits on auth endpoints.

**Audit trail:** `AuditEntityListener` (JPA lifecycle hooks) and `@Auditable` (AspectJ aspect) both write to the `audit_events` table, capturing old/new state with the acting user's ID.

**Image storage:** Product and category images are uploaded to Garage (a self-hosted S3-compatible service) via the AWS S3 SDK. `ImageStorageService` handles all object storage operations.

### Module Structure (`com.example.scaffold.*`)

| Package | Responsibility |
|---------|----------------|
| `security/` | JWT generation/validation, auth filters, login/register controllers |
| `category/` | Hierarchical categories, image serving from Garage |
| `product/` | Product catalog, variations, images, stock tracking |
| `user/` | User management, registration, admin CRUD |
| `order/` | Order lifecycle, cart logic, inventory validation |
| `promotion/` | Discount codes, redemption tracking, quote calculation |
| `audit/` | Compliance logging via entity listeners and AOP |
| `storage/` | S3/Garage abstraction for image upload/retrieval |
| `admin/` | Admin dashboard views and aggregated stats |
| `config/` | Spring Security, cache, rate limiting, S3 client config |
| `exception/` | Custom exception hierarchy |
| `common/` | Dual exception handlers, home controller |

### Frontend

Thymeleaf templates in `src/main/resources/templates/`. Styled with Tailwind CSS 4 + DaisyUI 5. HTMX handles partial page updates (search filtering, cart updates, email validation feedback). There is minimal vanilla JS — interactivity is mostly HTMX-driven.

CSS source is `src/main/resources/static/css/input.css`, compiled to `app.css` via npm.

### Database

PostgreSQL with 12 Flyway migrations (`src/main/resources/db/migration/`). Schema covers users, categories, products, product variations, product images, orders, order items, promotions, promotion redemptions, and audit events. Flyway runs automatically on startup.

### Configuration Profiles

- Default (`application.yaml`): connects to localhost services, port 8000
- Docker (`application-docker.yaml`): connects to `postgres`/`redis`/`garage` hostnames, enables DEBUG logging for scaffold code

### API Documentation

Swagger UI is available at `/swagger-ui/index.html` when the app is running.
