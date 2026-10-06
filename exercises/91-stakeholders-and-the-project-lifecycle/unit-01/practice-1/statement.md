# Practice: a design record that a sponsor and an engineer can both use

A utility wants an assistant that helps its billing agents decide whether a disputed charge is credited. You have been the architect through discovery and design, and now you write the record that goes to the sponsor for a decision and to the engineering team for the build. The draft in `starter/docs/design-record.md` is thin and partly wrong: it recommends the option that is newest and not the one that is cheapest, it promises a latency the agents cannot live with, it lists one segment and one assumption, and it leaves the sponsor with nothing to decide. In this practice you write the record. There is no program to write and no model is called: the tests grade your Markdown against a rubric. The figures are invented for the case below, and the rubric's numbers (a break-even accuracy, a limit of 80 words for the sponsor) are this course's teaching values. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there; each language folder holds its own copy.

## The case

| Fact | Value |
|---|---|
| Disputes | About 3,000 a day |
| Who waits | A billing agent on screen: the answer must come within 2 seconds at the 95th percentile, and the service must be available at least 99.5 percent of the time |
| Cost of a wrong credit | About 250 |
| Cost of one check by a person | 5 |
| Accountable | The billing operations manager |
| Segments | credit (cost per error 250), complaint (60) and status (12) |
| Measured accuracy | credit 63 percent, complaint 91 percent, status 98 percent, on 100 cases each |
| Options priced | People decide every dispute 315,000 a month and meets the service levels; the model decides every dispute 300,000 and misses them; the model drafts and a person decides every item 150,000 and meets them; routing by confidence with a person on the rest 80,000 and meets them |

## What is already written, and what you write

The record exists as a draft with the title, the eight headings and the case table in place; there is no code to write and no function to log from, because the tests read the document. You fill the gaps below, in this order, and each unlocks the cases named:

1. **Summary for the sponsor** is a `TODO` line: write it, at most 80 words, with the monthly cost and one decision asked for (`m1` for the filled section, `e8` for the words, the cost and the ask).
2. **Decision statement** is one thin sentence: add the volume, the latency, the two costs and the accountable role (`e1`).
3. **Options considered** recommends the newest option and leaves a rejection without a reason, with two options missing: fix the status column, add the reasons and the missing options (`e2`).
4. **Recommendation and trade-offs** is a `TODO` line: state the break-even rule and the monthly cost of the recommended option (`e3`).
5. **Accuracy by segment** has one row without a failure shape: add the other two segments, the costliest first, with a shape and a handling that follows the break-even (`e5`).
6. **Service levels** promises 5000 ms with no measure and an owner of `TBD`: correct the targets and name a measure and an owner on every row (`e4`).
7. **Pilot to scale** has one assumption with no test and no trigger: complete it and add three more, each stop trigger holding a number (`e6`).
8. **Hand-off and monitoring** is a `TODO` line: name the owner, the runbook, at least two monitors and the rollback to the previous model (`e7`).

The sections below describe the whole record.

## What to write

`docs/design-record.md` has a title and these eight sections, as level 2 headings, in this order:

1. **Summary for the sponsor**: at most 80 words, in plain words, with the monthly cost of the recommended option and the one decision you ask for.
2. **Decision statement**: the volume, the latency, the cost of an error and of a check, and the accountable role.
3. **Options considered**: a table with the columns Option, Monthly cost, Meets SLA, Status and Reason. At least four options. Exactly one is `recommended`, the others are `alternative` or `rejected`, and a rejected option has a reason of at least five words. The recommended option is the cheapest one that meets the service levels.
4. **Recommendation and trade-offs**: why, what the design gives up, and the rule "at or above N percent" with the break-even accuracy that follows from the two costs. It states the monthly cost of the recommended option.
5. **Accuracy by segment**: a table with Segment, Cases, Accuracy, Failure shape, Cost per error and Handling, the costliest segment first. A failure shape is one of `wrong amount`, `outdated figure`, `refusal`, `made-up clause` or `omitted exception`. A segment at or above the break-even accuracy is `auto`, one below it is `reviewed`.
6. **Service levels**: a table with Metric, Target, Measured by and Owner, for latency, availability and accuracy. The latency target is in `ms` and no more than the case allows, the availability target is in `percent` and no less, and the accuracy target is stated for the credit segment. Every row has a named owner (a role).
7. **Pilot to scale**: a table with Assumption, Test and Stop trigger, at least four rows, every stop trigger with a number.
8. **Hand-off and monitoring**: the owner of the service, the runbook, the monitors (at least two signals), the rollback and the previous model that stays available.

No section may be left with a `TODO`, and no file holds a home path or an address other than `example.com`.

## Why each part is there, and what you should see

1. **A sponsor decides in a minute, so the summary is short and asks for something.** *You should see* a summary of at most 80 words that holds the monthly cost and the decision.
2. **A requirement is a number with an owner.** *You should see* the volume, the latency, both costs and the accountable role in the decision statement.
3. **Price comes last, among the options that qualify.** *You should see* the cheapest option that meets the service levels recommended, and a reason for every rejection.
4. **The break-even follows from two costs.** *You should see* the rule "at or above 98 percent", since 2 percent of 250 is exactly the 5 that a check costs.
5. **A headline accuracy hides the segment that costs most.** *You should see* every segment, the costliest first, with its failure shape, and a person on every segment below the break-even.
6. **A promise is a target someone owns.** *You should see* three service levels within the case's limits, each with a measure and an owner.
7. **A pilot's friendly conditions end at scale.** *You should see* four assumptions, each with a test and a number that stops the roll-out.
8. **A hand-off is what lets someone else run it.** *You should see* an owner, a runbook, monitors and a rollback to a model that is still configured.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The eight sections are present in order, each filled, with no TODO |
| `e1` | The decision statement carries the volume, the latency, the error cost, the check cost and the accountable role |
| `e2` | At least four options, one recommended and it is the cheapest that meets the service levels, a reason for each rejection |
| `e3` | The break-even accuracy is 98 percent, stated as "at or above 98 percent", and the recommendation states the cost |
| `e4` | Latency at most 2000 ms (exactly 2000 is allowed), availability at least 99.5 percent, accuracy for credit, each with a measure and a named owner |
| `e5` | Segments costliest first, a known failure shape, and handling that follows the break-even (exactly 98 percent is `auto`) |
| `e6` | At least four assumptions, each with a test and a stop trigger that holds a number |
| `e7` | The hand-off names an owner, a runbook, a rollback to the previous model and at least two monitors |
| `e8` | The sponsor summary has at most 80 words (exactly 80 is allowed), states the cost and asks for a decision, and no file holds personal data |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
