# From a business problem to the simplest pattern

**Level:** Architect Professional · **Module 79:** From business problem to solution · **Page 1 of 2**
**Exams:** P1

**After this page you can** turn a business request into a decision statement a design can aim at, choose the lowest rung of the pattern ladder (one call, an augmented call, a workflow, an agent, a team of agents) that meets the requirement, say which of the five value pillars a design serves and how it is measured, and explain to a sponsor why two small components can beat one clever system.

Checked on 2026-10-04 against Anthropic's engineering article "Building effective agents" (published 2024-12-19; the article notes that parts of its tooling landscape have changed), Anthropic's article on its multi-agent research system, and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 1. This module teaches judgment and uses no model call: its worked example is arithmetic and tables, and module 80 turns the ladder into code.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the objectives of domain 1 name three patterns to choose between, "workflow, agentic, augmented LLM", and ask the candidate to translate a business problem into a Claude solution and to align it with value pillars. *What Anthropic's article says:* the augmented LLM, "an LLM enhanced with augmentations such as retrieval, tools, and memory", is the building block of the other two, and workflows and agents are systems made from it. *How to read both:* the exam treats the three as options of rising cost and autonomy, so choose the one that meets the stated requirement with the least machinery. A pattern chosen for its novelty, a team of agents where a retrieval call would do, is the answer the exam rejects.

## Why it matters

Domain 1 of the Professional exam, solution design and architecture, is 17 percent of the paper, and its questions start from a business sentence, not from an API. A vendor proposes one autonomous system for two different jobs. A sponsor asks for "AI for claims". A team wants agents because agents are the topic of the year. The architect is paid to ask what decision is being made, how often, how fast, at what cost of error and under whose accountability, and then to pick a design that meets the answers and no more. Everything later in Level 4 (reliability, models, deployment, cost, evaluation, governance) rests on this first choice.

## The idea

### A business problem is not yet a requirement

"Reduce claims handling time" names a hope. A design needs a **decision statement**: one sentence per decision that the system takes or supports, with the facts that bound it. Before any pattern is chosen, collect these.

| Question | Why the design needs it | Example answer |
|---|---|---|
| What is decided, and by whom today? | Names the work to replace, support or leave alone | A handler decides whether a claim is covered |
| How many a day, and when are the peaks? | Sets volume, capacity and the price per task | 2,000 a day, a third of them on Monday |
| How fast must the answer be, and for whom? | A person at a screen and a nightly job need different shapes | Within a minute for the handler, overnight for the audit |
| How wrong may it be, and what does a wrong answer cost? | Sets the accuracy bar and the review design (page 2) | A wrong approval costs far more than a wrong denial |
| What data does it touch, and under which rules? | Sets deployment and data handling (module 83) | Health information of EU residents |
| Who is accountable for the outcome? | Draws the automation boundary (page 2) | The insurer, through a named claims manager |

The answers are the requirements. If the sponsor cannot give a number for the fourth row, finding it is the first piece of work: a design aimed at "as accurate as possible" cannot be built, tested or priced.

### The ladder of patterns

Anthropic's article draws the line between two kinds of system: "workflows are systems where LLMs and tools are orchestrated through predefined code paths", and "agents are systems where LLMs dynamically direct their own processes and tool usage, maintaining control over how they accomplish tasks." Below both sits the single call with retrieval, tools and memory around it. Put them in order and you have the ladder.

| Rung | What it is | Choose it when |
|---|---|---|
| **One call** | A prompt and a reply | The task is one transformation with the input in hand |
| **Augmented call** | One call with retrieval, a tool or memory around it | The answer needs outside facts or one action, and the path is a single step |
| **Workflow** | Several calls and steps whose order your code fixes | The path is known in advance and the steps differ in kind |
| **Agent** | The model chooses the next step and the tools | The path depends on what is found and cannot be listed in advance |
| **Team of agents** | A coordinator and subagents | The parts are independent, the volume of reading exceeds one context and the value pays for the cost (module 80) |

The rule that makes the ladder useful is the article's: "find the simplest solution possible, and only increasing complexity when needed". It adds that for many applications "optimizing single LLM calls with retrieval and in-context examples is usually enough." Climbing costs something at every rung. Of autonomous agents the article says they come with "higher costs, and the potential for compounding errors", and the research write-up reports that "agents typically use about 4× more tokens than chat interactions, and multi-agent systems use about 15× more tokens than chats". A design that sits higher than its requirement is paying for flexibility nobody asked for, and is also harder to test, audit and explain.

Two tests settle most cases. **Is the path known?** If you can write the steps down and their order, the code should run them, because deterministic orchestration is cheaper, testable and auditable. **Does the work need the model to decide what to do next?** Only then does autonomy earn its price. A third test, whether a required control may depend on the model behaving, always has the same answer: a control that must hold is enforced in code outside the model.

### Two small components can beat one clever system

Scenario: the loan officers of a bank want two things. They want policy questions answered with citations to the policy text, and they want decline letters drafted from structured decision data. A vendor proposes one autonomous multi-agent system for both, since both involve documents. The two jobs have different shapes. The first is an augmented call: retrieve the passages, answer once, cite them. The second is a templated workflow: the decision data is structured, the letter has a fixed form, and the model's job is wording within it. Built apart, each has its own evaluation set and its own failure budget, a wrong citation for one and a wrong figure in a letter for the other, and each can be audited on its own. Built together, a wrong letter and a wrong answer share one set of risks, and the whole is as hard to test as its hardest part. Documents in common are not an architectural argument.

### The five value pillars

The guide asks the architect to align a solution with five pillars: efficiency, transformation, productivity, cost and performance service levels. Each is a claim about value, and a claim needs a baseline and a measurement before the build starts.

| Pillar | What it claims | A measure | What it does to the design |
|---|---|---|---|
| **Efficiency** | The same work, with less effort per unit | Handling time per claim against last quarter | Favours a workflow on the routine path, with people kept for exceptions |
| **Transformation** | Something that could not be done before | Share of customers served in a new way | Accepts a lower first-pass accuracy and a pilot, and measures adoption |
| **Productivity** | People get more done | Hours saved per person per week | Favours an augmented assistant with a person in the loop |
| **Cost** | Work costs less in total | Cost per completed task against the human baseline | Counts tokens, review time and rework, not model price alone |
| **Performance service levels** | The system meets agreed targets | 95th percentile latency, availability, accuracy by case type | Shapes model tier, caching, concurrency and accept-and-poll (module 84) |

The same feature can serve two pillars, and the pillars can pull apart: the fastest design may not be the cheapest, and the most accurate may miss its latency target. Naming the pillar the sponsor is paying for is what lets the architect justify a trade-off to the people who fund it (module 91).

### A worked example, from request to rung

Northwind Insurance asks for "AI for claims". Discovery gives these facts: 2,000 claims arrive a day; 85 percent are routine and follow a documented path (check the policy, check the documents, decide within a limit); 15 percent are disputed and need a person; a wrong approval is the costly error; handlers wait for the result on screen; claim files hold health information of EU residents.

Take the decisions one at a time. For the routine claims, the path is known, so the rung is a **workflow**: extract the facts from the documents, look up the policy in code, check the rules in code, and let the model draft the explanation. For policy questions from handlers, the answer needs outside facts and one step, so the rung is an **augmented call**. For the disputed claims, nobody can list the steps in advance, but a person decides anyway: the design is an assistant, an augmented call with a person in the loop, that gathers the file for the person. No decision calls for a team of agents. The pillar is efficiency, measured as handling time on routine claims, with cost per claim as the check. The health data sends module 83's questions to the front of the line. That is a design a board can read in one page, and each piece has a number to be held to.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Propose one autonomous multi-agent system for both jobs, since both involve documents."** It is tempting because one system sounds economical. The exam rejects it: shared subject matter is not an architectural argument, two different shapes need two different structures, and the combination is as hard to evaluate as its hardest part.
2. **"Choose the most capable pattern so that the design has room to grow."** It is tempting because capability looks like safety. The exam rejects it: complexity is paid for continuously, in tokens, testing and explanation, and is bought only by a stated requirement.
3. **"Start from the model and the framework, and find the use case afterwards."** It is tempting because the technology is exciting. The exam rejects it: the first output of discovery is the decision statement, with its volume, speed, error cost and accountability, and the pattern follows from it.

## Quiz

1. Scenario: Harbor Mutual's loan officers need policy questions answered with citations, and separately need decline letters drafted from structured decision data. A vendor proposes a single autonomous multi-agent system for both jobs. Which design fits best?
   - **a**: The vendor's system, since both jobs work on documents and one shared platform is easier to run and to staff
   - **b**: One agent that holds every tool, plus a routing prompt that picks the task to perform
   - **c**: Two parts: retrieval with a cited reply, plus a template-driven workflow for correspondence
   - **d**: One model tuned on both kinds of text, so that no outside lookup is needed at all

2. Scenario: Greywell Insurance's sponsor says the claims project exists to cut handling time per claim on routine claims, measured against last quarter. Which pillar does the sponsor want served?
   - **a**: Transformation, a capability that did not exist before the project
   - **b**: Efficiency, the same output with less effort for each unit
   - **c**: Cost, the whole process priced lower than before
   - **d**: Performance service levels, agreed targets that are met

<details>
<summary>Answer key</summary>

1. **c**. The two jobs have different shapes, so each gets the cheapest structure that fits and its own measures. *a* is ruled out because "Documents in common are not an architectural argument". *b* is ruled out because a single agent with every tool joins two risk profiles, and "two different shapes need two different structures". *d* is ruled out because the first job needs the current policy text: "The answer needs outside facts or one action".
2. **b**. Handling time per claim against a baseline is the efficiency pillar's measure. *a* is ruled out because transformation claims "Something that could not be done before", and routine claims were handled before. *c* is ruled out because cost counts "tokens, review time and rework, not model price alone" per task, and the sponsor named time. *d* is ruled out because that pillar's measures are "95th percentile latency, availability, accuracy by case type".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
