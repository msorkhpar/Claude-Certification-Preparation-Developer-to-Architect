# Reviewing a proposal, and the practice

**Level:** Architect · **Module 77:** Scenario: agentic tool builder · **Page 2 of 2**
**Exams:** X

**After this page you can** order the decisions of a gate that reviews a tool an agent has written, say why each limit is inclusive, explain what an approval covers and what an audit must be, and write the module's practice: the review of a proposal as a function.

Checked on 2026-10-04 against the Architect exam guide's task statements 1.4, 2.3 and 2.1, which this module reuses for a setting the exam does not test, and against the Claude API documentation pages "Define tools" and "Code execution tool". The practice is offline in Python, TypeScript, Java and Kotlin: the tests build proposals and read your report, and no proposed code is ever executed. The shapes of the proposal, of the policy and of the report, the scan of the code text and the order of the decisions are this course's own design for the capstone, not an Anthropic interface, and the statement says so.

## Why it matters

Page 1 divided the work: design, containment, permission, validation and audit. This page is about the permission step, the gate, because it is the one place where a decision about a new tool is made by something other than the agent. If the gate is vague, the agent decides for itself. If it is rigid in the wrong places, it sends back tools that were fine and lets through ones that were not. The practice writes it so that every rule has a case.

## The idea

### The order of the decisions

The gate has four outcomes and one rule about their order: the most serious wins. A proposal with a refusal is refused, whatever else is true of it. Otherwise a proposal with findings is sent back for revision. Otherwise a proposal that declares a permission needing approval is approved behind a gate. Only a proposal with none of these is approved outright.

A refusal comes before a revision, because a fix cannot make a forbidden call acceptable and a revision would invite the agent to try. A revision comes before a gate, because a person should be asked to approve a proposal that is worth reading and not one with a name that does not parse. The report keeps the findings of a refused proposal, so the agent, and the log, can see everything that was wrong and not only the first thing.

### What is refused, and what is only sent back

Three things refuse a proposal. Its code contains a call that the policy forbids. Its code shows a permission that the proposal did not declare, which is the case of a tool that says it reads and fetches a page. Or it declares a permission that the policy denies outright, such as the network. Four things only send it back: a name that is not in the fixed form, a description with too few words, a timeout above the limit and a memory request above the limit. The first group is about trust: the proposal contradicts itself or the rules. The second is about quality: it is fixable by an honest edit.

### Limits are inclusive

A limit is a number that the policy allows, so a value equal to the limit is allowed. The description needs at least the minimum number of words, and exactly the minimum is enough. The timeout may equal its limit and the memory may equal its limit, and one second or one megabyte more is flagged. A name may be 64 characters long, and 65 is too long. The practice has a case at exactly each of these values, because the error in a gate is almost always an off-by-one that no ordinary example reveals.

### What an approval covers

An approval belongs to one proposal. A changed tool is a new proposal and goes through the gate again, because the person approved what they read and not what the agent might write next. This is the same rule that the earlier modules apply to an irreversible action: a payment, a deletion or a write is authorised for the case in front of the person, and a standing approval for a class of effects is a hole. The gate's `approve_with_gate` outcome means that the tool may be installed and that its effects wait for a person each time they happen.

### What an audit is

The audit line names the tool and the decision, and the example's log adds what happened afterwards: it ran, it was rejected on its result, it was refused or it was declined. A log that the agent can edit is not an audit. The record has to be written by the gate and the runner and kept where the agent has no write access, because the questions it must answer come later and are about the agent: which tool changed a file, who approved it, and what the policy said that day.

### The practice: the review of a proposal

The practice is `exercises/77-scenario-agentic-tool-builder/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `review(proposal, policy)`: the findings, the permissions the code shows, the refusals and the decision with its audit line.

The tests build proposals and grade nine cases: a well-formed read-only tool, a description at exactly the minimum, a timeout and a memory at exactly their limits, a name of exactly 64 characters, forbidden calls listed in order, an undeclared and a denied permission, a write that needs a gate, the order of the decisions, and the alphabetical order of the permissions used. The starter fails all nine, the reference passes them, and each of ten planted wrong solutions per language fails on an assertion of the case it breaks: a description that must exceed the minimum, a timeout and a memory that must stay under their limits, a name that cannot reach 64 characters, a list that stops at the first forbidden call, an undeclared permission that is ignored, a denied permission that is ignored, a write that is approved outright, a revision that comes before a refusal, and a permission list in the order of the scan table.

## Traps

These are the answers that sound sensible and fail in an agent that builds tools, each with the reason it fails.

1. **"Send every flawed proposal back for revision, even a forbidden one."** It is tempting because feedback sounds helpful. It fails because a fix cannot make a forbidden call acceptable, and a revision invites the agent to keep trying.
2. **"Keep the limit strict, so that a timeout equal to the limit is flagged."** It is tempting because it leaves a margin. It fails because the policy says what is allowed, and a value that equals it is allowed: the margin belongs in the number that the policy sets.
3. **"Approve the tool once and let it change freely afterwards."** It is tempting because it spares the reviewer. It fails because the approval covered what was read, and a changed tool is a new proposal.

## Quiz

1. A proposal has a forbidden call in its code and a description of a single line. What does the gate decide, and what does the report list?
   - **a**: A revision request, since the short text is the first problem the gate meets
   - **b**: A refusal, with the weakness of the text still noted beside it
   - **c**: An approval behind a gate, since the forbidden call may be removed later in the run
   - **d**: A refusal, with the finding dropped because a refused tool is never revised

2. A policy allows a timeout of at most 10 seconds, and a proposal asks for exactly 10. What does the gate do?
   - **a**: Flags it, since a limit should leave some room below it for safety
   - **b**: Flags it, since only a request below the limit counts as safe to run
   - **c**: Accepts the figure, since a request that reaches the ceiling has not passed it
   - **d**: Accepts it, but only after a person has signed the proposal off

3. A reviewer approved a tool last week. Today the agent submits a changed version with an added write. What happens?
   - **a**: It goes through the gate again as a new proposal
   - **b**: It runs behind a gate if the old version had been gated as well
   - **c**: It runs, because the approval covers every version of that tool
   - **d**: It is refused, because a tool may be approved once and never changed

<details>
<summary>Answer key</summary>

1. **b**. A refusal wins and the findings stay in the report. *a* is ruled out because a refusal comes first: "A refusal comes before a revision, because a fix cannot make a forbidden call acceptable and a revision would invite the agent to try." *c* is ruled out because the refusal stands: "A proposal with a refusal is refused, whatever else is true of it." *d* is ruled out because the findings are kept: "The report keeps the findings of a refused proposal, so the agent, and the log, can see everything that was wrong and not only the first thing."
2. **c**. A limit is inclusive. *a* is ruled out because the margin is not the gate's to add: "the margin belongs in the number that the policy sets". *b* is ruled out because the value is allowed: "A limit is a number that the policy allows, so a value equal to the limit is allowed." *d* is ruled out because a timeout within the limit needs no person: "The timeout may equal its limit and the memory may equal its limit, and one second or one megabyte more is flagged."
3. **a**. A changed tool is a new proposal. *c* is ruled out because the approval is not blanket: "An approval belongs to one proposal." *b* is ruled out because the earlier gating does not carry over: "the person approved what they read and not what the agent might write next". *d* is ruled out because a changed tool is not forbidden, only reviewed again: "A changed tool is a new proposal and goes through the gate again".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: an agent that writes and proposes its own tools, which a gate reviews before anything runs. A tool's code opens a file for writing, while the proposal declares only reading, and the policy sends writes to a person. What does the gate do?
   - **a**: Refuses it, since an effect nobody mentioned cannot be approved on its behalf
   - **b**: Sends it back for a revision of the description, since the declaration is thin
   - **c**: Approves it behind a gate, since the policy already covers writes
   - **d**: Approves it outright, since a read is declared and the write is minor

2. Scenario: an agent that writes and proposes its own tools, which a gate reviews before anything runs. A team proposes to run generated tools in a container and to drop the gate, because the container limits the damage. Which gap remains?
   - **a**: Nothing, since a container makes every tool safe to run
   - **b**: Nothing checks what the code does against what was declared
   - **c**: The container's speed, which is lower than that of a direct run
   - **d**: The agent's description of the tool, which a container cannot read

3. Scenario: an agent that writes and proposes its own tools, which a gate reviews before anything runs. Which record satisfies the audit part of the design?
   - **a**: A file that the agent keeps and updates for itself after every one of its runs
   - **b**: A summary that the agent writes by itself at the end of each working day
   - **c**: One line per run, kept in the folder where the agent saves its tools
   - **d**: One line per decision and per run, kept where its subject has no access

4. Scenario: an agent that writes and proposes its own tools, which a gate reviews before anything runs. A proposed name is 64 characters of lower-case letters, digits and underscores, starting with a letter. What does the gate do with the name?
   - **a**: Flags it, since the longest name has to leave a spare character
   - **b**: Flags it, because names this long are hard for the model to choose
   - **c**: Accepts it, as it sits at the ceiling for such labels and not above it
   - **d**: Accepts it only if the description is also at least 64 words

<details>
<summary>Answer key</summary>

1. **a**. An undeclared effect is refused. *c* is ruled out because the gate approves only what was declared: "Its code shows a permission that the proposal did not declare, which is the case of a tool that says it reads and fetches a page." *b* is ruled out because a refusal is not a revision: "The first group is about trust: the proposal contradicts itself or the rules." *d* is ruled out because the gap is the finding: "A proposal with a refusal is refused, whatever else is true of it."
2. **b**. The container holds the run, and only the gate compares the code with its declaration. *a* is ruled out because a container is only one of the five jobs: "Containment is the environment's job." *c* is ruled out because speed is not the gap: "the example is about the decisions around a run". *d* is ruled out because the gate reads the description: "a gate can require a minimum of words, a name in a fixed form and an example that is valid for the schema".
3. **d**. The audit is kept out of the agent's reach. *a* is ruled out because an editable file is not an audit: "A log that the agent can edit is not an audit." *b* is ruled out because a summary by the agent is its own account: "The record has to be written by the gate and the runner and kept where the agent has no write access". *c* is ruled out because the agent can write where the log is kept: "kept where the agent has no write access".
4. **c**. A name may be 64 characters. *a* is ruled out because the length is allowed: "A name may be 64 characters long, and 65 is too long." *b* is ruled out because the gate checks the form and not the model's taste: "Four things only send it back: a name that is not in the fixed form". *d* is ruled out because the description has its own minimum: "The description needs at least the minimum number of words, and exactly the minimum is enough."

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario format is Anthropic's, and this module's setting and questions are written for this course.
