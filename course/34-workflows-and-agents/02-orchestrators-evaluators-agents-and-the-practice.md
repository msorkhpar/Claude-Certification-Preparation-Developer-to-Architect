# Orchestrators, evaluators, agents and the practice

**Level:** Developer · **Module 34:** Workflows and agents · **Page 2 of 2**
**Exams:** DV3; A1.6

**After this page you can** describe the orchestrator-workers and evaluator-optimizer workflows and when each fits, say what an agent adds and what it costs, name the stopping conditions and checks that keep an agent under control, and say what the practice builds and which failures it grades.

Checked against Anthropic's engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03, with its note that much of its tooling landscape has changed since). The practice ran offline in the course container in Python, TypeScript, Java and Kotlin, against model functions that the tests script. Nothing on this page was run against a live model.

## Why it matters

The last two workflow patterns are the ones where a model's output decides what the program does next, which makes them the ones that fail in interesting ways: a plan the model wrote badly, a worker that throws, a judge that answers in prose, a loop that never ends. An engineer who has written them once, with those failures handled, can read any framework's version of them. The exam asks which pattern fits a described task and what keeps an agent from running away.

## The idea

### Orchestrator-workers

In this workflow "a central LLM dynamically breaks down tasks, delegates them to worker LLMs, and synthesizes their results." It is "well-suited for complex tasks where you can't predict the subtasks needed", and coding is the article's example: the number of files to change and the change in each depend on the task. It looks like parallelization from the outside, and the difference is what the article calls flexibility: "subtasks aren't pre-defined, but determined by the orchestrator based on the specific input." Search tasks that gather from many sources are the other example.

Three things go wrong in the plain version, and the practice grades them. The plan arrives as model text, so the program reads it defensively: it takes what lies between the first `[` and the last `]`, parses it, keeps the string items, drops empty ones and repeats and caps the count. When nothing is usable, the plan falls back to the task itself, and the result says that it did. A worker can fail, and one failure must not stop the others: the subtask is marked failed and the rest run. And when every worker failed, nothing is combined, because a synthesis over no results is a made-up answer. A partial result is reported as partial.

### Evaluator-optimizer

In this workflow "one LLM call generates a response while another provides evaluation and feedback in a loop." It fits "when we have clear evaluation criteria, and when iterative refinement provides measurable value". The article names two signs of a good fit: model output can be demonstrably improved when a person states feedback, and the model can give that feedback. Literary translation is its example, and so are searches where the evaluator decides whether another round is worth it.

The loop needs an exit, or two. A score at or above a threshold accepts the draft. Otherwise the loop ends after a maximum number of rounds, and the best draft wins, not the last one. When two drafts tie on score, the earliest of them wins, because a late revision can be worse. The judge's reply is model text too. If it cannot be read, the sound reading is a score of zero with a message that says so, and the loop goes on: treating an unreadable reply as a pass would accept an unchecked draft. If the writer itself throws, the loop stops at once and reports the best draft so far, which may be none.

### The earlier patterns, side by side

The choice of a pattern follows the shape of the task, and the page before gave each pattern its sign. Prompt chaining is ideal when the task "can be easily and cleanly decomposed into fixed subtasks". Routing "works well for complex tasks where there are distinct categories that are better handled separately". Sectioning splits the task into independent parts, and voting means "Running the same task multiple times to get diverse outputs". All of them are workflows, "orchestrated through predefined code paths", and all of them call what the article names the augmented LLM, "an LLM enhanced with augmentations such as retrieval, tools, and memory". Agents are different in one respect: they "dynamically direct their own processes and tool usage". On the move to production the article is direct about frameworks: "don't hesitate to reduce abstraction layers and build with basic components as you move to production."

### Agents

An agent begins "with either a command from, or interactive discussion with, the human user". Once the task is clear it plans and acts on its own, "potentially returning to the human for further information or judgement". Two details of the article's description carry the engineering. "During execution, it's crucial for the agents to gain 'ground truth' from the environment at each step (such as tool call results or code execution) to assess its progress." And the task "often terminates upon completion, but it's also common to include stopping conditions (such as a maximum number of iterations) to maintain control."

The cost side is stated as plainly: "The autonomous nature of agents means higher costs, and the potential for compounding errors. We recommend extensive testing in sandboxed environments, along with the appropriate guardrails." Agents suit "scaling tasks in trusted environments". Module 31 is the case study: a computer use loop is an agent, and the container, the confirmation and the turn limit around it are its guardrails.

Because an agent is "typically just LLMs using tools based on environmental feedback in a loop", the tools carry the design. The article's third principle is to "carefully craft your agent-computer interface (ACI) through thorough tool documentation and testing", which is module 26's lesson about descriptions as the routing contract, seen from the agent's side.

### Combining the patterns, and keeping it small

The article treats the patterns as building blocks and not as a menu: "These building blocks aren't prescriptive. They're common patterns that developers can shape and combine to fit different use cases." The discipline is to measure: "you should consider adding complexity only when it demonstrably improves outcomes." Its three principles for agents are simplicity, transparency (showing the agent's planning steps) and the care for the agent-computer interface. The closing line is the one to remember for the exam: success "isn't about building the most sophisticated system. It's about building the right system for your needs."

### The practice: four patterns around a model

The practice is in `exercises/34-workflows-and-agents/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. The model is a function from a prompt to a reply that the tests script, so the prompts are part of the contract and a test can answer by the start of a prompt. You write `orchestrate`, `refine`, `route` and `vote`. The tests grade a plan that is read from prose, cleaned, capped and replaced when unusable; one failing worker that does not stop the others; a draft revised with feedback until the judge accepts it; the best draft when the rounds run out, with an unreadable judge scoring zero; a label read from a reply and a default route for anything else; the majority answer with a tie going to the answer seen first; and a writer that fails and ends the loop with the best draft so far. The starter fails all eight cases, the reference passes them, and each of the seven planted wrong solutions fails on an assertion of the case it breaks, for example a plan that is not capped, a loop that returns the last draft, or a vote that breaks a tie the wrong way.

## Traps

1. **Trusting the plan.** A model's plan is text. Parse it defensively, cap it and fall back when it is unusable, or one odd reply fails the whole task.
2. **Letting one worker fail the run.** Mark the subtask failed, run the rest, and report partial results as partial.
3. **Returning the last draft.** A refinement loop can get worse. Keep the best-scoring draft and the round it came from.
4. **Reading an unreadable judge as a pass.** If the verdict cannot be parsed, the draft is unchecked. Score it zero and say why.
5. **Skipping the stop.** An agent or a loop with no maximum on its iterations is an open bill and a compounding error.

## Quiz

1. A coding tool must change several files, but nobody can say in advance how many or which. Which pattern suits it?
   - **a**: Prompt chaining, with one fixed step for each file
   - **b**: Orchestrator-workers: a central model plans and delegates
   - **c**: Routing, with one handler for each file extension
   - **d**: Voting, with the same edit run several times for agreement

2. A pipeline makes many calls, uses search and code execution, and runs for minutes while the user waits. What would make the article call it an agent?
   - **a**: Making its calls in parallel and not in sequence
   - **b**: Using further tools such as a browser or a database
   - **c**: Running for hours and not minutes before it answers
   - **d**: Letting the LLM, not the program, choose each next step

3. A draft-and-review loop has no accepted draft after the allowed rounds. What does the practice's refine function return?
   - **a**: The last draft, since later revisions always improve on earlier ones
   - **b**: The highest-scoring version so far, and on a tie the earlier one
   - **c**: Nothing, because the loop keeps running until a draft passes
   - **d**: An error, since failing to reach the threshold is a failure

<details>
<summary>Answer key</summary>

1. **b**. The page says orchestrator-workers is "well-suited for complex tasks where you can't predict the subtasks needed", with coding as its example. *a* is ruled out because chaining is ideal when the task "can be easily and cleanly decomposed into fixed subtasks", and here the number of files is not known. *d* is ruled out because "Running the same task multiple times to get diverse outputs" is voting, which repeats one task and does not split a task. *c* is ruled out because routing "works well for complex tasks where there are distinct categories that are better handled separately", which is a choice of one path and not a plan with a variable length.
2. **d**. The page says agents "dynamically direct their own processes and tool usage". *a* is ruled out because "Sectioning splits the task into independent parts" and the page says "All of them are workflows", so parallel calls stay a workflow. *b* is ruled out because every pattern calls "an LLM enhanced with augmentations such as retrieval, tools, and memory", so tools do not mark an agent. *c* is ruled out because the line is drawn by who decides, as in "dynamically direct their own processes and tool usage", and not by how long a run lasts.
3. **b**. The page says that after the maximum rounds "the best draft wins, not the last one", and the earliest wins a tie. *a* is ruled out because "a late revision can be worse", which is why the best draft wins. *d* is ruled out because agents and loops "include stopping conditions (such as a maximum number of iterations) to maintain control", so reaching the maximum is a planned end and not a failure. *c* is ruled out because "The autonomous nature of agents means higher costs, and the potential for compounding errors", which is the cost of a loop with no exit.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team swaps a working three-step chain for a framework that hides the prompts, and debugging gets harder. Which advice from the article applies?
   - **a**: Keep the framework, and add a second one to watch it
   - **b**: Return to direct LLM API calls, and read any code beneath
   - **c**: Move the sequence into an agent so that the model finds the path
   - **d**: Wrap the framework in one more layer that logs each call

2. A classifier's output comes back as `REFUNDS.`, with capitals and a full stop, but the program's table is keyed by lowercase names. What should the program do before the table lookup?
   - **a**: Normalize the text into a form that matches a known route
   - **b**: Retry the classifier until an output matches a name exactly
   - **c**: Send the output to the default route without a lookup
   - **d**: Hand the output to a person who picks the route

3. A code review sends one snippet to the model three times in parallel and applies a threshold to the answers. Which variation is this?
   - **a**: Sectioning, where independent subtasks run side by side
   - **b**: Chaining, where each call processes the output of an earlier one
   - **c**: Routing, where a classifier picks a specialized followup
   - **d**: Voting, where repeated runs of one task are counted

4. A fan-out workflow plans three subtasks and one worker throws an exception. What does the practice's orchestrator do?
   - **a**: Stops the run and reports a failure, since a missing result makes the answer unsafe
   - **b**: Retries the failed worker until it succeeds, and then combines all three
   - **c**: Marks it failed, runs the others, and reports a partial answer
   - **d**: Drops the failed subtask without a trace and combines the other two as if complete

<details>
<summary>Answer key</summary>

1. **b**. The first page says "We suggest that developers start by using LLM APIs directly", and to understand the code under any framework. *a* is ruled out because frameworks "often create extra layers of abstraction that can obscure the underlying prompts and responses, making them harder to debug". *c* is ruled out because agents are for "open-ended problems where it's difficult or impossible to predict the required number of steps", and a fixed sequence is not one. *d* is ruled out because the article says "don't hesitate to reduce abstraction layers and build with basic components as you move to production", not to add one.
2. **a**. The first page says "The program normalizes the label, looks it up, and sends anything unknown to a default route." *b* is ruled out because the page sends "anything unknown to a default route" and describes no second call to the classifier. *c* is ruled out because the program "normalizes the label, looks it up", so a reply that matches after cleaning is not sent straight to the default. *d* is ruled out because "The failure to design for is a label that matches no route.", and the program meets it with a default and not with a person.
3. **d**. The first page defines voting as "Running the same task multiple times to get diverse outputs", and says "a vote is a count and a threshold". *a* is ruled out because sectioning is "Breaking a task into independent subtasks run in parallel", and here one task is repeated. *b* is ruled out because chaining is for steps "where each LLM call processes the output of the previous one", and these runs do not depend on each other. *c* is ruled out because "Routing classifies an input and directs it to a specialized followup task", and here nothing is classified.
4. **c**. The second page says one failure must not stop the others, "the subtask is marked failed and the rest run", and a partial result is reported as partial. *a* is ruled out because the page says "one failure must not stop the others". *b* is ruled out because "A worker can fail, and one failure must not stop the others", and the page describes no retry. *d* is ruled out because the page says "A partial result is reported as partial", so the failure is visible.

</details>
