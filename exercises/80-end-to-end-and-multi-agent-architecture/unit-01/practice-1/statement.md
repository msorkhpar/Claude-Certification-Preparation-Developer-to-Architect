# Practice: an architecture review against a rubric

A solution architect is asked to review three proposed designs for the same claims-intake service before the board meets: a plain workflow, a single agent and a
team of agents. The review has to be the same for every design, so the rubric is code: the findings it raises, the verdict that follows from them, and the pick of the
cheapest design that the rubric does not reject. The model is not called: the tests give you design descriptions. It is in Python, TypeScript, Java and Kotlin; pick
your language folder, open `starter/` and edit the file there.

Names are Python's (`review`, `verdict`, `cheapest_adequate`); TypeScript has `review`, `verdict` and `cheapestAdequate`; Java has the same camel-case names as static
methods of `ArchitectureReview` and Kotlin has top-level functions. A design is a map (JSON-like) and so is a finding, as the starters show.

## The design

A design has `name`, `pattern` (`workflow`, `augmented`, `agent` or `multi-agent`), `agents` (a count, default 1), `cost` (a whole number, the relative price of one
task), `path_known`, `parallel_independent`, `shared_context`, `needs_audit` and `writes_without_approval` (booleans; a missing one is false), and `stages`: a map with the
lists `input`, `processing`, `output` and `feedback`. An empty list or a missing key means the stage is absent.

## What to write

- `review(design)` returns a list of findings `{"rule", "severity"}`. The rubric:
  - `missing-stage:input`, `missing-stage:processing` and `missing-stage:output` (one finding per absent stage), severity `high`.
  - `no-feedback` when the `feedback` stage is absent, severity `high`.
  - `team-without-independence` when there is more than one agent and either the context is shared or the parts are not independent, severity `high`.
  - `unapproved-write` when the design writes without approval and an audit is needed, severity `high`.
  - `autonomy-without-need` when the pattern is `agent` or `multi-agent` and the path is known, severity `medium`.
  - `unvalidated-output` when the `output` stage is present and does not contain `validate`, severity `medium`.
  - The findings are ordered by severity (`high` first) and then by rule name.
- `verdict(findings)` is `reject` when any finding is `high`, otherwise `revise` when any is `medium`, otherwise `approve`.
- `cheapest_adequate(designs)` is the name of the design with the lowest `cost` among those whose verdict is not `reject`; equal costs are broken by the lower name. It is
  `None` (TypeScript: `null`; Java and Kotlin: `null`) when every design is rejected or the list is empty.

## Why each part is there, and what you should see

1. **The loop is part of the design.** A design that takes input, processes and answers, but never learns from its output, is a pipeline and not a system. *You should see*
   `no-feedback`, and a finding for each absent stage, both `high`.
2. **Autonomy has to be bought.** An agent or a team on a path that is already known costs several times a workflow's tokens for nothing. *You should see* a `medium`
   finding for a known path and none for an open one.
3. **A team needs parts that do not depend on each other.** *You should see* `team-without-independence` for a shared context and for dependent parts, and none for one agent.
4. **An unapproved write is judged by what the audit needs.** *You should see* `high` when an audit is needed and nothing when it is not.
5. **Output is checked before it leaves.** *You should see* `unvalidated-output` for an output stage without a validation step, and none when the stage is absent (the
   missing stage is already reported).
6. **The verdict follows the worst finding.** *You should see* `reject`, `revise` and `approve`, and the exact order of the findings.
7. **Price decides only among designs that pass.** *You should see* the cheapest design that is not rejected, ties broken by name, and `None` when none passes.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A sound design passes review with no findings and is approved |
| `e1` | A missing feedback loop or a missing stage is a `high` finding |
| `e2` | Autonomy is flagged only when the path is known |
| `e3` | A team needs independent parts and no shared context; one agent is exempt |
| `e4` | An unapproved write is a finding only when an audit is needed |
| `e5` | Output without a validation step is flagged; an absent output stage is not flagged twice |
| `e6` | The findings are ordered by severity and then rule, and the verdict follows the worst |
| `e7` | The cheapest design that is not rejected wins, ties go by name, and none passing gives no name |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
