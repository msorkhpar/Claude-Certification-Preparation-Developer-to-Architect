# Managed Agents: Anthropic runs the agent

**Level:** Developer · **Module 36:** Managed and self-hosted agents · **Page 1 of 2**
**Exams:** DV3; P1

**After this page you can** say what Claude Managed Agents is and which of Anthropic's options it is, name its four concepts and the order in which a run uses them, explain why an agent is versioned and what a session override does, list what the hosted harness takes off your hands, and say when it is the wrong choice.

Checked against the Managed Agents pages of the Claude API documentation on 2026-10-03: the overview, agent setup, tools, sessions and migration pages. The product is in beta, and every endpoint needs the beta header `managed-agents-2026-04-01`, which the SDKs set for you. Nothing on this page was run against the live service: no key was used and no request was sent. The only example is on page 2, and it checks configuration offline.

## Why it matters

Module 35 put the agent loop in your process: you start the Claude Code binary, you own the machine it runs on, and you answer its questions. Some workloads do not fit that. A task that runs for hours, a job that must survive a restart of your service, a scheduled run at three in the morning: each needs a sandbox, a place to keep the conversation and something to resume it. Managed Agents is Anthropic's answer, and the exam asks you to tell when to take it and what you give up. The questions are about ownership: who runs the loop, who runs the tools, and where the data sits.

## The idea

### Two ways to build, and the SDK in between

The overview opens with a two-column comparison.

| | Messages API | Claude Managed Agents |
|---|---|---|
| **What it is** | Direct model prompting access | Pre-built, configurable agent harness that runs in managed infrastructure |
| **Best for** | Custom agent loops and fine-grained control | Long-running tasks and asynchronous work |

The page says what the second column replaces: "Instead of building your own agent loop, tool execution, and runtime, you get a fully managed environment where Claude can read files, run commands, browse the web, and run code securely." Together with module 35, that makes four places to put an agent. You write the loop on the Messages API. You embed Claude Code's loop with the Agent SDK, in a process you operate. You use the CLI by hand. Or you let Anthropic host the harness. The Client SDK's tool runner, from module 26, drives the same kind of loop inside your process and hosts nothing. The harness comes with "built-in prompt caching, compaction, and other performance optimizations", which are things you would otherwise build or switch on yourself.

### Four concepts

The product is built around four concepts, and a run touches them in this order.

| Concept | What it is |
|---|---|
| **Agent** | The model, system prompt, tools, MCP servers, and skills |
| **Environment** | Where sessions run: an Anthropic-managed cloud sandbox, or a self-hosted sandbox on your own infrastructure |
| **Session** | A running agent instance within an environment, performing a specific task and generating outputs |
| **Events** | Messages exchanged between your application and the agent: user turns, tool results, status updates |

You create an agent once and reference it by ID. You create an environment. You start a session that names both. Then you send user messages as events, and "Claude autonomously runs tools and streams back results through server-sent events (SSE)." While it works you can send more events to steer it, or interrupt it. The page adds one fact that shapes the design: "Event history is persisted server-side and can be fetched in full." Your service holds an ID, not a conversation. Where the tools execute is the environment's job. Even a self-hosted sandbox leaves the loop with Anthropic: "Self-hosted sandboxes keep the orchestration on Anthropic's side but move tool execution into infrastructure you control."

### An agent is a versioned resource

An agent is "a reusable, versioned configuration that defines persona and capabilities." It bundles the model, the system prompt, the tools, the MCP servers and the skills. The required fields are a name and a model; the `tools` field combines the pre-built agent toolset (declared as `agent_toolset_20260401`), MCP tools and your own custom tools. The response carries a `version` that "starts at 1 and increments each time an update changes the agent."

Versions are what make a rollout controllable. A session created with just an agent ID "creates the session with the latest agent version", and a session can instead pin a version, so a new prompt can be promoted or rolled back without a deploy. For one run you can also override `model`, `system`, `tools`, `mcp_servers` or `skills`. The rule that surprises people is that overrides replace and never merge: "Overrides never merge with the agent's configuration, so a `tools` override must list every tool the session should have." They apply to that one session, and the agent resource and its version are untouched. A `model` override replaces the whole model object too, so the agent's own `effort` is not carried over.

### What the hosted harness gives you

The agent toolset has eight tools: `bash`, `read`, `write`, `edit`, `glob`, `grep`, `web_fetch` and `web_search`. Each is on when you include the toolset, and you can turn individual tools off, or start with everything off and enable a few. When an output "exceeds 100,000 characters (about 25,000 tokens), it is automatically written to a file in the sandbox", and the model gets a truncated preview with the path. Your own tools are declared as custom tools and run in your application: the session emits an `agent.custom_tool_use` event and you answer with a `user.custom_tool_result`. MCP servers are declared on the agent, and their credentials come from a vault on the session (page 2).

The overview lists the workloads that suit it: long-running execution, cloud infrastructure, self-hosted execution for compliance or data residency, "minimal infrastructure", stateful sessions with a persistent filesystem and history, and scheduled execution. It also names features that are further out: MCP tunnels and dreaming are "in a more limited research preview" within the beta.

### What it costs you

Three limits belong in the decision, all stated on the overview.

- **It is beta.** "Behaviors may be refined between releases to improve outputs." A contract that must not move belongs on the stable Messages API or in your own process.
- **It is stateful by design.** Sessions are long-running and "resume cleanly after pauses", and they "store conversation history, sandbox state, and outputs server-side. Because of this, Managed Agents is not currently eligible for Zero Data Retention or HIPAA Business Associate Agreement (BAA) coverage." You can delete sessions and files through the API, but the data exists on Anthropic's side first. A workload with a no-retention requirement cannot use the hosted option as it stands.
- **You give up per-run control.** The Agent SDK builds its options for each run and answers your callbacks in your process. Here the configuration is a persisted resource, and your approval step is an event you answer, not a function the harness calls.

### Moving an existing agent

The migration page says what changes. From a hand-written Messages API loop, you stop keeping the history array ("The session stores history server-side. Send events, receive events."), you stop iterating `tool_use` blocks for the built-in tools, you stop provisioning a sandbox, and you stop deciding when the loop is done, because "The session emits `session.status_idle` when the agent has nothing more to do." You still own the system prompt and model, which are now fields on the agent, and your custom tools, now answered as events. From the Agent SDK, `ClaudeAgentOptions` built for every run becomes an agent created once. The built-in tools run in the sandbox against `/workspace` and not on your filesystem, and `permission_mode` and `can_use_tool` become a per-tool permission policy, which page 2 covers.

## Traps

1. **Treating the beta as stable.** Behaviour can be refined between releases, and the product is outside Zero Data Retention. Check both against the requirement before choosing it.
2. **Expecting an override to merge.** A session override of `tools` lists every tool the session gets. A shorter list silently removes the rest.
3. **Reading "managed" as "your data stays with you".** Managed means Anthropic runs the loop, the sandbox and the storage. Where the tools run is a separate choice, made on page 2.

## Quiz

1. A team needs an agent to run a task for several hours, survive restarts of its own service, and be resumed the next day, and it does not want to build a sandbox. Which option fits?
   - **a**: The Client SDK's tool runner, which hosts the sandbox for the loop
   - **b**: The Messages API alone, which stores the conversation for each request
   - **c**: The Claude Code CLI, started again by hand each morning
   - **d**: The hosted harness, which keeps the session and its history on Anthropic's side

2. A session is created with an override that lists one tool. The agent itself defines five. How many tools does the session have?
   - **a**: One, because it takes the place of the whole set
   - **b**: Five, because an override cannot remove a tool of the agent
   - **c**: Six, because the override is added to the agent's tools
   - **d**: Zero, until the agent is updated to a new version

3. A compliance team requires Zero Data Retention for every Claude workload. What follows for Managed Agents?
   - **a**: It qualifies once the sandbox is self-hosted on the team's network
   - **b**: It qualifies, because ZDR applies to every endpoint of the API
   - **c**: It does not qualify, because sessions store history server-side
   - **d**: It qualifies after the sessions are deleted from the console

<details>
<summary>Answer key</summary>

1. **d**. The overview says the product is best for "Long-running execution", and that sessions "resume cleanly after pauses" with history and outputs stored server-side, so your service holds only an ID. *b* is ruled out because the Messages API is "Direct model prompting access", for "Custom agent loops and fine-grained control", and you keep the history yourself. *c* is ruled out because the CLI is something you drive yourself: "You use the CLI by hand." *a* is ruled out because the page says the tool runner "drives the same kind of loop inside your process and hosts nothing", while the sandbox belongs to the hosted product.
2. **a**. The page says "Overrides never merge with the agent's configuration, so a `tools` override must list every tool the session should have." *c* is ruled out because "Overrides never merge with the agent's configuration", so nothing is added to the agent's five tools. *b* is ruled out because "so a tools override must list every tool the session should have", and the agent's own list is not kept. *d* is ruled out because "They apply to that one session", and nothing waits for a new agent version.
3. **c**. The overview says "Managed Agents is not currently eligible for Zero Data Retention or HIPAA Business Associate Agreement (BAA) coverage", because sessions are stateful and stored server-side. *a* is ruled out because self-hosting moves only the tools: "keep the orchestration on Anthropic's side", so the session data still sits with Anthropic. *b* is ruled out because the overview says the product is "not currently eligible for Zero Data Retention", not that it is covered. *d* is ruled out because deleting sessions is a control you hold afterward, and "the data exists on Anthropic's side first", so the product is still not covered.

</details>
