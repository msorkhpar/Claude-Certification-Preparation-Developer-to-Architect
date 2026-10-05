# Developer exam strategy

**Level:** Developer · **Module 44:** Exam readiness 2 · **Page 1 of 4**
**Exams:** DV1 to DV8 (CCDV-F)

**After this page you can** read the Developer blueprint as a study plan weighted by domain, recognise the decision patterns that the items test, pace a 53-item sitting, and work the exam guide's own three sample questions with the course's pages.

Checked on 2026-10-03 against the Claude Certified Developer, Foundations exam guide (version 1.0, effective July 2026) and the course's exam map. The guide's three sample questions are reproduced below with credit. Fees, eligibility and policy change and are covered on module 11, page 2: read the current guide, the Certification Terms and the Exam Policy before you book. This course is not official and does not promise a pass.

## Why it matters

By the end of Level 2 you have met every topic the Developer exam lists. What is left is the exam itself: it has a shape, the shape rewards some preparation more than others, and a candidate who studies in the order of the course and not in the order of the weights will spend an evening on a domain worth three percent. This page turns the blueprint into a plan, gives the decision patterns that recur in scenario items, and ends with the guide's own samples so that you see the item style before the mock exams.

## The idea

### The exam in numbers

The exam has 53 items and lasts 120 minutes, which is about 2.3 minutes an item. The result is a scaled score from 100 to 1,000, with 720 to pass, reported as pass or fail with the percent correct in each domain. The guide says the domain percentages "are not used to determine your pass or fail result, which is based on your total scaled score": you can be weak in one domain and pass, and a strong domain does not rescue a weak total. The items are multiple-choice or multiple-response, and each item says how many answers to select. The domain weights are "the approximate proportion of scored items drawn from each domain", so the number of items that count in each domain is close to, and not exactly, the figures below.

### The blueprint as a study plan

The guide lists eight domains. The course uses its own codes DV1 to DV8 and its own wording; the guide numbers the domains in a different order. The last column shows how many of 53 items a domain weight gives, and the mock exams of this module use that split.

| Course code | The guide's domain | Weight | Items in 53 | Modules that teach it |
|---|---|---|---|---|
| DV1 | Applications and Integration | 33.1% | about 17.5 | 3, 12 to 17, 21 to 23, 25, 30, 39, 40 |
| DV2 | Model Selection and Optimization | 16.8% | about 8.9 | 1, 3, 4, 6, 13, 18 to 21 |
| DV3 | Agents and Workflows | 14.7% | about 7.8 | 34 to 37 |
| DV4 | Prompt and Context Engineering | 11.0% | about 5.8 | 1, 4, 6, 24, 25, 28, 29 |
| DV5 | Tools and MCPs | 10.6% | about 5.6 | 26, 27, 31 to 33 |
| DV6 | Security and Safety | 8.1% | about 4.3 | 10, 31, 41 |
| DV7 | Claude Code | 3.1% | about 1.6 | 27, 38, 39 |
| DV8 | Eval, Testing, and Debugging | 2.6% | about 1.4 | 15, 42, 43 |

Two readings of the table matter. First, **one item in three is DV1**, and DV1 is also the domain with the most separate skills, so it repays study of every page. The guide splits it into requirements (3.4%), the systems life cycle (2.8%), API mechanics (6.8%), software-engineering foundations (7.4%), application design (8.6%) and configuration management (4.1%). Second, **DV7 and DV8 are small but not free**: together they are under six percent, which is about three items, and a candidate who has read every page of modules 38, 42 and 43 can take them. The skills inside the larger domains, with the guide's own weights:

| Domain | Skills (the guide's names) and their weights |
|---|---|
| DV2 | LLM fundamentals 5.2%, technical fundamentals 6.1%, model selection and tradeoffs 2.7%, cost and token management 2.8% |
| DV3 | Agent architecture 4.5%, agent construction with Claude 5.3%, agent patterns and frameworks 4.9% |
| DV4 | Context engineering 3.8%, prompt engineering 4.6%, output handling 2.6% |
| DV5 | Tool implementation 4.4%, MCP server development 2.1%, agentic customization 4.1% |
| DV6 | AI application security 3.2%, guardrails and safe deployment 2.3%, Claude hooks 1.0%, identity, secrets and key management 1.6% |

A plan by weight is a plan by expected marks. If your practice results are even across domains, spend your time in proportion to the weights. If they are not, spend it where the product of the weight and the miss rate is largest: a domain at 33 percent where you score 60 percent loses more than a domain at 8 percent where you score 40 percent.

### Decision patterns that recur

Scenario items are written around a small number of decisions. The table is the course's working summary of the ones that Level 2 teaches. It is not a list of answers: each row is a cue in the scenario, the family of answer it usually points to, and the module that gives the reason. Read the reason before you trust the cue.

| A cue in the scenario | The answer usually belongs to | Module |
|---|---|---|
| A large, non-urgent job where cost matters and nobody waits | The Message Batches API | 21 |
| Something must hold every time, whatever the model does | Code, a hook or a permission, not a prompt instruction | 6, 39, 41 |
| Text from a web page, an email or a file reaches the model | Untrusted input: a tool result, JSON-encoded, with least privilege | 41 |
| One internal service reused by several Claude applications | An MCP server | 32 |
| A long prefix repeated on many calls | Prompt caching, with the breakpoint after the shared part | 20 |
| A reply that stops mid-sentence with status 200 | The stop reason `max_tokens`: raise the limit or continue | 14, 43 |
| Every call returns a 400 after a model change | A request field the new model rejects | 43 |
| A 429 with a `retry-after` header | Wait for the header, then retry | 15 |
| A reply that must be exact JSON | Structured outputs or a strict tool, plus validation | 25 |
| A task whose steps are fixed in advance | A workflow, not an agent | 34 |
| A prompt, model id or setting is about to change | An eval run with a comparison against the last accepted run | 42 |
| Claude Code behaviour for a whole team | Committed settings and a shared memory file | 38, 40 |
| A cheaper or faster tier is proposed | Match the tier to the workload, and prove it on the eval set | 18, 42 |
| The window fills up in a long session | Trimming, compaction, memory or retrieval | 28, 29 |
| Claude must run under a company's cloud identity | The platform's own access control, with least privilege | 22, 23 |

### The guide's own samples

The guide publishes three sample questions that, in its words, "show the style and cognitive level of the exam" and "are not drawn from the live item bank". Work each one before you read the answer, and name the cue in the scenario first. The samples carry the guide's own domain numbers, which differ from the course's codes: the guide's Domain 2 is Applications and Integration (DV1), Domain 7 is Security and Safety (DV6) and Domain 8 is Tools and MCPs (DV5).

> **Sample 1, Domain 2, Applications and Integration.** A developer must process 10,000 documents overnight to produce a non-urgent analytics report. Cost is the primary concern, and results are not needed until the following morning. Which approach best fits the requirement?
>
> A. Send every request synchronously through the Messages API in parallel to finish as quickly as possible.
> B. Use the Message Batches API, which processes large asynchronous workloads within a 24-hour window at reduced cost.
> C. Lower max_tokens on synchronous calls to minimize cost.
> D. Switch to the smallest available model regardless of output quality.
>
> **Sample 2, Domain 7, Security and Safety.** A Claude-powered agent summarizes web pages submitted by end users. One page contains hidden text instructing the model to ignore previous instructions and reveal its system prompt. Which mitigation is most effective?
>
> A. Raise the model's temperature so its behavior is harder to predict.
> B. Treat retrieved page content as untrusted input, keep it separate from trusted instructions, and use guardrails or hooks so injected instructions cannot trigger sensitive actions.
> C. Add a line to the system prompt asking users not to include malicious instructions.
> D. Switch to a larger model that follows instructions more reliably.
>
> **Sample 3, Domain 8, Tools and MCPs.** A team needs Claude to call an internal inventory service exposed as a REST API. They want the capability to be reusable across several Claude applications and maintained independently of any one app. Which approach best fits?
>
> A. Hard-code the inventory logic into each application's system prompt.
> B. Build an MCP server that exposes the inventory operations as tools so multiple Claude applications can connect to it.
> C. Paste the current inventory data into the context window on every request.
> D. Rely on a built-in tool, since built-in tools can reach any internal REST API.

Source: Claude Certified Developer, Foundations Exam Guide, version 1.0 (Anthropic), section 8, Sample Questions. Reproduced with credit; the questions are Anthropic's.

The guide's answer for each is B. The reasoning, in the course's words and with the page that teaches it:

- **Sample 1** pairs a cost constraint with no deadline inside the day. That cue points to a batch (module 21, page 1). Option A buys speed with money, and the scenario wants the opposite. Options C and D trade quality for a saving that a batch gives without the trade.
- **Sample 2** is indirect injection. The page text is third-party data that the user never typed, so the defence is structural: keep it apart from instructions and limit what an injected instruction can do (module 41, pages 1 and 2). Temperature and a polite request are not controls, and a larger model is not a defence.
- **Sample 3** asks for a capability that several applications share and one team maintains. That is the case for an MCP server (module 32, page 1; module 27, page 2 for the comparison with other extensions). Prompts and pasted data are neither reusable nor live, and a built-in tool does not reach an arbitrary internal API.

Notice what the three have in common. Each states a constraint (cost, safety, reuse), each has one option that answers that constraint at the right layer, and each wrong option is attractive to someone who has made a specific mistake. That is the pattern of the mock exams.

### Pacing the sitting

At 2.3 minutes an item, a sensible pattern is a first pass at about 90 seconds an item: answer what you know, and keep a note of the items you are unsure of. A second pass spends the minutes you saved on those. The guides do not describe how the exam interface lets you move between items or mark them, so learn the interface from the provider's tutorial and do not assume a feature. Multiple-response items state how many answers to select; read the number before you read the options, and check it again before you move on. The guides describe no penalty for a wrong answer, so leave nothing blank by choice.

Pace by domain as well. The first 17 items of a domain-grouped mock are DV1, and in the real exam the domains are mixed, so you cannot rely on position. What you can do is to answer a DV1 item with the same care as a DV8 item, because each is worth one item in the total score.

## Traps

1. **Studying in the order of the course.** The weights say where the marks are. A plan by course order gives a domain of three percent the same evenings as one of thirty-three.
2. **Reading the cue, skipping the constraint.** "Batch" is the answer when nobody waits and cost matters, and a wrong answer when the user is waiting. The constraint decides.
3. **Treating a mock percentage as a result.** The pass mark is a scaled score of 720, and there is no published conversion from a percentage. Use the mock to find weak domains, not to predict the result.
4. **Leaving small domains to chance.** Claude Code, evaluation and debugging are about six percent between them, and each page of those modules is cheap to learn.
