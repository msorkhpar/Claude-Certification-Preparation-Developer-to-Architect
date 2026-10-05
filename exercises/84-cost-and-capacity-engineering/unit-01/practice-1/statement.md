# Practice: the decisions of an internal gateway

Four product teams call Claude through one internal gateway that holds the credentials, so that the platform team can route requests, enforce budgets, show each team what it spent and
protect callers that cannot wait. In this practice you write the four decisions that gateway makes: the model a request is sent to, whether a request is admitted against the team's
budget, the cost table that each team sees, and whether a caller gets its answer on the same connection or through accept-and-poll. The model is not called: the tests give you requests,
policies and usage rows. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`route`, `admit`, `showback`, `delivery`); TypeScript has the same names; Java has the same names as static methods of `GatewayBudget` and Kotlin has top-level functions.
Numbers are whole numbers (money in cents, tokens, seconds, percent), so that every language gives the same figures. Maps and lists are JSON-like, as the starters show. A refusal is
`ValueError` (TypeScript: an `Error`; Java and Kotlin: `IllegalArgumentException`).

## What is already written, and what you write

The starter is a working gateway with six gaps cut out of it. Everything that is plumbing is written and correct: the grouping of rows by team, the ordering of the showback and the rows of the result. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value (`None`, `null`, `0`), so the starter runs and fails every case on an assertion. Debug a gap by logging its input with the `log` line at the top of the file (the starter already logs the input of one function; add your own `log.debug` lines the same way); a run shows the lines you logged under the failing case. Write the gaps in this order (the Java and Kotlin names are the camel-case forms, TypeScript has no leading underscore):

1. `_chosen_model` unlocks `m1` and `e6`: the model the policy picks for a request, with a pin honoured only when allowed.
2. `route` unlocks `e1`: the model once the budget status is known (refused when blocked, cheaper when warned).
3. `admit` unlocks `e2`: allow, warn or block against the budget.
4. `_cost` unlocks `e3`: what one row of tokens costs, and the refusal of a model without a price.
5. `_to_cents` unlocks `e4`: a team's total rounded to a cent, once.
6. `delivery` unlocks `e5`: sync or accept-and-poll.

About fifteen lines in all. The sections below describe the whole gateway.

## What to write

- `route(request, policy, status)`: `request` is `{task, model}` (`model` is an optional pin), `policy` is `{allowed, routes, default, cheaper}`, `status` is the result of `admit`.
  - `block` gives `None` (null): the request is refused.
  - A pinned model is used only when it is in `policy.allowed`; otherwise the model is `policy.routes[task]`, or `policy.default` for a task the table does not know.
  - On `warn` the model is replaced by `policy.cheaper[model]` when it has an entry, and kept when it has none.
- `admit(spend, budget, estimate)`: one more request that is estimated to cost `estimate` cents, for a team that has spent `spend` of `budget`. `block` when the budget is not positive or when
  `spend + estimate` is **more than** the budget; `warn` when it reaches 80 percent of the budget (80 percent itself warns) and is not over; otherwise `allow`.
- `showback(rows, prices)`: a row is `{team, model, input, cache_read, output}` (token counts), `prices[model]` has `input`, `cache_read` and `output` in cents per million tokens. For each
  team add up tokens times price over all of its rows, divide by one million and round **once**, to the nearest cent with halves up. Return `[{team, cents}]`, most expensive first and by team
  name among equals. A row whose model has no price is refused with `unknown model: <name>`: a gateway never guesses a price.
- `delivery(p95_seconds, timeout_seconds, margin_percent)`: `sync` when `p95_seconds` plus the margin (in percent) fits within `timeout_seconds`, with an exact fit allowed; otherwise
  `accept-and-poll`.

## Why each part is there, and what you should see

1. **The gateway owns the model choice.** One table beats each team's habit. *You should see* each task go to its model and an unknown task to the default.
2. **A budget degrades before it stops.** *You should see* the cheaper model for a team near its limit and a refusal for a team over it.
3. **Admission is exact at the edges.** *You should see* 80 percent warn, 100 percent warn and one cent over block, and a team without a budget blocked.
4. **Showback prices every token kind.** Cache reads are cheap and output is dear; a bill that ignores either is wrong. *You should see* 450 and 700 cents.
5. **Rounding happens once.** Rounding each row first loses or gains cents. *You should see* two rows of 0.4 cent become 1 cent.
6. **A hard latency limit changes the shape of the call.** *You should see* `accept-and-poll` as soon as the slow case, with its margin, no longer fits.
7. **A pin is a request, not a right.** *You should see* a pin outside the allowed list ignored.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A request follows the route table of the gateway |
| `e1` | A team near its budget is moved to a cheaper model, and a team over it is refused |
| `e2` | A request is admitted, warned or blocked against the budget |
| `e3` | Showback adds each team's tokens at the price of the model and refuses an unknown model |
| `e4` | Showback rounds each team's total to a cent once |
| `e5` | A caller with a hard latency limit gets accept-and-poll when the slow case does not fit |
| `e6` | A model pinned by a team is honoured only when the policy allows it |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
