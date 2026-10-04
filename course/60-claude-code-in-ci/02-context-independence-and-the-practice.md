# What the run knows: context, independence and the practice

**Level:** Architect · **Module 60:** Claude Code in CI · **Page 2 of 2**
**Exams:** A3.6; S5

**After this page you can** give a CI run the project context it needs, keep the reviewer independent of the session that wrote the code, put earlier findings and existing tests in the prompt so that a re-run reports only what is new, write review criteria that a run can follow, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation pages "Run Claude Code programmatically", "Best practices for Claude Code" (fresh context for review) and "Claude Code GitHub Actions", documenting behaviour up to Claude Code v2.1.286. The practice is a workflow, a schema, a criteria file and a decision function, graded by Python and TypeScript test suites, offline, on the course's model of the documented command-line behaviour (`examples/60-ci-gate`); nothing in it starts Claude Code. This page deepens module 38 and module 40 and does not repeat them. The command and the gate are the first page.

> **Exam guide and current product.** *What the guide states (task statement 3.6), and so what the exam keys:* the CI task (3.6) asks for CLAUDE.md to give the run testing standards, fixture conventions and review criteria; session context isolation, because "the same Claude session that generated code is less effective at reviewing its own changes compared to an independent review instance"; prior review findings included in the prompt when a re-run follows new commits, "instructing Claude to report only new or still-unaddressed issues"; and existing test files in the prompt of a test-generation run, so that suggestions do not duplicate covered scenarios. *What the current product does (documentation checked 2026-10-03):* the same ideas, with one wrinkle for CLAUDE.md: it is read by an ordinary run, and "`--bare` ... skip[s] auto-discovery of hooks, skills, ... auto memory, and CLAUDE.md", so a bare CI run gets its criteria through `--append-system-prompt-file`. The documentation's advice on review is the guide's: "A fresh context improves code review since Claude won't be biased toward code it just wrote." On the exam, answer with CLAUDE.md for criteria, an independent instance for review, and findings in the prompt for re-runs; in a bare job, pass the file.

## Why it matters

A pull request is reviewed on every push. The first push gets six comments; the second push, after the author fixed four of them, gets the same six and two new ones, and the author stops reading. A test-generation step proposes a test for a branch that an existing test already covers, three times in a week. Both failures have one cause: each run starts from nothing, and the prompt did not say what had already happened. Scenario S5 asks how to make the step report only what is new, and the answer is in the prompt, not in the model.

## The idea

### The criteria live in a file

"Be conservative" and "report only important issues" do not change what a reviewer reports, because they leave the threshold unstated. The criteria file says what to report, what to skip, and what each severity means, with a concrete example at each level; module 61 shows why the examples matter. The same file holds the testing standards a generation run needs: where tests live, which fixtures they use, and what makes a test worth adding ("a branch or an edge case that no existing test covers"). In an ordinary run Claude reads `CLAUDE.md` itself, and the documentation's advice for CI is to keep it concise because it is read on every run. In a bare run the file is passed explicitly with `--append-system-prompt-file`; a bare run without it reviews with no criteria at all, and nothing says so.

### An independent reviewer

A session that wrote a change carries its reasoning, and it is "less effective at reviewing its own changes" for that reason. In CI the arrangement is easy: the review job is a separate run that sees the diff and the criteria, and nothing the author's session decided. The documented patterns for interactive work are a second session as reviewer, or a reviewer subagent in a fresh context that "sees only the diff and the criteria you give it, not the reasoning that produced the change". Two cautions from the documentation: a reviewer asked to find gaps will usually report some, so tell it to flag only what affects correctness or the stated requirements; and the independent run is a separate invocation, not a second pass in the same conversation.

### A re-run needs memory it does not have

A CI run does not remember the last one. For the review that follows new commits, put what was already reported in the prompt, say not to repeat it, and say to report only issues that are new or still unaddressed. For test generation, put the existing tests in the prompt and say not to suggest a covered scenario. The practice's prompt builder puts the instructions and the two lists first and the diff last, in tagged sections: that order is the course's design, which keeps the instructions and the lists together. The prompting guidance for very long inputs is the reverse, with the long material near the top and the question after it, so for a large diff end the prompt with a one-line restatement of the task.

A record of findings between runs, which the prompt carries, is the only state the pipeline has. Where it lives (a comment on the pull request, a file, a store) is a design choice; the exam only asks that it reaches the prompt.

### Posting the result

Because the answer is structured, each finding becomes an inline comment with its file, line, severity and suggested fix; a `detected_pattern` field per finding lets the team analyse which patterns draw dismissals and tune the criteria. The Claude Code GitHub Action does the plumbing for the common case, taking `claude_args` such as `--max-turns` and `--allowedTools`, and the documentation lists the same controls for cost: a concise CLAUDE.md, a turn limit, job timeouts and concurrency limits.

### The example

<!-- example: m60-ci-gate tabs: python,typescript -->
```python
"""A Claude Code review step in CI, from the command line to the exit status: build the headless command, lint a command someone wrote, and gate on the JSON the run prints."""
```
<!-- /example -->

### The practice: a review job that fails closed

The practice is in [`exercises/60-claude-code-in-ci`](../../exercises/60-claude-code-in-ci/unit-01/practice-1/statement.md). You write the workflow (headless, bare, JSON with a schema, a turn limit, read-only tools, the criteria file, a timeout and a secret), the schema of a finding, the criteria file, the prompt builder that carries earlier findings and existing tests, and the gate that turns the run's output into a job status and comments. It is graded by test suites in Python and TypeScript, offline. The configuration files have no Java or Kotlin edition, because no YAML reader is available offline for those two here and a second edition would test the same files. The statement lists nine cases, and each says what you should see when it works.

## Traps

1. **"Have the session that wrote the change also review it; it knows the code best."** It is tempting because the session has the context. The exam rejects it: that is the reason it reviews badly, since it carries the reasoning that produced the change. An independent instance sees the diff and the criteria.
2. **"Run the review again on each push with the same prompt."** It is tempting because each run is correct on its own. The exam rejects it: without the earlier findings in the prompt, every push repeats them. Include them and ask for new or unaddressed issues only.
3. **"Tell the reviewer to be conservative and report only high-confidence issues."** It is tempting because it sounds like a precision filter. The exam rejects it: the threshold is still unstated. Explicit categories, what to skip and a severity example at each level are what change the output.
4. **"The bare run will read the repository's CLAUDE.md like any other."** It is tempting because the file is in the checkout. The documentation rejects it: bare mode skips CLAUDE.md, so the criteria go in with `--append-system-prompt-file`.

## Quiz

1. A review job runs on every push and keeps posting remarks that the author has already fixed. What fixes it?
   - **a**: Raise the turn limit so that the run can compare with earlier comments
   - **b**: Put earlier reports in the prompt and ask for new issues only
   - **c**: Tell the reviewer to be more conservative about what it reports
   - **d**: Run the review inside the session that wrote the change

2. A job that drafts additional checks keeps proposing situations that are already exercised. What belongs in its prompt?
   - **a**: A request for many more checks so that coverage becomes certain
   - **b**: The production code alone, to keep the whole prompt short
   - **c**: A lower turn limit, so that fewer proposals come back at all
   - **d**: The test files that exist, plus a request to skip covered cases

<details>
<summary>Answer key</summary>

1. **b**. A run has no memory of the last one, so what was reported must be in the prompt. *a* is ruled out because "A CI run does not remember the last one", and a longer loop does not change that. *c* is ruled out because such wording does not change what a reviewer reports, since vague instructions "leave the threshold unstated". *d* is ruled out because the writing session is "less effective at reviewing its own changes" and still would not know what was said on earlier pushes.
2. **d**. The remedy is to show the run what the suite already covers. *a* is ruled out because more proposals add duplicates, while the aim is "so that suggestions do not duplicate covered scenarios". *b* is ruled out because without the tests the run cannot see what is covered: "put the existing tests in the prompt". *c* is ruled out because a record of earlier work carried by the prompt "is the only state the pipeline has", and a turn limit adds none.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. A team asks the same session that wrote a change to review it afterwards, and the verdicts are nearly always approvals. Which change fits?
   - **a**: Ask the same session to think harder before it approves
   - **b**: Give the session the full history of the change to review against
   - **c**: Start a separate run that receives only the diff and the criteria
   - **d**: Lower the number of turns so that the session decides faster

2. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. A job runs `claude --bare -p`, and its reviews ignore the testing and severity rules that sit in the repository's `CLAUDE.md`. What is the cause and the fix?
   - **a**: The file is too long, so cut it until it fits the prompt
   - **b**: Headless runs read the file only on the first push of a branch
   - **c**: The rules need a `paths` header before a headless run applies them
   - **d**: That mode skips it, so supply it with `--append-system-prompt-file`

3. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests and to suggest tests. The policy keeps findings at medium severity or above, disables the style category, and fails the job on high findings. A valid run returns one high style finding and one low bug finding. What does the job do?
   - **a**: It passes and posts no comment
   - **b**: It fails, because a high finding exists
   - **c**: It passes and posts the low bug as a comment
   - **d**: It fails, because a finding in a disabled category counts as an error

<details>
<summary>Answer key</summary>

1. **c**. An independent run sees the change without the reasoning that produced it. *a* is ruled out because the writing session is "less effective at reviewing its own changes", and more effort does not remove the bias. *b* is ruled out because a reviewer "sees only the diff and the criteria you give it, not the reasoning that produced the change". *d* is ruled out because "the independent run is a separate invocation, not a second pass in the same conversation".
2. **d**. Bare mode skips CLAUDE.md, so the criteria must be passed in by hand. *a* is ruled out because length is not the issue: "In a bare run the file is passed explicitly" and otherwise it is not read at all. *b* is ruled out because "A CI run does not remember the last one", and no first-push behaviour exists. *c* is ruled out because no header is involved, and "a bare run without it reviews with no criteria at all, and nothing says so".
3. **a**. The high finding is dropped with its category and the low one falls below the floor, so nothing is kept. *b* is ruled out because "categories the team disabled are dropped", so the high style finding cannot fail the job. *c* is ruled out because "findings below a floor are dropped", and a low finding is below a medium floor. *d* is ruled out because only "a kept finding at a failing severity" fails the job, and a disabled category is dropped before that.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
