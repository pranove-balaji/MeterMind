# Definition of Done — Each MeterMind Commit

A numbered commit is ready for the user's manual commit only when applicable checks pass:

## Code quality
- The change addresses only its planned commit, apart from necessary small prerequisites documented in the handoff.
- Code follows existing package/style conventions and uses meaningful names.
- Controllers are thin; billing/business rules live in services.
- Input DTOs are validated; callers cannot supply authoritative prices or impersonate another tenant.
- No secrets, `.env`, build output, credentials or personal data are added.

## Persistence and money (when applicable)
- Schema changes are in a new Flyway migration.
- Previously applied migrations are not edited.
- JPA schema validation agrees with the migration-created schema.
- Credit balance, event and ledger are transactionally consistent.
- Money uses `BigDecimal`/`DECIMAL`; credit units use integer types.
- Tenant scoping, idempotency and concurrency concerns are addressed at the relevant stage.

## Tests and evidence
- Add/update tests for new behavior and important failure cases.
- Run the focused test(s) and `clean verify` when the local environment permits it.
- Report exact commands and whether they passed, failed or were not run.
- Do not conceal failures with skipped tests or `|| true`.
- Update API/docs/examples when user-visible behavior changes.

## User handoff
- Explain the concept being taught.
- Describe changed files and key methods/annotations.
- Give precise manual test steps and expected outcomes.
- List known limitations and any follow-up risk.
- Provide the planned commit message plus `git add`/`git commit` commands, but do not execute them.
- Stop; do not start the next planned commit until asked.
