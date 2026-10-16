# MeterMind — Data Model and Billing Invariants

This is a logical design, not a demand to create every table in the first migration. Add tables in the relevant commit and use a new Flyway migration for every later schema change.

## 1. Core entities

| Entity/table | Important fields | Notes |
|---|---|---|
| `organizations` | `id`, `name`, `created_at` | A business/tenant that owns plans and customers. |
| `app_users` | `id`, `organization_id`, `customer_id` (nullable), `email`, `password_hash`, `role`, `enabled`, timestamps | Login identities. Email uniqueness should be defined intentionally; in a multi-tenant product the desired global/tenant scope must be clear. |
| `plans` | `id`, `organization_id`, `name`, `monthly_price`, `monthly_credits`, `active` | `monthly_price` is MySQL `DECIMAL`, mapped to Java `BigDecimal`; credits are integer. |
| `feature_prices` | `id`, `organization_id`, `plan_id`, `feature_code`, `credits_per_unit`, `active` | Pricing is scoped to an organization's plan. Add uniqueness for the chosen scope/code. |
| `customers` | `id`, `organization_id`, `display_name`, `email`, `user_id` (nullable until linked), timestamps | Customer billing profile belongs to one organization. |
| `subscriptions` | `id`, `organization_id`, `customer_id`, `plan_id`, `status`, `current_period_start`, `current_period_end` | Enforce that customer and plan belong to the same organization. |
| `credit_wallets` | `id`, `customer_id`, `balance`, `updated_at` | One wallet per customer account in the MVP; balance is nonnegative when hard limits apply. |
| `credit_ledger_entries` | `id`, `wallet_id`, `entry_type`, `amount`, `balance_after`, `reference_type`, `reference_id`, `actor_user_id`, `reason`, `created_at` | Positive integer amount plus a direction/type, or a documented signed convention. Do not mix conventions. Append-only by application logic. |
| `usage_events` | `id`, `organization_id`, `customer_id`, `subscription_id`, `feature_code`, `units`, `unit_credit_cost`, `credits_charged`, `idempotency_key`, `request_hash`, `created_at` | Save the price snapshot. Unique scope should prevent charging one customer twice for the same key. |
| `alert_rules` | `id`, `organization_id`, `customer_id` (nullable for org-wide), `rule_type`, `threshold_value`, `enabled` | Validate units/meaning of each threshold type. |
| `notifications` | `id`, `organization_id`, `customer_id`, `rule_id`, `event_key`, `title`, `message`, `created_at`, `read_at` | Deduplicate repeated alerts with a stable event/rule reference where appropriate. |
| `invoices` | `id`, `organization_id`, `customer_id`, `period_start`, `period_end`, `currency`, `subtotal`, `status`, timestamps | For MVP, label preview/draft clearly. |
| `invoice_lines` | `id`, `invoice_id`, `description`, `quantity`, `unit_price`, `amount` | Store money as `DECIMAL`/`BigDecimal`; specify rounding. |

Use UUIDs or numeric keys consistently; prefer one choice consistently across entities, DTOs and migrations. Do not force UUIDs if the already-created project has a coherent strategy; if changing it, explain why.

## 2. Relationships

- One organization owns many users, plans, feature prices, customers, subscriptions and invoices.
- A plan has many feature prices.
- A customer can have a subscription to a plan and one wallet in the MVP.
- A wallet has many ledger entries.
- A usage event belongs to one customer and subscription and may reference one corresponding debit ledger entry.
- An invoice has many invoice lines.

The service must verify matching organization/customer/plan ownership. Database foreign keys alone do not enforce all tenant rules.

## 3. Ledger representation

Pick and document one model:

**Recommended beginner model:** `amount` is always a positive integer; `entry_type` determines whether the movement is a grant/credit or debit/decrease; `balance_after` records the wallet balance after the movement. Reversal/adjustment entries reference the entry they compensate. Do not update or delete an old entry to “fix” an error.

A reconciliation function/test should verify:

```text
initial balance + all grants/positive adjustments - all debits = wallet balance
```

If credits can expire or be split into grant lots in a future version, this simple formula needs to account for expiry/forfeiture entries.

## 4. Atomic usage transaction

A debit should be a single `@Transactional` service operation that:

1. Authenticates and resolves the caller's customer/organization from the security context.
2. Checks for the idempotency key and validates that repeated payloads match.
3. Resolves an active price within the correct organization/plan/subscription.
4. Validates positive units and performs overflow-safe `unitCost * units` calculation.
5. Locks the wallet row for update or uses a tested atomic conditional debit.
6. Rejects insufficient balance/hard-limit violations.
7. Updates wallet balance and inserts the usage event and ledger movement.
8. Commits all changes together; any failure rolls all changes back.
9. Returns the existing response for an identical replay without a second debit. A reused key with a different request body returns conflict.

A pre-check alone is insufficient to prevent duplicate events because two requests can race. Add a database unique constraint for the idempotency scope and handle the unique-key conflict intentionally. The database is the final authority.

## 5. Concurrency test

Given a wallet with 100 credits, run two requests concurrently that each need 80 credits. Exactly one should succeed and the final balance must be 20. The losing request must not leave a usage row or debit entry. Also test concurrent retries with the same idempotency key.

## 6. Grant and renewal idempotency

A monthly included-credit grant should reference the subscription and billing-period identifier. Put a unique constraint on the grant reference (or a dedicated subscription-period record) so rerunning a scheduled job cannot grant the same period twice. Never implement renewal as an unguarded `balance += credits` cron job.

## 7. Money and credit units

- Credit units: Java `long`, MySQL `BIGINT`, always integer for this MVP.
- Money: Java `BigDecimal`, MySQL `DECIMAL(p,s)` with an intentional precision/scale; define currency and rounding.
- Do not assume 1 credit equals one unit of currency.
- Keep money-charge calculations separate from credit-balance arithmetic.
- If the system later supports currencies, persist the currency code with each price/invoice line and never infer it from locale.

## 8. Indexes and constraints

At minimum consider:
- unique `credit_wallets.customer_id`
- unique `(customer_id, idempotency_key)` on usage events, if keys are customer-scoped
- useful index `(customer_id, created_at)` for history queries
- useful index `(organization_id, created_at)` for owner aggregation
- unique plan/feature code within the chosen tenant/plan scope
- foreign keys for all required parent rows
- check constraints for positive quantities/amounts where supported by the target MySQL version, plus application validation

Inspect actual generated SQL and integration-test constraints against MySQL 8.4.
