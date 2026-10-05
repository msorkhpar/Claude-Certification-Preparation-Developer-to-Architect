# Diagnosing a weak output, and a template builder

**Level:** Foundations · **Module 6:** Prompting fundamentals · **Page 4 of 4**
**Exams:** DV2, DV4, AS1, AS7, A4 (A4.1)

**After this page you can** tell why a prompt underperforms (missing context, ambiguity, the wrong feature
or the wrong model), change one thing at a time to fix it, and build a prompt template that is graded on
its structure.

Checked against the Associate exam guide (domain 7, version 1.0, July 2026), the Claude API documentation
(prompt engineering overview, choosing a model, reduce hallucinations) on 2026-10-02, and by running the
practice below offline in the course container (Python, TypeScript, Java and Kotlin). Replies shown are
hand-scripted and labelled illustrative.

## Why it matters

A weak answer is a symptom. The Associate exam's last domain, troubleshooting and optimisation, asks you
to diagnose a failure type and apply a controlled fix; the Developer exam asks the same of an application.
The common wrong move is to change five things at once, or to reach for a bigger model, and then not know
what worked. The documentation's overview makes the underlying point: "not every success criteria or
failing eval is best solved by prompt engineering", and for latency and cost "you can sometimes improve
latency and cost more easily by selecting a different model."

## The idea

### Diagnose first: four causes

Read the weak output against the prompt and ask which of these it looks like:

| Cause | What you see | The fix |
|---|---|---|
| **Missing context** | Generic or invented content; the answer ignores facts only you know | Supply the material, the audience, the purpose |
| **Ambiguity** | A reasonable answer to a different question; inconsistent results across runs | Say exactly what is wanted; add constraints, an example, a format |
| **The wrong feature** | The job needs something the prompt cannot give (current data, exact arithmetic, your files) | Use a tool, retrieval, code or a Project; do not write a longer prompt |
| **The wrong model** | Correct instructions, correct context, still a capability or speed miss | Evaluate another tier or raise effort (module 3) |

Two further sources to rule out: a **hallucination** (module 1: ask for quotes, allow "I don't know") and a
**format** failure (state the format and validate in code, page 2).

<!-- illustrative -->
Three weak outputs and their diagnosis. The symptoms are hand-written for this page.

```text
1. "Summarise our Q3 churn."  ->  a plausible paragraph with percentages we never supplied
   Diagnosis: missing context (no data in the prompt) and a hallucination. Fix: supply the data,
   and add "use only the figures below; say if something is missing".

2. "Make the tone better."  ->  a rewrite that is more formal, when casual was meant
   Diagnosis: ambiguity ("better"). Fix: state the audience and the tone, show one example.

3. "What did the competitor announce yesterday?"  ->  confident, out-of-date answer
   Diagnosis: wrong feature. The model has a cut-off and no live access. Fix: a search or retrieval
   step, or paste the announcement. A cleverer prompt does not fix it.
```
<!-- /illustrative -->

### Change one thing at a time

Once you have a hypothesis, test it alone:

1. **Fix the evaluation set** (page 3): the same inputs, and the criteria for a good answer.
2. **Change one thing**: add the context, or the constraint, or the example, or the format, or switch the
   model. Not several.
3. **Re-run the whole set** and compare against the last version, not just the case that annoyed you.
4. **Keep the change only if it helped** and nothing else got worse. Write down what you changed and why,
   so the prompt has a history (module 24, versioning prompts).

Why one at a time: if you change the context, the format and the model together and the result improves, you
do not know which change did it, you carry changes that did nothing, and the next regression has no trail.
It is the same discipline as debugging code.

### Cheaper and faster at equal quality

Optimisation comes after correctness: first reach the quality bar; then look for the cheaper way to hold it. Options, from the pages you have read: a shorter prompt that keeps the same results,
reusing context instead of resending it (Projects in the apps, caching in the API, module 20), a smaller
tier or lower effort where the evaluation set says quality holds (module 3), and moving exact steps into
code (module 4).

## The practice: a prompt template builder

You have seen the parts of a specification (page 3), tags (page 2) and the problem of untrusted text in a
prompt. The practice makes them a function: **a prompt template builder, graded on structure**.

You write one function that takes a spec (task, role, context, documents, examples, constraints, output
format) and a map of variables, and returns the prompt string. It is the kind of code every Claude
application contains in some form. The tests do not ask a model anything, because they do not need to:
they check the **structure** of the string, which is the part your code is responsible for.

The statement, with the exact format, is in
`exercises/06-prompting-fundamentals/unit-01/practice-1/statement.md`. The folder has a language folder per
course language: `python`, `typescript`, `java` and `kotlin`, each with a `starter`, the `tests` and a
`run.sh` or build file. The starter fails every test; your job is to make them pass.

The cases, in the language-neutral ids the tests use:

| Id | What it checks |
|---|---|
| `m1` | A full spec renders every section in order |
| `e1` | Absent optional sections are omitted, not rendered empty |
| `e2` | Variables fill in one pass; a missing one is named in the error |
| `e3` | A blank task is refused |
| `e4` | Document text cannot close its own tag |
| `e5` | Placeholders inside documents stay literal |
| `e6` | Documents and examples keep their order and numbering |

Three of these are about safety, not formatting: `e4` and `e5` are the template-level defences that
module 41 builds on (data is marked, escaped and never treated as a template), and `e2` is the guard
against a prompt that goes out with a hole in it. A guarantee belongs in code, and this is that code.

## Traps

1. **Rewriting the whole prompt at once.** Several simultaneous edits leave you unable to say what helped.
2. **Blaming the prompt for a feature gap.** No wording makes a model know yesterday's news or add
   exactly. Fix the feature, not the phrasing.
3. **Judging by the case that annoyed you.** A fix that repairs one input and quietly breaks three others is
   a regression; run the set.

## Quiz

1. A prompt asks Claude to describe "our current pricing", and the reply lists plausible but wrong tiers. The
   team's first idea is a longer prompt with stronger wording. What is the best diagnosis?
   - **a**: The rate card never reached the request, so the answer is a guess
   - **b**: The wording lacks force, so a longer prompt with firmer phrasing will make the tiers right
   - **c**: The wrong model is answering, so moving up one tier fixes it
   - **d**: The format is unclear, so the reply needs an explicit table layout

2. After a revision of a classification prompt, one ticket that used to fail now passes, but two that used to
   pass now fail. Four changes were made at once. What should the team do?
   - **a**: Keep all four, since the case that failed before now passes
   - **b**: Add a fifth to cover the two new failures, then re-run the set
   - **c**: Revert, then reapply the edits singly, checking the whole set each time
   - **d**: Switch to a larger model to absorb the interactions between the four

<details>
<summary>Answer key</summary>

1. **a**. Content absent from the prompt is filled with a plausible guess, which is missing context plus a hallucination (the diagnosis table and its first illustrative case). *b* is ruled out because "No wording makes a model know yesterday's news or add exactly", and by the same reasoning no wording supplies a rate card. *c* is ruled out because the table reserves the wrong-model cause for "Correct instructions, correct context, still a capability or speed miss", and here the context is missing. *d* is ruled out because the symptom is "Generic or invented content; the answer ignores facts only you know", which the table assigns to missing context, not format.
2. **c**. One change at a time against a fixed set is the method, and a regression means the edits interact (the one-at-a-time section). *a* is ruled out because "A fix that repairs one input and quietly breaks three others is a regression". *b* is ruled out because "Several simultaneous edits leave you unable to say what helped", and a fifth edit adds to them. *d* is ruled out because the method says "or switch the model. Not several", and a larger model adds yet another variable.

</details>

## Module quiz

This quiz covers all four pages of the module.

1. A classification prompt gets most tickets right but keeps mislabelling sarcastic ones, and nobody can see
   why. Which step fits best?
   - **a**: Swap in a larger model and see whether the sarcastic cases improve
   - **b**: Leave the wording alone and sort the failures against the diagnosis table
   - **c**: Add twenty more examples of sarcasm and rewrite the instructions at the same time
   - **d**: Rewrite the instructions to name sarcasm outright and see whether the labels settle

2. A team asks for a label and gets free text, and their parser breaks when a reply starts with "Sure!". They
   call Claude Opus 5.5. Which pair of actions best fits the module?
   - **a**: Request longer replies and trim them by length
   - **b**: Prefill the reply with an opening brace so it cannot start with chat
   - **c**: State the exact shape up front, then check and clean the output in code
   - **d**: Remove the parser and rely on the model to answer with the label only

3. A prompt for brainstorming product names returns ten near-identical suggestions. A colleague points out
   that the team's research prompt, which says to be precise and cite sources, works well. Which change
   fits?
   - **a**: Lower the randomness setting so every name is the most probable one
   - **b**: Copy the research wording, "be precise and cite sources", into this prompt
   - **c**: Supply three near-identical sample names so the model sees the target
   - **d**: Ask for many varied options first, then evaluate them in a second pass

4. A drafting prompt produces bland correspondence. The team wants replies in a specific house style. Which
   addition is most likely to get it?
   - **a**: A stronger role line: "You are the best business writer alive"
   - **b**: Three to five real past emails, wrapped in example tags
   - **c**: A longer list of style adjectives: warm, crisp, confident, human
   - **d**: A higher effort setting so Claude thinks harder about tone

5. A prompt asks Claude how many units of a product the warehouse holds right now, and it answers with a
   confident, wrong number. Which change addresses the cause?
   - **a**: Wire in a query against the live database and let the model phrase the result
   - **b**: Ask for a confidence score on each count so that doubtful ones can be dropped later
   - **c**: Move to the largest tier, whose knowledge is the most complete
   - **d**: Tell Claude to answer only with the numbers it is certain about

<details>
<summary>Answer key</summary>

1. **b**. Diagnosis comes before any change: the failures are read against the four causes (page 4), without altering the prompt. *a* is ruled out because a larger model is the fix only for "Correct instructions, correct context, still a capability or speed miss", which nobody has established. *c* is ruled out because the method is to "Change one thing at a time", and this changes two at once. *d* is ruled out because the page puts "Diagnose first: four causes" ahead of any edit, and an instruction rewrite is a guess until the cause is known.
2. **c**. Specify the format in the prompt and check it in code, and page 2 adds stripping a stray preamble. *b* is ruled out because prefilled responses on the last assistant turn "are no longer supported" from Claude 4.6 and return a 400 error. *a* is ruled out because the fix for a stray preamble is to "ask for the answer inside tags, and strip what slips through in code", not to trim by length. *d* is ruled out because "Code that consumes the output must still validate it".
3. **d**. Brainstorming wants breadth first and judgment later, with a second pass to evaluate (page 3, strategy per task type). *b* is ruled out because an instruction that helps a research prompt "can be noise in a brainstorm" (page 3, third trap). *c* is ruled out because examples must "vary enough that Claude doesn't pick up unintended patterns", and near-identical samples teach one pattern (page 2). *a* is ruled out because "the approach that suits extraction (precision, low variety) works against ideation", and the most probable name is the least varied; module 1 adds that the models this course uses do not accept sampling settings.
4. **b**. A handful of real past emails in tags puts the target house voice into the context, which the page says is how examples steer tone and structure (page 2). *a* is ruled out because "A role is a request, not a credential", and it names no target to copy. *c* is ruled out because "Stacking adjectives instead of constraints" is the first trap of page 1. *d* is ruled out because thinking is "where Claude decides when and how much to think, steered by the effort parameter", which sets reasoning depth and not voice.
5. **a**. The count lives in a system the model cannot see, so the job needs a feature the prompt cannot give: a lookup, with the model only phrasing the result (the diagnosis table's wrong-feature row). *d* is ruled out because the page says "Fix the feature, not the phrasing". *b* is ruled out because for a missing feature "A cleverer prompt does not fix it", and a score from the same model adds no data. *c* is ruled out because the wrong-model cause is "Correct instructions, correct context, still a capability or speed miss", and no tier knows a private count.

</details>
