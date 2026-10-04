# Practice: a review specification that cuts false positives

A review job in CI posts too many comments, and developers have stopped reading them. The prompt says "be conservative", its examples are all findings, and nobody
knows which category is the noisy one. In this practice you write the three decisions behind a precise review: the function that builds the prompt from a
specification and refuses a specification that is vague or incomplete, the report that computes how much developers trust each category and which patterns
they dismiss, and the function that decides what a run does with an incomplete request. The model is not called: the tests give you specifications,
verdicts and requests. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`build_review_prompt`, `category_report`, `next_step`); TypeScript has `buildReviewPrompt`, `categoryReport` and `nextStep`; Java has the same
camel-case names as static methods of `ReviewSpec` and Kotlin has top-level functions. Results are maps and lists, as the starters show. A refusal is `ValueError`
(TypeScript: an `Error`; Java and Kotlin: `IllegalArgumentException`).

## What to write

- `build_review_prompt(spec, diff)`: `spec` has `criteria` (a list of `{id, report, skip, severity: {high, low}}`) and `examples` (a list of `{verdict, code, reason}`, a
  `report` example also has a `category` that is the `id` of a criterion).
  - Refuse the specification when there are no criteria; when a `report` or `skip` text is empty or blank; when either text holds a phrase that names no pattern
    (`VAGUE` in the starter, matched without regard to case) and the message says `vague`; or when `severity.high` or `severity.low` is empty or blank.
  - Refuse the examples when there are fewer than two or more than four, when the verdicts are not exactly a report and a skip (a set of one kind, or any other value, is refused),
    when an example has no reason, or when a report example names a category that is not a criterion.
  - The prompt has this exact shape, with one `criterion` block per criterion and one `example` block per example, in the order given, and the diff last:

    ```text
    <criteria>
    <criterion id="bug">
    Report: ...
    Skip: ...
    Severity high: ...
    Severity low: ...
    </criterion>
    </criteria>
    <examples>
    <example verdict="report" category="bug">
    <code>...</code>
    <reason>...</reason>
    </example>
    <example verdict="skip">
    <code>...</code>
    <reason>...</reason>
    </example>
    </examples>
    <diff>
    ...
    </diff>
    ```
- `category_report(findings, min_reviewed=5, min_precision=0.5)`: each finding is `{category, verdict, detected_pattern}` where the verdict is `accepted` or `dismissed`. Return
  `{"categories": {name: {"reviewed", "precision", "disable", "top_dismissed"}}, "disable": [names]}`.
  - `precision` is the accepted share, rounded to two decimals.
  - `disable` is true when the category has at least `min_reviewed` findings and a precision below `min_precision` (at the boundaries it stays enabled); the list
    of names to disable is sorted.
  - `top_dismissed` lists at most three `[pattern, count]` pairs of dismissed findings, by count (largest first) and then by name; accepted findings do not count.
- `next_step(request, required, defaults, attended)`: a field is missing when it is absent, `None` or blank. Return `{"action", "ask", "assumptions"}`.
  - Every missing field that has a default is assumed (listed in `assumptions` with its default); the others are unresolved.
  - Nothing unresolved: `proceed`. Unresolved and `attended`: `ask`, naming only the unresolved fields (in the order of `required`). Unresolved and not attended: `stop`, which
    never asks.

## Why each part is there, and what you should see

1. **Explicit criteria.** A report text, a skip text and a severity example at each level are what move precision; "be conservative" is not one. *You should see* five vague
   phrasings refused in both texts, and each missing part refused.
2. **Examples on the border.** Two to four examples with a report and a skip, each with a reason. *You should see* sets of one, five, one kind or without a reason refused.
3. **Order.** Criteria, then examples, then the diff last. *You should see* the exact prompt above.
4. **Trust by category.** A noisy category is switched off while its prompt is improved, and only with enough reviews to mean it. *You should see* `style` disabled at
   six reviews and 33 percent, `naming` kept at four reviews, and the boundary cases kept.
5. **Dismissed patterns.** The pattern behind each dismissal shows what to fix. *You should see* the three most dismissed patterns, ties broken by name.
6. **Attended or not.** A person can be asked; a CI run cannot. *You should see* `ask` for a person, `stop` for CI, and defaults assumed and stated in both.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The prompt has criteria first, then examples, then the diff last, in the exact shape above |
| `e1` | Vague wording is refused in both the report and the skip text, with `vague` in the message |
| `e2` | A criterion needs a report, a skip and a severity example for high and low |
| `e3` | Two to four examples with a report and a skip, each with a reason and a known category |
| `e4` | A category with enough reviews and low precision is disabled, and the boundaries hold |
| `e5` | The most dismissed patterns are listed by count and then name, capped at three |
| `e6` | An attended run asks only what it cannot assume and states its assumptions |
| `e7` | An unattended run never asks: it states assumptions or stops |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
