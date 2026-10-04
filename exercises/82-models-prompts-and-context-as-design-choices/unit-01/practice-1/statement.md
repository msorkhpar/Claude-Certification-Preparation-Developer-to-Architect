# Practice: a prompt plan under a token budget

A support assistant sends the same role text and policy on every request, a customer record, a conversation history and a question. The architect has three decisions to make
in code: how to assemble the request so that the part that never changes can be cached and the budget is respected, which model to use for a workload, and whether two requests
can share a cached prefix. The model is not called: the tests give you modules, workloads and prompts. It is in Python, TypeScript, Java and Kotlin; pick your language folder,
open `starter/` and edit the file there.

Names are Python's (`assemble`, `choose_model`, `reusable_prefix`, `tokens`, `MIN_CACHEABLE`); TypeScript has `assemble`, `chooseModel`, `reusablePrefix`; Java has the same
camel-case names as static methods of `PromptPlan` and Kotlin has top-level functions. A module, a prompt and a model are maps, as the starters show. A refusal is `ValueError`
(TypeScript: an `Error`; Java and Kotlin: `IllegalArgumentException`).

## What to write

- `tokens(text)` is given: one token per four characters, rounded up. `MIN_CACHEABLE` is 512 tokens.
- `assemble(modules, variables, budget)` takes modules `{name, text, static, priority}` (`priority` is a whole number and defaults to 0; it matters only for dynamic modules) and returns
  `{"blocks": [{name, text}], "tokens", "dropped": [names], "breakpoint"}`.
  - Static modules come first, in the order given; dynamic modules follow, in the order given, with every `{variable}` in their text replaced from `variables` (extra variables are
    ignored). A variable with no value is refused with `missing variable: <name>`.
  - A static module whose text holds a `{variable}` is refused, and the message says `static`: a value that changes in the prefix breaks the cache.
  - While the total tokens exceed `budget`, drop the dynamic module with the lowest `priority`; of two with the same priority, drop the **later** one. Static modules are never dropped:
    when only static modules are left and they still exceed the budget, refuse with `over budget`. `dropped` lists the names in the order they were dropped; `tokens` counts what is left.
  - `breakpoint` is the index of the last static block when there is at least one static module and the static prefix has at least `MIN_CACHEABLE` tokens; otherwise it is `None`
    (TypeScript, Java and Kotlin: `null`).
- `choose_model(workload, models)`: `workload` is `{tier, max_latency_ms}`, a model is `{name, tier, latency_ms, price_out}`. Return the name of the cheapest model (by `price_out`) whose
  tier is at least the workload's and whose latency is within the limit; equal prices are broken by the lower name; `None` when no model fits.
- `reusable_prefix(a, b)` takes two assembled prompts. When both have a breakpoint, the breakpoints are equal and every block up to and including it is identical (name and text) in both,
  return the tokens of those blocks; otherwise return 0.

## Why each part is there, and what you should see

1. **Static first.** The cache reads a prefix, so what never changes goes first. *You should see* the order role, policy, question, history for a shuffled input, and the breakpoint after the policy.
2. **No variable in the prefix.** One changing word in a static block invalidates everything after it. *You should see* it refused.
3. **Variables are checked.** *You should see* a missing variable refused and an unused one ignored.
4. **A budget drops the least useful first.** *You should see* `extra` dropped before `history` and the static modules untouched, then a refusal when nothing droppable is left.
5. **A prefix below the minimum is not cached.** *You should see* the breakpoint at exactly 512 tokens and none at 511.
6. **A model is chosen for the workload.** *You should see* the cheapest model that fits and `None` when the latency rules every model out.
7. **Reuse is all or nothing.** *You should see* 512 reusable tokens for a different question and 0 for an edit of either static block.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Static modules come first and the breakpoint follows the last one |
| `e1` | A variable in a static module is refused |
| `e2` | Dynamic variables are filled and a missing one is refused |
| `e3` | The lowest priority dynamic module is dropped first, and a tie drops the later one |
| `e4` | Static modules are never dropped, and a budget they exceed is refused |
| `e5` | A prefix under the minimum gets no breakpoint |
| `e6` | The cheapest model that meets the tier and the latency wins, ties go by name |
| `e7` | Only an identical static prefix can be reused |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
