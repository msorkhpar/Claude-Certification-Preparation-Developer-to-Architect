# Results, annotations and large tool sets, and the practice

**Level:** Architect · **Module 52:** Designing tool interfaces · **Page 2 of 2**
**Exams:** A2.1; S1; S3; S4

**After this page you can** shape a tool's result so that it carries signal and fits the context, page a long result with a cursor and a note that steers the agent, say how far a tool's annotations can be trusted and what the SDK uses them for, explain why a large tool set degrades selection and what tool search changes, judge when a tool surface needs redesign and not more prompting, and write the module's practice: a grader for a tool set.

Checked on 2026-10-03 against the Claude API documentation page "Define tools", Anthropic's engineering article "Writing tools for agents", the Model Context Protocol specification (the Tools and Pagination pages of version 2026-07-28) and the Agent SDK pages on custom tools and tool search. The practice runs offline in Python, TypeScript, Java and Kotlin; it calls no model. The paging contract, the lint rules and the trust rule of the practice are this course's own, built on that advice and labelled as such.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* giving an agent too many tools (its example is 18 where 4 or 5 would do) degrades selection by raising the complexity of the decision, so each agent should see a small set; it is a tool-distribution point that module 54 takes up. *What the current product says (documentation checked 2026-10-03):* the Agent SDK page on tool search gives the scale at which accuracy suffers, "Tool selection accuracy degrades with more than 30-50 tools loaded at once", and a mechanism for large sets that the guide does not name: "Tool definitions can consume large portions of the context window (50 tools can use 10-20K tokens)", so with tool search on, definitions are withheld and the agent "searches for relevant ones when the task requires a capability not already loaded". The count where trouble starts is therefore larger than the guide's example, and the remedy is not only fewer tools per agent but also loading definitions on demand. On the exam, choose the scoped, smaller set per agent; in your own code, scope first, then defer what is left.

## Why it matters

A tool can be well described and still hurt the agent: it returns four thousand rows and the context is gone before the agent can act; its server calls it "read-only" and a client believes it; the team connects six servers and the model now chooses among seventy tools. The exam asks about each of these in the guise of scenarios S1, S3 and S4, and the answers share one idea: the interface includes what comes back, what is claimed about the tool, and how many tools are on offer.

## The idea

### What a result should carry

Tool results are read by a model with a limited context, which is a different reader from a program. The documentation's advice is "Design tool responses to return only high-signal information. Return semantic, stable identifiers (for example, slugs or UUIDs) rather than opaque internal references, and include only the fields Claude needs to reason about its next step. Bloated responses waste context and make it harder for Claude to extract what matters." The article gives the reason and a technique: resolving "arbitrary alphanumeric UUIDs to more semantically meaningful and interpretable language" improves precision by reducing hallucinations, and a `response_format` enum (`concise` or `detailed`) lets the agent ask for identifiers only when a later call needs them. In the article's example the concise response used about a third of the tokens.

### Page a long result

A tool that can return a lot must not return it all. The article suggests "some combination of pagination, range selection, filtering, and/or truncation with sensible default parameter values for any tool responses that could use up lots of context", and says Claude Code limits tool responses to 25,000 tokens by default (module 55). It adds: "We expect the effective context length of agents to grow over time, but the need for context-efficient tools to remain." Its example of a good tool is a search: "you might choose to implement a `search_contacts` or `message_contact` tool instead of a `list_contacts` tool."

MCP defines the pagination the course's contract copies. The cursor is an opaque string: "Pagination in MCP uses an opaque cursor-based approach, instead of numbered pages", the server decides the size of a page and "clients MUST NOT assume a fixed page size", and a response carries `nextCursor` when more results exist. The course's pager is a design in the same spirit, with four rules that the practice tests:

- the limit is a whole number of at least 1, cut to a maximum (50 here), so an agent cannot ask for everything at once;
- the cursor hides its content and an invalid one is refused, so the agent cannot invent a position;
- the page also stops at a size cap, taking at least one item, however long, so that one huge item still gets through;
- when more remains, a note says how to go on, "Showing 10 of 25 results; pass next_cursor to continue, or narrow the query with a filter."

The last rule is the one a team forgets. The article: "If you choose to truncate responses, be sure to steer agents with helpful instructions. You can directly encourage agents to pursue more token-efficient strategies, like making many small and targeted searches instead of a single, broad search." A list that silently stops at fifty looks complete to a model, and it answers a question about "all the orders" with fifty. The note is part of the result.

### Annotations: a claim, not a control

A tool can carry annotations, "Optional properties describing tool behavior" in the specification's words. The specification is plain about their weight: clients "MUST consider tool annotations to be untrusted unless they come from trusted servers" (module 32 has the full discussion). The Agent SDK uses the hints in one place: `readOnlyHint` "Controls whether the tool can be called in parallel with other read-only tools", with a default of false; the others are "Informational only". Its page says what that means for a team: "Annotations are metadata, not enforcement. A tool marked `readOnlyHint: true` can still write to disk if that's what the handler does. Keep the annotation accurate to the handler."

Three things follow for design. Mark your own read-only tools read-only, since that lets the SDK run them side by side. Do not let any control depend on a hint from a server you do not run, because a false one costs you a confirmation you needed. And lint for contradictions (the practice's rule): a tool called `delete_order` that says it is read-only, or a `remove_item` that says it is not destructive, is wrong in one place or the other, and a reviewer should look at both.

### Large tool sets and tool search

Tool definitions are part of every request. The tool search page gives both costs: they "can consume large portions of the context window (50 tools can use 10-20K tokens)", and "Tool selection accuracy degrades with more than 30-50 tools loaded at once." Tool search answers it: definitions are withheld and the agent "receives a summary of available tools and searches for relevant ones when the task requires a capability not already loaded"; up to five of the most relevant are loaded, and they stay until the SDK compacts the messages where the agent found them. It costs a round trip: "Tool search adds one extra round-trip each time Claude searches for tools", so it pays off at scale, and "With fewer than ~10 tools whose definitions fit comfortably in the context window, loading everything upfront is typically faster." Names matter more here, since the agent searches by them, which is a second reason for the prefixes of page 1.

Whether it is on is a setting. The page says "Tool search is on by default, with the exceptions listed in Configure tool search", and its `ENABLE_TOOL_SEARCH` variable chooses the mode: with `auto` the SDK counts the tokens of the definitions that can be deferred against the context window, and "When the total reaches 10% of the window, tool search activates. Below that, the SDK loads every tool definition into context upfront." `auto:5` moves the point to 5%, and `false` loads everything on every turn.

Tool search is a second line of defence. The first is giving each agent only the tools of its role (module 54), because a tool the agent never sees cannot be chosen wrongly and costs nothing to define.

### When to redesign and not to reprompt

Collect the numbers the article recommends: accuracy, "the total runtime of individual tool calls and tasks, the total number of tool calls, the total token consumption, and tool errors". They point at the fix. "Lots of redundant tool calls might suggest some rightsizing of pagination or token limit parameters is warranted; lots of tool errors for invalid parameters might suggest tools could use clearer descriptions or better examples." The article also warns that "Too many tools or overlapping tools can also distract agents from pursuing efficient strategies." A model that repeatedly picks a tool that cannot do the job is the signal for the surface itself: the tools cannot be told apart from their texts, and rewording the system prompt for the fourth time will not change that. The article's reminder is that even small changes to a description can matter: "Even small refinements to tool descriptions can yield dramatic improvements."

### What Java and Kotlin teams use

Everything on this page applies unchanged: results, pagers and lint rules are plain code, and the practice is in all four languages. The SDK's `readOnlyHint` and tool search belong to the Agent SDK (Python and TypeScript); in a loop of your own (module 45) you run read-only tools concurrently yourself, by the same rule.

### The practice

The practice is `exercises/52-designing-tool-interfaces/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin, written as four steps, each with the reason the exam cares and what you should see when it works. You write `lint_tool` (twelve rules), `lint_tool_set` (duplicates, overlap and size), `page_results` (opaque cursor, limit, size cap, note) and `effective_hints` with `parallel_safe` (a hint counts only from a trusted server).

Nine cases grade it: the main path, names, descriptions, schemas and examples, list tools and hints, the set, the cursor and the limit, the size cap, and the trust rule. The starter fails all nine, the reference passes them, and each of twenty planted wrong solutions per language fails on an assertion of the case it breaks: a name with spaces, a vague name matched in lower case only, two sentences accepted, a boundary of one phrase only, a required name that is not a property, an example outside its enum, a boolean taken for an integer, an undescribed parameter, a list tool that needs a limit but no cursor, a hint check that skips the destructive hint, two tools with one name, an overlap at exactly the threshold missed, a set one too large flagged, an unclamped limit, a missing note, an invalid cursor accepted, a page one item short at the cap, a cut page never reported, and hints honoured from an untrusted server, for the hint itself and for parallel calls.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Return every match and tell the model to ignore what it does not need."** It is tempting because nothing is withheld. The exam rejects it: bloated responses waste context and make it harder for the model to extract what matters. Page the result and say how to narrow it.
2. **"Cut the list off at a fixed length and return the rest as nothing."** It is tempting because the context stays small. The exam rejects it: if you truncate, you must steer the agent, or a partial list looks like the whole one. Put the continuation and the filter advice in a note.
3. **"The tool says it is read-only, so skip the confirmation."** It is tempting because the server knows its own tool. The exam rejects it: annotations are untrusted unless the server is trusted, and they are metadata, not enforcement. Let a hint help a good client, and never let it replace a check.
4. **"Connect every server to every agent, since tool selection is the model's job."** It is tempting because more capability seems better. The exam rejects it: selection degrades as tools pile up and the definitions fill the context. Give each agent the tools of its role first, then defer the rest.

## Quiz

3. A `search_orders` tool returns every match, sometimes thousands of rows, and the agent runs out of context before it can answer. What change helps most?
   - **a**: Send every row in a compact format and let the model skip the extras
   - **b**: Serve a bounded page with a cursor and a note on how to continue
   - **c**: Keep only the first fifty rows, sorted with the newest first
   - **d**: Move the agent to a model with a larger context window

4. A `find_documents` tool answers with the sentence "Found these documents: Maintenance Schedule, Lab Access Plan." The next tool, `read_document`, requires an exact handle for one document, and the agent keeps making them up. What change fits best?
   - **a**: Return each hit with a stable identifier and the fields the following step needs
   - **b**: Add a prompt rule that tells the model to derive each id from the title it was shown
   - **c**: Return every stored field of each hit, so that nothing is missing
   - **d**: Lengthen the sentence, describing each document in more prose

<details>
<summary>Answer key</summary>

3. **b**. A page with a cursor bounds the result, and the note tells the agent how to go on. *a* is ruled out because "Bloated responses waste context and make it harder for Claude to extract what matters." *c* is ruled out because "A list that silently stops at fifty looks complete to a model", so the agent would answer from part of the matches. *d* is ruled out because the article expects "the need for context-efficient tools to remain".
4. **a**. The tool should hand the next step a real identifier, in a result trimmed to what that step needs. *b* is ruled out because the documentation asks the tool to "Return semantic, stable identifiers (for example, slugs or UUIDs) rather than opaque internal references", not the model to reconstruct them. *c* is ruled out because a result should "include only the fields Claude needs to reason about its next step". *d* is ruled out because more prose adds length and no handle: "Bloated responses waste context and make it harder for Claude to extract what matters."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with four tools: `get_customer`, `lookup_order`, `process_refund` and `escalate_to_human`. Over a week of logs it sends many plain refund requests to `escalate_to_human`, and three rewrites of the system prompt have not changed that. What does this most likely indicate?
   - **a**: The set holds too many tools for the model to choose among reliably
   - **b**: The prompt still needs a sharper rule about which tool to prefer
   - **c**: The descriptions fail to separate the options and need a redesign
   - **d**: The prompt lacks worked examples of refund requests for the model

2. Scenario S3, a multi-agent research system. A coordinator delegates to a web search agent and a document agent, and the cited reports mix up their sources. Both agents hold tools named `analyze_content` and `analyze_document` whose descriptions are nearly the same. Which change fits best?
   - **a**: Keep both names and add a prompt rule to use the first for web pages
   - **b**: Rename one for fetched pages and narrow its text to those pages
   - **c**: Merge the pair into one `analyze` tool with a `source` parameter
   - **d**: Add a parameter where each agent explains its reasoning

3. Scenario S4, developer productivity with Claude. The agent is built on the Claude Agent SDK and helps engineers explore unfamiliar codebases and understand legacy systems. It uses one MCP server with eight tools whose schemas take about 3% of the context window, and the team sets `ENABLE_TOOL_SEARCH=auto`. What does the SDK do?
   - **a**: Loads all of them at the start, as they are under the activation level
   - **b**: Defers all eight and loads them on demand, since the mode is enabled
   - **c**: Loads all eight, then defers them once the conversation passes 10%
   - **d**: Defers all but the tools that the first prompt happens to name

<details>
<summary>Answer key</summary>

1. **c**. Repeated wrong selection that survives prompt rewrites means the tools cannot be told apart from their texts, which is a design fault. *b* is ruled out because the fault is in the surface, and "rewording the system prompt for the fourth time will not change that". *a* is ruled out because four tools are far below the scale at which "Tool selection accuracy degrades with more than 30-50 tools loaded at once." *d* is ruled out because examples in the prompt "leave the descriptions as thin as they were", and the refund and escalation texts still read alike.
2. **b**. A web-specific name and description removes the overlap at its source. *a* is ruled out because the overlap stays in the descriptions, and the page says to "keep the prompt free of tool-specific triggers". *c* is ruled out because a merged tool "hides two contracts behind one name", while a fetched page and an uploaded document are different jobs. *d* is ruled out because "A parameter that asks for the model's thinking or step-by-step reasoning may lead to a `reasoning_extraction` refusal."
3. **a**. Under `auto` the point is 10% of the window, and below it everything loads at the start. *b* is ruled out because "Below that, the SDK loads every tool definition into context upfront." *c* is ruled out because the SDK "counts the tokens of the definitions that can be deferred against the context window", not the length of the conversation. *d* is ruled out because the choice turns on the size of the definitions, not on the prompt: "When the total reaches 10% of the window, tool search activates."

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
