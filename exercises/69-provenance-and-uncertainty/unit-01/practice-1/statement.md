# Practice: a ledger of claims that keeps its sources

A research system sends three subagents to read different sources, and a synthesis agent writes the report. In the draft, a market-size figure appears without a source, two reports that disagree were
averaged into a number nobody published, and a figure from 2022 is shown as if it contradicted one from 2024. In this practice you write the ledger that prevents those three failures: a finding that
must carry its source and its date, a merge that records agreement, change over time and real conflict without choosing a winner, a coverage note that says what is well supported and what is missing, and
the rendering of each kind of content in a fitting form. The model is not called. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`check_finding`, `merge`, `coverage_note`, `render`); TypeScript has the camel-case names; Java has the same camel-case names as static methods of `Ledger` with the records the
starter defines (`Finding`, `Src`, `Val`, `Entry`, `Gap`, `Coverage`); Kotlin has top-level functions and data classes. Dates are ISO text (`2024-05-01`) and compare as text.

## What is already written, and what you write

The starter is a working ledger of claims with eight gaps cut out of it. Everything that is plumbing is written and correct: the grouping of findings by claim and by value, the refusal of a finding that fails the check, the first rows of the coverage note, the table and prose headers and the refusal of an unknown content type. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The required fields (unlocks `e1`): a finding is missing a field of REQUIRED (`claim`, `value`, `source`, `date`) when it is absent, empty or only blanks; the check returns the names of the missing fields in that order.
2. Every source once (unlocks `m1`): a claim's value keeps each pair of source and date once, even when two findings repeat it, in the order they came.
3. Agreed, changed or in conflict (unlocks `e2`, `e3`): one value is `agreed`; several values with two of them from the same date are a `conflict` that keeps both; several values from different dates are a `changed` claim, ordered by the earliest date of each value.
4. The well supported claims (unlocks `e4`): an agreed claim whose value has two or more different sources is well supported; one with a single source is listed apart as `single_source`; changed and contested claims have their own lists.
5. The gaps (unlocks `e5`): a planned claim that has no finding is a gap, with the reason given for it or `no source found`.
6. The financial table (unlocks `e6`): financial data is a table: the header `| Source | Date | Value |`, the separator `|---|---|---|` and one row per source with its value.
7. The technical list (unlocks `e8`): technical findings are a list: the claim and a colon, then one `- value (source, date)` line per source.
8. The disagreement in the news (unlocks `e7`): news is prose; when the sources disagree it ends with `The sources disagree.`, and when the figures come from different dates with `The figures are from different dates.`.

`m1` needs gap 2. About twenty-two lines in all. The steps below describe the whole ledger, so you can see how your gaps are used.

## What to write

- A finding is `{claim, value, source, date}`. `check_finding(finding)` returns the names of the required fields that are missing or blank (only spaces counts as blank), in the order `claim`, `value`,
  `source`, `date`; an empty list means the finding is complete.
- `merge(findings)` refuses (raises `ValueError` in Python, an `Error` in TypeScript, `IllegalArgumentException` in Java and Kotlin) when any finding is incomplete. Otherwise it returns one entry
  `{claim, status, values}` per claim, in the order the claims first appear. `values` lists the distinct values of the claim in the order they first appear, each as `{value, sources}`, where `sources`
  lists the distinct `{source, date}` pairs of that value in order. The `status` is `agreed` when the claim has one value; `conflict` when two findings of the claim have different values **and the same
  date**; otherwise `changed`. For a `changed` claim the values are listed from the one with the earliest date to the latest (by the earliest date among its sources). Values are never dropped, merged or
  averaged.
- `coverage_note(planned, merged, unavailable)` returns `{well_supported, single_source, changed, contested, gaps}`. For the merged entries, in their order: `conflict` goes to `contested`, `changed` to
  `changed`, and an `agreed` claim goes to `well_supported` when its value has at least two distinct source names, otherwise to `single_source`. `gaps` lists, in the order of `planned`, each planned
  claim that has no entry as `{claim, reason}`, where the reason is `unavailable[claim]` or `no source found`.
- `render(entry, kind)` returns text for a merged entry, one row for each source of each value in order. `kind` is `financial`, `news` or `technical`; any other is refused.
  - `financial`: a table. The lines `| Source | Date | Value |` and `|---|---|---|`, then `| <source> | <date> | <value> |` for each row.
  - `technical`: the line `<claim>:` followed by `- <value> (<source>, <date>)` for each row.
  - `news`: one line of prose, `<claim>: ` followed by `<value> (<source>, <date>)` for each row joined with `; `, then a full stop. A `conflict` adds ` The sources disagree.` and a `changed` entry adds
    ` The figures are from different dates.`

## Why each part is there, and what you should see

1. **Provenance is a required field.** A claim without a source cannot be checked, and a claim without a date cannot be interpreted. *You should see* the missing fields named, and the merge refusing the finding.
2. **Keep every source.** Agreement from two sources is stronger than one, and the reader may want either. *You should see* every distinct source of every value in the merged entry.
3. **Annotate a conflict; do not choose.** Two credible sources that disagree on the same date are a finding about the topic. *You should see* both values with their sources and the status `conflict`.
4. **A date can explain a difference.** A figure from 2022 and one from 2024 are not in conflict. *You should see* `changed`, with the older value first.
5. **Say what the report does not cover.** A gap with a reason is information. *You should see* the planned claims with no finding listed with their reason, and single-source claims set apart.
6. **Fit the form to the content.** Numbers compare in a table, events read as prose, technical findings list well. *You should see* three different shapes from the same entry.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Findings are merged per claim with every source kept once |
| `e1` | A finding without a source or a date is refused |
| `e2` | Two values from the same date are a conflict that keeps both |
| `e3` | Two values from different dates are a change, not a conflict |
| `e4` | The coverage note separates what is well supported from what is not |
| `e5` | A planned claim with no finding is a gap with its reason |
| `e6` | Financial data is rendered as a table |
| `e7` | News is rendered as prose that says when sources disagree |
| `e8` | Technical findings are a list, and an unknown content type is refused |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
