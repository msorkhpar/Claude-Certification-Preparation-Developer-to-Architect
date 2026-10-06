# Review criteria

Review only the lines that this pull request changes, and the code those lines call.

## Report

- A bug: a value that can be missing is used without a check, a loop that never ends, a condition that can never be true.
- A security problem: input that reaches a query or a command without being escaped, a secret in the source.
- A performance problem that grows with the input: a query inside a loop.

## Skip

- Formatting and naming that the linter already checks.
- A pattern that the project uses in other files, even when you would write it differently.
- Code that this pull request does not change.

## Severity

- high: the change can lose or expose data, or stop a request from finishing. Example: a query built by joining the user's text into the SQL string.
- medium: the change gives a wrong result in a case that can happen. Example: the last page of a list is dropped when the count divides evenly.
- low: the change works but is harder to maintain than it needs to be. Example: the same lookup is repeated three times in one function.
