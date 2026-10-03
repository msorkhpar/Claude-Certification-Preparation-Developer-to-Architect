# Invoice API

## Commands

- Tests: `make test`. Lint: `make lint`.

## Git workflow

- Branches are named `feature/<ticket>`; never commit to `main`.
- A commit message starts with the ticket id and says why the change is needed.
- Every change goes through a pull request that links the ticket.

## CI

- Claude runs in GitHub Actions: on `@claude` mentions and as a reviewer on every pull request.
- Prompts live in `prompts/`. Change one only with a new version and a changelog entry.
