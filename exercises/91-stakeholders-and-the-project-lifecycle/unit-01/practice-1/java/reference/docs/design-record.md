# Design record: dispute assistant

## Summary for the sponsor

We recommend a design in which the model drafts every reply and a person checks only the answers it is unsure about. It costs 80,000 a month against 315,000 for people alone, and it meets the two-second and 99.5 percent targets. Credit answers are right 63 times in 100, so a person approves every credit. We ask for one decision today: approve the pilot of this design for the billing team, with credit kept manual until the measured figures improve.

## Decision statement

The assistant helps a billing agent decide whether a disputed charge is credited. About 3,000 disputes arrive a day, and the agent waits on screen, so the answer must come within 2 seconds at the 95th percentile. A wrong credit costs about 250 and a check by a person costs 5. The billing operations manager is accountable for each credit, and the model prepares the file and does not decide.

## Options considered

| Option | Monthly cost | Meets SLA | Status | Reason |
|---|---|---|---|---|
| People decide every dispute | 315,000 | yes | rejected | The most expensive option and the slowest to answer |
| The model decides every dispute | 300,000 | no | rejected | Wrong credits cost more than the checks it removes |
| The model drafts and a person decides every item | 150,000 | yes | alternative | Safe, but it pays for checks on answers that are already right |
| Route by confidence, with a person on the rest | 80,000 | yes | recommended | The cheapest design that meets the service levels |

## Recommendation and trade-offs

We recommend routing by confidence. The break-even accuracy follows from the two costs: an item is worth a check when the expected error cost exceeds the cost of the check, and 2 percent of 250 is 5. A slice that is right at or above 98 percent is not reviewed, and any slice below it is. The design costs 80,000 a month, which is 235,000 less than people alone, and it gives up speed in the credit slice, where a person must approve. It is also exposed to confident wrong answers, which an independent check against the source reduces and does not remove.

## Accuracy by segment

| Segment | Cases | Accuracy | Failure shape | Cost per error | Handling |
|---|---|---|---|---|---|
| credit | 100 | 63 percent | wrong amount | 250 | reviewed |
| complaint | 100 | 91 percent | omitted exception | 60 | reviewed |
| status | 100 | 98 percent | outdated figure | 12 | auto |

## Service levels

| Metric | Target | Measured by | Owner |
|---|---|---|---|
| Latency, 95th percentile | 2000 ms | Trace timings of every answer, per day | Platform engineering lead |
| Availability | 99.5 percent | Share of requests answered without error, per month | Platform engineering lead |
| Accuracy by segment | Credit at least 98 percent after review | Weekly sample read by the quality team, per segment | Billing quality lead |

## Pilot to scale

| Assumption | Test | Stop trigger |
|---|---|---|
| Inputs were typical | Sample production disputes and score them by segment | Credit accuracy falls below 60 percent on the sample |
| Staff covered edge cases by hand | Count escalations per 100 disputes | More than 12 escalations per 100 disputes |
| Capacity was never close | Replay peak load against the rate limits | Any 429 error at 70 percent of the limit |
| Reviewers kept up | Compute reviewer hours from volume and routing | The review queue is older than 4 hours |

## Hand-off and monitoring

The billing operations manager is the owner of the service, and the platform engineering lead owns the runbook that explains each alert. Monitors are refusals, tokens per answer, the share of answers a person flagged and the 95th percentile latency, each compared with a baseline in both directions. Rollback sends every request to the previous model, which stays configured and tested until its own retirement date. The record is reviewed at each stage of the roll-out.
