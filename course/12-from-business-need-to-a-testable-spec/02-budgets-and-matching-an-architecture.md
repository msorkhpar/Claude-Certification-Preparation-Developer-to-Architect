# Latency, throughput, region and cost budgets, and the architecture that fits

**Level:** Developer · **Module 12:** From business need to a testable spec · **Page 2 of 3**
**Exams:** DV1, DV2

**After this page you can** write the non-functional budgets of a Claude feature as numbers, and pick the simplest
architecture (one call, a chain, tools, retrieval, a batch) that meets them.

Checked against the Claude API documentation (rate limits, batch processing, data residency, models overview,
errors) on 2026-10-02 for `claude-fable-5-1`, `claude-opus-5-5`, `claude-sonnet-5-5` and `claude-haiku-4-5-20251001`.
Limits, prices and model dates move: the figures below are the documentation's on that date and are re-checked at
release. No live API call was made for this page.

## Why it matters

The same summary feature can be a request a user waits for, a nightly job over a million tickets, or a step inside an
agent that makes forty calls. Each has a different answer to "how fast", "how many at once" and "how much". The
exam asks these as architecture questions: given a requirement, which design meets it with the least machinery? A
design chosen before the budgets are written down is a guess, and a guess is expensive to move once it is in
production.

## The idea

### Four budgets, each a number

| Budget | The question | What moves it |
|---|---|---|
| **Latency** | How long may the user or the calling system wait? Time to the first output, or to the whole answer? | Output length (a reply is produced token by token), model tier, streaming, how many calls are chained |
| **Throughput** | How many requests and tokens per minute at the peak, and for how long? | Your organisation's rate limits (requests, input tokens and output tokens per minute), concurrency, batching |
| **Region** | Where may inference run and where may data be stored? | The `inference_geo` setting, workspace geography, or the cloud platform that serves Claude |
| **Cost** | What may one unit of work cost, and what may the month cost? | Tokens in and out, model tier, prompt caching, batch processing, how many calls are chained |

A budget is a number with a unit and a place to measure it: "the first characters appear within 1.5 seconds at the
95th percentile", "the nightly job finishes within 4 hours", "at most 2,000 requests in any minute at month end", "no
more than 0.4 cents per ticket". If the requirement is "fast" or "cheap", the next design decision has nothing to
aim at.

### Latency: what you can and cannot tune

A model writes its answer one token at a time, so **the length of the output is the largest part of the wait**, and
the input matters less than people expect. Three levers follow, in the order to try them:

1. **Ask for less output.** A summary of 80 words returns sooner than one of 300, whatever model produces it.
2. **Stream.** Streaming does not shorten the total time, but the user sees the first words almost at once. Module 17
   builds it. For a person waiting at a screen, time to the first output is often the number that matters.
3. **Choose the tier for the job.** The models overview describes Claude Haiku 4.5 as the fastest and Sonnet 5.5 as
   speed and intelligence together; Opus 5.5 and Fable 5.1 are the tiers for demanding agentic and reasoning work.
   A cheaper, faster tier is correct only if your evaluation set (page 1) says its quality holds.

Chaining calls multiplies latency: three sequential calls cost roughly the sum of their times. A requirement of two
seconds rules out a five-call pipeline before any code exists.

### Throughput: rate limits are a requirement on you

The API enforces limits per organisation and per model class, in three units: requests per minute (RPM), input
tokens per minute (ITPM) and output tokens per minute (OTPM). The documentation gives the standard limits by usage
tier, and on 2026-10-02 they read:

| Usage tier | Monthly spend cap | Claude Sonnet 5.5 (RPM, ITPM, OTPM) |
|---|---|---|
| Start | 500 US dollars | 1,000 requests, 2,000,000 input tokens, 400,000 output tokens |
| Build | 1,000 US dollars | 5,000 requests, 5,000,000 input tokens, 1,000,000 output tokens |
| Scale | 200,000 US dollars | 10,000 requests, 10,000,000 input tokens, 2,000,000 output tokens |

Two details change the arithmetic. The API limits with a **token bucket**, which the documentation describes as
capacity that is "continuously replenished up to your maximum limit, rather than being reset at fixed intervals", so
a burst can hit a limit even when the per-minute average is far below it. And for most models **only uncached input
tokens count toward ITPM**, which makes prompt caching a throughput lever as well as a cost lever. Module 16 builds the
client side of all this; here the point is to write the peak load as a number and compare it with the tier.

A request counts against the request limit however short its reply is, and a higher tier raises how much you may send
per minute; it does not make one call finish sooner. A requirement that exceeds the limits has three honest answers:
ask for a higher tier, smooth the load over time, or move the work to a **batch**.

### Cost: tokens, tier and batches

Cost is tokens in, tokens out, the price of the tier, and the number of calls. Current prices are on the pricing page
and are not copied here; the levers are stable:

- **Fewer tokens**: shorter prompts, shorter outputs, no repeated material (caching reuses a stable prefix).
- **A cheaper tier** where the evaluation set allows it.
- **Batches.** The Message Batches API processes requests asynchronously and the documentation says "most batches
  finishing in less than 1 hour while reducing costs by 50%". It is the right design when nobody is waiting: nightly
  classification, bulk evaluation, back-filling a field.
- **Fewer calls**: one call that returns three fields beats three calls that each return one.

The token counting endpoint estimates input tokens before you send, which is how a cost budget is checked in a test
without spending anything.

### Region: where inference runs

Some requirements are legal, not technical: customer data must be processed in a given geography. The Claude API has
a per-request `inference_geo` setting with two values, `"global"` (the default) and `"us"`, supported on Claude 4.6
and later; the documentation says requests that set it on Claude Haiku 4.5 or earlier models "return a 400 error".
US-only inference is priced at 1.1 times the standard rate. Rate limits are shared across all geos, so the setting
does not give you a second pool. A workspace can restrict the allowed geos and set a default. On Amazon Bedrock
and Google Cloud the region comes from the endpoint instead, and `inference_geo` does not apply. So a data-residency
requirement can rule out a model, change a price and decide which platform serves the feature, which is why it is
written down in the first week.

### Matching an architecture to the requirement

Start from the simplest design and move up only when a requirement forces it.

| If the requirement says... | The simplest fit | Why not simpler |
|---|---|---|
| One answer from the input alone, a person waits | **One call**, streamed | There is nothing else to fetch or decide |
| Several dependent steps, each checkable | **A chain** of calls with code between them | One prompt cannot be checked in the middle |
| The answer needs live data or an action | **Tool use**: the model asks, your code runs the tool | The model cannot see your systems |
| The answer must come from your own documents | **Retrieval**, then one call with the passages | The model's training does not contain them |
| The path depends on what is found and cannot be listed in advance | **An agent loop** | A fixed chain cannot branch on discoveries |
| Large volume, nobody waiting, cost matters | **A batch** | An interactive call pays for latency you do not need |

Each row is taught later (tools in module 26, retrieval in module 28, agents from module 34). The skill tested here
is the reading of the requirement: the words "live", "your own documents", "nobody is waiting" and "cannot be listed
in advance" each point at one row.

## Traps

1. **Choosing a design before writing the budgets.** A five-step agent for a feature that has to answer in two
   seconds fails its first requirement; the budget would have said so in a minute.
2. **Counting average load against a limit that works as a bucket.** Month-end bursts exhaust a bucket even when the
   daily average looks comfortable. Write the peak, not the mean.
3. **Treating a residency rule as a deployment detail.** It can remove a model from the list and change the price, so
   it belongs in the requirements.

## Quiz

1. A feature must label two million archived tickets overnight, and nobody reads the labels until morning. The team
   proposes a pool of workers that call the API one ticket at a time all night. Which change fits the requirement
   best?
   - **a**: Stream every reply from the workers so that the first words of each label arrive sooner
   - **b**: Hand the whole job to the batch interface, which halves the price and runs unattended
   - **c**: Move the workers to the fastest tier so that the overnight window is certain to be enough
   - **d**: Chain a second call after each label that checks the label before the result is stored

2. A requirement says customer data may only be processed in the United States. A developer plans to use
   `claude-haiku-4-5-20251001` and set `inference_geo` to `"us"` on every request. What does the page predict?
   - **a**: The calls succeed and are billed at 1.1 times the standard rate for inference kept in the US
   - **b**: The calls succeed and run in any region, because the unsupported setting is quietly dropped
   - **c**: The calls fail with a 429 error, because the capacity inside the US for that model is limited
   - **d**: The calls fail with a 400 error, because the setting is not supported on that model

<details>
<summary>Answer key</summary>

1. **b**. Nobody is waiting and volume is large, which the page assigns to a batch: "most batches finishing in less than 1 hour while reducing costs by 50%". *a* is ruled out because streaming "does not shorten the total time", and no reader is watching the words arrive. *c* is ruled out because a faster tier is correct only "if your evaluation set (page 1) says its quality holds", and it does not change the cost. *d* is ruled out because "chaining calls multiplies latency" and also cost, and the requirement gives no reason for a second call.
2. **d**. The page says a request that sets the geography on Claude Haiku 4.5 or an earlier model "return a 400 error". *a* is ruled out because the 1.1 times price belongs to models where the setting is "supported on Claude 4.6 and later", which excludes this one. *b* is ruled out because the documentation says such requests "return a 400 error", so the setting is rejected and not dropped. *c* is ruled out because the documentation says such requests "return a 400 error", which is not a capacity error.

</details>
