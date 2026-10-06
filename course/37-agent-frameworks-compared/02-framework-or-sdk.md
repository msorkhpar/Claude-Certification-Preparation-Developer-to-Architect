# When a framework beats the SDK

**Level:** Developer · **Module 37:** Agent frameworks compared · **Page 2 of 2**
**Exams:** DV3

**After this page you can** give the engineering advice on starting with direct API calls, list the problems a framework solves and the one it adds, and choose between a framework, the Agent SDK, the Messages API and Managed Agents for a given requirement.

Checked on 2026-10-03 against "Building effective agents" (published 2024-12-19, which carries a note that much of its tooling landscape has changed), the LangGraph overview and the Pydantic AI home page. Nothing here was run against a framework. The example is the one on page 1, and it is not repeated.

## Why it matters

The question on the exam is a decision, not a definition: given a requirement, which tool? The answer is almost never "the most powerful one". It is the least machinery that meets the requirement, with a reason you can state.

## The idea

### What the engineering advice says

The post begins with the rule: "When building applications with LLMs, we recommend finding the simplest solution possible, and only increasing complexity when needed." It adds that this "might mean not building agentic systems at all", and that "For many applications, however, optimizing single LLM calls with retrieval and in-context examples is usually enough."

On frameworks the post is balanced. They "make it easy to get started by simplifying standard low-level tasks like calling LLMs, defining and parsing tools, and chaining calls together." The cost is named in the next sentence: "they often create extra layers of abstraction that can obscure the underlying prompts and responses, making them harder to debug." The advice follows: "We suggest that developers start by using LLM APIs directly", and later "don't hesitate to reduce abstraction layers and build with basic components as you move to production." The page itself notes that the post is from December 2024 and that the tooling has changed since, which is a reason to take the principle and not the product list.

### What a framework is worth

Weigh what it gives against what it hides. A framework gives you a loop you did not write, tool schemas and parsing, retries, tracing hooks, and in some families state and persistence. Pydantic AI's page lists agents that "survive restarts and run for days", with "human-in-the-loop approval built in", and LangGraph lists "durable execution, streaming, human-in-the-loop". These are features you would otherwise build. What the framework hides is the prompt and the response, which are the things you debug. A framework earns its place when the feature it brings is one you need and would build badly, and not before.

A second cost is coupling. Your state format, your tool definitions and your tests then follow the framework's releases. Keeping the model call behind a thin function of your own limits the damage.

### The five options for an Anthropic stack

| Option | Choose it when | What you own |
|---|---|---|
| Messages API with your own loop | You need exact control, or the task is one call or a short chain | The loop, the tools, the state |
| Client SDK tool runner | The loop is plain tool use and you want less code | Tools and state, in your process |
| Agent SDK | You want Claude Code's loop, tools and permissions in your service | The process and its machine |
| A framework | You need its state, resume or typed output, and will pay in coupling | The graph or schema, plus upgrades |
| Managed Agents | The task is long-running or asynchronous and you want no sandbox to run | Configuration, the budget, the data policy |

Read the table as an order of questions. Start from the top and stop at the first row that meets the requirement. A requirement of "survive a restart and resume" points at a graph's checkpoints, at Managed Agents, or at a store you write. A requirement of "the reply must be a refund record" points at a typed layer, which you can also write with one validation function. A requirement of "the process must be auditable" favours any option in which the route is fixed in code.

### Testing the choice

Write the requirement as a sentence with a number in it: "resume within a minute of a crash", "no more than two model calls per ticket". Then check whether the simplest option meets it. If the Messages API does, stop there. If it does not, name the missing feature, and pick the smallest option that supplies it. A framework chosen for a feature nobody can name is a framework chosen for fashion.

## Traps

1. **Starting with the framework.** The advice is to start with direct calls and add layers when a need appears, because the layers hide the prompts.
2. **Counting features instead of needs.** A framework that offers persistence helps only when the product must resume. Otherwise it is code to maintain.
3. **Coupling the whole codebase to one framework.** Keep your tools and your model call behind your own functions, so that a change of framework is a change in one place.

## Quiz

1. A team's feature is one model call that summarises a document, with retrieved passages added to the prompt. What does the engineering advice suggest?
   - **a**: A graph framework with durable state and resume support
   - **b**: The Messages API alone, refined with in-context examples
   - **c**: The Agent SDK running Claude Code's loop in the service
   - **d**: Managed Agents with the session stored on Anthropic's side

2. A framework agent gives odd answers, and nobody on the team can read the exact text it sent to the model. Which cost of frameworks is this?
   - **a**: Coupling, as state formats follow the framework's releases
   - **b**: A missing feature, as frameworks offer no tracing hooks
   - **c**: Unneeded features, as persistence adds code to maintain
   - **d**: Hidden prompts, as layers obscure the prompts and responses

3. A product must survive a restart and resume a long task, and the team does not want to operate the sandbox in which the code runs. Which option is the smallest that fits?
   - **a**: The Messages API, with a loop that the team writes and stores
   - **b**: Managed Agents, with the session kept on Anthropic's side
   - **c**: A graph framework, hosted and upgraded by the team itself
   - **d**: The Claude Code CLI, started by a person every morning

<details>
<summary>Answer key</summary>

1. **b**. The post says "For many applications, however, optimizing single LLM calls with retrieval and in-context examples is usually enough." *a* is ruled out because "A framework earns its place when the feature it brings is one you need and would build badly, and not before.", and one call needs no durable state. *c* is ruled out because "Start from the top and stop at the first row that meets the requirement.", and one call is met by the first row, before the Agent SDK. *d* is ruled out because Managed Agents is chosen when "The task is long-running or asynchronous", and this is one short call.
2. **d**. The post says frameworks "often create extra layers of abstraction that can obscure the underlying prompts and responses, making them harder to debug." *a* is ruled out because coupling is that "Your state format, your tool definitions and your tests then follow the framework's releases.", which says nothing of reading the prompt. *b* is ruled out because a framework gives you "tool schemas and parsing, retries, tracing hooks", so the feature is not missing. *c* is ruled out because "Otherwise it is code to maintain." is the cost of features nobody needs, and the symptom here is a prompt nobody can read.
3. **b**. The table gives Managed Agents the task that is "long-running or asynchronous" for those who want "no sandbox to run", with the session stored by Anthropic. *a* is ruled out because a loop of your own leaves "The loop, the tools, the state" and the sandbox to the team, which is more than the smallest option. *c* is ruled out because a graph is chosen when you "need its state, resume or typed output, and will pay in coupling", and the team would still host it. *d* is ruled out because the table row for Managed Agents reads "The task is long-running or asynchronous and you want no sandbox to run", and a person starting a CLI each morning is not a service that resumes.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Regulators will audit an approval chain whose steps never change. Which style does the first page match to that need?
   - **a**: Graph-based, with nodes and edges drawn beforehand
   - **b**: Model-driven, with the model choosing at each turn
   - **c**: Typed, with each reply validated against a schema
   - **d**: A mix in which the model picks the route and a graph logs it

2. A team wants to be able to replace its agent framework later. Which design choice keeps that cheap?
   - **a**: Copying the framework's source into the product's repository
   - **b**: Storing the sessions in the format that the framework defines
   - **c**: Wrapping each model call and tool in a function of its own
   - **d**: Importing the framework's types directly in each module

<details>
<summary>Answer key</summary>

1. **a**. The first page says to use the graph style "when the process is known and must be auditable: an approval chain, an order flow". *b* is ruled out because the table gives the next step to "The model, at each turn", and "The route is whatever the model chooses". *c* is ruled out because "Types guard the edges of the model's freedom. They do not choose the route." *d* is ruled out because "The model fills in a node. It does not pick the route."
2. **c**. The page says "Keeping the model call behind a thin function of your own limits the damage". *a* is ruled out because the trap is "Coupling the whole codebase to one framework", and a copy of the source ties the repository to one. *b* is ruled out because "Your state format, your tool definitions and your tests then follow the framework's releases." *d* is ruled out because the aim is "so that a change of framework is a change in one place", and imports in each module spread the change.

</details>
