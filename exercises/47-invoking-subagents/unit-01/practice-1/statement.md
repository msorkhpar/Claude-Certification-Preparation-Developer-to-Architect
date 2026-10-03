# Practice: invoking subagents with the Agent SDK

A coordinator built on the Agent SDK does not call subagents directly. It lists them in the options, the model starts one by calling the
`Agent` tool, and the SDK gives you the messages of the whole tree. The code that you write is everything around that call: the
definitions you register, the limits that bound the team, how a spawn is recognised in the stream, how the messages that ran inside a
subagent are told from the coordinator's own, the brief that carries the facts a subagent cannot get any other way, and findings that keep
their sources. Write those parts. The practice is in Python and TypeScript only, because it needs the Agent SDK; a Java or Kotlin team
runs the Claude Code command line as a subprocess, or builds the team on the Messages API as the examples of module 46 do. Pick your language folder (`python` or `typescript`), open `starter/` and edit the file there. The tests never call a model: the last case starts the
SDK against the course's stand-in for the Claude Code binary, so the SDK code that you call is the real one.

## What to write

Names are Python's; the TypeScript names are in camel case (`buildOptions`, `bySubagent`, `makeBrief`, `packageFinding`, `mergeFindings`,
`runTeam`), and `cli_path`, `max_budget_usd`, `max_turns` and `max_concurrent` are the arguments `cliPath`, `maxBudgetUsd`, `maxTurns`
and `maxConcurrent`.

- `build_options(agents, cwd, cli_path=None, max_budget_usd=2.0, max_turns=20, max_concurrent=5)` returns the SDK's options. `agents` is a
  map from a name to `{"description", "prompt", "tools" (optional), "model" (optional)}`.
  - A name must be lower-case letters, digits and hyphens, starting with a letter; a description or a prompt that is missing or blank is
    refused. Refuse by raising `ValueError` (TypeScript: throw an `Error`).
  - Register each as an agent definition with its description, prompt, tools and model. A definition with no `tools` gets the read-only
    tools `Read`, `Grep` and `Glob`, never "every tool" (that is what an omitted list means to the SDK). The tool `Agent` is removed from
    every subagent's list, so that only the coordinator spawns. An explicitly empty list stays empty. A missing model is `inherit`.
  - The options auto-approve the spawn and the read-only tools (`allowed_tools` is `Agent`, `Read`, `Grep`, `Glob`), run in `cwd` with the
    permission mode `default` and no setting sources, and carry the turn and budget limits. The environment limits the depth of nesting to
    `1` (`CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`) and the number of subagents at once to `max_concurrent`
    (`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`, as a string). In TypeScript the SDK replaces the child's environment with `env`, so keep the
    current `process.env` in it. When `cli_path` is given, set it (`cli_path` in Python, `pathToClaudeCodeExecutable` in TypeScript).
- `spawned(messages)` returns one `{"id", "subagent_type", "description", "prompt"}` for each call of the tool that starts a subagent, in
  order. The tool is called `Agent`, and `Task` before Claude Code v2.1.63, so match both. `subagent_type` is `general-purpose` when the call
  gives none. Other tool calls and text blocks are not spawns.
- `by_subagent(messages)` returns a map from the id of each spawn to `{"subagent_type", "messages", "tools"}`: how many messages carry
  that id as their `parent_tool_use_id`, and the names of the tools used in them (each once, in order). Messages that belong to the
  coordinator, with no parent, count for nobody.
- `make_brief(task, files=None, facts=None, output=None)` writes the prompt: `Task: {task}` (stripped), then `Files:` with one `- item` line per
  file, then `Known:` with one line per fact, then `Return: {output}`. Items are stripped, blank ones are dropped, and a section with no
  item or no output is left out. A blank task is refused as above.
- `package_finding(claim, url=None, title=None, page=None)` returns `{"claim": the stripped claim, "source": {...}}`: the source holds
  only the parts that were given, and is null when none was. The source is never written into the claim.
- `merge_findings(findings)` merges findings whose claims are equal once case and spacing are ignored, in order of first appearance. Each
  result is `{"claim": the first spelling, "sources": every different source, in order, "attributed": whether there is any}`.
- `run_team(prompt, options)` (async) collects every message of a single-shot run and returns `{"messages", "error"}`. The SDK raises after
  an error result, so the messages received so far are kept and the error text is returned, or null when the run ended well.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The options register each agent with its description, prompt, tools and model, and auto-approve the spawn |
| `e1` | A subagent gets read-only tools by default and never the right to spawn another |
| `e2` | A bad name, a blank description or a missing prompt is refused |
| `e3` | Depth, concurrency, budget and turn limits are set on the options |
| `e4` | A spawn is recognised under both tool names, and nothing else is |
| `e5` | Messages from inside a subagent are grouped under the call that started it |
| `e6` | A brief holds the task and every fact in a fixed layout |
| `e7` | A finding keeps its source apart, and merging keeps every source |
| `e8` | A run through the SDK shows the spawn, the inner messages and the agents sent to the binary |

Run the tests with the command in the language folder's `run.sh`.
