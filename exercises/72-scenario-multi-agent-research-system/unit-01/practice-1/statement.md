# Practice: the coordinator's last step in a research run

A coordinator hands subtasks to a search subagent, a document-analysis subagent and a synthesis subagent. The subagents have finished, some of them with a failure, and the coordinator has
a list of results. Before the report is written, the coordinator must decide what it knows: which of the question's scopes were really covered, which claims several sources agree on,
which claims the sources disagree about, what failed and what could be tried instead, and what the report cannot say. In this practice you write that step. The model is not called: the tests
give you the results of the subagents. The shapes of a result and of the report are this course's own design for the capstone, not an Anthropic interface. It is in Python, TypeScript, Java and
Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`synthesize`); TypeScript has the same name and the same fields. Java has the static method `Synthesis.synthesize` and the records `Finding`, `Failure`, `Result`, `Claim`,
`Conflict` and `Report`; Kotlin has the top-level function `synthesize` and the data classes of the same names.

## What to write

A finding is `{claim, value, source, date}`. A result of a subagent is `{scope, status, findings, error}`: `status` is `ok` (with `findings`) or `error` (with `error`, an object
`{type, query, partial, alternatives}`, where `partial` is a list of findings that came back before the failure and `alternatives` lists queries to try instead).

`synthesize(required, results)` takes the scopes the question needs, in order, and returns a report with:

- `covered`: the required scopes (in the order of `required`) that have an `ok` result with at least one finding; `gaps`: the other required scopes; `status`: `complete` when there are no gaps and
  `partial` otherwise.
- `claims`: a list, sorted by the text of the claim, with `{claim, value, sources, partial}` for every claim on which all findings agree. `sources` is a list of `{source, date}` in the order
  the findings arrived, without duplicates. Findings come from `ok` results and from the `partial` lists of errors. `partial` is true only when every finding of the claim came from a `partial` list.
- `conflicts`: a list, sorted by claim, with `{claim, values}` for every claim that has two or more different values; `values` lists `{value, source, date}` for each distinct value and source, in
  arrival order. A conflicting claim is not in `claims`: the report does not choose.
- `errors`: for each result with status `error` whose scope is not covered, `{scope, type, query, alternatives}`. An error that a later `ok` result for the same scope made up for is not listed.
- `partial`: the gaps for which an error returned partial findings.
- `note`: `all scopes covered` when there are no gaps; otherwise `not covered: ` and the gaps in order, separated by `, `, each as `<scope> (<type> on '<query>')` when an unresolved error is recorded for it,
  `<scope> (not researched)` when no result at all names the scope (the plan never covered it), and `<scope> (no findings)` otherwise.

## Why each part is there, and what you should see

1. **Coverage is checked against the question, not against the plan.** The exam's example is a report on the creative industries that covers only visual arts: every subagent succeeded and the
   coordinator's decomposition was too narrow. *You should see* a scope nobody researched reported as a gap and as `not researched`, which differs from a scope whose search failed.
2. **A failure travels with its context.** A bare "search failed" tells the coordinator nothing. *You should see* the type, the query and the alternatives kept, and a note that names them.
3. **A partial result is still information, and it is not coverage.** *You should see* partial findings in the claims, flagged, while their scope stays a gap.
4. **A conflict is information.** Two sources with two values are both shown, with their dates, and the report does not average or pick. *You should see* a conflict that names both sources and no
   claim for it.
5. **A status that cannot lie.** `complete` means that every scope was covered. *You should see* `partial` whenever a scope is missing, whatever the reason.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A full run with agreement: claims with every source, a complete status and the all-clear note |
| `e1` | No results: every scope is a gap, `not researched` |
| `e2` | A scope the plan never covered and a scope whose search failed read differently in the note |
| `e3` | Two values for one claim are a conflict naming both sources and no claim |
| `e4` | Partial results kept and flagged, not covering their scope, and an error that a retry made up for disappears |
| `e5` | An `ok` result with no findings does not cover its scope |
| `e6` | Claims sorted, sources in arrival order without duplicates, scopes in the order of `required` |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
