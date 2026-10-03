# Code-graded and model-graded evals

**Level:** Developer · **Module 42:** Evaluation · **Page 2 of 3**
**Exams:** DV8; A4.4

**After this page you can** choose a grader for a criterion (an exact label, a pattern, a field in a JSON object, a similarity score, a model grader on a scale), write a model-graded check whose reply a program can read, say what the documentation recommends for a model grader, and use the same checks at run time in a validate-and-retry loop.

Checked on 2026-10-03 against the Claude API documentation page "Define success criteria and build evaluations", whose examples cover exact match, cosine similarity, ROUGE-L and three model-graded scales, and the Prompt engineering overview. The grader semantics described under "The graders the practice uses" are the course's own design. The judge prompt and replies shown are labelled illustrative, and nothing on this page calls a model.

## Why it matters

A test set is only as useful as the way each case is graded. A grader that is too loose passes bad output; one that is too strict fails good output and teaches the team to ignore the suite. The exam asks which grader fits which criterion, and it asks the quieter question too: when a model grades a model, what must be true for the grade to mean something?

## The idea

### Pick the cheapest grader that can tell the difference

The documentation's second principle is to "Structure questions to allow for automated grading (for example, multiple-choice, string match, code-graded, LLM-graded)." The list runs from the cheapest grader to the most expensive, and the course's rule is to use the first one that can decide the case.

| Grader | Decides | Example from the page |
|---|---|---|
| Exact match | A categorical answer: a label, a choice | Sentiment analysis, where the code normalizes "whitespace and case" |
| String or pattern match | An answer that must contain, or must not contain, something | A card number that must not appear |
| Structure check | A reply that must parse and carry the right fields | A JSON object whose `label` field equals `spam` |
| Similarity score | Whether two texts say the same thing | Cosine similarity of sentence embeddings, for consistency; ROUGE-L, the longest common subsequence, for summaries |
| Model grader | A judgement no code can make | A scale for tone, a yes-or-no for a privacy leak, a scale for use of context |
| Human grader | Anything, slowly | A sample, to check the model grader |

The page's examples pair each criterion with a grader: task fidelity with exact match, consistency with cosine similarity, relevance and coherence with ROUGE-L, tone and style with a model-graded Likert scale, privacy preservation with a model-graded binary classification, and context utilization with a model-graded ordinal scale. The similarity and ROUGE-L graders need a library and, for cosine similarity, an embedding model, so this module's code uses the first three and the model grader.

### Code-graded checks

A code grader is a function from the output to pass or fail, plus a reason. Three decisions matter more than the code.

- **What is normalised.** The page's exact-match example normalises white space and case, because "Positive" and "positive" are the same answer. It does not accept "positively" or an answer wrapped in a sentence. Decide what the application's contract is, and grade exactly that.
- **Search or whole match.** A pattern that must be present is a search of the output. A pattern that anchors the whole output is a statement that nothing else may appear, and the model's polite preamble then fails the case. Choose on purpose.
- **Parsing is part of the test.** A structure check parses the whole output as JSON. Output that arrives as a code fence, or with a sentence before the object, fails with the reason `not json`, and that failure is information: it is a format failure the application must handle, as module 25 shows. Grade the output your application finally uses. If your code strips the fence before it parses, the eval runs that code; if it does not, the eval reports the failure.

A structure check also compares types. The string `"3"` is not the number `3`, and `1` is not `true`. A grader that compares both sides as text passes values that a program reading the field would reject.

### Model-graded checks

When no code can decide, a model can. The page's tone example sends the output to a grader with this prompt, in the page's own words:

```text
Rate this response on a scale of 1-5 for being {target_tone}:
<response>{model_output}</response>
1: Not at all {target_tone}
5: Perfectly {target_tone}
Output only the number.
```

Source: Claude API documentation, "Define success criteria and build evaluations" (Likert scale example).

Four properties make that prompt a good grader, and each is a rule for your own.

1. **A scale with anchors.** The ends are named ("Not at all", "Perfectly"), so a 4 means roughly the same thing from run to run.
2. **The output is delimited.** The text being graded sits inside tags, so the grader does not mistake it for an instruction.
3. **One number comes back.** "Output only the number" lets the code read the grade with a strict parse. The page's code converts the trimmed reply to an integer.
4. **A different model grades.** A comment repeated in the page's model-graded examples says: "Generally best practice to use a different model to evaluate than the model used to generate the evaluated output."

Add a rule of the course's own: **a reply that is not a bare score counts as ungradable, and an ungradable case fails.** A grader that answers "Score: 4, because it is polite" has broken its contract, and a lenient parse that fishes a digit out of prose will sometimes read a digit that was never the grade. Failing loudly shows you the grader needs a better prompt. Count the ungradable cases in the report, so a drift in the grader is visible.

The documentation does not give a method for checking a model grader. The course's advice is to grade a sample by hand, compare the two, and re-check whenever the grader's model or prompt changes. A model grader is a measuring instrument, and an instrument is calibrated.

<!-- illustrative -->
```text
criterion: empathetic     threshold: 4

reply "5"                    -> pass, score 5
reply " 4\n"                 -> pass, score 4 (the reply is trimmed)
reply "3"                    -> fail, below threshold
reply "Score: 4"             -> fail, ungradable (not a bare score)
reply "I'd say 4 or 5"       -> fail, ungradable
reply "6"                    -> fail, ungradable (outside the scale)
no judge supplied            -> fail, ungradable
```
<!-- /illustrative -->

The replies are hand-written for this page; they show the contract of the practice's judge check.

### Non-determinism

Output from the same prompt varies from run to run, so one run is one sample. The sampling settings that would pin it down are not available on every model: the Sonnet 5.5 and Opus 5.5 migration pages say that a non-default value of the sampling parameters is rejected (module 18). For an eval the answer is to repeat. Run each case several times, and pass it only if every run passes. A case that passes sometimes is **flaky**, and it is worth a report line of its own: it marks an instruction that the model follows most of the time, which in production means a visible failure a few times a day.

### The graders the practice uses

The practice at the end of the module defines four checks and one report shape, so that the harness is the same in every language.

| Check | Passes when | Reasons for a failure |
|---|---|---|
| `exact` | The output equals the expected text after trimming, one space per run of white space, and lower-casing | `mismatch` |
| `regex` | The pattern is found anywhere in the output | `mismatch` |
| `json_field` | The whole output is one JSON object with the field and a value of the same type | `not json`, `missing field`, `mismatch` |
| `judge` | The judge replies with a bare 1 to 5 at or above the threshold (default 4) | `below threshold`, `ungradable` |

### The same checks at run time

An eval grades a set of cases once. The same checks can run on every live reply: validate the output, and when a check fails, send Claude one more request that includes the specific failure, such as "the field `label` was missing". This validate-and-retry loop (the exams call it the validation and retry pattern, and the Architect level of this course builds it) repairs format and constraint errors. It does not repair a missing fact. If the document never contained the invoice number, a second attempt can only produce a guess, so such a case should end as "not present", not as another retry. Bound the loop, log each failure reason, and the same log becomes new test cases.

## Traps

1. **A model grading itself without a rubric.** Asking the model that wrote an answer whether it is good gives a high score for almost everything. Use a different model, an anchored scale and a threshold.
2. **A grader that accepts what the application would reject.** Comparing JSON fields as text, or stripping a fence that production code does not strip, makes the eval pass and the product fail.
3. **A lenient parse of the grader's reply.** Taking the first digit in the text turns a broken grader into a plausible number. Require a bare score and count what is ungradable.
4. **One run per case.** A flaky case passes the day you run it and fails in production. Repeat the run for the cases that matter.

## Quiz

1. A team's classifier must return one of three labels, and a script has to compare it with the expected label on 2,000 cases overnight. Which grader fits?
   - **a**: A model grader that rates each answer on a scale
   - **b**: Normalised exact match with the reference answer
   - **c**: A human reviewer who reads a sample of the output
   - **d**: Cosine similarity between embeddings of both texts

2. A model that scores output replies "Score: 4, because the tone is polite" for one case, and the harness cannot parse it. How should the harness record that case?
   - **a**: As a failure with the reason that the reply was ungradable
   - **b**: As a pass, since the reply mentions a score above the threshold
   - **c**: As a skipped case that is left out of the pass rate
   - **d**: As a score of 4 read from the first digit of the text

3. A check expects the field `count` to equal the number 3, and the application's output holds the string "3". The grader compares both sides as text and passes it. What is wrong?
   - **a**: Nothing is wrong, because the digits are the same in both forms of it
   - **b**: The check should have used a model grader to judge the number
   - **c**: The type differs, and a program reading the value would reject it
   - **d**: The expected value should have been written as a string instead

<details>
<summary>Answer key</summary>

1. **b**. The page lists exact match for "tasks with clear-cut, categorical answers", and the course's rule is to "use the first one that can decide the case". *a* is ruled out because a model grader belongs to "A judgement no code can make", and a label comparison is not that. *c* is ruled out because a human grader decides "Anything, slowly", and 2,000 overnight cases need an automated grader, since the page says to "Structure questions to allow for automated grading". *d* is ruled out because similarity scores answer "Whether two texts say the same thing", which is a different question from whether two labels are equal.
2. **a**. The page says "a reply that is not a bare score counts as ungradable, and an ungradable case fails". *b* is ruled out because the contract is "Output only the number", and the page says "an ungradable case fails" whatever the prose mentions. *c* is ruled out because a skipped case would hide the grader's drift, while the course says to "Count the ungradable cases in the report". *d* is ruled out because "a lenient parse that fishes a digit out of prose will sometimes read a digit that was never the grade".
3. **c**. The page says that a grader comparing both sides as text "passes values that a program reading the field would reject". *a* is ruled out because "A structure check also compares types", and matching digits do not make matching types. *b* is ruled out because a structure check decides this by parsing, and a model grader belongs to "A judgement no code can make". *d* is ruled out because the criterion fixes the expected type, and a grader that follows the output instead of the contract would pass the very failure it exists to catch, as trap 2 describes: "Comparing JSON fields as text".

</details>
