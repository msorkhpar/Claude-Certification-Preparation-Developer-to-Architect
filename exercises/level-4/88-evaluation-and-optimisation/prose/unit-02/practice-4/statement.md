# Practice: an evaluation kit

A team reports one accuracy figure for its assistant and ships whatever raises it. Two versions with the same figure are called equal although one of them fails the refunds that cost twenty times as much as a status question; a live test with a hundred cases is called a win; a version that lost three right answers in the refund segment ships because it gained three elsewhere; a wrong answer is blamed on the model before anyone checks whether the right document was retrieved; and the cheapest model is chosen without asking whether it is fast enough. In this practice you write the pieces that replace that: a report by segment with the cost of the errors, the latency percentile, the A/B verdict at 95 percent, the gate for a shadow run, the order of a diagnosis and the choice of a model under limits. The model is not called: the tests give you graded cases, counts and timings. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`segment_table`, `percentile`, `ab_verdict`, `shadow_gate`, `diagnose`, `choose_model`); TypeScript has the camel-case names (`segmentTable`, `abVerdict`, `shadowGate`, `chooseModel`); Java has the same camel-case names as static methods of `EvalKit`; Kotlin has top-level functions. Python and TypeScript use tuples and arrays: a graded case is `(segment, correct)`, a line of the report is `(segment, cases, right, percent, cost)`, a case run on both versions is `(segment, old_ok, new_ok)` and a model option is `(name, accuracy, p95, cost)`. Java and Kotlin use the records and data classes shown in the starter (`Result`, `Line`, `Paired`, `Gate`, `Option`). The function `pct` (whole percent, half up, integers only) is already written.

## What is already written, and what you write

The starter is a working kit with seven small gaps cut out of it. The grouping of the cases by segment, the counting of lost, gained and protected cases and the plumbing of every function are written, and so is `pct`. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks; a gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the lines under the failing case. Write them in this order (the Java and Kotlin names are the camel-case forms):

1. `_error_cost` unlocks `m1` and `e1`: the wrong answers times the segment's cost, 1 when it has none.
2. `_order` unlocks `m1` and `e1`: the costliest segment first, ties by name.
3. `percentile` unlocks `e2`: the nearest-rank value of unsorted input.
4. `ab_verdict` unlocks `e3` and `e4`: the minimum size, the degenerate pools and the 95 percent test.
5. `_decision` unlocks `e5`: ship or hold from the protected losses, the losses and the gains.
6. `diagnose` unlocks `e6`: the order of the questions.
7. `choose_model` unlocks `e7`: the cheapest option inside both limits.

About twenty lines in all. The sections below describe the whole kit.

## What to write

- `segment_table(results, costs)` groups the graded cases by segment and returns one line per segment: its cases, its right answers, `pct` of them, and the cost of its errors, which is the number of wrong answers times the segment's entry in `costs` (1 when the segment has no entry). The lines are ordered by cost, the highest first, and by segment name when costs are equal. No results give no lines.
- `percentile(values, p)` is the nearest-rank percentile: sort the values and take the one at rank `ceil(p * n / 100)`, counting from 1. The values may arrive in any order. No values give 0.
- `ab_verdict(x1, n1, x2, n2, min_n=200)` compares the old version (`x1` right of `n1`) with the new one (`x2` of `n2`). If either arm has fewer than `min_n` cases the verdict is `too few cases`. If the pooled number right is 0 or is all of them, it is `no clear difference`. Otherwise it is a two-proportion test at 95 percent, with D = `x2*n1 - x1*n2`, N = `n1 + n2` and X = `x1 + x2`: the difference is clear when `D*D*N*10000` is at least `38416*n1*n2*X*(N-X)` (the square of 1.96, scaled so that only integers are used). A clear difference is `new is better` when D is positive and `old is better` otherwise; anything else is `no clear difference`.
- `shadow_gate(pairs, protected)` takes the cases run on both versions. A case is lost when the old version was right and the new one was not, and gained in the opposite case. Return the decision, the number lost, the number gained and the sorted, distinct protected segments that lost a case. The decision is `hold` when any protected segment lost a case or more cases were lost than gained, and `ship` otherwise.
- `diagnose(found, supported, format_ok, passes_on_stronger)` says where to look first for a wrong answer: `retrieval or data` when the evidence was not found, then `ungrounded answer` when the answer claims what the evidence does not support, then `format instructions` when the reply has the wrong shape, then `prompt or task` when the case fails on a stronger model too, and `model mismatch` otherwise.
- `choose_model(options, min_accuracy, max_p95)` returns the name of the cheapest option whose accuracy is at least `min_accuracy` and whose p95 latency is at most `max_p95`; equal costs are broken by name. When no option qualifies the result is `none`.

## Why each part is there, and what you should see

1. **Report by segment.** *You should see* refunds at the top with the largest error cost, while the overall figure would have looked fine.
2. **A default for the unknown.** *You should see* a segment with no price still cost something per error, and an empty set give an empty report.
3. **Tails, not means.** *You should see* the 95th percentile of four timings be the slowest one, whatever order they arrived in.
4. **Enough cases first.** *You should see* a test with 199 cases in one arm decide nothing.
5. **A difference that is not chance.** *You should see* a gap of 28 right answers in 500 cases each called clear and a gap of 21 not called, and both directions named.
6. **A gate with two keys.** *You should see* a version held for a protected regression even when its gains match its losses, and held for a net loss in segments that are not protected.
7. **A diagnosis order.** *You should see* a case with no evidence sent to retrieval before anybody tunes a prompt.
8. **Two limits, then the price.** *You should see* the cheapest model that is both accurate and fast enough, and `none` when nothing is.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A segment table reports accuracy and error cost with the costliest segment first |
| `e1` | A segment with no cost entry costs one per error and no results give an empty table |
| `e2` | A percentile uses the nearest rank and does not need sorted input |
| `e3` | A test with fewer cases than the minimum in either arm decides nothing |
| `e4` | A difference is called only when it clears the 95 percent bar and the better side is named |
| `e5` | A shadow run is held for a regression in a protected segment or for more losses than gains |
| `e6` | Diagnosis checks the evidence, then the grounding, then the format, then the stronger model |
| `e7` | Model choice takes the cheapest option that meets the accuracy floor and the latency limit |
