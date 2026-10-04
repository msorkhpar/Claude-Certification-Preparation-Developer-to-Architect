# Passing context, running subagents in parallel and keeping sources

**Level:** Architect · **Module 47:** Invoking subagents · **Page 2 of 2**
**Exams:** A1.3

**After this page you can** write a brief that carries what a subagent cannot get any other way, decide when subagents should run in parallel and what bounds them, keep the content of a finding apart from its source so that attribution survives the synthesis, say what a resumed subagent keeps, and write the module's practice: the code around the Agent tool.

Checked on 2026-10-03 against the Claude Code documentation pages "Subagents in the SDK", "Intercept and control agent behavior with hooks" and "Subagents in Claude Code", and Anthropic's engineering article on its multi-agent research system, with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice runs offline in Python and TypeScript through the course's stand-in for the Claude Code binary; no model and no network are used. The structured-finding design in the last section is this course's own, built on the article's advice and labelled as such.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* subagents are run in parallel by emitting several spawn calls in a single coordinator response, not across separate turns, and the context that a subagent needs, with source details kept apart from content, goes into its prompt. *What the current product does (documentation checked 2026-10-03):* the same holds for context, since only the prompt string passes from parent to subagent. For timing, subagents run in the background by default, so a spawn call that omits `run_in_background` returns without waiting, and Claude sets it to false when it needs the result first. The exam keys several calls in one response for parallel work; in the SDK, also check whether the coordinator must wait for a result before its next step.

## Why it matters

A subagent works on what it is given. The exam's third task of the first domain is about giving it the right thing, in the right form, and getting something back that the coordinator can use: complete context in the prompt, parallel invocation where the parts are independent, and results in a structure that keeps the source of every claim. The failures are familiar. A subagent re-reads files the coordinator already read, three subagents run one after another when they could have run together, and a final report quotes a figure that nobody can trace.

## The idea

### What a subagent has, and what you must put in the brief

The documentation lists what a (non-fork) subagent's context holds:

| The subagent receives | The subagent does not receive |
|---|---|
| Its own system prompt (`AgentDefinition.prompt`) and the Agent tool's prompt | The parent's conversation history or tool results |
| Project `CLAUDE.md` (loaded through `settingSources`), unless the agent sets `omitClaudeMd` | Preloaded skill content, unless listed in `AgentDefinition.skills` |
| Tool definitions, inherited or the subset in `tools` | The parent's system prompt |

The rule that follows is the one from module 46, in the SDK's words: the Agent tool's prompt is "the only content you pass from parent to subagent". So the brief is where the coordinator hands over facts that it already has: the paths of the files that matter, the error text it saw, the decisions it already made, and the shape the answer must take. The practice's `make_brief` fixes a layout so that nothing is left to the writer's mood:

```text
Task: Review auth.py for injection risks
Files:
- src/auth.py
Known:
- the login query is built with string formatting
Return: findings, one line each
```

The task comes first and is one sentence. The files are listed so that the subagent does not search for them. The known facts spare it from re-deriving what is settled, and carry the constraints ("do not touch vendor/"). The return format tells it what the coordinator will do with the answer. A brief that has these four parts is longer than "review auth.py", and the extra lines are what you pay to avoid a subagent's wasted turns.

The other direction is the subagent's final message. "The parent receives the subagent's final report, but may summarize it in its own response. To preserve subagent output verbatim in the user-facing response, include an instruction to do so in the prompt or `systemPrompt` option you pass to the main `query()` call." If the exact words of a report matter (a list of findings with line numbers) say so, or take the report from the tool result yourself instead of from the coordinator's paraphrase.

### Parallel subagents

"Multiple subagents can run concurrently, so independent subtasks finish in the time of the slowest one rather than the sum of all of them." The documentation's example is a code review that runs a style checker, a security scanner and a test-coverage agent together. Parallel invocation fits when the parts are independent, as in module 46's partition. It does not fit when one part needs another's output: that is a sequence, and the coordinator should call the first, read its result and then write the second brief with it.

The SDK bounds the fan-out in ways you should know before you rely on the model's judgement.

- **Background by default.** "Subagents run in the background by default. An Agent tool call that omits the `run_in_background` input launches a background subagent, and Claude sets `run_in_background: false` when it needs the result before continuing." A coordinator that needs a result before its next step should wait for it.
- **Concurrency.** At most 20 subagents run at once by default (`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`). Past it, the spawn is refused with the tool result `Concurrent subagent limit reached`, and the coordinator reads it like any other result.
- **Depth and spend.** Nesting goes three layers deep by default (`CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH`), and the budget cap counts every subagent's spending: at the cap "spawning another subagent fails with `Budget limit reached`".
- **Delegation appetite.** The documentation notes that "Claude Opus 5 delegates to subagents more readily than earlier models", which is why the three limits matter most there, and that a prompt instruction only steers: "set the limits as well. Claude Code enforces them however Claude decides to delegate."

Tracking the team uses the hooks of module 49: `SubagentStart` and `SubagentStop` fire as each starts and finishes, and the hook input carries the agent's id and type, so a coordinator can aggregate the results of parallel tasks without parsing the stream. A subagent that stops at its own `maxTurns` returns partial output, marked as such, and "you can resume the agent to continue": the completed spawn's tool result holds an `agentId`, and resuming means passing the session id as `resume` and the agent id in the prompt, with the same definition. A resumed subagent keeps its whole earlier conversation, which is what a fresh spawn does not have.

### Keep the content apart from the metadata

A research subagent returns claims. Each claim came from somewhere: a page, a document, a table on page 12. When the subagent writes its report as prose, the sources drift into sentences, get shortened or are dropped, and by the time two subagents' reports are merged nobody can say which source supports which sentence. The design that keeps them is to give each finding a **content** part (the claim) and a **metadata** part (url, title, page, date), as separate fields, and to have the coordinator merge by content and carry the metadata through. This is the course's own advice, in line with the research article's separation of the writers from a citation step: attribution is a property of the data, and the synthesis cannot be asked to rebuild it from prose.

The practice's `package_finding` and `merge_findings` model it:

```text
{"claim": "Rates held in 2024", "source": {"url": "https://example.com/a", "title": "Bank statement", "page": 3}}
```

Claims that are equal once case and spacing are ignored are one claim with every source: two subagents that found the same fact independently strengthen it, and the reader sees both sources. A claim with no source is marked unattributed, so the synthesis can say so instead of presenting it as established. The metadata is never written into the claim string, because then the merge could not recognise that two sentences are the same claim.

### The practice

The practice is `exercises/47-invoking-subagents/unit-01/practice-1/statement.md`, in Python and TypeScript. You write the code around the Agent tool:

- `build_options`: definitions with explicit read-only tools by default and no right to spawn, validated names and descriptions, the auto-approved tools, and the depth, concurrency, budget and turn limits;
- `spawned` and `by_subagent`: recognise a spawn under both tool names and group the messages that ran inside each subagent;
- `make_brief`: the fixed layout of the brief above;
- `package_finding` and `merge_findings`: content apart from source, with every source kept;
- `run_team`: collect the messages of a run and keep them when the SDK raises after an error result.

The tests grade nine cases, the last through the real SDK against the stand-in: the starter fails all nine, the reference passes them, and each of ten planted wrong solutions per language fails on an assertion of the case it breaks: a definition with no tools list, nesting allowed, an unchecked name, no depth limit, a dropped budget, one tool name only, the coordinator's messages credited to a subagent, a brief without the facts, a merge that keeps only the first source, and a source written into the claim.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"The task alone is a sufficient brief; the subagent can find the rest."** It is tempting because the subagent has tools. The exam rejects it: the subagent does not have your conversation, so it re-reads and re-derives, or guesses. Put the files, the facts and the return format in the brief.
2. **"Start the subagents one at a time, so that nothing conflicts."** It is tempting because it is easy to reason about. The exam rejects it for independent parts: they finish in the time of the slowest when run together, and several calls in one coordinator response start them together. Run dependent parts in sequence, and write the second brief with the first result.
3. **"The coordinator will repeat a subagent's report word for word."** It is tempting because the report is right there. The exam rejects it: the coordinator may summarize. If the words matter, instruct it to keep them, or read the tool result.
4. **"Write the source into the claim, as a sentence."** It is tempting because it reads well. The exam rejects it: the merge can no longer see that two sentences are one claim, and a summary can drop the source with the sentence. Keep the content apart from the metadata.

## Quiz

3. A coordinator spawns a subagent when it did not set `run_in_background`, and then writes its answer to the user before the subagent has reported. What explains it?
   - **a**: The spend cap was reached, so the subagent never started and nothing was reported
   - **b**: Delegated work launches immediately unless the call asks for a foreground wait
   - **c**: Subagents are started one at a time, so the second one was still queued
   - **d**: The concurrency limit refused the spawn without telling the coordinator anything


4. A research coordinator merges the prose reports of three subagents. The final answer repeats a figure that nobody can trace to a document, and two subagents had reported the same fact in different words. Which change fits best?
   - **a**: Keep only the first source of every repeated fact, so that answers stay short
   - **b**: Ask the synthesizer to cite sources by recalling where each figure appeared
   - **c**: Write the source into each claim sentence, in brackets right after the text
   - **d**: Return each claim and its origin as separate fields, and combine on claims

<details>
<summary>Answer key</summary>

3. **b**. The documentation's default is a background launch, and a coordinator that needs the result first has to ask for a foreground run. *a* is ruled out because a spent cap is reported, not silent: "spawning another subagent fails with `Budget limit reached`". *c* is ruled out because the SDK runs them together: "Multiple subagents can run concurrently, so independent subtasks finish in the time of the slowest one rather than the sum of all of them." *d* is ruled out because the refusal is a result that the coordinator reads: "the spawn is refused with the tool result `Concurrent subagent limit reached`, and the coordinator reads it like any other result".

4. **d**. Content and metadata in separate fields let the merge recognise one claim and keep every source. *b* is ruled out because "attribution is a property of the data, and the synthesis cannot be asked to rebuild it from prose." *c* is ruled out because "The metadata is never written into the claim string, because then the merge could not recognise that two sentences are the same claim." *a* is ruled out because the sources are the evidence: "two subagents that found the same fact independently strengthen it, and the reader sees both sources".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A CI coordinator runs in bypassPermissions, and its reviewer definition lists only the tools Read and Grep. A teammate fears that the reviewer can still do anything, because the mode skips prompts. Which limit remains?
   - **a**: Nothing at all, because the mode removes every restriction on the tree
   - **b**: The permission mode in its own definition, which overrides the parent's mode entirely
   - **c**: Its own capability set, since whatever is left out is absent from its session
   - **d**: The parent's allowed tools, which cap what the mode may approve

2. The final answer of a coordinator paraphrases a subagent's findings and drops the line numbers that the user needs. Which fix fits best?
   - **a**: Raise the subagent's turn limit so that its report becomes much longer and fuller
   - **b**: Tell the parent to keep the report verbatim, or read the tool result directly
   - **c**: Remove the return-format line from the brief so that the report is less rigid
   - **d**: Switch the subagent to a larger model so that its summaries keep more of the detail

3. Six delegates run side by side, and each asks for approval before reading files, which floods the operator. Which design fits best?
   - **a**: Take the Read tool out of the definitions, so that nothing can ever prompt
   - **b**: Set bypassPermissions in each agent definition, so that none of them asks again
   - **c**: Run the six one after another, so that only one asks at any given time
   - **d**: Pre-approve those lookups in a PreToolUse hook, or in inherited rules

<details>
<summary>Answer key</summary>

1. **c**. A tool that is not in the subagent's session cannot be called, whatever the mode. *b* is ruled out because a definition cannot loosen a bypass parent or override it: "A subagent runs in `bypassPermissions` mode only when the parent session itself does." *a* is ruled out because "A tool you leave out isn't in the subagent's session at all: Claude works without it, with no permission prompt or error." *d* is ruled out because "allowed_tools does not constrain bypassPermissions".
2. **b**. The parent may summarize, so the words that matter must be kept by instruction or read from the result. *a* is ruled out because the paraphrase happens in the parent, which "may summarize it in its own response". *c* is ruled out because the format line decides what comes back: "The return format tells it what the coordinator will do with the answer." *d* is ruled out because a larger model does not stop the coordinator from summarizing: "The parent receives the subagent's final report, but may summarize it in its own response."
3. **d**. A hook or inherited rules answer the repeated requests once, in one place. *b* is ruled out because a definition cannot set bypass: "A subagent runs in `bypassPermissions` mode only when the parent session itself does." *c* is ruled out because sequencing independent work costs time: "independent subtasks finish in the time of the slowest one rather than the sum of all of them". *a* is ruled out because a subagent without the tool cannot read: "A tool you leave out isn't in the subagent's session at all: Claude works without it".

</details>
