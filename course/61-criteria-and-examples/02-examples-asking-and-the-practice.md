# Examples that settle the hard cases, asking and the practice

**Level:** Architect · **Module 61:** Criteria and examples · **Page 2 of 2**
**Exams:** A4.2; S5

**After this page you can** choose few-shot examples that settle the ambiguous cases and show an acceptable pattern next to a finding, give each example a reason, place examples between the criteria and the diff, decide when a request with missing fields is asked about and when its assumptions are stated, and write the module's practice.

Checked on 2026-10-03 against the Claude prompting guidance pages on using examples effectively and on structuring prompts, with the exam guide's task statement 4.2. Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on a review specification (`examples/61-criteria-lint` holds the checks the page describes). This page deepens module 24 (examples and structure) and does not repeat it. The criteria and the trust in a category are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for few-shot prompting (4.2) it prescribes "2-4 targeted few-shot examples" for ambiguous scenarios, each showing the reasoning for why one action was chosen over the plausible alternatives; examples that demonstrate the desired output format; examples that distinguish acceptable patterns from genuine issues, to reduce false positives while enabling generalisation; and examples for consistent handling of varied document structures. *What the current guidance says (checked 2026-10-03):* "A few well-crafted examples ... improve accuracy and consistency", and they should be relevant, diverse and structured: "Include 3-5 examples for best results." Examples go in `<example>` tags, several in `<examples>`. The count differs from the guide's (two to four against three to five); both say "a few", and neither says more is better. On the exam, a count is a sanity check and the stronger signals are targeted, with reasons, and including an acceptable pattern; the practice accepts two to four.

## Why it matters

The criteria say to skip minor style and report a null dereference, and the reviewer still flags a guard clause that is perfectly safe, because it looks like the pattern "unchecked value" at a glance. Rules do not settle a case that looks like two rules at once; a pair of examples does. One shows the unsafe access and why it is reported; the other shows the guarded access and why it is skipped. Scenario S5 asks what to add to a prompt that is accurate on clear cases and noisy on borderline ones, and the answer is a few examples placed on the border.

## The idea

### Examples on the border

Few-shot examples are not a sample of typical cases; typical cases are already handled by the criteria. They are chosen where the reviewer is unsure, which are the cases a human reviewer also hesitates over. Three properties make an example work:

1. **Targeted.** It shows a case that the criteria alone do not settle, such as an access that looks unsafe and is guarded.
2. **Both verdicts.** At least one example is reported and at least one is skipped. A set whose every example is a finding teaches the reviewer to report everything; the acceptable pattern is the example that cuts false positives while still letting the reviewer generalise to new code.
3. **With a reason.** Each example says why the verdict was chosen over the plausible alternative. The reason is what transfers to a case that matches no example.

They should also mirror the real output: show the format the answer must have, since examples are the surest way to steer format as well as judgement. And they should be diverse enough that the reviewer does not learn an accident: five examples that all end with the same label teach the label (module 44's mock shows the failure).

### Where examples go

Examples sit between the criteria and the input, each in its own tag, with the diff last. Structure helps the model tell instructions from examples from data: `<criteria>`, `<examples>` with one `<example>` per case, then `<diff>`. The practice asks for the order criteria, examples, diff last, and its tests check it; that order is the course's design, which keeps the criteria and the examples together. The prompting guidance for very long inputs is the reverse, with the long material near the top and the question after it, so for a large diff end the prompt with a one-line restatement of the task.

### Ask, or state the assumption

A request is sometimes incomplete: no branch named, no reviewer. What a run does depends on whether anyone can answer. In an attended session (a developer at the keyboard) the right move is to ask only what cannot be assumed, and to state the assumptions made for the rest ("assuming `main`"). In an unattended run (CI) nobody can answer, so it never asks: it proceeds on stated assumptions where a default exists, and stops with a clear failure where a required field has none. Asking in CI produces a job that waits for a person who is not there. The example for this is the practice's `next_step`: given the request, the required fields, the defaults and whether the run is attended, it returns `proceed`, `ask` or `stop`, with the assumptions listed.

### The example

<!-- example: m61-criteria-lint tabs: python,typescript -->
```python
"""Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.

The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
"""
VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment")


def lint_criterion(criterion):
    """Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example."""
    found = []
    for key in ("report", "skip"):
        text = criterion.get(key) or ""
        if not text.strip():
            found.append(f"no-{key}")
        elif any(phrase in text.lower() for phrase in VAGUE):
            found.append(f"vague-{key}")
    for level in ("high", "low"):
        if "`" not in (criterion.get("severity") or {}).get(level, ""):
            found.append(f"no-{level}-example")
    return found


def lint_examples(examples):
    """Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why."""
    found = []
    if not 2 <= len(examples) <= 4:
        found.append("two-to-four")
    if {e["verdict"] for e in examples} != {"report", "skip"}:
        found.append("both-verdicts")
    if any(not e.get("reason") for e in examples):
        found.append("reason-missing")
    return found


def trust(verdicts, min_reviewed=5, min_precision=0.5):
    """Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved."""
    table = {}
    for category, verdict in verdicts:
        row = table.setdefault(category, {"reviewed": 0, "accepted": 0})
        row["reviewed"] += 1
        row["accepted"] += 1 if verdict == "accepted" else 0
    for row in table.values():
        row["precision"] = round(row["accepted"] / row["reviewed"], 2)
        row["off"] = row["reviewed"] >= min_reviewed and row["precision"] < min_precision
    return table


VAGUE_CRITERION = {"report": "Be conservative and only flag important problems.", "skip": "", "severity": {"high": "Something serious.", "low": "A small thing."}}
GOOD_CRITERION = {"report": "A comment whose claimed behaviour contradicts the code.", "skip": "Minor style and patterns the codebase already uses.",
                  "severity": {"high": "A null dereference such as `user.profile.name` when `user` may be None.", "low": "A misleading name such as `total` for a count."}}


def main():
    print("vague criterion:", lint_criterion(VAGUE_CRITERION))
    print("good criterion:", lint_criterion(GOOD_CRITERION))
    one_side = [{"verdict": "report", "reason": "The comment says sum, the code multiplies."}, {"verdict": "report", "reason": ""}, {"verdict": "report", "reason": "Unchecked None."}, {"verdict": "report", "reason": "Off by one."}, {"verdict": "report", "reason": "Wrong key."}]
    print("five reports, one without a reason:", lint_examples(one_side))
    print("a report and a skip:", lint_examples([{"verdict": "report", "reason": "r"}, {"verdict": "skip", "reason": "s"}]))
    verdicts = [("bug", "accepted")] * 9 + [("bug", "dismissed")] * 1 + [("style", "accepted")] * 2 + [("style", "dismissed")] * 6 + [("naming", "dismissed")] * 3
    for category, row in trust(verdicts).items():
        print(f"{category}: reviewed {row['reviewed']}, precision {row['precision']}, switch off: {row['off']}")


if __name__ == "__main__":
    main()
```
```text
vague criterion: ['vague-report', 'no-skip', 'no-high-example', 'no-low-example']
good criterion: []
five reports, one without a reason: ['two-to-four', 'both-verdicts', 'reason-missing']
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: False
style: reviewed 8, precision 0.25, switch off: True
naming: reviewed 3, precision 0.0, switch off: False
```
```typescript
/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment"];
export type Criterion = { report?: string; skip?: string; severity?: { high?: string; low?: string } };
export type Example = { verdict: string; reason?: string };

/** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
export function lintCriterion(criterion: Criterion): string[] {
  const found: string[] = [];
  for (const key of ["report", "skip"] as const) {
    const text = criterion[key] ?? "";
    if (text.trim() === "") found.push(`no-${key}`);
    else if (VAGUE.some((phrase) => text.toLowerCase().includes(phrase))) found.push(`vague-${key}`);
  }
  for (const level of ["high", "low"] as const) if (!(criterion.severity?.[level] ?? "").includes("`")) found.push(`no-${level}-example`);
  return found;
}

/** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
export function lintExamples(examples: Example[]): string[] {
  const found: string[] = [];
  if (examples.length < 2 || examples.length > 4) found.push("two-to-four");
  if ([...new Set(examples.map((e) => e.verdict))].sort().join() !== "report,skip") found.push("both-verdicts");
  if (examples.some((e) => !e.reason)) found.push("reason-missing");
  return found;
}

export type Row = { reviewed: number; accepted: number; precision: number; off: boolean };

/** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
export function trust(verdicts: Array<[string, string]>, minReviewed = 5, minPrecision = 0.5): Record<string, Row> {
  const table: Record<string, Row> = {};
  for (const [category, verdict] of verdicts) {
    const row = (table[category] ??= { reviewed: 0, accepted: 0, precision: 0, off: false });
    row.reviewed += 1;
    row.accepted += verdict === "accepted" ? 1 : 0;
  }
  for (const row of Object.values(table)) {
    row.precision = Math.round((row.accepted / row.reviewed) * 100) / 100;
    row.off = row.reviewed >= minReviewed && row.precision < minPrecision;
  }
  return table;
}

export const VAGUE_CRITERION: Criterion = { report: "Be conservative and only flag important problems.", skip: "", severity: { high: "Something serious.", low: "A small thing." } };
export const GOOD_CRITERION: Criterion = { report: "A comment whose claimed behaviour contradicts the code.", skip: "Minor style and patterns the codebase already uses.",
  severity: { high: "A null dereference such as `user.profile.name` when `user` may be None.", low: "A misleading name such as `total` for a count." } };

function main() {
  console.log("vague criterion:", JSON.stringify(lintCriterion(VAGUE_CRITERION)));
  console.log("good criterion:", JSON.stringify(lintCriterion(GOOD_CRITERION)));
  const oneSide = [{ verdict: "report", reason: "The comment says sum, the code multiplies." }, { verdict: "report", reason: "" }, { verdict: "report", reason: "Unchecked None." }, { verdict: "report", reason: "Off by one." }, { verdict: "report", reason: "Wrong key." }];
  console.log("five reports, one without a reason:", JSON.stringify(lintExamples(oneSide)));
  console.log("a report and a skip:", JSON.stringify(lintExamples([{ verdict: "report", reason: "r" }, { verdict: "skip", reason: "s" }])));
  const verdicts: Array<[string, string]> = [...Array(9).fill(["bug", "accepted"]), ["bug", "dismissed"], ...Array(2).fill(["style", "accepted"]), ...Array(6).fill(["style", "dismissed"]), ...Array(3).fill(["naming", "dismissed"])];
  for (const [category, row] of Object.entries(trust(verdicts))) console.log(`${category}: reviewed ${row.reviewed}, precision ${row.precision}, switch off: ${row.off}`);
}

if (import.meta.main) main();
```
```text
vague criterion: ["vague-report","no-skip","no-high-example","no-low-example"]
good criterion: []
five reports, one without a reason: ["two-to-four","both-verdicts","reason-missing"]
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: false
style: reviewed 8, precision 0.25, switch off: true
naming: reviewed 3, precision 0, switch off: false
```
<!-- /example -->

### The practice: a review specification that cuts false positives

The practice is in [`exercises/61-criteria-and-examples`](../../exercises/61-criteria-and-examples/unit-01/practice-1/statement.md). You write the function that builds the review prompt from a specification (refusing vague or incomplete criteria and a bad example set, and putting criteria, examples and the diff in that order), the report that computes each category's precision and the most dismissed patterns, and the function that decides whether an incomplete request is asked about, assumed or stopped. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists eight cases, each saying what you should see when it works.

## Traps

1. **"Give the reviewer as many examples as possible."** It is tempting because more data should help. The exam rejects it: the guide asks for two to four targeted examples, and a long list teaches accidents. A few examples that settle the ambiguous cases do more.
2. **"Show only the issues to report."** It is tempting because those are the cases the reviewer must catch. The exam rejects it: without an acceptable pattern the reviewer reports every look-alike, and the false positives remain.
3. **"Pick examples that are typical of the codebase."** It is tempting because typical cases are the common ones. The exam rejects it: typical cases are settled by the criteria; examples earn their place on the ambiguous ones.
4. **"In CI, ask the author which branch to review."** It is tempting because asking is safer than guessing. The exam rejects it: an unattended run has nobody to answer, so it states the assumption or stops.

## Quiz

1. A review prompt handles clear bugs well but keeps marking a null access that is already protected as risky. What should be added?
   - **a**: Ten more samples of obvious bugs to reinforce the pattern
   - **b**: A line asking the reviewer to be more careful with null values
   - **c**: A flagged sample and a skipped sample side by side, each with its reason
   - **d**: A second pass in which the reviewer rates its own confidence in every remark

2. A review job in CI receives a request with no reviewer named, and the project holds no default reviewer. What should the run do?
   - **a**: Ask the author who the reviewer should be, and wait
   - **b**: Stop with a plain failure that lists the absent field
   - **c**: Pick a reviewer at random and say nothing about it
   - **d**: Carry on without a reviewer and omit it from the output

<details>
<summary>Answer key</summary>

1. **c**. An acceptable pattern shown next to a finding, with reasons, settles the borderline case. *a* is ruled out because "examples earn their place on the ambiguous ones", and obvious bugs are already handled. *b* is ruled out because "Rules do not settle a case that looks like two rules at once", and an instruction to be careful names no pattern. *d* is ruled out because a rating of its own confidence adds no case to learn from, and examples "are chosen where the reviewer is unsure".
2. **b**. An unattended run cannot ask, and a required field with no default cannot be assumed. *a* is ruled out because "Asking in CI produces a job that waits for a person who is not there". *c* is ruled out because a guess is allowed only as a stated assumption where a default exists: "it proceeds on stated assumptions where a default exists". *d* is ruled out because a run with nothing to assume "stops with a clear failure where a required field has none", and dropping the field hides the problem.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. The labels for how bad a finding is are free text, and the gate cannot compare them with its floor. Which change fixes it?
   - **a**: Ask the reviewer to use stronger adjectives for urgent findings
   - **b**: Let the gate guess the level from words in the issue text
   - **c**: Give each level a worked sample and close the field to a fixed list
   - **d**: Drop the floor so that every label is accepted as it is

2. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. One group of findings has 40 percent precision. Its dismissals all trace to line-length remarks, while its other remarks are always accepted. What fits?
   - **a**: Disable the whole group until every remark in it improves
   - **b**: Fix the criteria for that one source of noise and keep the rest enabled
   - **c**: Delete the group's criteria and start over with none
   - **d**: Accept the noise because the precision is above a third

3. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. A review run starts from a request whose branch name is blank, and the project defines `main` as the default. What should it do?
   - **a**: Ask the author which branch is meant and wait for an answer
   - **b**: Stop with a failure, since a required field is missing
   - **c**: Proceed on `main` and state that assumption in its output
   - **d**: Choose a branch at random and leave it unmentioned

<details>
<summary>Answer key</summary>

1. **c**. A level with a code sample has a boundary, and a closed list can be compared with a floor. *a* is ruled out because "A definition with a code example gives the reviewer a case to compare with", and adjectives give none. *b* is ruled out because the gate needs a value it can compute with: "an open, free-text severity cannot be computed with". *d* is ruled out because the gate "drops findings below a floor and fails the job at a severity", and without a floor it has no policy.
2. **b**. The recorded pattern shows where the dismissals come from, so only that part is adjusted. *a* is ruled out because "The fix is then to adjust the criteria for the one pattern and not to disable the category". *c* is ruled out because "Deleting the category from the criteria loses the examples that the next attempt needs". *d* is ruled out because the noise has a cost, since "each dismissed comment lowers trust in the accepted ones".
3. **c**. A run that cannot ask proceeds on a stated assumption where a default exists. *a* is ruled out because "Asking in CI produces a job that waits for a person who is not there". *b* is ruled out because it stops only without a default: "it proceeds on stated assumptions where a default exists". *d* is ruled out because a silent guess omits the step the page requires, which is "to state the assumptions made for the rest".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
