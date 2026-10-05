# Invoice API

A small service that stores invoices and exports them as CSV.

## Commands

<!-- TODO 1 of 8 (unlocks m1): one bullet each for the test and the lint command, each command in backticks.
     Example: - Run the tests: `make test` -->
- TODO

## Conventions

- Python 3.12, type hints on public functions.
- Handlers live in `src/api/handlers/`, one module per resource.
- Branch names are `feature/<ticket>`; commit messages start with the ticket id.

## Architecture

<!-- TODO 2 of 8 (unlocks m1): import the architecture notes with an @ line instead of naming them.
     Example: See @docs/other.md for the details. -->
See the architecture notes for the module layout.

## Gotchas

- Tests need `INVOICE_DB_URL` to point at the local SQLite file; `make test` sets it.
- IMPORTANT: never edit files under `migrations/` by hand; add a new migration instead.
