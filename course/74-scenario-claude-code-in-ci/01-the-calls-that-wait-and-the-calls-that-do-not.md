# The calls that wait and the calls that do not

**Level:** Architect · **Module 74:** Scenario: Claude Code in CI · **Page 1 of 2**
**Exams:** A3.6; S5

**After this page you can** decide for each job of a pipeline whether it runs in real time or as a batch by asking who waits for it, make a Claude Code run end by itself in a pipeline, bound it with a turn limit and a list of read-only tools, and audit a pipeline description for the mistakes the exam's scenario is built on.

Checked on 2026-10-04 against the Claude Code documentation pages "Run Claude Code programmatically" and "Best practices for Claude Code" (the pages name Claude Code version 2.1.286), the Claude API documentation page on batch processing, and the Architect exam guide (version 1.0, scenario 5 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small JSON files; it does not call Claude. This page is a capstone: it uses modules 21, 60 and 61 and puts them in the order the exam asks about.

## Why it matters

Scenario S5 of the Architect exam is Claude Code inside a CI/CD pipeline: automated code review, generated test cases and feedback on pull requests, with prompts that give actionable feedback and few false positives. Its primary domains are Claude Code configuration and workflows, and prompt engineering with structured output. The questions begin with the plumbing and then move to the design. A step hangs because it waits for a person who is not there. A manager wants the discount of batch processing for everything. A review of many files contradicts itself. Each is answered by a rule about where a call sits in the pipeline, and the wrong options are rules that fit another place.

## The idea

### The scenario in plain words

A team runs three Claude jobs. A review of each pull request that has to finish before the merge button works. A report on technical debt that is generated overnight and read the next morning. And the generation of tests for the files a pull request changes. The team wants the bill to fall, the review to be useful instead of noisy, and the pipeline never to hang or to turn green by accident.

### The requirement decides the design

| The requirement | The design | Why the tempting choice fails |
|---|---|---|
| The step must run with nobody at the keyboard | `claude -p "..."` | There is no headless environment variable and no `--batch` flag on the command line; `-p` is the documented way to run without a person. Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs |
| Lower the cost of the overnight report | The Message Batches API, from a script | Nothing is wrong with it: nobody waits for the report |
| Lower the cost of the check that blocks the merge | Keep it in real time | A batch has no latency guarantee, and a developer is waiting |
| The run must end whatever the model does | A turn limit and a list of tools in the command | A prompt that asks the run to be quick is a request, not a limit |
| The job must fail when no valid answer came back | A gate on the exit status and the JSON (module 60) | A green job that said nothing is the failure the gate prevents |

### Run without a person

In a pipeline nobody can answer a prompt, so a run that would ask a question stops there for ever. The way out is `-p` (or `--print`): it processes the prompt, prints the result and exits. There is no headless environment variable and no `--batch` flag on the command line; `-p` is the documented way to run without a person. Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs. With `-p` the useful companions are `--output-format json`, which prints one object that a script can read, and `--json-schema`, which makes the answer follow a schema (module 60 gives the full command).

A run with no person must end by itself: `--max-turns` stops a loop, and `--allowedTools` lists what runs without asking, because nobody can approve a prompt. A prompt that asks the run to be quick is a request, not a limit. A longer job timeout only lets a loop run longer. When a run fails, re-running it repeats the cost with no reason to expect a different result, so the job reports the failure and a person decides.

### Who waits decides the API

The Message Batches API costs half as much and may take up to 24 hours, with no guaranteed latency. That is the whole trade, and it settles which jobs may use it. A check that blocks a merge has a developer waiting, so it cannot depend on a job that may take a day. A technical debt report that is read the next morning has nobody waiting, so it takes the discount. Results of a batch come back matched by `custom_id`, so their order is no reason to avoid it. Polling a batch does not make it finish sooner. A fallback to real time when a batch is slow adds a second path and a second bill, and the simple answer is to match each job to the API that fits it. One more detail of the example: a batch job's command is a script that submits the work, so the `-p` rule applies to commands that start with `claude` and not to a script.

### The audit: a checklist that reads the pipeline

The example describes a pipeline in a small JSON file of jobs, with the fields that the checklist needs: who the job's `audience` is (`waiting` or `scheduled`), its `api` (`realtime` or `batch`), its `passes`, its `session`, what it is given as `context`, its `tools` and its `command`. That shape is this course's own, not a product file. The checklist reports, for each job in order:

- `no-print-flag`: a command that starts `claude` without `-p` or `--print`.
- `blocking-batch`: a job someone waits for that runs as a batch. `batchable`: a scheduled job that runs in real time and pays full price.
- `single-pass-review`, `shared-session`, `no-prior-findings` and `writes`: the design of a review, which page 2 explains. A `no-existing-tests` finding is the same idea for the test job.

For a pipeline with no findings the example prints each job with its API and its passes.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the `-p` (or `--print`) flag is the documented way to run non-interactively, the answers that name a headless environment variable or a `--batch` flag describe things that do not exist, and the Message Batches API saves 50% at the cost of a processing time of up to 24 hours with no latency guarantee, which suits overnight jobs and not blocking checks. *What the product does now (documentation checked 2026-10-04):* the same. `claude -p` runs a prompt and exits, `--output-format json` and `--json-schema` shape the output, and a batch is completed within 24 hours with results matched by `custom_id`. So on the exam, choose by who waits; in a real pipeline, also choose by how much the job costs and how soon its result is read, because a fast batch is a bonus and never a promise. Module 63, "Batch and multi-pass review", goes further.

### The example

The example loads each pipeline and runs the checklist. It prints one line of facts per pipeline, then the findings for the draft, and for the fixed pipeline each job with its API and passes. The program and its output are the same in all four languages.

<!-- example: m74-pipeline-check tabs: python -->
<!-- /example -->

The draft puts the merge check in a batch, reads all files in one pass in the session that wrote the code, gives it a shell tool, leaves the overnight report in real time, and starts the test job without `-p`. The fixed pipeline has none of those findings.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Set an environment variable that turns on headless mode."** It is tempting because it sounds like how tools are switched. The exam rejects it: no such variable exists.
2. **"Redirect the input from an empty file so that the run cannot wait."** It is tempting because it stops the hang on some programs. The exam rejects it: it is a workaround and does not change how Claude Code runs.
3. **"Move both jobs to batch for the discount, and poll until the check is ready."** It is tempting because the saving is real. The exam rejects it for the merge check: polling does not make it finish sooner.
4. **"Keep both in real time, because batch results come back in any order."** It is tempting because order sounds like a risk. The exam rejects it: `custom_id` matches the results.

## Quiz

1. A pipeline step runs `claude "Analyze this pull request"` and hangs. What fixes it?
   - **a**: Point its input at an empty file so it cannot wait
   - **b**: Export an environment variable that selects headless mode
   - **c**: Add a batch flag so the call is queued
   - **d**: Add the print flag so that the run answers and exits

2. A pipeline has a gate that stops merges until it passes and a dependency digest that people read on Monday. Which calls move to batch?
   - **a**: Just the one nobody waits on, while the other stays in real time
   - **b**: Both of them, with polling to find out when the gate is ready
   - **c**: Neither of them, since the results of a batch arrive in any order
   - **d**: Both of them, with a fallback to real time when a batch is slow

3. What makes a run that nobody watches end by itself?
   - **a**: A longer timeout on the job that contains it
   - **b**: A prompt that asks it to finish as quickly as it can
   - **c**: A turn limit and a list of permitted tools in the command
   - **d**: Running it again whenever it fails to produce an answer

<details>
<summary>Answer key</summary>

1. **d**. `-p` is the documented way to run without a person. *b* is ruled out because no such variable exists: "There is no headless environment variable and no `--batch` flag on the command line". *c* is ruled out for the same reason: "There is no headless environment variable and no `--batch` flag on the command line". *a* is ruled out because it is a workaround: "Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs."
2. **a**. Nobody waits for the digest and a developer waits for the gate. *b* is ruled out because waiting is the problem: "Polling a batch does not make it finish sooner." *c* is ruled out because matching exists: "Results of a batch come back matched by `custom_id`, so their order is no reason to avoid it." *d* is ruled out because it adds a path: "A fallback to real time when a batch is slow adds a second path and a second bill".
3. **c**. A limit and a tool list are set in the command. *b* is ruled out because a sentence is no limit: "A prompt that asks the run to be quick is a request, not a limit." *a* is ruled out because time does not stop a loop: "A longer job timeout only lets a loop run longer." *d* is ruled out because repeating the run changes nothing: "re-running it repeats the cost with no reason to expect a different result".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on a pipeline step that hangs, and the second its sample question on moving workflows to batch processing, both rewritten here.
