# Review criteria for CI

Be conservative and only report high-confidence findings.

## Report
- Bugs: code that crashes or returns a wrong result on a reachable input, for example an unchecked `None` or an off-by-one in a loop bound.
- Security: user input that reaches a query, a shell command or a file path without validation.
- A comment or docstring whose claimed behaviour contradicts what the code does.

## Skip
- Minor style: naming, line length, import order.
- Patterns that the codebase already uses in other files.

## Severity
- high: data loss or a security hole, for example `DELETE FROM orders` built from request input.

## Testing standards

My notes are in /home/dev/review-notes.
