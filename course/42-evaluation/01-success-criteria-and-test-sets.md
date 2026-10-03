# Success criteria and test sets

**Level:** Developer · **Module 42:** Evaluation · **Page 1 of 3**
**Exams:** DV8; A4.4

**After this page you can** turn "it should work well" into success criteria that are specific, measurable, achievable and relevant, split them into dimensions that can each be checked, and build a test set that mirrors the real task, covers its edge cases and can be graded automatically.

Checked on 2026-10-03 against the Claude API documentation pages "Define success criteria and build evaluations" and "Prompt engineering overview". The case file shown is the course's own format, the one the practice at the end of the module reads. Nothing on this page calls a model.

## Why it matters

A prompt, a model id or a setting changes, and somebody says "it feels better". The exam scenarios in the evaluation domain all start one step earlier: how would you know? The answer is a set of criteria written before the first prompt is tried and a set of test cases that can be run again in minutes. Without them every change is an opinion, and a regression is found by a customer.

## The idea

### Criteria first

The prompt engineering overview opens with what it assumes you already have: "A clear definition of the success criteria for your use case", "Some ways to empirically test against those criteria" and "A first draft prompt you want to improve". The first two come before the third. The same page adds a warning: "Not every success criteria or failing eval is best solved by prompt engineering." Its example is that you can sometimes improve latency and cost more easily by selecting a different model. An eval tells you what is failing; it does not say that the prompt is the cure.

### Four traits of a good criterion

The success criteria page gives four, and each is a test you can apply to a draft.

| Trait | The page says | A draft fails it when |
|---|---|---|
| Specific | "Clearly define what you want to achieve." | It says "good performance" where it could say "accurate sentiment classification" |
| Measurable | "Use quantitative metrics or well-defined qualitative scales." | Nobody can say what number or scale a result would have |
| Achievable | "Base your targets on industry benchmarks, prior experiments, AI research, or expert knowledge." | The target is beyond what current frontier models can do |
| Relevant | "Align your criteria with your application's purpose and user needs." | It protects something the product does not need, such as strict citation accuracy in a casual chatbot |

The page's own contrast is a bad criterion, "The model should classify sentiments well", and a good one: "The sentiment analysis model should achieve an F1 score of at least 0.85 (Measurable, Specific) on a held-out test set* of 10,000 diverse Twitter posts (Relevant), which is a 5% improvement over the current baseline (Achievable)." Note the parts: a metric, a threshold, a data set, and a baseline to beat.

### Common criteria, and how each is checked

The page lists eight common criteria. The course adds the column that matters for the rest of the module: what kind of grader can check it.

| Criterion | The page asks | Usually graded by |
|---|---|---|
| Task fidelity | "How well does the model need to perform on the task?", including edge cases | Code against an expected answer |
| Consistency | "How similar do the model's responses need to be for similar types of input?" | Repeated runs, or a similarity score |
| Relevance and coherence | "How well does the model directly address the user's questions or instructions?" | A model grader with a rubric |
| Tone and style | "How well does the model's output style match expectations?" | A model grader on a scale |
| Privacy preservation | "What is a successful metric for how the model handles personal or sensitive information?" | Code (a pattern that must not appear) and a model classifier |
| Context utilization | "How effectively does the model use provided context?" | A model grader on a scale |
| Latency | "What is the acceptable response time for the model?" | Code (a timer) |
| Price | "What is your budget for running the model?" | Code (token usage times price) |

The last column is the course's reading, not the page's. The point is that two of the eight, latency and price, never need a model, and the first needs one only when the answer is not a fixed label.

### Several dimensions at once

"Most use cases need multidimensional evaluation along several success criteria." The page's example puts four numbers on one test set: an F1 score of at least 0.85, "99.5% of outputs are non-toxic", "90% of errors would cause inconvenience, not egregious error" and "95% response time < 200ms". The consequence for the harness is that a run is not one number. It reports each dimension, and a change ships only if every dimension holds. An average can rise while a dimension that matters falls, which page 3 shows with a run.

### A test set that mirrors the task

The test page gives three principles for building evals:

1. **Be task-specific.** "Design evals that mirror your real-world task distribution. Don't forget to factor in edge cases!" Its list of edge cases: irrelevant or nonexistent input data, overly long input data or user input, for chat use cases poor, harmful or irrelevant user input, and ambiguous test cases "where even humans would find it hard to reach an assessment consensus".
2. **Automate when possible.** "Structure questions to allow for automated grading (for example, multiple-choice, string match, code-graded, LLM-graded)."
3. **Prioritize volume over quality.** "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals."

The third is the one engineers resist. A hundred cases graded by a script, run on every change, find more regressions than ten cases that a person grades carefully once.

Where do cases come from? The pages do not say; the course's advice follows from the principles. Sample real inputs, with personal data removed first (module 41). Add a case for every bug that reached production, so the same bug cannot return unseen. Write the awkward inputs by hand: an empty input, a very long one, the wrong language, a request that tries to change the instructions. Give each case a stable id and one or more tags (`core`, `edge`, a language, a customer segment), because the tags are how a run reports by dimension.

Keep a slice of the set aside. The page's example speaks of a "held-out test set": data you did not tune on. A prompt refined against all of the cases will do well on them, and the score then says nothing about new input. Page 3 returns to this.

### The shape of a case

The practice at the end of the module reads cases in this form. It is the course's own format, written as JSON so that every language reads it.

```json
{"id": "sarcasm-1", "input": "Oh great, another crash", "tags": ["edge"],
 "check": {"type": "exact", "expected": "negative"}}
```

A case names its input, its tags and one check. The check says how the output is graded, and page 2 covers the kinds: an exact label, a pattern, a field in a JSON object, and a score from a model grader.

### A criterion rewritten

<!-- illustrative -->
```text
Before:  "Ticket summaries should be good."
After:   Summaries of the 60-ticket set are at most 80 words and name the product (code-graded, 100 percent);
         a model grader rates at least 90 percent of them 4 or 5 for accuracy on a 1-to-5 scale;
         the p95 response time is under 6 seconds; no summary contains a card number (a pattern, 100 percent);
         the 10 cases tagged edge (empty thread, two languages, a pasted log) pass at 80 percent or more.
```
<!-- /illustrative -->

The wording is hand-written for this page. Each clause names a data set, a metric and a threshold, and the edge cases get their own line so that the average cannot hide them.

## Traps

1. **A criterion nobody can measure.** "Good", "accurate" and "natural" are wishes until they carry a number or a scale and a data set.
2. **One average.** A single pass rate lets an improvement in the common case pay for a failure in the rare one. Report by tag and set a threshold for each.
3. **A set made of the cases you tuned on.** The score is then a memory test. Keep cases the prompt has never been shaped by.
4. **Grading only what is easy to grade.** A set of exact-match questions is cheap, but if the real task is open-ended writing, the set measures a different task.

## Quiz

1. A team's goal reads "the extraction model should work well". Which rewrite meets the documentation's four traits of a good criterion?
   - **a**: Raise the field-level F1 above 0.9 on 500 held-out invoices, beating today's 0.86
   - **b**: Make the model as accurate as an expert human reviewer on every invoice
   - **c**: Improve extraction until reviewers feel the output is clearly better
   - **d**: Reach full accuracy on all fields so that no invoice ever needs a manual check

2. A support bot's eval set contains 40 polite, well-formed questions. Production shows empty messages, pasted logs and abusive input. What is the best change to the set?
   - **a**: Replace the questions with a smaller group graded by hand
   - **b**: Add tagged cases for those awkward situations, sampled from live traffic
   - **c**: Keep the set unchanged and raise the passing threshold
   - **d**: Ask the model to write more polite questions of the same kind

3. A prompt is refined for a week against a fixed set of 200 examples, and the pass rate climbs from 71 to 96 percent. The launch review wants to know whether 96 percent will hold on new traffic. Which evidence answers that?
   - **a**: The same fixed set rerun with a stricter grader
   - **b**: A longer list of the prompt changes made during the week
   - **c**: A fresh slice of cases that played no part in any tuning
   - **d**: The pass rate on the same set averaged over five runs

<details>
<summary>Answer key</summary>

1. **a**. It names a metric (F1), a threshold above the current baseline, and a data set the model was not tuned on, which are the parts of the page's good example: "a held-out test set* of 10,000 diverse Twitter posts (Relevant), which is a 5% improvement over the current baseline (Achievable)". *b* is ruled out because a target fails the Achievable trait when "The target is beyond what current frontier models can do", and this one names no metric. *c* is ruled out because a feeling has no number, while a criterion must "Use quantitative metrics or well-defined qualitative scales". *d* is ruled out because full accuracy on every field ignores "Base your targets on industry benchmarks, prior experiments, AI research, or expert knowledge".
2. **b**. The first principle is to "Design evals that mirror your real-world task distribution", and its list of edge cases includes irrelevant or nonexistent input data and "Poor, harmful, or irrelevant user input". *a* is ruled out because the page says "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals". *c* is ruled out because a stricter threshold on the same easy cases still measures a different task and, in the page's words, "Don't forget to factor in edge cases!". *d* is ruled out because the course's advice is to "Write the awkward inputs by hand", and more polite questions add none.
3. **c**. The page's example speaks of a "held-out test set", which is "data you did not tune on", and only that can say how the prompt will do on new input. *a* is ruled out because a stricter grader on the same set still scores the cases the prompt was shaped by, and the trap says "Keep cases the prompt has never been shaped by". *b* is ruled out because a list of changes records effort and carries no score, while the page asks for "Some ways to empirically test against those criteria". *d* is ruled out because repeating the tuned set only averages out noise, and the trap says "The score is then a memory test".

</details>
