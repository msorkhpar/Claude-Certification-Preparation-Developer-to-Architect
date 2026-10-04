# A headless run, its JSON and the job status

**Level:** Architect · **Module 60:** Claude Code in CI · **Page 1 of 2**
**Exams:** A3.6; S5

**After this page you can** run Claude Code without a person with `-p`, ask for one machine-readable object with `--output-format json` and `--json-schema`, read the answer from `structured_output`, give the run a turn limit and read-only tools, and write the gate that fails the job whenever the run did not produce a valid answer.

Checked on 2026-10-03 against the Claude Code documentation pages "Run Claude Code programmatically", "CLI reference", "Claude Code GitHub Actions" and the Agent SDK pages on structured outputs, documenting behaviour up to Claude Code v2.1.286. Nothing here ran the real binary, needed a key or touched the network: the example builds the command and decides the job from sample output shaped like the documented result message. This page deepens module 38 (headless runs and the flags that bound them) and module 40 (Claude in the software life cycle) and does not repeat them. What the run knows, and the criteria, are the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.6), and so what the exam keys:* for CI/CD integration (3.6) it names the `-p` (or `--print`) flag for non-interactive mode, `--output-format json` together with `--json-schema` to produce structured findings that can be posted as inline comments, and CLAUDE.md as the way to give the CI run project context. *What the current product does (documentation checked 2026-10-03):* all of it. `-p` runs a prompt non-interactively; with `--output-format json` the output is one object, and with `--json-schema` it "includes metadata about the request (session ID, usage, etc.) with the structured output in the `structured_output` field". An invalid schema is an error: "`claude` exits with `Error: --json-schema is not a valid JSON Schema`". The `format` keyword is accepted but only as an annotation. `--max-turns` "Exits with an error when the limit is reached". `--bare` is recommended for scripted calls, and a bare run skips CLAUDE.md. On the exam, the guide's three items are the answer; for a real pipeline add the failure handling on this page.

## Why it matters

A team adds a review step to its pull requests. The first version runs `claude -p "review this"` and writes the prose to a file; it exits with status 0 whatever happened. One night the run hits its turn limit and prints an error, the job stays green, and a pull request with an unchecked `None` is merged under a green check. Scenario S5 asks which design makes the step trustworthy, and the exam's answer has two halves: make the output machine-readable so it can be checked, and make every kind of failure visible in the job status.

## The idea

### The command

A CI review step has seven parts, and each one answers a question:

| Part | Flag | The question it answers |
|---|---|---|
| Non-interactive | `-p` | Who answers prompts? Nobody: the run starts, works and exits |
| One object out | `--output-format json` | How does a script read the result? As one JSON object |
| A checked answer | `--json-schema '<schema>'` | What shape must the answer have? The schema's, validated before the run ends |
| A bounded run | `--max-turns 8` | What stops a loop? The run exits with an error at the limit |
| Least privilege | `--allowedTools "Read,Grep,Glob"` | What may it do without asking? Read and search only |
| A reproducible start | `--bare` | Which machine's settings apply? None: no hooks, skills, MCP servers, memory or CLAUDE.md |
| The context | `--append-system-prompt-file CLAUDE.md` | Where do the criteria come from? The file, passed on purpose |

A bare run needs `ANTHROPIC_API_KEY` in the environment, because it does not use a subscription login; in the workflow that is a secret reference, never a literal. The diff goes in on standard input or in the prompt. In a headless run nobody can approve a prompt, so the tool list must be complete and minimal: read tools for a review, and no bare `Bash`, which would let the run execute anything.

### What the run prints

With `--json-schema`, the object has the answer in `structured_output` beside the run's metadata: the result text, the session id, the cost. The documented result message also carries a `subtype`: `success`, or an error such as `error_max_turns` or `error_max_structured_output_retries` ("No valid output remained after multiple attempts"), with `is_error` set for the failures. One more case is documented on purpose: "A result can also end with subtype `success` but no `structured_output` value ... Treat that case as a failure as well."

The JSON shape of the command-line output follows the SDK's result message; the command-line pages document `result`, `structured_output`, `session_id` and the cost fields, and the SDK pages document the subtypes. The course's model uses the SDK's field names and was not compared with the binary's output.

### The gate

A job's status is a decision, and a decision made on prose is a guess. The gate reads the exit status of `claude` and the JSON, and fails the job in every case where no valid answer exists:

1. `claude` exited with a non-zero status;
2. the output is not a JSON object;
3. `is_error` is set, or the `subtype` is not `success`;
4. there is no `structured_output`;
5. the answer breaks the schema.

A job never passes by saying nothing. Retrying inside the job repeats cost with no reason to expect a different result, so the failure is reported and a person or a scheduled run decides. Only an answer that clears all five is read for findings, and then policy decides what a finding does: findings below a floor are dropped, categories the team disabled are dropped, and a kept finding at a failing severity (`high`, say) fails the job while lower ones only comment. This is the same fail-closed rule that module 48 applies to a workflow's gates.

The schema has two jobs. It makes the answer checkable, and with closed enums for category and severity it makes the policy computable: a severity that is free text cannot be compared with a floor. Keep to the features structured outputs support (types, `enum`, `required`, nested objects and arrays); make a field optional only when the value can really be absent.

### The example

<!-- example: m60-ci-gate tabs: python,typescript -->
```python
"""A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints."""
```
<!-- /example -->

## Traps

1. **"Run `claude -p` and treat exit status 0 as a passed review."** It is tempting because the step ran. The exam rejects it: a run can end in a limit, an error subtype or an empty result, so the job must read the JSON and fail unless a valid answer is present.
2. **"Ask for prose and parse the findings with a regular expression."** It is tempting because it needs no schema. The exam rejects it: `--output-format json` with `--json-schema` returns a validated object in `structured_output`, which is what inline comments are built from.
3. **"Allow `Bash` so the review can run `git diff`."** It is tempting because the review needs the diff. The exam rejects it: a bare `Bash` allows any command in an unattended run. Pass the diff in, or list a narrow pattern such as `Bash(git diff *)`.
4. **"A `success` subtype means the findings are there."** It is tempting because success sounds final. The documentation rejects it: a run can end with `success` and no `structured_output`, and that is a failure.

## Quiz

1. A CI step runs Claude Code unattended, and each issue it reports must be posted as an inline note. Which flags make the reply usable for that?
   - **a**: `--output-format text` with a prompt asking for a bulleted list
   - **b**: `--max-turns` with a high limit so that the list is complete
   - **c**: `--output-format json` with `--json-schema`
   - **d**: `--bare` alone, which formats replies for scripts

2. A review run ends with the subtype `success`, yet the object holds no `structured_output`. What should the job do?
   - **a**: Fail, because no valid answer exists
   - **b**: Pass, since the run reports success
   - **c**: Retry the same request until a value appears
   - **d**: Pass, with a note that nothing was reported

<details>
<summary>Answer key</summary>

1. **c**. The schema makes the answer a validated object in `structured_output`. *a* is ruled out because text leaves the entries to be parsed from prose, and "a decision made on prose is a guess". *b* is ruled out because a limit bounds the loop and does not shape the answer: "The run exits with an error at the limit". *d* is ruled out because the flag controls what the run loads, with "no hooks, skills, MCP servers, memory or CLAUDE.md", and formats nothing.
2. **a**. A run without a valid answer is a failure whatever its subtype says. *b* is ruled out because the documentation says "Treat that case as a failure as well". *c* is ruled out because "Retrying inside the job repeats cost with no reason to expect a different result". *d* is ruled out because "A job never passes by saying nothing", and a silent pass cannot be told from a failed run.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
