# MeterMind — Architecture and Build Boundaries

## 1. Chosen shape

Use a **modular monolith**: one Spring Boot application, one MySQL database, clearly separated domain packages. This keeps deployment and debugging manageable for a beginner while still showing sound separation of concerns.

## 2. Request flow

```text
Browser (Thymeleaf + HTML/CSS/JS)
          |
          v
Spring MVC Controller  ---> request DTO / validation
          |
          v
Domain Service          ---> authorization + business rules
          |
          v
Spring Data JPA Repository ---> MySQL transaction / constraints
          |
          v
Domain Entities and Flyway-managed schema
```

A controller should parse the request, use the authenticated identity, validate input and delegate. A service enforces pricing, tenant ownership, credit sufficiency, idempotency and transaction boundaries. Repositories read/write persisted state. Templates display DTO/model data; they do not calculate authoritative billing values.

## 3. Suggested package modules

Adapt these names to the existing base package; don't rename an existing project indiscriminately.

```text
com.metermind
├── MeterMindApplication.java
├── common/       # exception handler, common error DTOs
├── identity/     # AppUser, roles, login, registration or account provisioning
├── organization/ # Organization / tenant
├── catalog/      # Plan, feature price
├── customer/     # Customer billing account
├── subscription/ # Subscription, billing period/status
├── wallet/       # CreditWallet, CreditLedgerEntry, wallet service
├── usage/        # UsageEvent, request/result DTOs, ingestion service/controller
├── alerts/       # AlertRule, Notification, evaluation logic
├── insights/     # forecast and recommendation logic
└── billing/      # invoice preview, invoice lines, renewal grant service
```

For each module, introduce Controller, Service, Repository, Entity and DTO only where they serve an actual need. Avoid generic base repositories or premature interfaces that don't add value.

## 4. Persistence and configuration

- MySQL 8.4 is the expected local/CI database.
- Flyway migrations in `src/main/resources/db/migration/` are the schema source of truth.
- Configure the datasource through environment variables; provide safe local placeholders only.
- `spring.jpa.hibernate.ddl-auto=validate` after the first schema migration exists. Do not use `update` as a substitute for migrations.
- Disable Open Session in View (`spring.jpa.open-in-view=false`) and explicitly fetch/assemble data in service/controller paths as needed.
- Store timestamps consistently in UTC and document billing period timezone assumptions.

## 5. UI approach

Use Thymeleaf templates under `src/main/resources/templates/`, static assets under `src/main/resources/static/`, and optional Chart.js for usage charts. Use a simple shared layout and navigation. No JavaScript build step is required. UI controls are never the sole enforcement point for permissions or billing limits.

## 6. Error handling

Centralize errors with `@ControllerAdvice` / `@RestControllerAdvice` (or separate handler strategies when HTML and JSON responses need different formats). Return clear, non-secret errors. For API operations, use suitable status codes: 400 validation failure, 401 unauthenticated, 403 forbidden, 404 not found/hidden tenant resource, 409 idempotency conflict or invalid state, and 422 only if the API deliberately uses it for a business-rule rejection. Keep this convention consistent and documented.

## 7. CI/CD architecture

- GitHub Actions CI runs Maven verification against a MySQL service on pushes and pull requests.
- Build a runtime container image with a multi-stage Dockerfile; runtime runs as a non-root user.
- On version tags such as `v0.1.0`, publish an immutable/versioned image to GitHub Container Registry with repository-scoped `GITHUB_TOKEN` permissions.
- This is build/test plus artifact delivery. A live deployment step is deferred until the user chooses a host/platform and provides required secrets/configuration safely.

## 8. Deliberately deferred architecture

Do not add a message broker, service mesh, microservices or separate frontend in the first 15 commits. The current traffic and scope do not justify the operational complexity. A future outbox/queue can be introduced for reliable asynchronous notifications if the product grows.
