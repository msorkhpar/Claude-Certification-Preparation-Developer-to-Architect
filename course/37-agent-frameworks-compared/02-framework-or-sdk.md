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
   - **a**: A graph framework, so that the single call is saved as a checkpoint file
   - **b**: Improving the lone request with in-context examples and a search step
   - **c**: A managed agent, so that no code has to run inside the service itself
   - **d**: A multi-agent system, so that one agent reviews the finished work of another agent

2. A developer reports that a framework agent gives odd answers and that nobody can see the text sent to the model. Which cost of frameworks is this?
   - **a**: A licence fee that rises with every request the agent makes to a model
   - **b**: A cap on the number of tools that one agent may hold at a time
   - **c**: A duty to run the model on hardware that the team owns and operates
   - **d**: Extra layers that obscure the underlying prompts and responses

3. A product must survive a restart and resume a long task, and the team does not want to operate the sandbox in which the code runs. Which option is the smallest that fits?
   - **a**: The Messages API, with a loop that the team writes and stores
   - **b**: Managed Agents, with the session kept on Anthropic's side
   - **c**: A graph framework, hosted and upgraded by the team itself
   - **d**: The Claude Code CLI, started by a person every morning

<details>
<summary>Answer key</summary>

1. **b**. The post says "For many applications, however, optimizing single LLM calls with retrieval and in-context examples is usually enough." *a* is ruled out because the page says a framework "earns its place when the feature it brings is one you need", and one call needs no checkpoint. *d* is ruled out because the advice is "finding the simplest solution possible, and only increasing complexity when needed." *c* is ruled out because Managed Agents is chosen when "The task is long-running or asynchronous", and this is one short call.
2. **d**. The post says frameworks "often create extra layers of abstraction that can obscure the underlying prompts and responses, making them harder to debug." *a* is ruled out because the page lists what a framework hides and what it couples: "Your state format, your tool definitions and your tests then follow the framework's releases", and it names no fee. *b* is ruled out because the page lists what a framework gives you, "tool schemas and parsing, retries, tracing hooks", and names no tool cap. *c* is ruled out because "Keeping the model call behind a thin function of your own limits the damage", and the page sets no hardware duty.
3. **b**. The table gives Managed Agents the task that is "long-running or asynchronous" for those who want "no sandbox to run", with the session stored by Anthropic. *a* is ruled out because a loop of your own leaves "The loop, the tools, the state" and the sandbox to the team, which is more than the smallest option. *c* is ruled out because a graph is chosen when you "need its state, resume or typed output, and will pay in coupling", and the team would still host it. *d* is ruled out because the table row for Managed Agents reads "The task is long-running or asynchronous and you want no sandbox to run", and a person starting a CLI each morning is not a service that resumes.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team must choose between a graph framework and a model-driven loop for a fixed approval chain that regulators audit. Which is better, and why?
   - **a**: The route written down in code, because it is open to inspection
   - **b**: The loop, because the model reorders approvals to save time and effort
   - **c**: The loop, because a model-driven route is easier to reproduce later
   - **d**: The graph, because it removes the need for any tests of the flow itself

2. A team adopts a framework and later wants to leave it. Which earlier decision would have made leaving cheap?
   - **a**: Copying the framework's source into the repository of the product
   - **b**: Using the framework's internal types in every module of the code
   - **c**: Keeping tools and the model call behind its own thin functions
   - **d**: Storing the state in the private format that the framework defines

<details>
<summary>Answer key</summary>

1. **a**. The first page says a graph is for a process that is "known and must be auditable", with the next step decided "You, in advance". *b* is ruled out because the table gives the next step to "The model, at each turn", and an approval chain must not be reordered. *c* is ruled out because "The route is whatever the model chooses", and that is harder to reproduce, not easier. *d* is ruled out because the page leaves the checks in place: "Validation proves the shape."
2. **c**. The page says "Keeping the model call behind a thin function of your own limits the damage", and the trap advises "Keep your tools and your model call behind your own functions". *a* is ruled out because the trap is "Coupling the whole codebase to one framework", and a copy of the source would tie the repository to one. *b* is ruled out because internal types spread the same coupling: "Your state format, your tool definitions and your tests then follow the framework's releases." *d* is ruled out because a private state format is the first thing the page lists as following the releases: "Your state format, your tool definitions and your tests".

</details>
