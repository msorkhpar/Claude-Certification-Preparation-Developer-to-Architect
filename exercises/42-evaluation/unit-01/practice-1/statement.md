# Practice: an eval harness

An eval answers one question before you ship a change to a prompt, a model or a setting: did the application get better or worse, on the
cases that matter? The harness is the code that asks it the same way every time. It runs each test case through your application, grades the
output with a check that can be automated, adds the grades up by the dimension the success criteria name, and compares one run with the
last so that a regression cannot hide behind a better average. In this practice you write that harness. Nothing here calls a model or the
network: the application under test and the model-graded judge are plain functions that the tests pass in, so every run is exact. Pick
your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there.

## The given parts

The Java and Kotlin folders give you `Json` (parse text into maps, lists, strings, numbers, booleans and null, and write them back).
Python and TypeScript have JSON built in. A case, a report and every result are plain data: a map with string keys in every language.
Integers read from JSON are `Long` in Java and Kotlin; your code must accept any `Number` where a number is read.

## What to write

Names are written in Python style; TypeScript uses camelCase (`runEval`). Java and Kotlin put the functions on `Harness`
(`Harness.grade`, `Harness.runEval`, `Harness.meets`, `Harness.compare`) and take the model and the judge as functions from text to text
(`Function<String, String>` in Java, `(String) -> String` in Kotlin; the judge may be null).

A **case** is `{"id", "input", "tags": [...] (optional), "check": {...}}`. The check is one of four kinds:

- `{"type": "exact", "expected": text}`: the output equals the expected text after both are trimmed, every run of white space is one
  space, and both are lower-cased.
- `{"type": "regex", "pattern": text}`: the pattern is found anywhere in the output (a search, not a whole-text match).
- `{"type": "json_field", "field": name, "equals": value}`: the whole output, apart from surrounding white space, is one JSON object that
  has the field, and the field's value equals `equals` **with the same JSON type** (the string `"3"` is not the number `3`, and `1` is not
  `true`). The value is a string, an integer or a boolean.
- `{"type": "judge", "criterion": text, "threshold": integer (optional, default 4)}`: a model-graded check.

`grade(case, output, judge)` returns `{"passed", "reason"}`. The reason is `ok` when the case passed; otherwise `mismatch` (exact, regex, or
a field with another value), `not json` (the output is not a JSON object), `missing field`, `below threshold` or `ungradable`. A judge
check also returns `"score"` (the integer, or null when there is none).

- The judge is called with exactly this prompt, with the criterion and the output filled in, and its lines separated by `\n`:
  `Rate this response on a scale of 1-5 for <criterion>:`, `<response><output></response>`, `1: Not at all <criterion>`,
  `5: Perfectly <criterion>`, `Output only the number.`
- The judge's reply, once trimmed, must be one digit from 1 to 5. Anything else, a reply that raises an error, or no judge at all is
  `ungradable` and the case fails. A score at or above the threshold passes; a lower one is `below threshold`.
- Exact, regex and json checks never call the judge.

`run_eval(cases, model, judge=None, repeats=1)` runs every case, in order, `repeats` times through `model(case["input"])` and grades each
output with `grade`. A run in which the model raises an error is a failed run with the reason `model error`; the other cases still run.
A case **passes only if every one of its runs passes**; its reason is `ok`, or the reason of its first failed run. A case is **flaky** when
some of its runs passed and some failed. The report is
`{"total", "passed", "pass_rate", "results": [{"id", "passed", "reason", "flaky"}], "by_tag": {tag: {"passed", "total"}}, "flaky": [ids]}`:
`pass_rate` is `passed / total` (0.0 with no cases), `by_tag` counts every case under each of its tags (tags in order of first
appearance), and `flaky` lists the flaky case ids in order.

`meets(report, criteria)` checks a report against success criteria and returns `{"met", "failures"}`. The criteria are
`{"min_pass_rate": number, "tags": {tag: minimum rate}, "max_flaky": integer}`, and each key is optional. A criterion that is missing is not
checked. Failures are listed in this order: `overall` when the pass rate is below `min_pass_rate`; `tag:<name>` for each tag, in the
criteria's order, whose rate (`passed / total`) is below its minimum or that no case carries; `flaky` when more than `max_flaky` cases are
flaky. A rate equal to its minimum passes. `met` is true when there are no failures.

`compare(baseline, current)` compares two reports by case id and returns `{"regressions", "fixed", "added", "removed",
"pass_rate_delta", "ok"}`: the ids that passed in the baseline and fail now, the ids that failed and pass now, the ids only in the current
report, and the ids only in the baseline (each in report order, regressions, fixed and added in the current report's order and removed in
the baseline's); the current pass rate minus the baseline's; and `ok`, which is true only when there are no regressions **and** no removed
cases. A better average never excuses a regression, and a case that vanished from the set is not a pass.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A run grades each case with its own check (exact, regex), reports the pass rate and the order of the results, and an empty set has rate 0.0 |
| `e1` | An exact check ignores case and spacing only: a longer word, a different word and an empty output fail; a regex is a search, and an anchored one still fails on extra text |
| `e2` | A json field check: a JSON object, the field present, the same typed value; a fenced block, prose around the JSON and an array are `not json` |
| `e3` | A judge check: the exact prompt, a bare score at the threshold passes, a lower score fails, and a reply that is not a bare score, no judge or a judge that raises is `ungradable` |
| `e4` | A model error fails its own case with `model error` and the run goes on |
| `e5` | Tags report their own pass rates; `meets` judges the overall rate, each tag (a missing tag fails) and the flaky count, and a rate equal to its minimum passes |
| `e6` | `compare` names regressions, fixes, added and removed cases; a better average with a regression is not ok; a removed case is not ok |
| `e7` | Repeated runs: a case passes only if every run passes, a mixed case is flaky, a consistently failing case is not flaky, and the model is called once per run |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
