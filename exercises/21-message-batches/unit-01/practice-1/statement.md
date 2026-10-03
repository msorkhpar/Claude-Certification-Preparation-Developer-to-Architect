# Practice: submit a batch and handle its results

A Message Batch takes many Messages requests at half price and returns one result per request, in any order, up to 24
hours later. Write the three functions around it: build the request list, cut a big job into batches, and read the
results. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there.
The rules come from the Claude documentation on batch processing, read on 2026-10-02; the lesson pages explain them. Nothing
here touches the network: results are the lines of the `.jsonl` file the API serves at `results_url`.

## The given types

| Name | Meaning |
|---|---|
| item | `{"id": <your key>, "params": <a Messages request body>}` |
| request | `{"custom_id": ..., "params": ...}`, one element of a batch's `requests` |
| result line | one line of JSON: `{"custom_id": ..., "result": {...}}` where `result.type` is `succeeded` (with `message`, whose `content` blocks and `usage` are those of a normal reply), `errored` (with `error.error.type`, such as `invalid_request_error` or `overloaded_error`), `canceled` or `expired` |
| `BatchError` | the error to raise: `field` names the offending part (`custom_id`, `params.max_tokens`, `params.stream`, `params.speed` or `size`) |

## `build_requests(items)` (TypeScript `buildRequests`, Java `Batches.buildRequests`, Kotlin `buildRequests`)

Return the requests, one per item, `custom_id` being the item's id, in the same order. Raise `BatchError` when:

1. an id is not 1 to 64 characters of letters, digits, hyphens and underscores (`custom_id`), or is used twice (`custom_id`);
2. `max_tokens` is missing or below 1 (`params.max_tokens`);
3. `stream` is true (`params.stream`) or `speed` is present (`params.speed`).

## `split_batches(requests, max_requests=100000, max_bytes=268435456)` (TypeScript `splitBatches`, Java `Batches.splitBatches`, Kotlin `splitBatches`)

Cut the requests, in order, into consecutive batches so that no batch holds more than `max_requests` requests or more than
`max_bytes` bytes. A request's size is the length in UTF-8 bytes of its compact JSON (no spaces after `,` or `:`). A single
request larger than `max_bytes` raises `BatchError` with `size`. An empty list gives an empty list.

## `collect(requests, result_lines)` (TypeScript `collect`, Java `Batches.collect`, Kotlin `collect`)

Return a map with:

- `outcomes`: one per request **in request order**, matched by `custom_id` and never by position: `{custom_id, status}` plus
  `text` (the text blocks joined) and `usage` for `succeeded`, `error_type` for `errored`; a request with no result line
  has `status` `missing`;
- `retry`: ids worth sending again unchanged, in request order: `expired`, `canceled`, `missing`, and `errored` for any type
  other than `invalid_request_error`;
- `fix`: ids of `errored` results of type `invalid_request_error`, which fail again until the request is corrected;
- `unknown`: custom ids in the file that no request carries, in file order (they are otherwise ignored);
- `usage`: the sums of `input_tokens`, `output_tokens`, `cache_creation_input_tokens` and `cache_read_input_tokens` over the
  `succeeded` results only (the other kinds are not billed).

Blank lines are skipped, and when one custom id appears twice the first result wins.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Results are matched to requests by custom id, not by position |
| `e1` | A custom id is 1 to 64 safe characters and unique |
| `e2` | Parameters a batch cannot take are refused |
| `e3` | A big job is cut in order by request count and by size |
| `e4` | Invalid requests are fixed, the other failures are retried |
| `e5` | A request with no result is missing and retried, and a stranger id is reported |
| `e6` | Only succeeded requests count toward usage |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
