# Professional exam strategy

**Level:** Architect Professional · **Module 94:** Exam readiness 4 · **Page 1 of 4**
**Exams:** P1 to P7 (CCAR-P)

**After this page you can** read the Professional blueprint as a study plan weighted by domain, recognise the decision patterns that the scenario items test across Level 4, pace a 63-item sitting, and work the exam guide's own three sample questions with the course's pages.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, effective July 2026, exam code CCAR-P) and the course's exam map. The guide's three sample questions are reproduced below with credit. Fees, eligibility and policy change and are covered on module 11, page 2: read the current guide, the Certification Terms and the Exam Policy before you book. This course is not official and does not promise a pass.

## Why it matters

By the end of Level 4 you have met every topic the Professional exam lists, and you have met them as an architect meets them: in scenarios where a business sentence comes first and a design choice last. What is left is the exam itself. It has a shape, the shape rewards some preparation more than others, and a candidate who studies in the order of the course and not in the order of the weights will spend an evening on a domain worth seven percent. This page turns the blueprint into a plan, gives the decision patterns that recur in the scenario items of Level 4, and ends with the guide's own samples so that you see the item style before the mock exam.

## The idea

### The exam in numbers

The exam has 63 items and lasts 120 minutes, which is about 1.9 minutes an item, against 2.3 for the Developer exam. The result is a scaled score from 100 to 1,000, with 720 to pass, reported as pass or fail with the percent correct in each domain. As in the Developer exam, the pass decision rests on the total scaled score and not on a domain: a weak domain can be outweighed, and a strong one does not rescue a weak total. The items are multiple-choice or multiple-response, and each item says how many answers to select. The fee is 175 US dollars and the credential is valid for 12 months from the date it is awarded. The guide recommends three or more years in systems architecture or platform engineering and six months of hands-on work with Claude or a comparable system in production; it requires nothing, and awards the credential on the exam alone. The weights are "the approximate proportion of scored items drawn from each domain", so the number of items in each domain is close to, and not exactly, the figures below.

### The blueprint as a study plan

The guide lists seven domains. The course uses its own codes P1 to P7, in the guide's order. The "Items in 63" column shows how many of 63 items a domain weight gives, and the mock exam of this module uses that split.

| Course code | The guide's domain | Weight | Items in 63 | Level 4 modules that teach it |
|---|---|---|---|---|
| P1 | Solution Design and Architecture | 17% | about 10.7 | 79, 80, 81, 83, 93 |
| P2 | Claude Models, Prompting and Context Engineering | 13% | about 8.2 | 82, 84 |
| P3 | Integration | 19% | about 12.0 | 85, 86, 87 |
| P4 | Evaluation, Testing and Optimisation | 16% | about 10.1 | 84, 87, 88, 89 |
| P5 | Governance, Safety and Risk Management | 14% | about 8.8 | 81, 83, 90 |
| P6 | Stakeholder Communication and Lifecycle Management | 14% | about 8.8 | 79, 89, 91 |
| P7 | Developer Productivity and Operational Enablement | 7% | about 4.4 | 92 |

Three readings of the table matter. First, **integration is the largest single domain**, at about one item in five, and it is the one in which a wrong answer is most often a plausible mechanism for the wrong situation, so its modules (85 to 87) repay a second reading. Second, **the stakeholder-and-lifecycle domain rewards judgement more than recall**: P6 is 14 percent, and its items ask what to say, to whom and in what order, which a candidate who studied only the technical modules misses. Third, **P7 is small but narrow**: four items, almost all from one module (92), on settings that have a documented answer. A candidate who has read that module twice can take them. Module 93 is the capstone: it draws on all seven domains at once and is a good place to test which domain a scenario belongs to before you answer it.

A plan by weight is a plan by expected marks. If your practice results are even across domains, spend your time in proportion to the weights. If they are not, spend it where the product of the weight and the miss rate is largest: a domain at 19 percent where you score 60 percent loses more than a domain at 7 percent where you score 40 percent.

### Decision patterns that recur

Scenario items are written around a small number of decisions. The table is the course's working summary of the ones that Level 4 teaches. It is not a list of answers: each row is a cue in the scenario, the family of answer it usually points to, and the module that gives the reason. Read the reason before you trust the cue.

| A cue in the scenario | The answer usually belongs to | Module |
|---|---|---|
| A sponsor asks for "as accurate as possible" | A needed accuracy from the cost of an error and the cost of a check | 79 |
| The steps are known and fixed | A workflow in code, not an agent | 79, 80 |
| Two parts pass their own tests and fail together | A written contract at the boundary, above all for the empty case | 80 |
| A retry pays twice after a lost response | An idempotency key recorded with the effect and reused on every retry | 81 |
| An agent that keeps failing keeps costing | A circuit breaker, a checkpoint and a failure kept in its branch | 81 |
| The cache hit rate is low and the bill is high | Static modules first, no changing value in the prefix, a prefix above the minimum | 82, 84 |
| A model must be chosen for a workload | The cheapest that meets the tier and the latency limit | 82 |
| A regulated customer asks where its data goes | A platform, a region, a retention arrangement and a workspace, read into a check | 83 |
| Personal data would reach the model | Identifiers swapped for tokens before the call, and an audit log with no content | 83, 90 |
| 429 errors at a low average rate | The limit that binds, with cache reads not counted | 84 |
| The caller times out before the work ends | Accept the job, return an identifier and deliver by polling | 84 |
| Confident wrong answers after a document refresh | A stale index: replace the chunks of a changed document, check versions | 85 |
| A rule and its exception are split | Cut at the structure of the data, and filter by rights before ranking | 85 |
| Which way to connect a capability | A direct call, a custom tool, an MCP server or agent-to-agent, by counterpart and path | 86 |
| An agent acts with rights the user lacks | Allow a call only when the user holds the scope too | 86 |
| Dozens of tools, falling accuracy | Remove what the role does not need, and defer the long tail | 86 |
| A wrong answer in a multi-agent run | A trace, read from the deepest failing span | 87 |
| The overall score is equal and refunds got worse | A report by segment with a protected segment and a gate | 88 |
| A live test with few cases | The minimum sample, and a shadow run first | 88 |
| A model is retiring | The calendar, the audit, the settings, the gate, a staged roll-out and the way back | 89 |
| A control must hold when its part is down | Fail closed, and a person before the irreversible | 90 |
| A sponsor must decide | A short plain summary, the trade told, one decision asked | 91 |
| A team needs one policy | Managed settings, with locks built from lists | 92 |

### The guide's own samples

The guide publishes three sample questions that, in its words, are "illustrative items" that "show the style and cognitive level of the exam" and "are not drawn from the live item bank". Work each one before you read the answer, and name the cue in the scenario first. The samples carry the guide's own domain numbers, which here match the course's codes: Domain 3 is integration (P3), Domain 2 is models, prompting and context (P2) and Domain 4 is evaluation and optimisation (P4).

> **Sample 1, Domain 3, Integration.** A team exposes a customer-support agent that can read tickets, draft replies, issue refunds, and delete user accounts. Support staff only ever need to read tickets and draft replies. Applying least-privilege principles, which change best reduces risk?
>
> A. Add logging to the refund and delete tools so misuse can be audited later.
> B. Remove the refund and delete tools from the agent's configuration entirely.
> C. Keep all tools but add a confirmation prompt before refunds and deletions.
> D. Replace the agent with a larger model that follows instructions more reliably.
>
> **Sample 2, Domain 2, Models, Prompting and Context.** An application sends the same 8,000-token system prompt and policy document on every request, followed by a short, varying user message. Latency and cost are both concerns. Which optimization most directly addresses both?
>
> A. Truncate the policy document to the first 1,000 tokens.
> B. Switch to the smallest available model regardless of task fit.
> C. Place the static system prompt and policy before the dynamic content and enable prompt caching.
> D. Move the policy document into a few-shot example block.
>
> **Sample 3, Domain 4, Evaluation and Optimization.** A RAG system suddenly returns confident but incorrect answers after a document refresh, while latency and model version are unchanged. What is the most likely first place to investigate?
>
> A. The model weights have silently changed.
> B. The retrieval/indexing step is returning irrelevant or stale chunks.
> C. The temperature setting is too low.
> D. The context window has shrunk.

Source: Claude Certified Architect, Professional Exam Guide, version 1.0 (Anthropic), section 8, Sample Questions. Reproduced with credit; the questions are Anthropic's.

The guide's answers are B for sample 1, C for sample 2 and B for sample 3. The reasoning, in the course's words and with the page that teaches it:

- **Sample 1** is least privilege, and the cue is that the staff "only ever need" two of four capabilities. The keyed answer removes the others, because logging and confirmation prompts act after the privilege exists and a larger model does not change what the agent may do (module 86, page 2).
- **Sample 2** pairs a stable prefix with a short varying message. The cue points to the order of a request and to caching: stable content first, the variable part last, and a prefix long enough to be cached. Truncation loses policy, the smallest model risks quality, and few-shot placement creates no reusable prefix (module 82, page 1).
- **Sample 3** is the signature of a stale index: a refresh, confident wrong answers, and no change in latency or model. The first place to look is the retrieval and indexing step, where an old chunk competes with the new one (module 85, page 2; module 88, page 2 for the diagnosis order).

Each of the three states a constraint (risk, cost and latency, correctness after a change), each has one option that answers the constraint at the right layer, and each wrong option is attractive to someone who made a specific mistake. That is the pattern of the mock exam.

### Pacing the sitting

At 1.9 minutes an item, a sensible pattern is a first pass at about 75 seconds an item, which takes about 79 minutes: answer what you know, and keep a note of the items you are unsure of. A second pass spends the 40 minutes you saved on those. The guides do not describe how the exam interface lets you move between items or mark them, so learn the interface from the provider's tutorial and do not assume a feature. Multiple-response items state how many answers to select; read the number before you read the options, and check it again before you move on. The guides describe no penalty for a wrong answer, so leave nothing blank by choice.

The scenario items of this exam are longer than those of the Developer exam: two or three sentences before the question. Read the last sentence first, to know what is being asked ("first", "best", "most likely", "which finding"), then read the scenario for the constraint, then the options. An option that is true in general and does not answer the constraint is the usual distractor.

## Traps

1. **Studying in the order of the course.** The weights say where the marks are. A plan by course order gives the 7 percent domain the same evenings as the 19 percent one.
2. **Reading the cue, skipping the constraint.** "Gateway" is the answer when many teams share credentials, budgets and a record, and a wrong answer to a question about lowering the token bill. The constraint decides.
3. **Treating a mock percentage as a result.** The pass mark is a scaled score of 720, and there is no published conversion from a percentage. Use the mock to find weak domains, not to predict the result.
4. **Leaving the small domain to chance.** P7 is about four items and almost all of them come from one module of settings, which is cheap to learn and easy to lose.
