# The tool builder: what the agent may write

**Level:** Architect · **Module 77:** Scenario: agentic tool builder · **Page 1 of 2**
**Exams:** X

**After this page you can** say what an agent that builds its own tools adds to the risks of an agent that only uses them, split the work into design, containment, permission, validation and audit, explain why a text scan of generated code is a first check and never a sandbox, and follow a run in which proposed tools are refused, held for a person or run and checked.

Checked on 2026-10-04 against the Claude API documentation pages "Define tools" (descriptions and `input_examples`) and "Code execution tool" (the sandboxed container), and against the Architect exam guide's task statements 2.1, 2.3 and 1.4, which this module reuses. No published blueprint tests this scenario: it goes beyond the exam's six settings and is taught to make the course complete. The example runs offline in Python, TypeScript, Java and Kotlin, and it executes no generated code: the proposals and their results are made-up data, so the example is about the decisions around a run. This page is a capstone: it uses modules 26, 41, 48, 52 and 54, which treat tool use, injection, gates, interfaces and least privilege in full.

## Why it matters

An agent that uses a fixed set of tools has an attack surface that the team chose and reviewed. An agent that writes its own tools has a surface that it chooses at run time, in text, and that text is code. Every question of the earlier modules comes back in a sharper form: who decides what the tool may touch, who reads what it returns, who can undo what it did. The architect's answer is not to forbid the pattern, which is useful, but to put each decision where it can be enforced: in a container, in a policy, in a gate and in a log, and not in the agent's promise.

## The idea

### The scenario in plain words

An agent is given a task that no existing tool covers. It designs a tool for it: a name, a description, an input schema, the permissions it needs and the code. A gate reviews the proposal. An approved tool runs in a container, and its result goes back to the agent as data. The team keeps an audit of what was proposed, decided and run. The setting is beyond the exam's six, and it applies what the exam teaches about tool interfaces, least privilege and guarantees in code to a tool that did not exist an hour ago.

### Five jobs, five homes

| Job | What it is | Its home |
|---|---|---|
| Design | A name and a description that let the model choose the tool correctly, and a schema | The proposal, checked by the gate |
| Containment | What the code can reach when it runs | The environment: a container with nothing the tool does not need |
| Permission | Which effects the tool may have, and which need a person | A policy that the gate enforces |
| Validation | Whether the result is what the schema promised | Code that checks the result before the agent uses it |
| Audit | A record of what was proposed, decided and run | A log that the agent cannot edit |

The first row is old advice with a new owner. The documentation says of descriptions: "Provide extremely detailed descriptions. This is by far the most important factor in tool performance." A tool the agent has written is chosen by the same model that wrote it, which makes the description both easier to get wrong and easier to check: a gate can require a minimum of words, a name in a fixed form and an example that is valid for the schema. The documentation adds that "Each example must be valid according to the tool's `input_schema`."

### A text scan is not a sandbox

It is tempting to read the generated code for dangerous calls and to trust a clean scan. The gate in this module does read the code, and its first lines say why that is only the first check. A scan finds what its authors thought of: a call spelled differently, built from strings or imported by name slips past it. The scan is useful for what it does well: it refuses the obvious, it compares what the code does with what the proposal declares, and it costs nothing.

Containment is the environment's job. The documentation describes what a managed sandbox looks like: "All operations run in a secure, sandboxed container. The container has no internet access, so Claude can't download packages at runtime". It lists the same properties as a design: "Internet access: Completely disabled for security", "Full isolation from host system and other containers", and "Each request runs in a new container unless you pass an earlier response's container ID back". The lesson for a team that runs generated code itself is the same, and it is a list of absences: no network, no secrets, no shared state, a short life and limits on time and memory.

### Read a symptom as a failure shape

| What the team finds | The failure shape | The first fix | Taught in |
|---|---|---|---|
| A generated tool fetched a web page that the proposal never mentioned | Code that does more than it declared | Compare what the code does with the permissions declared, and refuse the difference | This module |
| The agent chose the wrong one of two tools it wrote | A description that does not separate them | Require a description of a minimum length with the input form and when to use it | Module 52 |
| A tool wrote a file that nobody approved | An effect with no gate | Hold every write for a person, in code | Modules 48 and 54 |
| A tool's result contained instructions and the agent followed them | A result trusted as an instruction | Treat the result as data and check it against its schema | Module 41 |
| A run used far more time and memory than expected | No limits | A timeout and a memory limit in the policy, and in the container | This module |
| Nobody can say which tool changed a file last week | No audit | A log line for every proposal, decision and run | This module |

### The example

The example decides four proposals from the permissions each asks for. The first tool only reads, so it runs without a person. The second reads and writes, so it waits for a reviewer, who approves it; its result is missing a field that its schema promised, and the check rejects it. The third asks for the network, which the policy denies, so it is refused whatever its code says. The fourth asks only to write, waits for the reviewer, and is declined. The last line is the audit tally. Nothing generated is executed: the results are made-up data, and the point of the example is the path around the run.

<!-- example: m77-tool-gate tabs: python,typescript,java,kotlin -->
```python
```
<!-- /example -->

Every line is the output of the container, and all four languages print the same lines. The check at the end is the part to notice. A tool that the agent wrote returns data that the agent will read, and a result that does not match its schema is rejected before the agent sees it, which is also the answer to a result that tries to speak in the agent's voice.

## Traps

These are the answers that sound sensible and fail in an agent that builds tools, each with the reason it fails.

1. **"Scan the generated code for dangerous calls, and run it if the scan is clean."** It is tempting because a scan is cheap and catches the obvious. It fails because a scan finds only what its authors thought of, and containment belongs to the environment: no network, no secrets, short life, limits.
2. **"Let the agent declare its own permissions and trust the declaration."** It is tempting because the agent knows what its code does. It fails because the declaration is the claim to be checked: the gate compares what the code does with what was declared and refuses the difference.
3. **"Approve once, and let the tool run unattended afterwards."** It is tempting because it removes a person from the loop. It fails for a tool with an irreversible effect: a write waits for a person every time, and the approval covers the proposal that was reviewed and not a later version.

## Quiz

1. A generated tool declares that it reads files, and its code fetches a web page. What does the gate in this module's practice do?
   - **a**: Approves it with a gate, since reading and fetching are both low-risk effects here
   - **b**: Refuses it, since the proposal never mentioned that effect
   - **c**: Sends it back for a better description, since the declaration was incomplete
   - **d**: Runs it once in a container to see whether the fetch succeeds as intended

2. A team runs generated tools in a container that has no network access, no secrets and a short lifetime. A colleague says that the code scan is therefore unnecessary. What is the best reply?
   - **a**: It is a cheap first check for the obvious, and the surroundings hold back the rest
   - **b**: The scan is unnecessary only when the container is also given the secrets it needs to work
   - **c**: The scan is the real protection, and the container only adds some speed to each run
   - **d**: The scan is unnecessary, since containers make generated tools safe whatever their code does

3. In the example, a tool that was approved by the reviewer returns a result without the field that its schema promised. What happens to the result?
   - **a**: It is passed to the agent with a warning attached to the front of the result
   - **b**: It is filled with a default and passed on, so that the run can continue
   - **c**: It is refused before the agent uses it, and the audit records a rejection
   - **d**: It is rejected, and the reviewer's approval is withdrawn for every later tool in the run

<details>
<summary>Answer key</summary>

1. **b**. Code that does more than it declared is refused. *a* is ruled out because the difference is the finding: "the gate compares what the code does with what was declared and refuses the difference". *c* is ruled out because a refusal is not a revision: "Code that does more than it declared". *d* is ruled out because nothing proposed is run to find out: "Containment is the environment's job."
2. **a**. The scan is the cheap first check. *b* is ruled out because the container holds no secrets: "The lesson for a team that runs generated code itself is the same, and it is a list of absences: no network, no secrets, no shared state, a short life and limits on time and memory." *c* is ruled out because the scan finds only what its authors thought of: "A scan finds what its authors thought of: a call spelled differently, built from strings or imported by name slips past it." *d* is ruled out because the scan still does useful work: "The scan is useful for what it does well: it refuses the obvious".
3. **c**. A result is checked before the agent uses it. *a* is ruled out because a result that does not match is not passed on: "a result that does not match its schema is rejected before the agent sees it". *b* is ruled out because the check does not repair data: "Treat the result as data and check it against its schema". *d* is ruled out because the approval covers one proposal: "the approval covers the proposal that was reviewed and not a later version".

</details>
