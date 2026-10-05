# Inventory platform

Shared instructions for everyone who works on this repository. Area conventions live in `.claude/rules/` and load when matching
files are touched. The architecture notes are imported below.

@docs/standards/architechture.md

## Always
- Commit messages use the imperative mood and stay under 72 characters in the first line.
- Run `npm test` before saying a task is done.
- Do not add dependencies without asking.

## API handlers
- Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.
- Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.
- Handlers are `async` and never swallow a rejected promise.
- Document each endpoint with an OpenAPI comment above its handler.

## Where things are
- Area conventions: `.claude/rules/` (testing, API handlers, Terraform).
- Files in `db/migrations/` are protected by the permission rules, not by this file.
- Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.

## About me
- I prefer short answers with no preamble.
- My sandbox API is at http://localhost:4010 with the dev token from my shell profile.
- My notes are in /home/dev/notes.
