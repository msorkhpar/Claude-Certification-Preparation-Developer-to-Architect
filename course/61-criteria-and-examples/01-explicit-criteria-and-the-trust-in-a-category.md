# Explicit criteria and the trust in a category

**Level:** Architect · **Module 61:** Criteria and examples · **Page 1 of 2**
**Exams:** A4.1; S5

**After this page you can** replace a vague review instruction with explicit categories, a skip list and a severity definition with a code example at each level, measure how much developers trust each category from their verdicts, switch off a noisy category while its prompt is improved, and record the pattern behind each finding so that the dismissals can be analysed.

Checked on 2026-10-03 against the Claude prompting guidance pages on being clear and direct and on structuring prompts, and the Claude Code best-practices page, with the exam guide's task statement 4.1. Nothing here called a model: the example is a set of checks over criteria and developer verdicts, written as plain code. This page deepens module 24 (prompt engineering for applications) and module 42 (evaluation) and does not repeat them. Where those modules treat prompts in general, this one treats the review prompt of module 60. Examples in the prompt and unattended runs are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for precision (4.1) it prescribes "explicit criteria" over "vague, confidence-based instructions": specify which issues to report (bugs, security) and which to skip (minor style, local patterns), because "general instructions like 'be conservative' or 'only report high-confidence findings' fail to improve precision"; temporarily disable a category with a high false-positive rate to restore trust while its prompt is improved; and define severity levels with concrete code examples for each. *What the current product's guidance says (checked 2026-10-03):* "Claude responds well to clear, explicit instructions. Being specific about your desired output can help enhance results." The same documentation gives the review lesson in another place: a reviewer prompted to find gaps "will usually report some, even when the work is sound", so tell it to flag only gaps that affect correctness or the stated requirements. On the exam, the answer to a precision problem is explicit categories and examples, never an adjective about confidence.

## Why it matters

A review job posts a comment on every pull request about naming, line length and import order. Developers dismiss them, then dismiss everything, including the one comment per week about a missing null check that matters. The first reaction is to add a line to the prompt: "be conservative" or "only report issues you are confident about". The model has no scale for conservative, and the comments do not change. Scenario S5 asks what does work, and the answer has three parts: say what to report and what to skip, show what each severity looks like, and when a category is the problem, turn it off until it is fixed.

## The idea

### Say the pattern, not the attitude

An instruction can name a pattern or an attitude. "Report a comment or docstring whose claimed behaviour contradicts what the code does" names one, and a reviewer can check a line against it. "Only report important issues" names none, and the reviewer decides what is important by its own guess, which differs between runs. The test is whether two people reading the sentence would flag the same lines. The criteria that work are categorical:

| Part | What it says | Example |
|---|---|---|
| Report | The patterns that count, by category | Bugs: an unchecked `None`, an off-by-one in a loop bound; security: user input reaching a query |
| Skip | What not to report, as explicit as the report list | Minor style such as naming and line length; patterns the codebase already uses elsewhere |
| Severity | One definition per level, each with code | high: `DELETE FROM orders` built from request input; low: a variable called `total` that holds a count |

The skip list matters as much as the report list. Most false positives are in the categories the prompt never mentioned, and they stop only when the prompt says to skip them. The example's `lint_criterion` refuses a criterion that is vague in either list, lacks a skip list, or has a severity level without a code example.

### Severity needs a worked case

"High means serious" gives no boundary. A definition with a code example gives the reviewer a case to compare with: "high: data loss or a security hole, for example `DELETE FROM orders` built from request input". Give an example for each level, so that the middle level is bounded from both sides. Severity is also what the pipeline uses: module 60's gate drops findings below a floor and fails the job at a severity, so an open, free-text severity cannot be computed with.

### Trust is measured, and a bad category is switched off

Developers vote on findings with their behaviour: they fix them or dismiss them. Record each verdict by category, and the precision of a category is the share of its findings that were accepted. A category with enough reviews and a low precision does damage out of proportion to its value, because each dismissed comment lowers trust in the accepted ones. The guide's remedy is to disable the category temporarily while its prompt is improved, and to bring it back when the examples and criteria for it are right. The example's `trust` computes the table: reviewed, precision, and whether to switch the category off, with a minimum number of reviews so that two unlucky comments do not decide it.

Disabling is a policy applied after the model's answer (module 60's gate drops the disabled categories), or a line in the prompt, and it is reversible. Deleting the category from the criteria loses the examples that the next attempt needs.

### Record the pattern behind each finding

Add a `detected_pattern` field to each finding in the schema, such as `missing-none-check` or `line-length`. When developers dismiss findings, the count of dismissals per pattern shows which patterns draw the dismissals: a category with 40 percent precision may contain one pattern that is always rejected and three that are always accepted. The fix is then to adjust the criteria for the one pattern and not to disable the category. This is the systematic analysis of dismissal patterns that the guide names, and it needs the field to exist before the first review.

### The example

<!-- example: m61-criteria-lint tabs: python,typescript -->
```python
"""Three checks that keep a review prompt precise."""
```
<!-- /example -->

## Traps

1. **"Add 'be conservative' to the prompt to cut the false positives."** It is tempting because it asks for exactly the outcome wanted. The exam rejects it: the instruction states an attitude and no pattern, and "fail[s] to improve precision". Name what to report and what to skip.
2. **"Ask the model for a confidence score and only post findings above 0.8."** It is tempting because it looks quantitative. The exam rejects it: self-reported confidence is another vague instruction, and the criteria still decide what is reported. Use explicit categories, examples and the developers' own verdicts.
3. **"Delete the noisy category from the prompt for good."** It is tempting because the noise stops. The exam prefers disabling it temporarily while its prompt is improved, because trust is restored and the category can return with better criteria.
4. **"Describe severity in words: high means serious, low means minor."** It is tempting because the words are clear to a person. The exam rejects it: a level without a code example has no boundary, and the guide asks for a concrete code example at each level.

## Quiz

1. A review prompt says 'only report high-confidence issues', yet developers still dismiss most remarks about naming and layout. What fixes it?
   - **a**: Phrase the confidence threshold in stricter words
   - **b**: Ask the reviewer to explain each remark at much greater length
   - **c**: Send fewer files to each review run
   - **d**: Spell out which patterns to flag and which to leave alone

2. One category of findings was dismissed in two thirds of its first twenty reviews, and developers have begun skipping the other remarks too. What does the guide recommend for that category?
   - **a**: Switch it off for now while its prompt and examples improve
   - **b**: Delete it from the criteria for good
   - **c**: Keep it and mark every one of its findings as low severity at once
   - **d**: Keep it and attach a confidence score to each finding

<details>
<summary>Answer key</summary>

1. **d**. Naming the patterns to report and to skip is what changes the output. *a* is ruled out because stricter wording of the same attitude still names no pattern, and such instructions "fail to improve precision". *b* is ruled out because a longer explanation leaves the categories unmentioned: "Most false positives are in the categories the prompt never mentioned". *c* is ruled out because a smaller review changes the volume only, while a pattern is something "a reviewer can check a line against it".
2. **a**. Disabling a high false-positive category while its prompt is improved restores trust in the rest. *b* is ruled out because "Deleting the category from the criteria loses the examples that the next attempt needs". *c* is ruled out because the remarks would still be posted and dismissed, since "each dismissed comment lowers trust in the accepted ones". *d* is ruled out because "self-reported confidence is another vague instruction".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
