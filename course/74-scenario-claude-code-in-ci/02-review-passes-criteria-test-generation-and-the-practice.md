# Review passes, criteria, test generation and the practice

**Level:** Architect · **Module 74:** Scenario: Claude Code in CI · **Page 2 of 2**
**Exams:** A3.6; S5

**After this page you can** split the review of a large pull request into a pass for each file and an integration pass, run the review in a fresh session that is told what was found before, write criteria that name cases instead of attitudes, give a test generator what it needs to avoid repeats, and correct a draft pipeline in the module's practice: four files, graded by tests, with no program to write.

Checked on 2026-10-04 against the Claude Code documentation pages "Run Claude Code programmatically" and "Best practices for Claude Code", and against the Architect exam guide (version 1.0, scenario 5 and its sample questions). The practice is offline in Python, TypeScript, Java and Kotlin: the tests read the files you write, and nothing calls Claude. The practice's checks are this course's own, built on the documented behaviour and the exam's lessons, and the statement says so.

## Why it matters

Page 1 placed each call in the pipeline. The other half of the scenario is what the review says. A review that produces detailed notes for some files and a glance for others, that contradicts itself between files, that repeats yesterday's comments and that flags what the linter already checks is a review the team learns to ignore, and then the real findings are lost with the noise. The fixes are not a better model or a longer prompt. They are structure: how the work is divided, who does it, and what it is told.

## The idea

### One pass over many files does not hold

Attention thins as the number of files in one pass grows: some files get detailed notes and others get a glance, and the same pattern may be flagged in one file and approved in another. The remedy that fits the cause is to divide the work. A pass for each file looks for local issues with the same depth every time. A separate integration pass then looks at what crosses files: the data that flows from one module to another, a function whose callers disagree about it. A larger context window does not fix attention quality. Keeping only the issues that three runs agree on would hide real bugs that are found only now and then. Asking authors to split their pull requests moves the work to people and does not improve the review.

### Review in a fresh session, and say what was found

A reviewer should not be the author. The best-practices page puts it directly: "A fresh context improves code review since Claude won't be biased toward code it just wrote." In a pipeline that means the review runs as its own session and not as a continuation of the one that generated the change. The second thing a review needs is memory of its own earlier output. A pull request gets new commits, the review runs again, and without the earlier findings it repeats every comment that the author has already fixed or answered. Giving the run the earlier findings, and asking it to report only what is new or still open, keeps the comments useful. Both go in the pipeline description: `session` is `fresh`, and `context` includes `prior_findings`.

### Criteria are cases

A sentence that asks for more care is a request; the criteria are the case list. "Be conservative" and "only report important issues" do not say what to leave out, so they do not change what the review does. What changes it is naming the cases. The criteria file has three lists. What to report: a value that can be missing and is used unchecked, input that reaches a query without being escaped. What to skip: formatting the linter checks, a pattern the project uses elsewhere, code the pull request did not change. And what each severity means, with one example for each, because the example shows the model where the line between medium and low is, which a definition alone does not (module 61). The severity then belongs in the schema as a closed list, so a script can compare it with a floor. A finding that must say where it is, how bad it is, what is wrong and what to do about it should say so in the schema's `required` list.

### The review reads, and nothing else

A review changes nothing, so its tools are `Read`, `Grep` and `Glob`, listed in the job and in the `--allowedTools` of the command. A shell tool is a way to change things, and a pipeline that nobody watches should not hold one for a job that only reads. The test-generation job is different on purpose: it writes test files, so it has `Edit`, and it is not held to the read-only rule. The audit checks the rule only for reviews.

### Test generation: tell it what exists

A test generator that is only told to "write tests" writes the obvious ones, and some of them are copies of tests that exist. A prompt that asks for fewer tests says nothing about which tests are worth writing. Without the existing tests in the prompt, the generator has no way to avoid repeating them. So the prompt carries two things: the changed files and the existing tests, as placeholders that the pipeline fills. And it says in a few lines what a useful test is: one that checks a behaviour a caller can see, covers one case, and fails when the behaviour breaks; and what not to write, such as a test of a getter or a second test of a covered case.

### The practice: correct a draft pipeline

The practice is `exercises/74-scenario-claude-code-in-ci/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. The starter is a draft of four files: a pipeline description with the mistakes of page 1, a schema in which severity is free text, criteria that say "be conservative", and a test prompt that says "write tests". You correct them: the merge check in real time and the report as a batch; two review passes in a fresh session with the earlier findings; headless commands with JSON, a turn limit and read-only tools; a schema with a closed severity and required fields; criteria with report and skip lists and an example for each severity; and a test prompt with the changed files, the existing tests and a definition of a useful test.

The tests grade eight cases: who waits decides between real time and batch; a review has a pass for each file and then an integration pass; a review runs in a fresh session and is given the earlier findings; every command that starts `claude` is headless, JSON and bounded, and the schema has a closed severity and required fields; the review can only read; the criteria name what to report, what to skip and an example for each severity, with no vague phrase; the test prompt passes the existing tests and says what a useful test is; and no file holds a personal path, an address or a key. The starter fails all eight. The reference passes them. Each of nineteen planted wrong solutions per language fails on an assertion of the case it breaks: a review in a batch, a report in real time, a single pass, the passes in the wrong order, a shared session, no earlier findings, a missing `-p`, a missing schema, a missing turn limit, a shell tool, an approved edit tool, a free-text severity, a finding without a suggestion, no skip list, a severity without an example, a vague instruction, a test prompt without the existing tests or without a definition, and a home path.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Use a model with a larger context window for the same single pass."** It is tempting because more room sounds like more attention. The exam rejects it: a larger context window does not fix attention quality.
2. **"Run the review three times and keep what two runs agree on."** It is tempting because agreement sounds like confidence. The exam rejects it: it would hide real bugs that are found only now and then.
3. **"Ask authors to keep pull requests small."** It is tempting because small pull requests are easier. The exam rejects it as the fix: it moves the work to people and does not improve the review.

## Quiz

4. A review of nine changed modules gives deep notes on some and shallow ones on others, and flags a pattern in one while approving it in another. How should it be restructured?
   - **a**: A model with a larger context window, for the same single pass
   - **b**: A pass for each file, then one pass on how the files fit together
   - **c**: Three full runs, keeping only the issues that two of them report
   - **d**: A rule that pull requests may not change more than four files

5. The same conversation that wrote a change is asked to judge it and approves it, though a colleague spots a flaw at once. What improves the check?
   - **a**: Giving the same conversation a longer turn limit for the judging
   - **b**: Asking the same conversation to read its own change more carefully
   - **c**: Adding a sentence that tells the judge in that conversation to be critical
   - **d**: A new session that is given the criteria and the earlier findings

6. A generator often writes cases that duplicate ones the project already has. What should the prompt change?
   - **a**: Ask for fewer tests in every run of the generator
   - **b**: Carry the existing tests and say what makes a test useful
   - **c**: Ask for tests of every function the change touches
   - **d**: Ask the generator to explain each test it decides to write

<details>
<summary>Answer key</summary>

4. **b**. Dividing the work answers the cause, thinning attention. *a* is ruled out because room is not attention: "A larger context window does not fix attention quality." *c* is ruled out because agreement hides rare finds: "Keeping only the issues that three runs agree on would hide real bugs that are found only now and then." *d* is ruled out because it shifts the work: "Asking authors to split their pull requests moves the work to people and does not improve the review."
5. **d**. A fresh session has no bias toward the change, and the earlier findings stop repeats. *b* is ruled out because the bias stays: "A fresh context improves code review since Claude won't be biased toward code it just wrote." *c* is ruled out because a sentence is a request: "A sentence that asks for more care is a request; the criteria are the case list." *a* is ruled out because time does not remove bias: "A reviewer should not be the author."
6. **b**. The generator needs to know what exists and what is worth writing. *a* is ruled out because fewer is not better: "A prompt that asks for fewer tests says nothing about which tests are worth writing." *c* is ruled out because more coverage adds repeats: "Without the existing tests in the prompt, the generator has no way to avoid repeating them." *d* is ruled out because an explanation changes neither: "A test generator that is only told to \"write tests\" writes the obvious ones, and some of them are copies of tests that exist."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S5, a pipeline that reviews pull requests and writes tests. A job must decide whether to fail, but the seriousness of each comment is free text. What fixes that?
   - **a**: Ask the model to put the word critical at the very start of any serious comment it makes
   - **b**: Ask for JSON that follows a schema where severity is one of a few fixed values
   - **c**: Count the comments and fail the job once they pass a fixed number chosen by the team
   - **d**: Search the prose of every comment for the words that usually go with a bad finding

2. Scenario S5, a pipeline that reviews pull requests and writes tests. The command reports success although the run stopped at its turn limit, and the job is green. What is the remedy?
   - **a**: Read the exit status and the JSON, and fail whenever no valid answer exists
   - **b**: Raise the turn limit until the run always finishes well before it reaches that limit
   - **c**: Run the command again whenever the output that comes back looks a little too short
   - **d**: Remove the turn limit so that a run stops only when all of its work is finished

3. Scenario S5, a pipeline that reviews pull requests and writes tests. After more commits arrive, the second run repeats comments that the author has already fixed. What should the run be given?
   - **a**: The earlier findings, with an instruction to report only what is new or still open
   - **b**: A larger number of turns, so that it reads the whole history of the change before it starts
   - **c**: The previous run's complete session, resumed with the latest commits added at the end of it
   - **d**: A stricter list of severities, so that fewer of the comments will ever reach the author

4. Scenario S5, a pipeline that reviews pull requests and writes tests. The job that only looks at code has been given the right to run whatever it likes on the machine. What is the problem?
   - **a**: Shell access output is not valid JSON, so the schema of the answer cannot be applied to it
   - **b**: Shell access slows every run down, and a turn limit would then end that run far too soon
   - **c**: Shell access changes things, so the list should hold reading and searching tools only
   - **d**: Shell access needs a person to approve each call, and nobody is there to give that approval

<details>
<summary>Answer key</summary>

1. **b**. A closed list can be compared with a floor. *a* is ruled out because a word in prose is a convention: "A sentence that asks for more care is a request; the criteria are the case list." *c* is ruled out because a count says nothing about how bad a finding is: "what each severity means, with one example for each". *d* is ruled out because the severity belongs in the schema: "The severity then belongs in the schema as a closed list, so a script can compare it with a floor."
2. **a**. The gate fails the job whenever no valid answer exists. *b* is ruled out because the limit exists to stop a loop: "`--max-turns` stops a loop". *c* is ruled out because a rerun changes nothing: "re-running it repeats the cost with no reason to expect a different result". *d* is ruled out because the limit is what ends the run by itself: "A run with no person must end by itself".
3. **a**. The run needs its own earlier output. *b* is ruled out because turns do not supply memory: "without the earlier findings it repeats every comment that the author has already fixed or answered". *c* is ruled out because a reviewer should start fresh: "the review runs as its own session and not as a continuation of the one that generated the change". *d* is ruled out because severity does not remove repeats: "Giving the run the earlier findings, and asking it to report only what is new or still open, keeps the comments useful."
4. **c**. A review changes nothing, so it needs only reading tools. *b* is ruled out because the issue is what the tool can do: "A shell tool is a way to change things". *a* is ruled out because the schema applies to the answer and not to the commands: "The severity then belongs in the schema as a closed list". *d* is ruled out because the command's approval is listed in advance: "`--allowedTools` lists what runs without asking, because nobody can approve a prompt".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The restructured review of a large pull request here follows the guide's sample question on splitting a review into passes, rewritten here.
