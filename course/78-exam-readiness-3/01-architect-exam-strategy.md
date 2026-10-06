# Architect exam strategy

**Level:** Architect · **Module 78:** Exam readiness 3 · **Page 1 of 5**
**Exams:** A1 to A5 (CCAR-F)

**After this page you can** read the Architect blueprint as a study plan weighted by domain, recognise the decision patterns that the scenario items test, pace a 60-item sitting built on four scenarios, and work three sample questions in the style of the exam guide with the course's pages.

Checked on 2026-10-04 against the Claude Certified Architect, Foundations exam guide (version 1.0, effective July 2026) and the course's exam map. Fees, eligibility and policy change and are covered on module 11, page 2: read the current guide, the Certification Terms and the Exam Policy before you book. This course is not official and does not promise a pass.

## Why it matters

Level 3 gave you every topic of the Architect exam and eight scenario capstones. What is left is the sitting itself. It has a shape: sixty items, four settings drawn from six, and a weight for each domain. A candidate who studies in the order of the course and not in the order of the weights spends an evening on a domain that is worth little, and a candidate who reads the cue in a scenario and skips its constraint picks the answer that was right in another scenario. This page turns the blueprint into a plan, gives the decision patterns that recur, and ends with three sample questions in the guide's style.

## The idea

### The exam in numbers

The published guide lists 60 items and 120 minutes of exam time, which is two minutes an item (the item count was not confirmed on an official page on 2026-10-05, so check the current guide). The result is a scaled score from 100 to 1,000, with 720 to pass, reported as pass or fail with the percent correct in each domain. The domain percentages are not used for the result, which depends on the total scaled score: a weak domain can be carried by the others, and a strong domain does not rescue a weak total. The items are multiple-choice or multiple-response, and each item says how many answers to select.

The questions sit inside scenarios. A sitting draws four of six settings, so you will not meet all six, and you cannot know which four. The six are a customer support resolution agent, code generation with Claude Code, a multi-agent research system, developer productivity, Claude Code in continuous integration, and structured data extraction. Level 3 teaches each as a capstone (modules 70 to 75) and adds two settings beyond the six (modules 76 and 77), which practise the same decisions in new surroundings.

### The blueprint as a study plan

The guide lists five domains. The last column gives the number of items that a weight gives in 60, and the mock exams of this module use that split, rounded so that the total stays 60.

| Domain | Weight | Items in 60 | Mock split | Modules that teach it |
|---|---|---|---|---|
| A1 Agentic architecture and orchestration | 27% | 16.2 | 16 | 45 to 51 |
| A2 Tool design and MCP integration | 18% | 10.8 | 11 | 52 to 56 |
| A3 Claude Code configuration and workflows | 20% | 12 | 12 | 57 to 60 |
| A4 Prompt engineering and structured output | 20% | 12 | 12 | 61 to 63 |
| A5 Context management and reliability | 15% | 9 | 9 | 64 to 69 |

Two readings of the table matter. First, **more than a quarter of the exam is A1**, and its seven topics (the loop, coordinator and subagents, invoking subagents, enforced prerequisites, hooks, decomposition, session state) interlock, so they repay study together. Second, **A3 and A4 are each a fifth**, and both reward hands-on habit: where an instruction lives, and what a schema does and does not guarantee. The scenario modules, 70 to 77, are not a domain. They are practice in using all five at once, so a miss in a scenario is a miss in a domain, and the fix is the domain's page.

A plan by weight is a plan by expected marks. If your results are even across domains, spend your time in proportion to the weights. If they are not, spend it where the product of the weight and the miss rate is largest: a domain at 27 percent where you score 60 percent loses more than a domain at 15 percent where you score 50.

### Decision patterns that recur

Scenario items are written around a small number of decisions. The table is the course's working summary of the ones that Level 3 teaches. It is not a list of answers: each row is a cue in the scenario, the family of answer it usually points to, and the module that gives the reason. Read the reason before you trust the cue.

| A cue in the scenario | The answer usually belongs to | Module |
|---|---|---|
| A rule must hold on every run, whatever the model does | Code: a hook or an enforced prerequisite, not a prompt line | 48, 49 |
| One message carries several separate concerns | Decompose, handle each with the facts of the session, compose one reply | 50 |
| A subagent does not know what the coordinator knows | Pass the facts in its prompt; it does not share the conversation | 47 |
| The agent picks the wrong one of two similar tools | Rewrite both descriptions with the input form and where each stops | 52 |
| A tool fails and the agent cannot tell why | A structured error with a category and whether a retry can help | 53 |
| An agent has a long list of tools and chooses badly | A scoped tool set for each agent | 54 |
| A shared command or a convention must reach every clone | A committed file in the project, not the home folder | 57, 58 |
| A pipeline step hangs waiting for input | Run Claude Code non-interactively with the print flag | 60 |
| Nobody waits for the result and cost matters | The batch interface, matched back by `custom_id` | 63 |
| A person waits for the result | Real time, not a batch | 63 |
| Output must match a format | A schema through a tool, then validation and a bounded retry | 62 |
| A long session starts to ignore an early instruction | Keep what must hold in a memory file or a hook, and treat compaction as lossy | 64 |
| A customer asks for a person, or the request is ambiguous | Escalate or ask, and do not guess | 65 |
| One subagent in a fleet fails | Return a structured error with what was done, and let the coordinator decide | 66 |
| A report says complete but part of the topic is missing | Derive the status from the scopes that were covered | 66, 72 |
| A large codebase does not fit in one pass | Explore in stages with summaries and scoped subagents | 67 |
| A model reports confidence that nobody has checked | Calibrate against labelled samples and review by stratum | 68 |
| A claim needs a source a reader can follow | Carry provenance with the claim, and mark what is uncertain | 69 |

### Three sample questions

The exam guide publishes sample questions, and its own words are that they show the style of the exam. The three below follow three of them, rewritten for this course. Work each one before you read the reasoning, and name the cue in the scenario first.

> **Sample 1.** A team wants a review command that every developer who clones the repository can run. Where should the command file live?
>
> A. In the home folder of the developer who wrote it.
> B. In the project's own commands folder, committed with the code.
> C. In a list of commands inside the memory file.
> D. In a settings array that Claude Code reads at start.
>
> **Sample 2.** A step in a build pipeline starts Claude Code with a prompt and the job hangs until the runner times it out. Which change fits?
>
> A. Set an environment variable that tells Claude Code it is running headless.
> B. Redirect standard input from an empty file.
> C. Run it with the print flag so that it works non-interactively and returns.
> D. Add a command-line flag that turns the run into a batch.
>
> **Sample 3.** A research run ends with a report marked complete, but one of four topics has no findings, because its search timed out and was never retried. Where is the defect?
>
> A. The status is not derived from the topics that were covered.
> B. The report agent should have written a longer summary of the others.
> C. The search agent should have returned an empty list in place of an error.
> D. The timeout was set too short for the search to finish.

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. They are rewritten here, and the options are the course's.

The answer to each is B, C and A in that order, and the reasoning, with the page that teaches it:

- **Sample 1** asks where a shared definition lives, and the cue is "every developer who clones". A folder in the project is committed and reaches every clone, while a home folder is personal (module 58). The memory file holds instructions and context, not command definitions, and the guide names the project's commands folder, not a settings array, as the shared place.
- **Sample 2** is a pipeline with no person. The documented way to run without one is the print flag (module 60). There is no headless environment variable and no flag that turns a run into a batch, and an empty input is a workaround that does not change how the tool runs.
- **Sample 3** is a status that claims more than the run did. The subagents worked within their assignments, so the fault is in the claim, and a status must come from coverage (modules 66 and 72). A longer summary does not repair it, an empty list hides the gap, and a longer timeout is a different question.

Notice what the three have in common. Each states a constraint (reach, no person, an honest status), each has one option that answers it at the right layer, and each wrong option is attractive to someone who has made a specific mistake. That is the pattern of the mock exams.

### Pacing the sitting

At two minutes an item, a sensible pattern is a first pass at about 90 seconds an item: answer what you know and keep a note of the items you are unsure of. A second pass spends the minutes you saved on those. The guide does not describe how the interface lets you move between items or mark them, so learn the interface from the provider's tutorial and do not assume a feature. Multiple-response items state how many answers to select; read the number before the options, and check it again before you move on. The guide describes no penalty for a wrong answer, so leave nothing blank by choice.

Read a scenario item in this order: the constraint first (who waits, what must hold, what is irreversible), then the cue, then the options. Eliminate by layer: an option that puts a guarantee in a prompt, or a decision in the wrong component, is usually the distractor. The real exam mixes its four scenarios, so you cannot rely on position. Answer a scenario you find dull with the same care as the one you like, because each item is worth the same.

## Traps

1. **Studying in the order of the course.** The weights say where the marks are. A plan by course order gives a domain of fifteen percent the same evenings as one of twenty-seven.
2. **Reading the cue, skipping the constraint.** "Batch" is the answer when nobody waits and cost matters, and a wrong answer when a person is waiting. The constraint decides.
3. **Treating a mock percentage as a result.** The pass mark is a scaled score of 720, and there is no published conversion from a percentage. Use the mock to find weak domains, not to predict the result.
4. **Preparing only the scenarios you like.** You cannot choose which four the sitting draws. Revise all six, and use the two extra capstones for practice.
