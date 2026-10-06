# Review criteria for CI

## Report
- Bugs: code that crashes or returns a wrong result on a reachable input, for example an unchecked `None` or an off-by-one in a loop bound.
- Security: user input that reaches a query, a shell command or a file path without validation.
- A comment or docstring whose claimed behaviour contradicts what the code does.

## Skip
- Minor style: naming, line length, import order.
- Patterns that the codebase already uses in other files.

## Severity
- high: data loss or a security hole, for example `DELETE FROM orders` built from request input.
- medium: a wrong result on a reachable input, for example `range(len(items) - 1)` that skips the last item.
- low: a misleading name or comment, for example a variable called `total` that holds a count.

## Testing standards
- Tests live in `tests/` and build their data from `tests/fixtures/`.
- A test is valuable when it covers a branch or an edge case that no existing test covers; a test that repeats an existing one is not.
