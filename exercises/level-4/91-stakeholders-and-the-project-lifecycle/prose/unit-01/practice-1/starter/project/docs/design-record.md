# Design record: dispute assistant

## Summary for the sponsor

TODO: one paragraph a sponsor can read in a minute.

## Decision statement

The assistant helps agents with disputes.

## Options considered

| Option | Monthly cost | Meets SLA | Status | Reason |
|---|---|---|---|---|
| The model decides every dispute | 300,000 | no | recommended | It is the newest idea |
| People decide every dispute | 315,000 | yes | rejected | |

## Recommendation and trade-offs

TODO: the recommendation and what it trades.

## Accuracy by segment

| Segment | Cases | Accuracy | Failure shape | Cost per error | Handling |
|---|---|---|---|---|---|
| status | 100 | 98 percent | | 12 | reviewed |

## Service levels

| Metric | Target | Measured by | Owner |
|---|---|---|---|
| Latency, 95th percentile | 5000 ms | | TBD |

## Pilot to scale

| Assumption | Test | Stop trigger |
|---|---|---|
| Inputs were typical | | |

## Hand-off and monitoring

TODO: who owns it and how it is watched.
