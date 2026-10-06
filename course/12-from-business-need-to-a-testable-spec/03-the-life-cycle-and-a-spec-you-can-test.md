# The life cycle, and a spec you can test

**Level:** Developer · **Module 12:** From business need to a testable spec · **Page 3 of 3**
**Exams:** DV1

**After this page you can** carry a Claude feature through develop, implement, operate and maintain, and write a
one-page spec whose every line maps to a test.

Checked against the Developer exam guide (domain 1, version 1.0, July 2026) and the Claude API documentation (models
overview, errors, rate limits, versioning) on 2026-10-02. No live API call was made for this page.

## Why it matters

A feature that passes its demo is about a quarter of the way through its life. The rest is the day a model is retired,
the week the traffic doubles, the morning the error rate climbs and nobody can say whether the cause is the prompt, the
provider or the network. The exam's wording for this is the life cycle of an application: develop, implement,
operate, maintain. A good spec is the thing that makes the later phases cheap, because every line of it is a test
that can be run again.

## The idea

### Four phases, one thread

| Phase | The work | The artefact it leaves |
|---|---|---|
| **Develop** | Turn the business need into requirements and budgets (pages 1 and 2); choose a design; build a first prompt and an evaluation set | The spec, the evaluation set, a decision record for the design |
| **Implement** | Write the integration: request building, response handling, errors, retries, tests against a scripted model | Code and tests that run offline; the practices of modules 13 to 17 are this phase |
| **Operate** | Run it: watch errors, latency, token use and cost; stay under the limits; handle incidents | Dashboards and logs keyed by request id; alerts on the budgets of page 2 |
| **Maintain** | Change it safely: new prompts, new models, new limits; retire what is deprecated | A regression run of the evaluation set on every change; a model-pinning and migration routine |

The thread through all four is **one list of testable statements**. The requirements of page 1 become the tests of the
implement phase, the budgets of page 2 become the alerts of the operate phase, and both become the regression suite of
the maintain phase.

### From spec line to test

Each line of a spec should be able to name the test that will fail if it is broken:

| Spec line | Test in the implement phase | Check in the operate phase |
|---|---|---|
| A summary is at most 80 words and names the product | Script over the evaluation set | A sampled daily run of the same script on live outputs |
| On a 429, 5xx or timeout, show the raw thread within 2 seconds | Scripted failures through the retry policy (module 15) | Alert when the fallback rate passes a threshold |
| A reply cut at `max_tokens` is never shown as complete | Scripted response with `stop_reason` `max_tokens` | Count of truncated replies per day |
| Peak load of 2,000 requests a minute | Load shape in a test against the scripted transport (module 16) | Alert at 80 percent of the tier's limit |
| No ticket text in logs | A log-capture test with planted text | Periodic log sampling |

Notice that the right-hand column exists only because the left-hand line was written as something observable. A line
that cannot name its test is a wish, and wishes do not survive the first incident.

### Operate: what to watch

Four signals, all available without reading a single prompt:

- **Error rate by status and type.** The API returns an error body with a `type` and a `request_id`; module 15 turns
  them into classes. A rise in 429s is a capacity story, a rise in 400s usually points at your requests (or at a spend limit you set, which also returns a 400, see module 15),
  a rise in 5xx is a provider story.
- **Latency**, as the percentile the budget names, measured at your edge.
- **Token use and cost**, from the `usage` object that comes back with every response.
- **Headroom against the limits.** The response headers report the remaining request and token allowance, so you
  can alert before the first 429.

Log the **request id** with every call: the documentation says to include it when you contact support about a specific
request. Do not log secrets or customer text unless the spec says you may.

### Maintain: models retire and limits change

Every model has a retirement date. The course's pinned list on 2026-10-02 shows `claude-haiku-4-5-20251001` with the
nearest date, not before 15 October 2026, and the other three models later in 2027. The practical routine:

1. **Pin the model id** in configuration, never in scattered literals. An alias that moves silently changes behaviour
   under you; a pinned id changes when you change it.
2. **Watch deprecations** and plan the migration before the date, not after it.
3. **Re-run the evaluation set** against the new model, and read the migration guide for changed behaviour. Newer
   models reject some settings that older ones accepted: the sampling parameters `temperature`, `top_p` and `top_k`
   are rejected with a 400 error on Claude Fable 5.1, Opus 5.5 and Sonnet 5.5 for any non-default value, and a request
   that prefills the assistant turn returns a 400 on Claude 4.6 and later.
4. **Re-check budgets**: a new model has its own price, latency and limits.

The API versioning policy helps: the documentation says Anthropic preserves existing input and output parameters but
"may add additional values to the output" and "add new variants to enum-like output values (for example, streaming
event types)". A client that rejects an unknown field or an unknown event type breaks on a change that is allowed,
and a retry cannot help, because the same reply is parsed the same way again. The `anthropic-version` header is
`2023-06-01`; the documentation calls earlier versions deprecated and "may be unavailable for new users", and the
additions arrive within the current one. Be tolerant of additions, strict about what you send.

### A one-page spec

<!-- illustrative -->
A complete first spec for the running example. The numbers are hand-written for this page.

```text
Feature: ticket summary in the support console            Owner: support platform team
Design: one streamed call per ticket (page 2), model id pinned in config
Budgets: first output <= 1.5 s (p95); <= 0.4 cent per ticket; peak 600 requests/min;
         inference in any region (no residency rule)
R1 Behaviour   Summary <= 80 words, names the product          -> evaluation script, 20 tickets
R2 Quality     >= 90% rated accurate by two leads              -> grader over the same set
R3 Safety      No card number in a summary                     -> 5 planted tickets
R4 Failure     Call fails or is refused or stops at max_tokens -> raw thread + notice, <= 2 s
R5 Operations  Log request id, status, latency, tokens         -> log test, no ticket text
Out of scope   Showing summaries to customers
Decides        Agents only read summaries; no action is driven by one
```
<!-- /illustrative -->

## Traps

1. **Treating the first successful demo as the finish line.** Operate and maintain are most of the cost of a feature;
   a spec with no operational lines has already skipped them.
2. **Floating model aliases.** A silent change of model is a silent change of behaviour. Pin an id, and change it on
   purpose with a regression run.
3. **Strict parsing of a loose contract.** A client that rejects an unknown field or event type breaks on a change the
   versioning policy allows.

## Quiz

1. A team's nightly report job started returning slightly different summaries after a provider released an update.
   The code names its model with an alias. What is the underlying mistake?
   - **a**: The evaluation set was too small to notice that the summaries had started to drift
   - **b**: The id was never pinned, so the new version went live with no regression run
   - **c**: The summaries were never written down as requirements before the first prompt was tried
   - **d**: The job logged no request ids, so nobody could trace which call had changed

2. A client library crashes whenever the API adds a new field to a response, although the API's documented rules
   allow additions. What is the best fix?
   - **a**: Pin an older API version header to stop any new property from appearing in the reply body
   - **b**: Validate each reply against a strict schema and reject any reply that has extra fields
   - **c**: Wrap the parsing step in a retry with backoff so that a later reply parses cleanly
   - **d**: Take only the properties the code needs and skip everything else in the reply

<details>
<summary>Answer key</summary>

1. **b**. An alias that moves "changes behaviour under you", and the routine is to pin an id and change it on purpose, with a regression run. *a* is ruled out because the routine asks for "a regression run of the evaluation set on every change", and no set was run on this change at all, whatever its size. *c* is ruled out because a missing spec is a separate fault, while the symptom is described by "a silent change of model is a silent change of behaviour". *d* is ruled out because the request id is logged to "include it when you contact support about a specific request", which helps a trace but does not stop the change.
2. **d**. The page says to be tolerant of additions and strict about what you send, which means reading only what the code uses. *a* is ruled out because earlier versions are deprecated and "may be unavailable for new users", and additions arrive within the current one. *c* is ruled out because "a retry cannot help, because the same reply is parsed the same way again". *b* is ruled out because "a client that rejects an unknown field or an unknown event type breaks on a change that is allowed".

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A feature must return a complete answer to a waiting user within two seconds. The team's design is a chain of five
   calls, each taking about one second. Which action fits best?
   - **a**: Stream the final stage so that its first words appear sooner for the person waiting
   - **b**: Merge or drop stages until the summed time of what remains sits inside the budget
   - **c**: Request a higher usage plan from the provider so that each individual call runs faster
   - **d**: Move the whole pipeline to asynchronous batches, which are processed at half the price

2. After a team swaps its model for a newer id, the support lead says the new one feels better. What should happen
   before release?
   - **a**: Rerun the fixed evaluation set on the candidate id and compare it with the acceptance rate
   - **b**: Accept the lead's impression, since the replacement is the more recent release of the family
   - **c**: Switch to the moving alias so that every later upgrade arrives without any change in the code
   - **d**: Compare the monthly bill of the two models over a single representative week of production traffic

3. A feature runs comfortably under its entry-level request limit on average, but a quarterly campaign sends four
   times that rate for ten minutes, and the first requests of the burst are refused. Which response fits best?
   - **a**: Spread the traffic over a longer window so the peak load stays under the allowance
   - **b**: Keep the current tier, since the daily average stays well below the request limit
   - **c**: Shorten every reply from the model so that each single request counts for less against the limit
   - **d**: Switch the residency setting to global so that a second and separate pool becomes available

4. A help site must answer visitors only from the company's own policy documents, one answer for each visitor, with
   the visitor waiting. Which design fits best?
   - **a**: Chain three separate calls that draft, check and polish the reply before it is shown
   - **b**: Run an agent loop that explores the documents until the loop is satisfied with what it found
   - **c**: Retrieve the passages that match each question and send them with it in a single call
   - **d**: Send each question in one streamed call and let the model answer from what it knows

<details>
<summary>Answer key</summary>

1. **b**. Chained calls add up, so the fix is to cut the sequence: "a requirement of two seconds rules out a five-call pipeline". *a* is ruled out because "streaming does not shorten the total time", and the requirement is for the complete answer. *c* is ruled out because a higher tier "does not make one call finish sooner". *d* is ruled out because a batch "is the right design when nobody is waiting", and a user is waiting.
2. **a**. The maintain routine is to re-run the evaluation set against the new model and compare, so the change is judged on numbers. *b* is ruled out because "a requirement that only a person's impression can settle is a wish". *c* is ruled out because "a silent change of model is a silent change of behaviour". *d* is ruled out because a bill covers cost alone, while the routine says to "re-run the evaluation set against the new model" for quality.
3. **a**. The bucket can be emptied by a burst whatever the average is, so the plan must fit the peak, and smoothing the load over time is one of the page's answers. *b* is ruled out because the page says "write the peak, not the mean". *c* is ruled out because "a request counts against the request limit however short its reply is". *d* is ruled out because "rate limits are shared across all geos", so no second pool appears.
4. **c**. Answers that must come from the company's own documents point to retrieval followed by one call. *b* is ruled out because an agent loop is for when "the path depends on what is found and cannot be listed in advance". *a* is ruled out because a chain is for "several dependent steps, each checkable", and three calls would also cost latency. *d* is ruled out because one call alone fits "one answer from the input alone", and for the company's documents "the model's training does not contain them".

</details>
