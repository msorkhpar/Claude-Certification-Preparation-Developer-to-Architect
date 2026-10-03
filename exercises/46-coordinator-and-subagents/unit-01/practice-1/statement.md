# Practice: a coordinator that delegates to isolated subagents

A research system has one coordinator and several subagents. The coordinator splits the question, hands each subagent a brief, reads
what comes back, asks for more where there are gaps, and writes one answer. A subagent knows only its brief: it does not see the
conversation, the plan or the other subagents' findings. Most of the failures of such a system are in the coordinator's code, not in
the model: duplicated work, a vague brief, one failure that stops the team, a review that sends everyone out again, and a team built for a
question that one agent could answer. Write the coordinator. Pick your language folder (`python`, `typescript`, `java` or `kotlin`),
open `starter/` and edit the file there. Nothing here touches the network: the four model roles are functions that the tests script.

## The given parts

| Name | Meaning |
|---|---|
| `planner(question)` | returns the plan: `{"delegate": bool, "answer": text or null, "subtasks": [{"scope": text, "brief": text}]}` |
| `subagent(brief)` | runs one subagent and returns its final report as text; it may throw |
| `reviewer(question, findings)` | returns the list of gap descriptions (texts) still to be researched; an empty list means nothing is missing |
| `synthesizer(question, findings)` | returns the final answer as text |
| a finding | `{"scope": text, "text": the subagent's report}` |

## What to write

`coordinate(planner, subagent, reviewer, synthesizer, question, max_agents=4, max_rounds=2)` (`coordinate` in every language, with the same
arguments) returns `{"status", "answer", "findings", "failed", "dropped", "gaps", "rounds", "subagent_calls"}`.

1. Call `planner` once. When `delegate` is false, return status `direct` with the planner's `answer`, no findings, and no subagent, reviewer or
   synthesizer call at all: a question that the coordinator can answer itself is not worth a team.
2. Clean the subtasks, in order. A subtask whose brief is empty or only spaces is dropped with the reason `empty brief`. A subtask whose
   scope (compared stripped and in lower case) was already kept is dropped with `duplicate scope`. Once `max_agents` subtasks are kept,
   every further one is dropped with `over limit`. A dropped entry is `{"scope": the scope as given, "reason"}`.
3. Call `subagent` once per kept subtask, in order, with **exactly its brief**: nothing from the plan, from the question or from another
   subagent's report is added. A report that is a non-blank string becomes a finding `{"scope", "text"}`. A call that throws, or a report
   that is blank, becomes a `failed` entry `{"scope", "error"}`: the exception's message, or `empty report`. The others still run.
   `subagent_calls` counts every call, successful or not.
4. When no subtask produced a finding (all failed, or none was kept), return status `failed`, answer null, and call neither the reviewer nor
   the synthesizer.
5. Review. Call `reviewer` with copies of the findings. Clean its answer: strip each gap, drop empty ones and repeats (the first stays) and
   keep at most `max_agents`. While gaps remain and fewer than `max_rounds` rounds were run: count a round, call `subagent` once per gap with
   the brief `Follow up: {gap}\nQuestion: {question}` (the scope of the new finding is the gap text), and call `reviewer` again. Subtasks that
   already have findings are not run again.
6. Call `synthesizer` once with copies of all findings, in the order they arrived. Status is `complete` when no gaps remain and nothing
   failed, otherwise `partial`. `gaps` is what the last review still reported (empty when complete), `rounds` the number of refinement rounds.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A hub sends one brief to each spoke, in order, and synthesizes what comes back |
| `e1` | A question the planner answers itself starts no team |
| `e2` | A subagent receives exactly its own brief |
| `e3` | Empty briefs and duplicate scopes are dropped, the team is capped, a plan with nothing usable fails without calls |
| `e4` | One failing subagent does not stop the others; a run with no findings neither reviews nor synthesizes |
| `e5` | A review sends only the gaps out again, cleaned, and stops when none are left |
| `e6` | The rounds are capped, the gaps that remain are reported and the status is `partial` |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
