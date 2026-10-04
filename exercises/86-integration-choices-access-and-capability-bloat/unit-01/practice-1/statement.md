# Practice: a capability review and a gateway

A platform team gives every agent every tool, lets all of them load at once, connects capabilities by whatever was quickest, checks only the user's rights when a tool is called, and has no single place that sees the traffic. In this practice you write the pieces that replace that: the audit that says which tools a role must lose, the plan that loads a few tools up front and defers the rest, the choice of mechanism, the check that a call is covered by both the user's and the agent's rights, and a gateway that authenticates, checks the model, the tool and the rate, routes an allowed request and records every decision. Nothing calls a model: the tests give you tools, scopes and requests. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`audit`, `plan_loading`, `choose_mechanism`, `authorize`, `gateway`); TypeScript has the camel-case names (`planLoading`, `chooseMechanism`); Java has the same camel-case names as static methods of `Capability`; Kotlin has top-level functions. The starter shows the types of each language: Python and TypeScript use plain dictionaries and objects, Java and Kotlin use records and data classes (`Agent`, `Tool`, `AuditResult`, `Plan`, `Request`, `Policy`, `Outcome`).

## What to write

- `audit(agent, catalog)` compares what an agent holds with what its role needs. `agent` has `holds` (a list of tool names), `needs` (a list) and `used` (tool name to the number of calls). `catalog` maps each tool to its `access` class (`read`, `draft`, `money` or `destroy`). Return `remove` (the held tools that are not needed, in the order held), `risky` (those of them whose access is `money` or `destroy`), `missing` (the needed tools that are not held, in the order needed) and `dormant` (the held tools that are needed and have no calls). A dormant tool is reported and never removed.
- `plan_loading(tools, usage, keep=4, search_tokens=350)` decides what loads up front. `tools` maps a tool to the tokens of its definition and `usage` maps it to its calls. A set of fewer than 10 tools whose definitions total at most 10,000 tokens loads whole: `search` is false, `load_now` lists every tool, `deferred` is empty and `tokens` is the total. Otherwise `search` is true, `keep` is first held between 3 and 5, `load_now` is that many tools ordered by calls (most first, ties by name), `deferred` is the others in the order given, and `tokens` is the loaded tools plus `search_tokens`.
- `choose_mechanism(consumers, counterpart, path)` returns `agent-to-agent` when the counterpart is an `agent`, `direct call in code` when the path is `fixed`, `MCP server` when more than one client uses the capability, and `custom tool` otherwise, checked in that order.
- `authorize(tool, user_scopes, agent_scopes, required)` looks the tool up in `required` (tool to the scope it needs). An unknown tool is `deny: unknown tool`; a scope the user lacks is `deny: user lacks <scope>`; a scope the agent lacks is `deny: agent lacks <scope>`; otherwise `allow`.
- `gateway(request, policy)` decides one request. The request has a `credential` (possibly none), a `model`, a `tool` (possibly none) and `recent`, the requests the team sent this minute. The policy maps credentials to teams and holds, per team, the models and tools it may use and its limit, plus `routes` from a model name to the model it is sent to. Check in this order: the credential (`unauthenticated`), the model (`model not allowed`), the tool when there is one (`tool not allowed`) and the rate (`rate limited`, when `recent` has reached the limit). A request that passes is `allow` with the reason `routed to <model>` (the route, or the name itself when none is listed). Return the decision, the reason and an `audit` record with the `team` (or `unknown`), the `model`, the `tool` (or `none`) and the `decision`. Every request has a record, denied ones included, and the record holds no content.

## Why each part is there, and what you should see

1. **Remove, do not watch.** *You should see* the refund and delete tools in the list to remove, named as risky.
2. **A tool the role needs is reviewed, not removed.** *You should see* an unused needed tool in `dormant` and not in `remove`.
3. **Definitions have a cost.** *You should see* a small set loaded whole and a large one deferred, by count or by size.
4. **A few tools stay loaded.** *You should see* the number of tools kept loaded stay between 3 and 5 and ties settled by name, not by the order they were listed.
5. **The mechanism follows the counterpart and the path.** *You should see* an agent peer reached agent to agent, a fixed path in code, and a shared capability in an MCP server.
6. **Two sets of rights.** *You should see* a call refused when the agent lacks a scope the user holds.
7. **One place decides, in a fixed order.** *You should see* an unknown caller denied before anything else is looked at.
8. **Every decision leaves a record.** *You should see* a denied request recorded with the team, the model, the tool and the decision.

## The cases

| Id | What it checks |
|---|---|
| `m1` | An agent loses the tools its role does not need and the risky ones among them are named |
| `e1` | A tool that is held and needed but never used is reported as dormant and never removed |
| `e2` | A small set loads whole and a large one defers when it has ten tools or over ten thousand tokens |
| `e3` | The number of tools kept loaded stays between three and five and ties are broken by name |
| `e4` | The mechanism follows the counterpart, then the path, then the number of clients |
| `e5` | A call needs the user's scope and the agent's scope and an unknown tool is refused |
| `e6` | The gateway checks the credential, then the model, then the tool, then the rate |
| `e7` | The gateway keeps a record of every decision with the team or unknown and no content |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
