# Polling, results and retries

**Level:** Developer · **Module 21:** Message Batches · **Page 2 of 2**
**Exams:** DV2

**After this page you can** poll a batch to completion, read its result file safely, say which result types are billed, decide
which failures to retry and which to fix first, and write the three functions around a batch.

Checked against the Claude API documentation (Batch processing) on 2026-10-02, and by running the example and the practice offline
in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The batch in the example is an illustrative, scripted
exchange in the API's shape, not a capture.

## Why it matters

The submission is the easy half. A batch of thousands of requests ends with a mixed result file: some answers, some errors, some
requests that never ran. A program that assumes every line is an answer, or that the lines come back in order, loses data
quietly. The exam asks how results are matched, what is billed and what is worth sending again.

## The idea

### Polling

A batch starts with `processing_status` of `in_progress` and moves to `ended` "once all the requests in the batch have finished
processing, and results are ready". Poll the batch by its `id`, with a wait between polls. Its `request_counts` field shows how
many requests are `processing`, `succeeded`, `errored`, `canceled` and `expired`, so a poll doubles as a progress bar. The
example below waits a recorded sixty seconds between polls, without sleeping, so that it runs at once.

Cancellation has one more state. Right after a cancel call the status is `canceling`; poll again until it ends. "Canceled batches end up
with a status of ended and may contain partial results for requests that were processed before cancellation." Cancellation may not be
instant, and the requests that had already run are still billed.

### The four result types

When processing has ended, every request has exactly one result:

| Type | Meaning | Billed |
|---|---|---|
| `succeeded` | the request worked and carries the message | yes |
| `errored` | an error such as an invalid request or an internal server error | no |
| `canceled` | the batch was canceled before this request was sent | no |
| `expired` | the 24 hours passed before this request was sent | no |

For the three failures the page says "You will not be billed for these requests." The 50 percent discount applies to what is billed: "All usage is charged at 50% of the standard API prices." The results are at the `results_url` of the
batch, as a `.jsonl` file: each line is one JSON object, and "it's recommended to stream results back rather than download them all at
once". The result lines carry `custom_id` and a `result` whose `type` is one of the four.

The ordering rule is worth quoting in full:

> Batch results can be returned in any order, and may not match the ordering of requests when the batch was created.

Source: Batch processing.

The page continues "always use the `custom_id` field" to match them. Position in the file means nothing.

### Which failures to retry

The four types are not all alike, and the retry rule follows from the cause:

| Result | Cause | What to do |
|---|---|---|
| `errored` with `invalid_request_error` | the request is wrong | fix it first; the same request fails again |
| `errored` with another type, such as `overloaded_error` | a fault on the service side | resubmit unchanged |
| `expired` | the batch ran out of time first | resubmit unchanged |
| `canceled` | you cancelled the batch | resubmit if you still want it |

The page adds the isolation fact: "the failure of one request in a batch does not affect the processing of other requests." So a
batch with a hundred bad requests still returns the other answers. Collect the failures into a new, smaller batch.

A request that appears in your list but has no line in the result file is a gap, and the practice calls it `missing`. A line whose
`custom_id` matches no request is a stranger, and the practice reports it without using it.

### Server tools in a batch

Server tools such as web search work in a batch. Because there is no open connection, "the batch loop runs more iterations per turn than
a synchronous request before it returns `stop_reason: "pause_turn"`". A result with `pause_turn` did not finish: continue it by
submitting the paused content again, in a batch or a synchronous request.

### Keeping the results

Results live for 29 days after `created_at`, and the batch can be deleted earlier with `DELETE /v1/messages/batches/{batch_id}` after
processing (cancel an in-progress batch first). Store what you need within the window. After that, "you may still view the Batch, but its results will no longer be available for download." A submitted batch cannot be changed either: "once a batch has been submitted, it cannot be modified". Remember, too, the size rule from page 1: "A
Message Batch is limited to either 100,000 Message requests or 256 MB in size, whichever is reached first."

### The example

The example submits four requests, polls twice, and reads a result file in which the lines are out of order, one request errored
on an invalid request and one expired. It matches every line to its request and sorts the failures into those to fix and those to send
again.

<!-- example: m21-batch-round-trip tabs: python,typescript -->
```python
"""A Message Batch from submission to results, against a scripted server.

The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
them. Waiting between polls is recorded, not slept.
"""
import json

import httpx2

from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-haiku-4-5-20251001"
TICKETS = {"t-1": "My parcel never arrived.", "t-2": "How do I change my address?", "t-3": "Charge me twice? Refund please.", "t-4": "x" * 10}


def batch(status, counts, results_url=None):
    return {"id": "msgbatch_illustrative", "type": "message_batch", "processing_status": status, "request_counts": counts,
            "ended_at": None if status != "ended" else "2026-10-02T10:40:00Z", "created_at": "2026-10-02T10:00:00Z",
            "expires_at": "2026-10-03T10:00:00Z", "cancel_initiated_at": None, "results_url": results_url}


def counts(processing=0, succeeded=0, errored=0, canceled=0, expired=0):
    return {"processing": processing, "succeeded": succeeded, "errored": errored, "canceled": canceled, "expired": expired}


def result_line(custom_id, result):
    return json.dumps({"custom_id": custom_id, "result": result})


def succeeded(label):
    return {"type": "succeeded", **{"message": message([text(label)], usage={"input_tokens": 30, "output_tokens": 3})}}


RESULTS = "\n".join([
    result_line("t-3", succeeded("billing")),
    result_line("t-1", succeeded("shipping")),
    result_line("t-4", {"type": "errored", "error": {"type": "error", "error": {"type": "invalid_request_error", "message": "messages: at least one message is required"}}}),
    result_line("t-2", {"type": "expired"}),
]) + "\n"

SCRIPT = [
    (200, batch("in_progress", counts(processing=4))),
    (200, batch("in_progress", counts(processing=2, succeeded=2))),
    (200, batch("ended", counts(succeeded=2, errored=1, expired=1), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results")),
    (200, batch("ended", counts(succeeded=2, errored=1, expired=1), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results")),  # results() looks the batch up first
    httpx2.Response(200, headers={"content-type": "application/x-jsonl"}, content=RESULTS.encode()),
]


def main():
    client, transport = scripted_client(*SCRIPT)
    requests = [{"custom_id": cid, "params": {"model": MODEL, "max_tokens": 50, "messages": [{"role": "user", "content": f"Label this ticket: {body}"}]}}
                for cid, body in TICKETS.items()]
    created = client.messages.batches.create(requests=requests)
    print("created:", created.id, created.processing_status, "| request ids sent:", [r["custom_id"] for r in transport.requests[0]["requests"]])
    waits = []
    status = created
    while status.processing_status != "ended":
        status = client.messages.batches.retrieve(created.id)
        waits.append(60)
        print("poll:", status.processing_status, status.request_counts.model_dump())
    print("waited between polls (recorded, not slept):", waits, "seconds")
    outcomes = {}
    for item in client.messages.batches.results(created.id):
        kind = item.result.type
        detail = item.result.message.content[0].text if kind == "succeeded" else (item.result.error.error.type if kind == "errored" else "")
        outcomes[item.custom_id] = (kind, detail)
        print(f"result: {item.custom_id} {kind} {detail}")
    print("in request order:", [(cid, outcomes[cid][0]) for cid in TICKETS])
    fix = [cid for cid, (kind, detail) in outcomes.items() if kind == "errored" and detail == "invalid_request_error"]
    retry = [cid for cid, (kind, _) in outcomes.items() if kind in ("expired", "canceled") or (kind == "errored" and cid not in fix)]
    print("fix before resubmitting:", fix, "| resubmit unchanged:", retry)


if __name__ == "__main__":
    main()
```
```text
created: msgbatch_illustrative in_progress | request ids sent: ['t-1', 't-2', 't-3', 't-4']
poll: in_progress {'canceled': 0, 'errored': 0, 'expired': 0, 'processing': 2, 'succeeded': 2}
poll: ended {'canceled': 0, 'errored': 1, 'expired': 1, 'processing': 0, 'succeeded': 2}
waited between polls (recorded, not slept): [60, 60] seconds
result: t-3 succeeded billing
result: t-1 succeeded shipping
result: t-4 errored invalid_request_error
result: t-2 expired 
in request order: [('t-1', 'succeeded'), ('t-2', 'expired'), ('t-3', 'succeeded'), ('t-4', 'errored')]
fix before resubmitting: ['t-4'] | resubmit unchanged: ['t-2']
```
```typescript
// A Message Batch from submission to results, against a scripted server.
// The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
// captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
// them. Waiting between polls is recorded, not slept.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-haiku-4-5-20251001";
export const TICKETS: Record<string, string> = { "t-1": "My parcel never arrived.", "t-2": "How do I change my address?", "t-3": "Charge me twice? Refund please.", "t-4": "x".repeat(10) };

const batch = (status: string, counts: object, resultsUrl: string | null = null) => ({
  id: "msgbatch_illustrative", type: "message_batch", processing_status: status, request_counts: counts,
  ended_at: status === "ended" ? "2026-10-02T10:40:00Z" : null, created_at: "2026-10-02T10:00:00Z",
  expires_at: "2026-10-03T10:00:00Z", cancel_initiated_at: null, results_url: resultsUrl,
});
const counts = (c: Partial<Record<"processing" | "succeeded" | "errored" | "canceled" | "expired", number>>) => ({ processing: 0, succeeded: 0, errored: 0, canceled: 0, expired: 0, ...c });
const line = (customId: string, result: unknown) => JSON.stringify({ custom_id: customId, result });
const succeeded = (label: string) => ({ type: "succeeded", message: message([text(label)], "end_turn", { input_tokens: 30, output_tokens: 3 }, MODEL) });

export const RESULTS =
  [
    line("t-3", succeeded("billing")),
    line("t-1", succeeded("shipping")),
    line("t-4", { type: "errored", error: { type: "error", error: { type: "invalid_request_error", message: "messages: at least one message is required" } } }),
    line("t-2", { type: "expired" }),
  ].join("\n") + "\n";

export const script = () => [
  { body: batch("in_progress", counts({ processing: 4 })) },
  { body: batch("in_progress", counts({ processing: 2, succeeded: 2 })) },
  { body: batch("ended", counts({ succeeded: 2, errored: 1, expired: 1 }), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results") },
  { body: batch("ended", counts({ succeeded: 2, errored: 1, expired: 1 }), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results") }, // results() looks the batch up first
  { text: RESULTS },
];

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

async function main() {
  const { fake, client } = clientFor(script());
  const requests = Object.entries(TICKETS).map(([custom_id, body]) => ({ custom_id, params: { model: MODEL, max_tokens: 50, messages: [{ role: "user" as const, content: `Label this ticket: ${body}` }] } }));
  const created = await client.messages.batches.create({ requests });
  console.log("created:", created.id, created.processing_status, "| request ids sent:", JSON.stringify(fake.seen[0].body.requests.map((r: any) => r.custom_id)));
  const waits: number[] = [];
  let status = created;
  while (status.processing_status !== "ended") {
    status = await client.messages.batches.retrieve(created.id);
    waits.push(60);
    console.log("poll:", status.processing_status, JSON.stringify(status.request_counts));
  }
  console.log("waited between polls (recorded, not slept):", JSON.stringify(waits), "seconds");
  const outcomes: Record<string, [string, string]> = {};
  for await (const item of await client.messages.batches.results(created.id)) {
    const r: any = item.result;
    const kind = r.type as string;
    const detail = kind === "succeeded" ? r.message.content[0].text : kind === "errored" ? r.error.error.type : "";
    outcomes[item.custom_id] = [kind, detail];
    console.log(`result: ${item.custom_id} ${kind} ${detail}`.trimEnd());
  }
  console.log("in request order:", JSON.stringify(Object.keys(TICKETS).map((cid) => [cid, outcomes[cid][0]])));
  const ids = Object.keys(outcomes);
  const fix = ids.filter((cid) => outcomes[cid][0] === "errored" && outcomes[cid][1] === "invalid_request_error");
  const retry = ids.filter((cid) => ["expired", "canceled"].includes(outcomes[cid][0]) || (outcomes[cid][0] === "errored" && !fix.includes(cid)));
  console.log("fix before resubmitting:", JSON.stringify(fix), "| resubmit unchanged:", JSON.stringify(retry));
}

if (import.meta.main) await main();
```
```text
created: msgbatch_illustrative in_progress | request ids sent: ["t-1","t-2","t-3","t-4"]
poll: in_progress {"processing":2,"succeeded":2,"errored":0,"canceled":0,"expired":0}
poll: ended {"processing":0,"succeeded":2,"errored":1,"canceled":0,"expired":1}
waited between polls (recorded, not slept): [60,60] seconds
result: t-3 succeeded billing
result: t-1 succeeded shipping
result: t-4 errored invalid_request_error
result: t-2 expired
in request order: [["t-1","succeeded"],["t-2","expired"],["t-3","succeeded"],["t-4","errored"]]
fix before resubmitting: ["t-4"] | resubmit unchanged: ["t-2"]
```
<!-- /example -->

Read the output. The first poll shows two still processing. The second shows `ended`, with one errored and one expired, and the two
`waited` entries show the polls were sixty seconds apart. The result lines arrive in the order t-3, t-1, t-4, t-2, and the program
restores the request order t-1 to t-4 by `custom_id`. Then the sorting: `t-4` errored with an invalid request, so it must be corrected
before it is resubmitted, and `t-2` expired, so it can be sent again unchanged.

## The practice: submit a batch and handle its results

You write three functions. `build_requests(items)` turns your items into batch requests and raises `BatchError` for an unsafe id, a
missing `max_tokens`, or a `stream` or `speed` field. `split_batches(requests, max_requests, max_bytes)` cuts a big job into batches
within both limits. `collect(requests, result_lines)` reads the result lines and returns the outcomes in request order, the ids to
retry, the ids to fix, the strangers and the usage of the succeeded requests. The statement is in
`exercises/21-message-batches/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests and a build file, and the
starter fails every test.

| Id | What it checks |
|---|---|
| `m1` | Results are matched to requests by custom id, not by position |
| `e1` | A custom id is 1 to 64 safe characters and unique |
| `e2` | Parameters a batch cannot take are refused |
| `e3` | A big job is cut in order by request count and by size |
| `e4` | Invalid requests are fixed, the other failures are retried |
| `e5` | A request with no result is missing and retried, and a stranger id is reported |
| `e6` | Only succeeded requests count toward usage |

Case `e6` is the billing rule in code: the sums include a `succeeded` result's token fields and nothing from the errored, canceled and
expired lines, since "You will not be billed for these requests."

## Traps

1. **Retrying every failure unchanged.** An `invalid_request_error` fails again each time and burns a day. Fix the request, then
   resubmit it.
2. **Treating a missing line as success.** A request without a result line is a gap. Compare the ids you sent with the ids you
   received.
3. **Counting every line in the usage totals.** Only the `succeeded` results carry tokens that are billed. Sum those.

## Quiz

1. The first line of a result file carries the id of the request sent third. What should the program do?
   - **a**: Treat the file as corrupt, since entries must follow the order of submission
   - **b**: Pair every entry with its source through the label you assigned, as the order is arbitrary
   - **c**: Sort by finishing time, since that is the order that counts for the match
   - **d**: Match by position once the first entries are read, since the rest follow suit

2. A batch ends with 900 succeeded results, 50 errored, 30 canceled and 20 expired. Which results are billed?
   - **a**: The 900 and the 20 that timed out, as those had reached a queue
   - **b**: The 900 and the 50 that failed on an error, as both ran a model
   - **c**: All 1,000, as the discount is a flat half of the total
   - **d**: The 900 that worked, as the other three kinds carry no charge

3. A result line for one request shows an error of type invalid_request_error. What is the sound next step?
   - **a**: Resubmit it unchanged, since a failure is usually a one-off
   - **b**: Correct the body, then send it in a later batch
   - **c**: Wait for the same batch to retry it, since that is automatic
   - **d**: Drop it, since an errored entry cannot join a new batch

<details>
<summary>Answer key</summary>

1. **b**. The page says "always use the `custom_id` field" to match, since the file order carries no meaning. *a* is ruled out because the file "may not match the ordering of requests when the batch was created". *c* is ruled out because the page names the match key: "always use the `custom_id` field". *d* is ruled out because "Batch results can be returned in any order".
2. **d**. The page says "You will not be billed for these requests." of the three failure types. *b* is ruled out because "You will not be billed for these requests." is stated for the errored results too. *c* is ruled out because "Only the `succeeded` results carry tokens that are billed." *a* is ruled out because an expired request means "the 24 hours passed before this request was sent", so it never ran.
3. **b**. The page's table says for this error "fix it first; the same request fails again", and then a new batch can carry it. *a* is ruled out because of "fix it first; the same request fails again". *c* is ruled out because "once a batch has been submitted, it cannot be modified", so a batch does not retry on its own. *d* is ruled out because the page says "Collect the failures into a new, smaller batch."

</details>

## Module quiz

This quiz covers both pages of the module.

1. A nightly job has 180,000 requests and wants the answers by the next evening. Which plan fits the module?
   - **a**: Send one batch and read the results in the order they were sent
   - **b**: Split into at least two batches and match each result by its id
   - **c**: Send them live with `stream: true` and skip the batch route
   - **d**: Send one batch with `speed` fast to finish before the next evening

2. A team tries to download its output 35 days after the job was created. What do they find?
   - **a**: The record is still viewable, but the files are gone
   - **b**: The downloads are fine, since the 29 days count from the end of processing
   - **c**: The record is deleted, since it expires together with its downloads
   - **d**: The downloads cost extra, since they come from an archive

3. A batch of 3,000 requests ends with 2,700 succeeded and 300 expired. What is the sound handling?
   - **a**: Resend the timed-out part unchanged and keep the finished answers
   - **b**: Resubmit all 3,000 so the results come from one run
   - **c**: Drop the 300, since an expired request cannot be sent again
   - **d**: Fix the 300 first, since an expired request is an invalid one

4. A program sums `input_tokens` over every line of a result file, and the total exceeds the invoice. What is the likely cause?
   - **a**: It counts the discount as a charge, since usage is reported at full rate
   - **b**: It counts cached tokens twice, since each entry repeats the prefix
   - **c**: It also adds entries for failures and cancellations, none of which are billed
   - **d**: It counts polling calls, since each poll is billed as a request

<details>
<summary>Answer key</summary>

1. **b**. The page says "A Message Batch is limited to either 100,000 Message requests or 256 MB in size, whichever is reached first", so 180,000 needs two, and the ids match the results. *a* is ruled out because "Batch results can be returned in any order". *c* is ruled out because "it's recommended to stream results back rather than download them all at once" applies to results, and a batch is the route for work nobody awaits. *d* is ruled out because the page 1 table marks `speed` as refused, and "Fast mode tunes synchronous latency" does not apply in a batch.
2. **a**. The page says "you may still view the Batch, but its results will no longer be available for download", after the 29-day window. *b* is ruled out because "Results live for 29 days after `created_at`", not after the end. *c* is ruled out because "you may still view the Batch" after the window. *d* is ruled out because the page says "Store what you need within the window." and names no archive.
3. **a**. The page's table says an expired request is resubmitted unchanged, and the finished answers stay. *b* is ruled out because "the failure of one request in a batch does not affect the processing of other requests." *c* is ruled out because an expired request means "the 24 hours passed before this request was sent", so it can go in again. *d* is ruled out because "the batch ran out of time first" is the cause of an expiry, not a bad request.
4. **c**. The page says "You will not be billed for these requests." for the failures, and "Only the `succeeded` results carry tokens that are billed." *b* is ruled out because the page says "Only the `succeeded` results carry tokens that are billed." and names no repeated prefix. *a* is ruled out because "All usage is charged at 50% of the standard API prices." *d* is ruled out because "Poll the batch by its `id`, with a wait between polls." returns a status and no message, so no tokens.

</details>
