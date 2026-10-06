# Hand-back: cloud-r5-tryit-read

240 try-it files judged in 62 practices; 16 practices fixed (56 files). Rule 3 is read as 3 to 5 result
prints: a single print of one multi-line value (a prompt, a hand-off, a scratchpad) counts once; a print
inside a loop counts once per pass.

| Level | Practices | Files judged | Passed | Practices fixed |
|---|---|---|---|---|
| 1 | 1 | 4 | 1 | 0 |
| 2 | 24 | 94 | 16 | 8 |
| 3 | 26 | 98 | 22 | 4 |
| 4 | 11 | 44 | 7 | 4 |

## Fixes (all four languages unless noted; Python and TypeScript run against reference)

- 20 prompt caching, rule 3: 2 lines; adds a `breakpoints at:` line.
- 22 cloud platforms, rule 3: 7 lines; each platform's url, model and headers on one line (4).
- 26 tool use, rules 3 and 5 (Python, TypeScript): one `messages:` line, as in Java and Kotlin.
- 30 vision and documents, rule 3: one `blocks:` line instead of one per block (3).
- 32 MCP fundamentals, rule 5 (TypeScript only): reads `notes://count` like the other three languages.
- 41 security and safety, rules 3 and 5 (Python only): drops the `audit records:` line the others lack (5).
- 42 evaluation, rule 3: `passed` and `pass rate` share one line (5).
- 43 debugging, rule 3: 2 lines; also diagnoses HTTP 504 `timeout_error` (3).
- 52 tool interfaces, rule 3: 2 lines; adds the count of rules the poor tool breaks.
- 68 calibrated confidence, rule 3: one `accuracy:` line instead of one per segment (4).
- 70 support agent, rules 3 and 5: Python/TypeScript printed 10 fields, Java/Kotlin one record; now four labelled lines in all.
- 77 tool builder, rule 3: 2 lines; adds a third proposal that writes a file.
- 80 architecture, rule 3: 2 lines; adds the cheapest design that is not rejected.
- 86 integration choices, rules 1 and 3: 7 tools stayed under the deferral threshold; now 12, so deferral shows.
- 88 evaluation and optimisation, rule 3: one `segments:` line (4).
- 89 migration, rule 3: 13 lines; calendar and changes each one line (5).

JVM edits (not compiled): Java and Kotlin of 20, 22, 30, 42, 43, 52, 68, 70, 77, 80, 86, 88, 89. Each
changes printed text or loop shape, or adds a call to an existing reference function whose signature was checked.

## Left open

- 32 and 35 Python and TypeScript were not run: the installed `mcp` and `claude-agent-sdk` are older than
  the pinned versions. Rerun them in the course container.
- Java and Kotlin are not run here; run `tryIt` for the JVM files above through the heavy-job slots.
- Checks: `make_tryit.py check`, `check_logger.py`, `check_personal_data.py --modules '.*'` exit 0.
