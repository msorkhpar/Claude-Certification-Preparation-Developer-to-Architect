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

## Why each step is there, and what you should see

1. **Delegate only when it pays** (step 1). The exam expects a coordinator that chooses its subagents from the complexity of the query and does not send everything through the full pipeline. *You should see* a question the planner answers itself finish with no subagent call.
2. **Partition the scope** (step 2). The exam asks for distinct subtopics or sources to minimise duplicated work, and a cap on the team. *You should see* the empty and the repeated subtasks listed as dropped with their reasons, and the team no larger than `max_agents`.
3. **Send each subagent its own brief and nothing else** (step 3). Subagents work in isolated context and do not inherit what the coordinator knows. *You should see* the briefs received equal the briefs planned, whatever the other subagents found, and one failing subagent leave the others' findings in place.
4. **Do not synthesize from nothing** (step 4). An error message is not evidence. *You should see* status `failed` with no reviewer or synthesizer call when no subagent returned a finding.
5. **Refine where the gaps are** (step 5). The exam describes a loop in which the coordinator finds gaps, re-delegates targeted queries and stops when coverage is enough. *You should see* only the gaps sent out again, cleaned and capped, and the first-round subtasks left alone.
6. **End with an honest status** (step 6). *You should see* `complete` only when nothing failed and no gap is left, and `partial` with the remaining gaps when the round limit stopped the loop.

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
