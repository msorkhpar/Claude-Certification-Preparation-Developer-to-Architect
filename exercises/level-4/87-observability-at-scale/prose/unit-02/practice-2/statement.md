# Practice: trace triage

A platform keeps a random one percent of its traces, so the failures it needs to explain are mostly gone; when an agent fails, its dashboard names the span that reported the error and not the one that failed; a stale index never shows up because nothing threw; its alert fires on a single bad minute and is ignored by the third night; and its log records carry whole prompts. In this practice you write the pieces that replace that: the reason a trace is kept, with its order of priority; the search for the deepest failing span; the drift check against a baseline; the alert over consecutive windows; the redaction of a log record; and the trail of one request across components. The model is not called: the tests give you spans, metrics, series and events. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`keep_trace`, `root_cause`, `drift`, `alert_at`, `redact`, `request_trail`); TypeScript has the camel-case names (`keepTrace`, `rootCause`, `alertAt`, `requestTrail`); Java has the same camel-case names as static methods of `Triage`; Kotlin has top-level functions. A span is `Span(id, parent, kind, name, status, ms, note)` and an event is `Event(request, ts, component, message)`; the starter shows them in each language, with `bucket` already written. The first span of a trace is its root. A `kind` is `agent`, `llm`, `tool` or `retrieval`; a `status` is `ok` or `error`.

## What is already written, and what you write

The starter is a working triage with seven gaps cut out of it. Everything that is plumbing is written and correct: `bucket`, the walk from a failing span up to the root that builds the path, and the answer when nothing failed. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value (`None`, an empty list, the first failed span), so the starter runs and fails every case on an assertion. Debug a gap by logging its input with the `log` line at the top of the file (the starter already logs the input of one function; add your own `log.debug` lines the same way); a run shows the lines you logged under the failing case. Write the gaps in this order (the Java and Kotlin names are the camel-case forms, TypeScript has no leading underscore):

1. `keep_trace` unlocks `m1` and `e1`: why a trace is kept, by the order error, slow, retries, feedback, sampled.
2. `_deepest` unlocks `e2`: the failing span that no other failing span hangs below.
3. `_blamed_retrieval` unlocks `e3`: the stale or empty retrieval to blame when nothing failed.
4. `drift` unlocks `e4`: the metrics that moved over the tolerance, in either direction.
5. `alert_at` unlocks `e5`: consecutive windows over the threshold, a dip starting the count again.
6. `redact` unlocks `e6`: a record without its content fields unless allowed by name.
7. `request_trail` unlocks `e7`: one request's events in time order.

About twenty lines in all. The sections below describe the whole triage.

## What to write

- `keep_trace(trace_id, spans, rate, feedback=False, slow_ms=5000)` returns why a trace is kept, checked in this order: `error` (any span has the status `error`), `slow` (the root took more than `slow_ms`), `retries` (one tool name appears on three or more spans of kind `tool`), `feedback` (the flag is set), then `sampled` when `bucket(trace_id)` is under `rate` and `dropped` otherwise.
- `root_cause(spans)` returns the layer (`kind`), the `name`, the reason `why` and the `path` of the origin of a failure. When spans failed, the origin is the first failing span, in the order given, that is not the parent of another failing span; `why` is `failed` and `path` runs from the root to the origin by name, following the parent ids (the root's parent is empty). When none failed, the first retrieval span whose note is `stale` or `no-hits` is blamed, with `why` set to that note and the path `[root name, span name]`. With neither, return the layer `none`, an empty name, the reason `no span failed` and an empty path.
- `drift(baseline, current, tolerance)` returns a line `<name> up <percent>%` or `<name> down <percent>%` for each metric whose change is over `tolerance` percent, sorted by name. The percent is `|current - baseline| * 100 / baseline`, whole numbers, rounded down; a change exactly at the tolerance is not over it. When the baseline is zero, a move from zero is 100 percent and no move is 0, and nothing is divided by zero.
- `alert_at(series, threshold, windows)` returns the index of the value that completes `windows` consecutive values over the threshold, or -1. A value equal to the threshold is not over it, and a value that is not over starts the count again.
- `redact(event, allowed=())` returns the record without the fields `prompt`, `response`, `tool_input` and `tool_output`, unless a field is named in `allowed`. All other fields stay.
- `request_trail(events, request)` returns `<component>: <message>` for each event of that request in order of `ts`, events with equal `ts` keeping the order they were given. An unknown request has an empty trail.

## Why each part is there, and what you should see

1. **Keep what you must explain.** *You should see* a failed trace kept at a rate of zero, and a healthy one kept by its id and not by chance.
2. **The order of the checks is the policy.** *You should see* a trace that failed and was slow called an error, and three calls of one tool called retries even when the user flagged it.
3. **The origin is the deepest failure.** *You should see* the orchestrator and the researcher passed over for the tool that failed, with the path from the root.
4. **A suspicion is not a fact.** *You should see* a stale retrieval blamed only when no span failed.
5. **A change in either direction.** *You should see* a metric that fell reported as well as one that rose, and a zero baseline handled.
6. **A change that lasts.** *You should see* one bad window give no alert when three in a row are required, and a dip start the count again.
7. **Records without content.** *You should see* the prompt and tool outputs gone, ids and counts kept, and a field allowed by name kept.
8. **One request, many components.** *You should see* the events of one request from every component in time order.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Every trace gets one reason and a healthy trace is kept by its id |
| `e1` | An error outranks a slow root, which outranks retries, which outrank a flag |
| `e2` | The root cause is the deepest failing span and its path starts at the root |
| `e3` | A stale or empty retrieval is blamed only when no span failed |
| `e4` | Drift reports a move in either direction over the tolerance and never divides by zero |
| `e5` | An alert needs consecutive windows over the threshold and a dip starts the count again |
| `e6` | A log record drops the content fields unless they are allowed by name |
| `e7` | A request's trail joins the events of every component in time order |
