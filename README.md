# MeterMind

> A credit-based usage-billing and consumption-monitoring platform prototype for AI and API SaaS businesses.

MeterMind allows SaaS owners to configure plans with included credit grants and per-feature credit prices, while enabling their customers to consume credits transparently through billable services. The platform records every balance movement in an append-only ledger, prevents overspending through atomic locking, warns before credit depletion, estimates runway, and provides explainable consumption recommendations.

---

## 1. What is Real vs. What is Simulated

To maintain honesty and production-grade engineering principles, MeterMind cleanly distinguishes what is fully enforced by the application versus what is simulated for demonstration:

### What is Real (Engineered & Enforced)
* **Double-Entry Style Credit Ledger:** Append-only transaction log (`credit_ledger_entries`) recording grants, debits, adjustments, and actor audit trails.
* **Concurrency-Safe Atomic Debits:** Server-enforced wallet row locking (`SELECT ... FOR UPDATE` or atomic conditional updates) with `@Transactional` guarantees preventing race conditions and balance overspending.
* **Server-Side Pricing & Price Snapshots:** Callers never supply credit prices; prices are determined server-side based on the customer's active plan and snapshotted onto each usage event.
* **API Idempotency:** Scoped idempotency keys with unique database constraints preventing duplicate charges on request retries and flagging payload mismatches with conflict errors (HTTP 409).
* **Multi-Tenant Isolation:** Complete data isolation between business organizations and between customer accounts.
* **Database Migrations:** Versioned Flyway migrations managing schema evolution for MySQL 8.4.
* **Automated CI/CD:** GitHub Actions workflow running automated verification against a live MySQL container service.

### What is Simulated (Honest Prototype Boundaries)
* **Payment Processing:** No real payment gateway (e.g., Stripe) is integrated. Card numbers are not accepted, processed, or stored.
* **Invoices:** Invoices are simulated previews/drafts generated from subscription fees and usage. They do not constitute legally binding tax invoices or calculate jurisdictional tax.
* **Credit Runway & Recommendations:** The "Credit Coach" uses deterministic, rule-based heuristics and mathematical averages over historical consumption windows; it does not invoke external machine learning or LLM models.
* **Alert Notifications:** In-app alerts and stored notification rows; external SMS/email gateways are not integrated.

---

## 2. Core Billing Invariants

Every feature in MeterMind adheres to non-negotiable accounting rules:
1. **Credits are Not Currency:** Credits are represented as integer `BIGINT` (`long`). Currency amounts (e.g., plan fees) use MySQL `DECIMAL` / Java `BigDecimal`. Floating-point (`double`, `float`) is strictly prohibited.
2. **Server-Side Pricing:** The client submits a feature code and unit quantity; the backend resolves and snapshots the active credit cost.
3. **Atomic Commit or Rollback:** Usage event creation, ledger entry insertion, and wallet balance updates commit together in a single transaction.
4. **Append-Only History:** Ledger entries are never edited or deleted. Adjustments are compensating entries with explicit reasons.
5. **Idempotency Final Authority:** Uniqueness constraints at the database layer act as the source of truth against concurrent races.

---

## 3. Technology Stack

* **Language & Runtime:** Java 17 / 21
* **Framework:** Spring Boot (Modular Monolith)
* **Data Access:** Spring Data JPA / Hibernate
* **Database & Migrations:** MySQL 8.4, Flyway
* **Security:** Spring Security (role-based access control with BCrypt password hashing)
* **Presentation:** Thymeleaf, HTML5, Vanilla CSS, Chart.js
* **Build Tool:** Maven with Maven Wrapper (`./mvnw` or `.\mvnw.cmd`)
* **CI/CD:** GitHub Actions, Docker (multi-stage non-root container)

---

## 4. Architecture & Package Structure

MeterMind is structured as a **modular monolith** under `com.example.MeterMind`:

```text
com.example.MeterMind
├── MeterMindApplication.java # Root entry point
├── common/                  # Global exceptions, error DTOs, utilities
├── identity/                # Authentication, AppUser, roles, password security
├── organization/            # Tenant / business organization domain
├── catalog/                 # Plans, feature pricing
├── customer/                # Customer billing profiles
├── subscription/            # Customer plan subscriptions & billing periods
├── wallet/                  # Wallets, append-only credit ledger, atomic debit
├── usage/                   # Idempotent usage event ingestion & price resolution
├── alerts/                  # Threshold monitoring and notifications
├── insights/                # Runway forecast and explainable Credit Coach
└── billing/                 # Simulated invoice previews & renewal grants
```

---

## 5. Development Roadmap (15 Commits / 3 Days)

MeterMind is developed incrementally across 15 focused commits:

* **Day 1: Working Project Foundation**
  * `01` — `docs: add MeterMind product and engineering specs`
  * `02` — `ci: add Maven verification workflow`
  * `03` — `build: configure MySQL and Flyway local development`
  * `04` — `feat(auth): add tenant-scoped identity and login roles`
  * `05` — `feat(catalog): add plans and feature credit pricing`
* **Day 2: Correct Credit Accounting**
  * `06` — `feat(customers): add customer accounts and subscriptions`
  * `07` — `feat(wallet): add grants and append-only credit ledger`
  * `08` — `feat(wallet): implement atomic debit and balance locking`
  * `09` — `feat(usage): add idempotent usage events and API`
  * `10` — `test(security): verify tenant isolation and billing invariants`
* **Day 3: Insight, Billing Preview and Delivery**
  * `11` — `feat(ui): add owner and customer dashboards`
  * `12` — `feat(alerts): add configurable usage thresholds and notifications`
  * `13` — `feat(insights): add runway forecast and explainable recommendations`
  * `14` — `feat(billing): add invoice previews and renewal simulation`
  * `15` — `ci(release): build and publish tagged container images`

---

## 6. Getting Started

### Prerequisites
* JDK 17 or higher
* Docker & Docker Compose (for local MySQL 8.4 container)
* Git

### Build & Test
```powershell
# On Windows
.\mvnw.cmd clean test-compile

# On macOS/Linux
./mvnw clean test-compile
```
