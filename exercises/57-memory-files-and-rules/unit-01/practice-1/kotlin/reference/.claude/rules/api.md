---
paths:
  - "src/api/**/*.ts"
---

# API handler conventions

- Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.
- Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.
- Handlers are `async` and never swallow a rejected promise.
- Document each endpoint with an OpenAPI comment above its handler.
