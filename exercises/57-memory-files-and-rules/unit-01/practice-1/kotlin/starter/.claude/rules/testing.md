# Testing conventions

- Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.
- Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.
- Mock the network with `msw`; a test never calls a live service.
- Each test checks one behaviour and its name states that behaviour.
<!-- TODO 3 of 7 (finish this to pass m1, e2, e6): scope this rule with YAML frontmatter above the title: a `paths` list with the globs `**/*.test.ts` and `**/*.test.tsx`, so it loads for test files wherever they are. -->
