# Architecture

The platform has three parts: a web client under `web/`, an HTTP API under `src/api/` and the infrastructure under `terraform/`.
The API owns the data; the client never talks to the database. Migrations live in `db/migrations/` and are applied by the release job.
