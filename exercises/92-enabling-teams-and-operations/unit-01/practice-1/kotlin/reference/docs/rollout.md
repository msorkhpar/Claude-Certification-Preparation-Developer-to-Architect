# Rollout plan

## Precedence

When a key is set in more than one place, the highest level wins.

| Level | Source | Who controls it |
|---|---|---|
| 1 | Managed settings | The organisation |
| 2 | Command line | The developer, for one session |
| 3 | Project local | The developer, for one project |
| 4 | Shared project | The team, in version control |
| 5 | User | The developer |

Server-managed settings apply to everyone in the organisation, and per-group policy is not yet supported there. To give one group a different policy, deploy a different file or profile to that group.

## Spend limits

Usage credits are on, so members can continue past their seat allowance, and limits are set at three levels. The group limits add up to the organisation limit and no more.

| Level | Name | Monthly limit |
|---|---|---|
| organization | all members | 20000 |
| group | Platform | 6000 |
| group | Product | 8000 |
| group | Data | 6000 |
| member | default | 500 |

## Adoption

Baseline: 4 weeks of data taken before the rollout.

Outcome targets:
- Share of merged pull requests with Claude Code assistance
- Time to merge for pull requests

Reported, not targeted: lines accepted and suggestions accepted, because they measure activity and not a result.
