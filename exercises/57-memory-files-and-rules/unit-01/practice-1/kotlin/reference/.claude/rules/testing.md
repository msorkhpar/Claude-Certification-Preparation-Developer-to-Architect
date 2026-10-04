---
paths:
  - "**/*.test.ts"
  - "**/*.test.tsx"
---

# Testing conventions

- Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.
- Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.
- Mock the network with `msw`; a test never calls a live service.
- Each test checks one behaviour and its name states that behaviour.
