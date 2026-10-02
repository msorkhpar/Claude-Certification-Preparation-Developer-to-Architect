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

1. A marketing manager with no engineering support needs weekly newsletters drafted in the house voice, using
   last quarter's issues as reference. Which approach fits best?
   - **a**: A Project in the app holding the tone guide and earlier editions
   - **b**: A scheduled script that calls the Messages API weekly with the issues attached
   - **c**: A bespoke agent on the Agent SDK that reads the archive before each draft
   - **d**: A cloud-platform deployment so the drafts stay inside the company account

2. A bank requires every call to a Claude model to run under the identity and permission rules of the
   hyperscaler it already buys infrastructure from. Developers still want the same models. Which option fits?
   - **a**: Give each developer a managed-agent environment under the bank's own tenancy
   - **b**: Keep calling the Messages API directly and add the bank's rules to the system prompt
   - **c**: Run Claude Code on every laptop, since it connects to the same engine everywhere
   - **d**: Take them through the matching cloud platform listing

3. A developer wrote "answer in French" into a Project's instructions. Her own integration, calling the
   Messages API, still replies in English. What is the best explanation?
   - **a**: The Messages API supports fewer languages than the apps do
   - **b**: Settings kept in the chat product are not sent with requests from code
   - **c**: French must be requested through a model parameter that the API leaves unset
   - **d**: The integration needs the Agent SDK before it can follow language rules

<details>
<summary>Answer key</summary>

1. **a**. A Project holds instructions and reference files for recurring non-developer work (the apps bullet and the first trap). *b* is ruled out because "A recurring chat workflow for a non-developer does not need code". *c* is ruled out because the Agent SDK row is for "Developers building custom agents", and a Project already holds the archive without code. *d* is ruled out because the cloud platforms row is for "Organisations that buy Claude through a cloud account", which the scenario never mentions.
2. **d**. The cloud platforms row carries the customer's cloud identity and permission model while serving the same models. *a* is ruled out because a managed agent suits "Teams with long-running, asynchronous work", and the page says nothing of it adopting the bank's tenancy. *b* is ruled out because "a cloud platform adds its own identity and permission model", while a system prompt only instructs the model. *c* is ruled out because the shared engine only means "your repo's CLAUDE.md files, settings, and MCP servers work across all of them", not that identity rules are enforced.
3. **b**. Where instructions live decides where they are read, and a Project's instructions do not reach the API (the carry-across section and the third trap). *a* is ruled out because "The model is the same family behind every door, so its capabilities and its quirks are the same". *c* is ruled out because the instruction lives "In the API, in the system prompt your code sends", and no model parameter is involved. *d* is ruled out because "Instructions written for one place do not appear in another unless you put them there", and no other surface is a precondition.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team's research job runs for hours with nobody watching: mostly routine page fetching and note-taking,
   with a few hard judgment calls. Which design fits?
   - **a**: Host it in managed infrastructure, with cheap workers for bulk and a stronger model to decide
   - **b**: Chat through an app Project, with a person starting each step by hand
   - **c**: Run the strongest tier through the Messages API from the team's own servers
   - **d**: Use the fastest tier for every step, and add a stronger one only if the job fails

2. A feature must call one specific Claude model for two years. In review, someone flags the id hard-coded in
   the source. What is the most likely concern?
   - **a**: Hard-coded ids bypass the pinned snapshot, so replies drift between calls
   - **b**: Dated ids are rejected by the Messages API in favour of aliases
   - **c**: A shorter alias would always follow new releases and should replace it
   - **d**: That snapshot has a retirement date, and nothing plans the move

3. A solo developer wants help restructuring a repository from the terminal, reading files and running the
   tests. Which surface fits best?
   - **a**: Claude Code, the agentic tool built for codebase work
   - **b**: The Agent SDK, to assemble a custom agent first
   - **c**: A Project in the app, with the files uploaded to its knowledge base
   - **d**: A Managed Agents session, since restructuring takes a while

4. A team calls the same Claude model through the Claude API and through Amazon Bedrock, reusing one config
   file with a single name string for both. Both platforms list the model as current and access is granted,
   but Bedrock answers that the name is unknown. What is the most likely cause?
   - **a**: Bedrock runs a different generation of the family, so the name never matched
   - **b**: Each vendor gives the family its own id, so the value must differ
   - **c**: The account lacks the identity and permission setup that Bedrock requires
   - **d**: Bedrock already retired that model, so its name is unknown there

<details>
<summary>Answer key</summary>

1. **a**. A job that runs for hours belongs in managed agents, and an orchestrator that hands bulk work to cheaper workers puts most tokens on the cheaper model while a stronger one takes the hard calls. *b* is ruled out because the apps give "Chat, Projects, artifacts, research, connectors; a person in the loop", and nobody is watching here. *c* is ruled out because the situation table points "A job runs for hours and should not tie up your servers" to managed agents. *d* is ruled out because "the cheapest model for everything under-serves exactly the cases that need depth".
2. **d**. The table shows retirement dates, and an id with a retirement date needs a migration plan before the date (the third trap). *a* is ruled out because "every Claude model id is a pinned snapshot", so replies do not drift under a fixed id. *b* is ruled out because "For Haiku 4.5 the dated id is the snapshot and the shorter name is an alias", so dated ids are accepted. *c* is ruled out because the page counts "including the dateless IDs used from the 4.6 generation on" among the pinned snapshots, so an alias is no more of a tracker.
3. **a**. Claude Code is the agentic coding tool that reads a codebase and runs commands. *b* is ruled out because the Agent SDK row is for "Developers building custom agents", and this developer wants help with a repository, not a new agent to build. *c* is ruled out because the Project row says "Instructions and uploads, not code", and a knowledge base cannot run the tests. *d* is ruled out because managed agents suit "Teams with long-running, asynchronous work", not an interactive refactor.
4. **b**. The cloud platforms bullet lists a separate id for each platform, for example anthropic.claude-opus-5-5 on Amazon Bedrock. *a* is ruled out because the page says "Same model family; different ids, access and sometimes features", so the generation is the same and only the id differs. *c* is ruled out because the scenario grants access and the error concerns the name: "The ids differ, and the platform sets its own dates". *d* is ruled out because the scenario says both platforms list the model as current, as the page's example "anthropic.claude-opus-5-5 on Amazon Bedrock" shows for a current model.

</details>
