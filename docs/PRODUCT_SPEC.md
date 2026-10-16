# MeterMind — Product Specification

## 1. Product statement

MeterMind is a small, understandable credit-based billing and consumption-monitoring platform for AI/API SaaS businesses. Business owners configure plans and per-feature credit prices. Their customers consume credits through a simulated billable service. The platform records every movement, shows where credits went, warns before depletion, estimates runway, and provides explainable suggestions.

**Positioning:** focus on credit transparency and actionable consumption control. Do not claim competitors lack metering, balance display or alerts; established billing platforms already provide many of those functions. MeterMind's portfolio hypothesis is a more focused customer-facing Credit Coach experience.

## 2. Personas

### Owner / Billing Admin
- Create and maintain plans and feature prices.
- Add customer accounts and attach a subscription/plan.
- Grant or adjust credits with a required reason.
- See usage across customers, active warnings and a simulated invoice preview.
- Configure usage thresholds and hard credit limits.

### Customer
- View their own wallet balance, plan and billing-period dates.
- Inspect their own events, ledger entries and consumption by feature/day.
- Choose a budget/alert preference where enabled.
- See a basic projected runout date and recommendations with explicit assumptions.
- Be denied new billable operations when credit is insufficient or an enabled hard cap blocks them.

## 3. MVP user journey

1. Owner signs in and creates a plan with included credits and feature pricing.
2. Owner creates a customer and assigns a subscription.
3. Wallet receives an initial credit grant, recorded in the ledger.
4. Customer submits a usage request for a feature and positive quantity.
5. Server determines the correct price, checks authorization and balance, then atomically stores the debit, event and ledger entry.
6. Duplicate usage requests with the same idempotency key do not double-charge.
7. Customer sees the changed balance/history; owner sees aggregate consumption.
8. Alert rules create notifications at thresholds and on a basic usage spike.
9. Credit Coach estimates runway and describes recommendations from deterministic rules.
10. Owner generates a simulated invoice preview from subscription fees and any configured chargeable items.

## 4. MVP scope

### Required
- Authentication and OWNER/BILLING_ADMIN/CUSTOMER authorization.
- Organization/tenant ownership and isolation.
- Plans and per-feature credit price catalogue.
- Customer records and subscription assignment.
- Credit wallet, append-only ledger, grants and debits.
- Atomic, concurrency-safe credit consumption.
- Usage events and scoped idempotency keys.
- Owner and customer dashboards with history and charts.
- Configurable balance/usage alerts and stored notifications.
- Basic runway forecast and transparent recommendations.
- Invoice preview; label all unprocessed payments and tax/accounting features as simulated.
- MySQL, Flyway, automated tests, GitHub Actions CI and versioned container delivery to GHCR.

### Explicitly out of scope for the first 15 commits
- Real payment collection, card storage, chargebacks or PCI handling.
- Tax calculation, legally compliant invoice numbering by jurisdiction, accounting integrations or revenue recognition.
- Multi-currency settlement or a real-money/credit exchange guarantee.
- Production-grade email/SMS delivery.
- AI model calls, ML training or opaque optimization recommendations.
- Microservices, Kafka, Redis or a separate Node/React frontend.
- Automatic deployment to a cloud host (GHCR delivery is included; hosting is chosen later).
- Complex expiring grant lots, carry-forward and FIFO credit allocation.

## 5. Product differentiators to demonstrate

1. **Credit runway:** estimate days to depletion as `availableCredits / averageDailyCreditsConsumed`; state the measurement window and show “not enough history” when data is sparse.
2. **Explainable Credit Coach:** display the metric/event pattern that caused the suggestion, not just a mysterious score.
3. **Guardrails:** configurable warnings and server-enforced hard balance limits.
4. **Usage anomaly hint:** compare a recent window to the customer's own baseline and flag unusual consumption; avoid claims of AI or fraud detection.
5. **Audit trail:** each movement is traceable to a usage event, actor/reason, timestamp and balance after the movement.

## 6. Commercial model for the prototype

Use integer credits separate from money. Plans can have a monthly fee represented as `BigDecimal`, a monthly included credit grant, and feature prices measured in integer credits per unit. The sample price list is fictional; allow owners to configure it.

The prototype may simulate subscriptions and invoice previews, but must not tell users that a payment succeeded unless an actual trusted payment integration is implemented.

## 7. Acceptance principles

- A displayed wallet balance agrees with its ledger entries and the outcome of completed operations.
- Duplicate usage requests do not charge twice.
- Concurrent use cannot overspend the wallet.
- One tenant cannot inspect or change another tenant's customers, plans, wallets, events, or invoices.
- Customer dashboards contain only that customer's information.
- Forecasts and recommendations show assumptions and do not promise guaranteed savings.
- A clean clone can be built and tested through the documented Maven wrapper and CI workflow.
