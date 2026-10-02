# The surfaces: one model behind many doors

**Level:** Foundations · **Module 3:** Claude's family and its surfaces · **Page 2 of 2**
**Exams:** all (AS3, DV1, DV2)

**After this page you can** place every way of using Claude (the apps, Claude Code, the API, the Agent SDK,
managed agents, the cloud platforms) by who uses it and what it hands you, and say what carries across them
and what does not.

Checked against the Claude API documentation (intro, models overview, managed agents overview), the Claude
Code overview and the Claude help centre page on Projects, read on 2026-10-02. The apps are taught in depth
in modules 7 and 8, Claude Code in modules 38 and 39, the Agent SDK in module 35, managed agents in
module 36 and the cloud platforms in modules 22 and 23; this page is the map.

## Why it matters

A question that begins "a team wants Claude to..." is half answered once you know which surface fits:
a person chatting, a developer calling an endpoint, a coding agent in a repository, a long-running job in
managed infrastructure, a company that must keep traffic inside a cloud account. Mixing the surfaces up is
the most common wrong option in scenario questions, and the commonest cause of an expensive rebuild.

## The idea

### The map

| Surface | Who it is for | What you get | You write |
|---|---|---|---|
| **The Claude apps** (web, desktop, mobile) | People doing knowledge work | Chat, Projects, artifacts, research, connectors; a person in the loop | Instructions and uploads, not code |
| **Cowork** | Placed here by name; see module 8 | Taught in module 8 from the official pages | See module 8 |
| **Claude Code** | Developers working in a repository | An agentic coding tool in the terminal, IDE extensions, the desktop app and the web | Memory files, settings, skills, hooks |
| **The Messages API** | Developers building products | Direct access to the model; your own loop, tools and state | Application code |
| **The Agent SDK** | Developers building custom agents | The Claude Code engine and tools as a library | Agent code |
| **Claude Managed Agents** | Teams with long-running, asynchronous work | A pre-built agent harness in managed infrastructure | Agent and environment configuration |
| **The cloud platforms** | Organisations that buy Claude through a cloud account | The same models through Amazon Bedrock, Google Cloud, Microsoft Foundry or Claude Platform on AWS | Application code, plus cloud configuration |

What each row is, from the official pages:

- **The apps.** A *Project* is, in the help centre's words, a self-contained workspace with its own chat
  histories and knowledge base: you can "upload relevant documents, text, code, or other files to a
  project's knowledge base", set custom instructions, and on team plans share it. Module 7 teaches
  configuring and maintaining one. Cowork is named here so you can place it; its detail comes in module 8
  from the official pages, not from this page.
- **Claude Code** is described as "an agentic coding tool that reads your codebase, edits files, runs
  commands, and integrates with your development tools", available in the terminal, IDE extensions, a
  desktop app and the browser. The page says each surface "connects to the same underlying Claude Code
  engine, so your repo's CLAUDE.md files, settings, and MCP servers work across all of them."
- **The Messages API** is described as "direct model prompting access", best for "custom agent loops and
  fine-grained control". Module 14 teaches it.
- **The Agent SDK** lets you "build your own agents powered by Claude Code's tools and capabilities, with
  full control over orchestration, tool access, and permissions" (Claude Code overview).
- **Claude Managed Agents** is a "pre-built, configurable agent harness that runs in managed
  infrastructure", in beta at the time of reading, built around four ideas: an **agent** (model, system
  prompt, tools, MCP servers, skills), an **environment** (where sessions run), a **session** (a running
  instance) and **events** (messages between your app and the agent). It is stateful, and the page says it
  is not currently eligible for zero data retention or HIPAA coverage.
- **The cloud platforms.** The models overview lists a separate id for each platform: for example
  `anthropic.claude-opus-5-5` on Amazon Bedrock and `claude-haiku-4-5@20251001` on Google Cloud, with Claude
  Platform on AWS and Microsoft Foundry using the Claude API ids. The platform sets its own lifecycle dates
  for Bedrock and Google Cloud. Same model family; different ids, access and sometimes features.

Source: Intro to Claude, Models overview and Managed Agents overview (Claude API documentation); Claude Code
overview; What are Projects (Claude help centre).

### One model, many surfaces: what carries across

The model is the same family behind every door, so its capabilities and its quirks are the same: tokens,
context windows, hallucination, steerability. What differs is everything wrapped around it:

- **What is sent with your text.** An app or a coding tool adds its own instructions, tools and defaults to
  what you type. The API sends only what you put in the request. The same sentence can therefore behave
  differently on two surfaces, because the surrounding context is different.
- **Who owns state.** An app keeps your conversation. The API keeps nothing; your code resends history
  (module 1). A managed agent keeps session state server-side.
- **Where instructions live.** In an app, in a Project's instructions. In Claude Code, in memory files and
  settings that travel with the repository across its surfaces. In the API, in the system prompt your code
  sends. Instructions written for one place do not appear in another unless you put them there.
- **Who is in the loop.** An app has a person at every step; an agent harness may run for hours; a cloud
  platform adds its own identity and permission model.

Choosing a surface is therefore choosing how much of that wrapping you want to own.

### Choosing by the situation

| The situation | The surface it points to |
|---|---|
| A manager wants to summarise reports and keep recurring context | An app, with a Project |
| A developer wants help changing a codebase | Claude Code |
| A product needs Claude inside its own screens | The Messages API |
| A team wants a bespoke agent reusing Claude Code's tools | The Agent SDK |
| A job runs for hours and should not tie up your servers | Managed agents |
| Security requires all traffic through the company's cloud account | A cloud platform |

## Traps

1. **Using the API for what a Project does.** A recurring chat workflow for a non-developer does not need
   code; building an endpoint adds cost and maintenance for nothing.
2. **Assuming ids and features are identical on every platform.** The ids differ, and the platform sets its
   own dates; read the model's page for the platform before you migrate.
3. **Expecting instructions to follow you between surfaces.** A Project's instructions do not reach the API,
   and a system prompt in your code does not reach the app. Put the instruction where it is read.

## Quiz

1. A marketing manager with no engineering support wants Claude to draft weekly newsletters in the house
   style, using last quarter's issues as reference. Which approach fits best?
   - **a**: Build a service that calls the Messages API weekly
   - **b**: Request a dedicated cloud-platform deployment
   - **c**: Run an Agent SDK program on a scheduled server
   - **d**: Create an app Project holding the guidelines and earlier editions

2. A bank requires that all model traffic stay inside its existing infrastructure account, under its own
   identity rules, while developers keep using the same models. Which surface fits?
   - **a**: The consumer apps on personal accounts
   - **b**: Calling them through the company's cloud provider
   - **c**: A managed agent in Anthropic's sandbox
   - **d**: The terminal tool on each laptop

3. A developer says: "I told Claude in our shared workspace to answer in French, so my API integration will too."
   What is wrong with this expectation?
   - **a**: French is unsupported on the API
   - **b**: Instructions stay hidden until a tier upgrade
   - **c**: The API ignores any language requests
   - **d**: Those instructions are read only inside the chat product

<details>
<summary>Answer key</summary>

1. **d**. A Project holds instructions and reference files for recurring non-developer work (the apps bullet). *a* is ruled out because the map says building application code buys nothing here. *c* is ruled out because an agent on a server is infrastructure for a task a Project does without code. *b* is ruled out because cloud platforms address account and traffic control, which the scenario does not mention.
2. **b**. The cloud platforms row is the one that keeps traffic in the customer's cloud account and identity model. *a* is ruled out because personal consumer accounts are outside the bank's control. *c* is ruled out because managed agents run in Anthropic-managed or self-hosted sandboxes, and the page says they are not eligible for zero data retention. *d* is ruled out because a developer tool on each laptop answers how developers work, not where the traffic and identity rules sit.
3. **d**. Where instructions live decides where they are read (the carry-across section). *a* is ruled out because nothing on the pages restricts languages on the API. *c* is ruled out because the API follows instructions in the request and system prompt. *b* is ruled out because tiers do not gate where an instruction is read.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A support team writes thousands of short, similar replies each hour and occasionally meets a delicate
   complaint. Which plan balances cost and quality?
   - **a**: Route routine drafts to a fast tier and rare complaints to a stronger one
   - **b**: Send every draft to the strongest tier for uniform quality
   - **c**: Send every draft to the fastest tier to protect the budget
   - **d**: Alternate tiers by time of day to even out the load

2. A new feature must call a specific Claude model for the next two years. A reviewer flags the model id in
   the code. What is the most likely concern?
   - **a**: The model's retirement date may arrive before the product's end
   - **b**: Dated ids cannot be used with the API
   - **c**: Ids must be changed with every request
   - **d**: Alias names raise the price per token

3. A solo developer wants help refactoring a repository from the terminal. Which surface fits?
   - **a**: A Project in the chat app
   - **b**: A bespoke integration on the Messages API
   - **c**: Claude Code
   - **d**: A managed agent session

4. A team pins `claude-haiku-4-5-20251001` in production and plans nothing else. Which statement is true,
   from the model table?
   - **a**: The id will track new releases automatically
   - **b**: The id is a pinned snapshot with a published retirement window
   - **c**: The id is valid only on cloud platforms
   - **d**: The id has a larger window than the top tier

<details>
<summary>Answer key</summary>

1. **a**. Matching the model to the task in both directions, with an evaluation set, is the worked decision. *b* is ruled out because it wastes cost and latency on routine work. *c* is ruled out because it under-serves the complaints that need depth. *d* is ruled out because time of day matches no task to a tier.
2. **a**. The table shows retirement dates, and a model with a near date needs a migration plan. *b* is ruled out because the table gives a dated id for Haiku 4.5. *c* is ruled out because a pinned id is meant to stay fixed. *d* is ruled out because the pages say nothing about alias pricing.
3. **c**. Claude Code is the agentic coding tool for a repository. *a* is ruled out because a chat Project has no repository access. *b* is ruled out because it means building the tool the product already provides. *d* is ruled out because a managed agent suits long-running asynchronous work in managed infrastructure, not a developer's interactive refactor.
4. **b**. The page states every id is a pinned snapshot and lists a retirement date for it. *a* is ruled out because a snapshot does not move. *c* is ruled out because the same id works on the Claude API, as the table shows. *d* is ruled out because the 200K window is the smallest in the table.

</details>
