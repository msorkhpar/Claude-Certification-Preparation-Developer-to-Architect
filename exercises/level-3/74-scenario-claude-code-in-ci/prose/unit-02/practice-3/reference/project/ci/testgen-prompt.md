# Generate tests

Write tests for these changed files:

{{changed_files}}

The project already has these tests. Do not repeat any of them:

{{existing_tests}}

## A useful test

- Checks a behaviour that a caller can see, not how the code is written inside.
- Covers one case: a normal value, an empty value, a value at a limit, or an error.
- Fails when the behaviour it checks breaks.

## Do not write

- A test of a getter or a constant.
- A second test of a case that an existing test already covers.
