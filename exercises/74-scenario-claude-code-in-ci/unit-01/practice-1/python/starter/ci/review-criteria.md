# Review criteria

<!-- TODO 4 of 6 (unlocks e5 and e7): finish the criteria and remove the personal path. Add a `## Severity` section with one line for each of high, medium and low,
each with a description and an `Example:`. Example of a line: a dash, `high:`, what makes it high, `Example:`, a concrete case. Delete this comment. -->

Review only the lines that this pull request changes, and the code those lines call. Draft kept in /home/dev/notes.

## Report

- A bug: a value that can be missing is used without a check, a loop that never ends, a condition that can never be true.
- A security problem: input that reaches a query or a command without being escaped, a secret in the source.
- A performance problem that grows with the input: a query inside a loop.

## Skip

- Formatting and naming that the linter already checks.
- A pattern that the project uses in other files, even when you would write it differently.
- Code that this pull request does not change.
