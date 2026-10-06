# Practice: distributing tools across agents

A multi-agent system that gives every agent every tool pays for it twice: the models choose worse among many tools, and an agent outside its specialisation
uses tools it should not. This practice has you write the decisions around the tool lists of a team: which agent gets which tool, what `tool_choice` a turn
can use on a given model, whether a reply made the call it had to, and whether a call to a tool that cannot be undone may run. The model is not called: the
tests give you catalogs, replies and calls. It is in Python, TypeScript, Java and Kotlin; The rules (the budget of five tools, the roles, the policy shape, the codes) are this course's own design, built on the guide's examples and on
the documentation's `tool_choice` table; the statement says where the documentation fixes something.

Names are Python's (`assign_tools`, `plan_turn`, `cache_impact`, `check_turn`, `authorize`); TypeScript has `assignTools`, `planTurn`,
`cacheImpact`, `checkTurn` and `authorize`; Java has the same camel-case names as static methods of `Distribute`; Kotlin has top-level functions.
Results are maps and lists, as the examples show. A refusal is `ValueError` (TypeScript: an `Error`; Java and Kotlin: `IllegalArgumentException`).

## What is already written, and what you write

The starter is a working tool distributor with eight gaps cut out of it. Everything that is plumbing is written and correct: the refusal of a duplicate catalog name, an unknown tool or a role without a specialisation, the grant of an explicitly named tool, the free and none choices, the first request of a conversation and the shape of every answer. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. A role's own tools (unlocks `m1`, `e2`): a role gets the tools whose tags share one with its specialisation, in catalog order, and never an irreversible tool by tag alone.
2. The budget (unlocks `e1`): a role with more tools than the budget is refused with an error that names the count and the budget.
3. Forcing and its fallback (unlocks `e3`): a model that accepts forcing gets `any` or the named tool as the native tool choice; a model that rejects it gets `auto`, with the offered tools (all of them, or only the named one), strict schemas and a check of the call.
4. The cache cost (unlocks `e4`): a changed tool list costs everything (`all`), a changed tool choice costs the cached messages (`messages`), and a repeat costs nothing (`none`).
5. The wrong tool (unlocks `e5`): when a particular tool was required first, a reply whose first call is another tool is `wrong_tool`.
6. The tool the policy names (unlocks `e6`): a tool that the policy does not name is refused with `unknown_tool`.
7. The amount and the customer (unlocks `e6`): for a tool with a cap, an amount that is not a whole number above zero is refused with `bad_amount`; a call whose customer is not the verified one is refused with `not_owner`.
8. The cap and the approval (unlocks `e7`): an amount above the cap is refused with `over_cap` and sent to a person (the cap is inclusive); an irreversible call without an approval is refused with `needs_approval`, and an approval never lifts the cap.

`m1` needs gap 1. About twenty lines in all. The steps below describe the whole distributor, so you can see how your gaps are used.

## Build it in four steps

### Step 1: `assign_tools(roles, catalog, budget=5)`

`catalog` is a list of `{"name", "tags": [..], "scoped": bool (optional), "irreversible": bool (optional)}`. `roles` maps a role name to
`{"specialisation": [tags], "extra": [tool names] (optional)}`. Return a map from each role to its list of tool names.

- A role gets every catalog tool that shares a tag with its specialisation and is not irreversible, in catalog order.
- Each name in `extra` is added after those (once). It must exist in the catalog. A tool outside the role's specialisation may be an extra only when it is marked
  `scoped` (a cross-role tool for a frequent need) or `irreversible` (an explicit grant); any other outside tool is refused.
- A role with no specialisation, a role whose list is longer than `budget`, and a catalog in which two entries have the same name are refused. A list exactly
  as long as the budget is fine.

**Why the exam cares.** The guide's example is 18 tools instead of 4 or 5, and a synthesis agent that tries web searches; its answer is scoped access, with a
narrow cross-role tool such as `verify_fact` for the frequent case and the coordinator for the rest. **What you should see when it works:** each role's list
holds only its own tools, the scoped tool appears for the role that is granted it, an irreversible tool appears only where someone wrote it down, and a bloated
list is refused instead of silently trimmed.

### Step 2: `plan_turn(model, need, tools, forced=None, manual_thinking=False)` and `cache_impact(previous, new)`

`need` is `free`, `none`, `any` (some tool must be called) or `named` (the tool `forced` must be called). Return
`{"tool_choice", "tools", "strict", "verify_call"}`.

- `free` is `{"type": "auto"}` and `none` is `{"type": "none"}`, on every model, with all the tools, `strict` false and `verify_call` false.
- On a model that accepts forcing, `any` is `{"type": "any"}` and `named` is `{"type": "tool", "name": forced}`, with all the tools, not strict, no check.
- The models `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-fable-5-1` and `claude-mythos-5-1` reject `any` and `tool`, and so does any model with
  manual extended thinking (`manual_thinking`). There the choice is `{"type": "auto"}`, `strict` is true and `verify_call` is true, and the tool list is
  all the tools for `any` and only the forced tool for `named`.
- An unknown `need`, or a `named` need whose tool is missing or not in the list, is refused.
- `cache_impact(previous, new)` compares two requests (maps with `tool_choice` and `tools`, as `plan_turn` returns them) and says what the change costs in prompt
  caching: `none` for the first request (previous is null) and for a request with the same tools and the same choice, `all` when the list of tools differs, and
  `messages` when only `tool_choice` differs. The tool list is checked first.

**Why the exam cares.** The guide forces a named tool on the first turn (for example `extract_metadata` before enrichment) and uses `any` to guarantee a call. The
current "Define tools" page says that on the models above both return a 400 error, that `auto` with strict tool use is the alternative, and that changing
`tool_choice` invalidates cached message blocks, and the caching page adds that modifying tool definitions invalidates the entire cache. **What you should see when
it works:** the same need gives a forced choice on one model and an `auto` turn with one offered tool and a check on another, a switch of choice costs the
messages, narrowing the list for the fallback costs everything, and a repeated request costs nothing.

### Step 3: `check_turn(blocks, need, forced=None)`

Return `ok`, `missed_call` or `wrong_tool` for a reply's blocks (maps with a `type`, and for a tool use a `name`). `free` and `none` are always `ok`. For `any`,
a reply with no `tool_use` block is `missed_call`. For `named`, no `tool_use` block is `missed_call`, and a first `tool_use` block that is not `forced` is
`wrong_tool`.

**Why the exam cares.** When forcing is not available, the guarantee moves into the loop: the loop looks at the reply. **What you should see when it works:**
a reply of text only is a miss, the right call is accepted wherever the text is, and a different tool first is not the named one.

### Step 4: `authorize(call, policy, approvals)`

`call` is `{"id", "tool", "amount" (optional), "customer", "verified_customer"}`. `policy` is `{"tools": {name: {"cap": whole number or null, "irreversible": bool}}}`.
`approvals` is a collection of call ids a person approved. Return `{"allowed", "code", "message", "escalate"}`, deciding in this order:

1. A tool not in the policy: `unknown_tool` (not allowed, no escalation).
2. A tool with a cap and an amount that is not a positive whole number (a boolean, a fraction, a string, missing): `bad_amount`.
3. A customer that is not the verified one, or no verified customer: `not_owner`.
4. An amount above the cap (equal is fine): `over_cap`, escalate true, and the message names the cap. An approval never lifts a cap.
5. An irreversible tool whose call id is not approved: `needs_approval`, escalate true.
6. Otherwise `ok`, allowed.

**Why the exam cares.** A numeric limit and the authorisation of an action that cannot be undone are enforced in the tool layer and answered with a result
that routes to escalation; the prompt is not the control. **What you should see when it works:** a refund of 500 with approval runs, one of 501 never does,
one of 50 without approval waits for a person, and a tool nobody listed is refused.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Each role gets only the tools of its specialisation, in catalog order |
| `e1` | A role over its budget, an unknown or unscoped outside tool, a duplicate catalog name and a role with no specialisation are refused |
| `e2` | An irreversible tool is given only by an explicit grant |
| `e3` | The tool choice follows the model: native forcing where accepted, `auto` with strict tools and one named tool elsewhere |
| `e4` | A changed tool choice costs the cached messages, a changed tool list costs the whole cache, and a repeat costs nothing |
| `e5` | A reply is checked against the call that was required |
| `e6` | An unknown tool, a wrong owner and a bad amount are refused, in that precedence |
| `e7` | The cap is inclusive, an approval is needed for an irreversible call and never lifts the cap |
