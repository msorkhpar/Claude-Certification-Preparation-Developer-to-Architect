# Review guidance

## Always check

- Money amounts are integer cents, never floats, in `src/billing/`.
- Every new endpoint in `src/api/handlers/` has a test under `tests/api/`.
- A database change comes with a migration in `migrations/`.

## Skip

- Formatting and import order: the linter owns them.
- Generated files under `src/generated/`.
