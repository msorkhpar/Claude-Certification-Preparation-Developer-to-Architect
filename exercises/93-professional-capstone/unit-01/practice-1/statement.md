# Practice: a launch review

A team brings a design to the architecture board: a claims assistant that reads policy documents, answers customers and passes the costly decisions to a person. The board does not read the whole design again each time. It runs a review that turns the design into findings, groups them by the seven domains of the Professional exam and returns one verdict. In this practice you write that review. The model is not called: the tests give you a design as a set of facts (flags) and a set of numbers, and you return findings. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`launch_review`, `verdict`, `scorecard`, `needed_accuracy`); TypeScript has the camel-case names (`launchReview`, `neededAccuracy`); Java has the same camel-case names as static methods of `LaunchReview`; Kotlin has top-level functions. The flags are a set of strings and the numbers are a map from a name to a whole number; a missing number counts as 0.

## What is already written, and what you write

The starter is a working review with eight gaps cut out of it. The plumbing is written: the `add` function that records a finding, the call of every domain's rules in turn, and the rules of the domains P2, P6 and P7. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks; a gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `_p1_rules` unlocks `e1`, `e4` and `e7`: the three rules of domain P1.
2. `_p3_rules` unlocks `e4` and `e8`: the four rules of domain P3.
3. `_p4_rules` unlocks `e1`, `e3` and `e4`: the four rules of domain P4.
4. `_p5_rules` unlocks `e4`, `e7` and `e8`: the five rules of domain P5.
5. `_order` unlocks `e3`: severity, then domain, then rule id.
6. `verdict` unlocks `e1` and `e2`: reject, revise or approve.
7. `scorecard` unlocks `e5`: the seven domain counts.
8. `needed_accuracy` unlocks `m1` and `e6`: the break-even accuracy, rounded up on the cost side.

About twenty lines in all. The sections below describe the whole review.

## What to write

- `launch_review(flags, numbers)` returns the findings, each a string `<severity> <domain> <rule>` such as `high P1 missing-feedback`. The findings run from high to medium to low, then by domain (P1 to P7), then by rule id in alphabetical order. The 22 rules are in the table below.
- `verdict(findings)` returns `reject` when any finding is high, `revise` when none is high and any is medium, and `approve` otherwise, so a design with only low findings is approved.
- `scorecard(findings)` returns a list of seven counts, the findings of P1, P2, P3, P4, P5, P6 and P7 in that order.
- `needed_accuracy(error_cost, review_cost)` returns the accuracy in whole percent above which a design should let answers go out without a check: 100 minus the review cost as a percent of the error cost, with that percent rounded up, and never below 0. It returns 0 when an error costs nothing or less.

## The rules

| Domain | Rule id | Severity | Fires when |
|---|---|---|---|
| P1 | `missing-feedback` | high | the flag `feedback_loop` is absent |
| P1 | `autonomy-without-need` | medium | `agent` or `team` is present and `path_known` is present |
| P1 | `team-below-price` | medium | `team` is present and `team_value_chats` is below 15 |
| P2 | `volatile-prefix` | medium | `volatile_prefix` is present |
| P2 | `model-not-measured` | low | `model_measured` is absent |
| P3 | `filter-after-ranking` | high | `filter_after_ranking` is present |
| P3 | `stale-index` | high | `replace_on_change` is absent |
| P3 | `tool-bloat` | medium | `tool_tokens` is above 10000 and `deferral` is absent |
| P3 | `agent-rights-only` | high | `agent_rights_only` is present |
| P4 | `no-protected-segment` | medium | `protected_segment` is absent |
| P4 | `small-eval-set` | low | `eval_cases` is below 20 |
| P4 | `no-way-back` | high | `rollback` is absent |
| P4 | `big-bang-rollout` | medium | `rollout_stages` is below 3 |
| P5 | `identifiers-reach-model` | high | `pii_reaches_model` is present |
| P5 | `residency-unmet` | high | `residency_unmet` is present |
| P5 | `audit-keeps-content` | medium | `audit_keeps_content` is present |
| P5 | `irreversible-without-person` | high | `irreversible_action` is present and `human_step` is absent |
| P5 | `retention-outside-window` | medium | `retain_days` is below `floor_days` or above `ceiling_days` |
| P6 | `no-accountable-owner` | medium | `owner` is absent |
| P6 | `sla-without-numbers` | medium | `latency_ms` is 0 or less, or `availability_tenths` is 0 or less |
| P6 | `accuracy-unstated` | low | `accuracy_stated` is absent |
| P7 | `unmanaged-team-settings` | medium | `team_size` is above 10 and `managed_settings` is absent |

## Why each part is there, and what you should see

1. **A design at the limits.** *You should see* a design that sits exactly on every threshold get no findings and an approval.
2. **High findings.** *You should see* one missing feedback loop, one filter after ranking or one missing way back reject the design.
3. **The verdict ladder.** *You should see* a medium finding ask for a revision and low findings alone pass.
4. **A stable order.** *You should see* severity first, then domain, then rule id, so two reviews of the same design read the same.
5. **Thresholds with an edge.** *You should see* each number pass exactly at its value and fail one step beyond it.
6. **The scorecard.** *You should see* the seven counts, with zero for a domain that has no finding.
7. **The accuracy a design needs.** *You should see* 98 for an error that costs 250 against a check that costs 5, 66 when three is the error cost and one the check, and 0 when a check costs as much as an error.
8. **Rules with a second fact.** *You should see* a rule stay quiet when its second fact is missing.
9. **Every flaw at once.** *You should see* all 22 findings for a design that breaks every rule.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A design that sits exactly at every threshold has no findings and is approved |
| `e1` | A missing feedback loop, a filter after ranking or no way back is a high finding and the design is rejected |
| `e2` | Medium findings revise the design and low findings alone approve it |
| `e3` | Findings are ordered by severity, then domain, then rule id |
| `e4` | Each numeric threshold passes exactly at its value and fails one step beyond |
| `e5` | The scorecard counts the findings of each domain from P1 to P7 |
| `e6` | The accuracy a design needs is the break-even rounded up from the two costs |
| `e7` | Rules that depend on a second fact fire only when both hold |
| `e8` | Retrieval and privacy flaws are high findings in domains P3 and P5 |
| `e9` | A design with every flaw gets all 22 findings and a scorecard that adds up |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
