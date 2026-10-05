# The Agent SDK: tools, permissions, hooks and custom tools

**Level:** Developer · **Module 35:** The Claude Agent SDK · **Page 2 of 3**
**Exams:** DV3; A1

**After this page you can** tell which tools exist from which are pre-approved, apply the order in which the SDK evaluates a tool call, choose between a hook, a rule and a callback, write a custom tool with the in-process MCP server and report its errors on purpose, and avoid the permission settings that silently do less than they look.

Checked against the Agent SDK pages of the Claude Code documentation (agent loop, permissions, hooks, custom tools and the Python and TypeScript references) on 2026-10-03, with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The behaviour shown in the example on the next page ran against the course's scripted stand-in, which applies a simplified copy of these rules. The real binary and a real model were not run.

## Why it matters

An agent that can run shell commands and edit files is only as safe as the policy around it, and the SDK's policy has several layers that interact: availability, pre-approval, deny rules, modes, hooks and a callback. Most mistakes are one layer doing less than its author assumed. The exam asks which layer decides, and in what order.

## The idea

### Availability is not approval

Two settings sound alike and do different things.

- **Which tools exist.** `tools` sets the built-in tools that are available. A bare name in `disallowed_tools` (such as `Bash`) removes the tool from Claude's context altogether, so Claude cannot even attempt it.
- **Which calls are pre-approved.** `allowed_tools` auto-approves the tools you list, and the documentation is explicit that "This does not restrict Claude to only these tools." Other unlisted tools fall through to `permission_mode` and `can_use_tool`. The documentation's agent loop page says: "Tools not listed are still available, and calls to them that need approval fall through to the permission mode and canUseTool."

A scoped deny rule is different again. `disallowed_tools=["Bash(rm *)"]` leaves `Bash` available and denies matching calls in every permission mode, `bypassPermissions` included. The match is on the command as written, so another spelling of the same command falls through to the mode. A deny rule is a guard against the obvious, not a sandbox.

### The order of evaluation

When Claude requests a tool, the SDK checks in this order: hooks, deny rules, ask rules, the permission mode, allow rules and, last, the `canUseTool` callback. Each step can end the decision. Hooks run first, so a hook can deny a call outright, and the warning on the permissions page is plain that "hooks run before every other step, and a hook deny applies even in bypassPermissions mode". A deny rule blocks even in `bypassPermissions`. An allow rule approves a call before the callback is consulted. The callback sees only what nothing earlier decided.

Three consequences follow, and the documentation states each.

1. "Auto-approved tools never reach canUseTool." A call that `allowed_tools`, `acceptEdits` or `bypassPermissions` approved skips the callback, so a check placed there is silently bypassed for that tool. To gate every call, use a `PreToolUse` hook.
2. "allowed_tools does not constrain bypassPermissions." Listing `Read` alongside `bypassPermissions` still approves every tool, `Bash`, `Write` and `Edit` included (apart from calls that an ask rule or a critical-path removal sends to the callback). To block specific tools in that mode, use deny rules.
3. In the TypeScript SDK, passing a callback in a configuration where an earlier step would approve first emits a process warning, with the code `CLAUDE_SDK_CAN_USE_TOOL_SHADOWED`. It triggers for `bypassPermissions` and for each bare `allowedTools` entry. A scoped entry such as `Bash(ls *)` does not trigger it.

The permission modes are `default` (calls that need approval go to your callback, and with no callback they are denied), `acceptEdits`, `plan`, `dontAsk` (never prompts, denies what would prompt), `auto` (a model classifier reviews actions) and `bypassPermissions` (for isolated environments only; in the TypeScript SDK it also needs `allowDangerouslySkipPermissions`). Say the mode explicitly. The TypeScript reference notes that "If you omit it, the session can start in auto mode", so an agent whose safety depends on the default mode should not rely on the default.

### Hooks

Hooks are callbacks that run your code at events in the agent's life: before a tool runs (`PreToolUse`), after it (`PostToolUse`, `PostToolUseFailure`), at prompt submission, when a subagent starts or stops, before compaction, at a permission request and at stop. A hook has an optional `matcher` that filters by tool name, for example `Write|Edit`, and a callback that receives the event and returns an output object. For `PreToolUse` the output sets `permissionDecision` to `allow`, `deny`, `ask` or `defer`, a `permissionDecisionReason` that tells the model why, and optionally `updatedInput`. Some events exist only in the TypeScript SDK.

When several hooks match they run in parallel, and the most restrictive result applies: "If any hook returns deny, the operation is blocked regardless of other hooks." That is also why each hook should act on its own and not rely on another having run first. The right tool for each job:

- **A hook** for a check that must run on every call, for logging and auditing, and for changing an input (it runs before the permission steps, so a veto is final).
- **A rule** for a fixed fact about a tool or a pattern.
- **The callback** for a decision that needs your program's judgement and was not settled earlier.

### Custom tools

A custom tool is a function that Claude can call, defined with the SDK's in-process MCP server: the `@tool` decorator in Python (the `tool()` helper in TypeScript), wrapped by `create_sdk_mcp_server` (TypeScript: `createSdkMcpServer`). The server "runs in-process inside your application, not as a separate process". You pass it in `mcp_servers`, and the key you choose becomes the server name in each tool's full name: `mcp__{server_name}__{tool_name}`, so `add` on a server `calc` is `mcp__calc__add`. A pattern such as `mcp__calc__*` names every tool of the server in an allow rule.

Two details of tools matter in production. First, annotations. Setting `readOnlyHint` lets the tool run in parallel with other read-only tools, and the documentation adds that "Annotations are metadata, not enforcement." A tool marked read-only can still write if its handler does. Second, errors. "A handler error doesn't stop the agent loop." The in-process server catches an uncaught exception and returns it as an error result that carries the raw exception message, and the loop goes on. (This is the Agent SDK's in-process server; a Python server built with the MCP SDK in module 32 reports an escaped exception without its message.) If the handler catches the error and returns `is_error` (TypeScript: `isError`), Claude sees the message you compose, which can say which request failed and what to try instead. So how you report an error decides what Claude reads, and not whether the query fails. In Python the decorator forwards only `content` and `is_error` from your return value.

## Traps

1. **Using `allowed_tools` as a whitelist.** It pre-approves. It does not remove anything, so other tools stay available and fall through to the mode and the callback.
2. **Putting the safety check in the callback.** Anything an earlier step approved never reaches it. Put checks that must run on every call in a `PreToolUse` hook.
3. **Pairing `bypassPermissions` with an allow list.** The list does not limit that mode. Use deny rules, and run it only in an isolated environment.
4. **Trusting a read-only annotation.** It is a hint for scheduling, and the handler is what decides whether the tool writes.
5. **Raising from a handler when the message matters.** An uncaught exception reaches Claude as its raw message. Catch and return `is_error` with a message written for the model.

## Quiz

1. A developer puts Read and Grep in allowed_tools, expecting the agent to be unable to run shell commands. What happens?
   - **a**: Every other tool is removed from the agent's context
   - **b**: Others stay reachable, and only those two are pre-approved
   - **c**: The run fails at startup because the list is incomplete
   - **d**: Unlisted tools are approved automatically, with no prompt at all

2. A hook returns a denial for a tool call that a matching allow rule would approve. Which result follows?
   - **a**: It runs, with the allow rule overriding the denial
   - **b**: It runs only when the permission mode is bypassPermissions
   - **c**: It goes to the permission callback for the final decision
   - **d**: It is blocked, with the earliest step deciding the outcome

3. A custom in-process tool divides by zero and the handler throws without catching. What does Claude see?
   - **a**: Nothing, because the query aborts before any result is produced
   - **b**: The raw exception message as an error result, and the loop continues
   - **c**: A message that you compose, since uncaught errors are reported as written
   - **d**: A generic failure notice with the message stripped for safety

<details>
<summary>Answer key</summary>

1. **b**. The page says allowed_tools "does not restrict Claude to only these tools", and that unlisted tools "fall through to permission_mode and can_use_tool". *a* is ruled out because "This does not restrict Claude to only these tools." *d* is ruled out because unlisted tools do not default to approval: they "fall through to permission_mode and can_use_tool". *c* is ruled out because "Tools not listed are still available, and calls to them that need approval fall through to the permission mode and canUseTool."
2. **d**. The page gives the order as hooks, deny rules, ask rules, the mode, allow rules and the callback, and says a hook can deny a call outright. *a* is ruled out because the order puts hooks first and "If any hook returns deny, the operation is blocked regardless of other hooks." *b* is ruled out because "hooks run before every other step, and a hook deny applies even in bypassPermissions mode". *c* is ruled out because "hooks run before every other step", while the callback is the last step and sees only what nothing earlier decided.
3. **b**. The page says the in-process server catches an uncaught exception and returns it as an error result that carries the raw exception message, and the loop goes on. *a* is ruled out because "A handler error doesn't stop the agent loop." *d* is ruled out because the result is an error result "that carries the raw exception message", not a stripped one. *c* is ruled out because "Claude sees the message you compose" only when the handler catches the error and returns is_error, and an uncaught exception does not do that.

</details>
