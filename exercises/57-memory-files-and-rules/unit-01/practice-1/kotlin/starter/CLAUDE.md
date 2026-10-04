# Inventory platform

Everything Claude needs to know, in one file.

@docs/standards/architechture.md

## Always
- Commit messages use the imperative mood and stay under 72 characters in the first line.
- Run `npm test` before saying a task is done.
- Do not add dependencies without asking.

## Testing
- Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.
- Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.
- Mock the network with `msw`; a test never calls a live service.
- Each test checks one behaviour and its name states that behaviour.

## API handlers
- Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.
- Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.
- Handlers are `async` and never swallow a rejected promise.
- Document each endpoint with an OpenAPI comment above its handler.

## Terraform
- Every resource carries the `owner` and `cost-centre` tags.
- Run `terraform fmt` and `terraform validate` before proposing a change.
- Pin provider versions with `~>` constraints.

## Protected files
- Never edit files under `db/migrations/`.

## About me
- I prefer short answers with no preamble.
- My sandbox API is at http://localhost:4010 with the dev token from my shell profile.
- My notes are in /home/dev/notes.

## History
- Background note 1: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 2: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 3: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 4: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 5: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 6: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 7: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 8: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 9: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 10: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 11: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 12: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 13: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 14: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 15: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 16: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 17: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 18: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 19: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 20: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 21: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 22: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 23: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 24: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 25: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 26: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 27: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 28: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 29: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
- Background note 30: this section explains why the team made an earlier choice and is kept for history; it changes no behaviour.
