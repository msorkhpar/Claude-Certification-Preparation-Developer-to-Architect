# The prefix, the breakpoint and the usage fields

**Level:** Developer · **Module 20:** Prompt caching · **Page 1 of 2**
**Exams:** DV2

**After this page you can** say what a prompt cache stores, place `cache_control` so that requests share it, read the three
usage fields to tell a write from a read from a miss, and explain why a request that was marked for caching can still return
no cache hit.

Checked against the Claude API documentation (Prompt caching, Pricing) on 2026-10-02, and by running the example offline in
the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The server in the example is an illustrative,
hand-written stand-in that applies the documented prefix rule, not a capture of the real service.

## Why it matters

Most Claude applications send the same large block of text again and again: a system prompt, tool definitions, a document,
the history of a conversation. Without caching, every request pays full input price and full processing time for it. With
caching, the repeated part is read at a fraction of the price. The exam asks where the breakpoint goes, what a response's usage
fields mean, and why a cache that should hit does not.

## The idea

### A cache stores a prefix

The documentation describes what is stored in one sentence:

> Prompt caching references the entire prompt: `tools`, `system`, and `messages` (in that order), up to and including the block designated with `cache_control`.

Source: Prompt caching.

Read it as a hierarchy. The cache key is a hash of everything from the start of the request up to the marked block, in the order
tools, then system, then messages. A change anywhere inside that span gives a different hash, and the request starts over. The
page says "Cache hits require 100% identical prompt segments". That is why stable content goes first, and why anything that
varies from request to request, a timestamp for instance, goes after the last breakpoint.

### Two ways to mark it

**Automatic caching** is one `cache_control` field at the top level of the request: `{"type": "ephemeral"}`. The system places
the breakpoint on the last cacheable block and moves it forward as a conversation grows. It suits multi-turn chats.

**Explicit breakpoints** put `cache_control` on individual blocks. They suit prompts whose sections change at different rates:
tools rarely, the system prompt daily, the question every time. You can place up to four. An automatic breakpoint uses one of
those four slots, and a request with four explicit ones already placed returns a 400 when it also asks for automatic caching.

Marking costs nothing by itself: "Cache breakpoints themselves don't add any cost." You pay for what is written, what is read
and what is neither.

### Three usage fields

Every response reports what the cache did, inside `usage`:

| Field | Meaning |
|---|---|
| `cache_creation_input_tokens` | tokens written to the cache by this request |
| `cache_read_input_tokens` | tokens read from the cache by this request |
| `input_tokens` | tokens after the last breakpoint, which are not cached |

The last row is the one people get wrong. `input_tokens` is not the size of the prompt when caching is on. The page gives the
sum:

> total_input_tokens = cache_read_input_tokens + cache_creation_input_tokens + input_tokens

Source: Prompt caching.

Its worked example has 100,000 cached tokens read, none written and 50 after the breakpoint, for 100,050 in all. A dashboard
that plots only `input_tokens` shows a number that falls as caching succeeds, and a bill that does not match it. The three
fields are also the test of whether caching worked: "if both `cache_creation_input_tokens` and `cache_read_input_tokens` are 0,
the prompt was not cached".

### The minimum

A prefix shorter than the model's minimum is not cached, and nothing says so. The page lists the minimum per model, the same on
every platform where the model is available. For the course models:

| Model | Minimum cacheable prompt |
|---|---|
| Claude Fable 5.1 | 512 tokens |
| Claude Opus 5.5 | 512 tokens |
| Claude Sonnet 5.5 | 512 tokens |
| Claude Haiku 4.5 | 4,096 tokens |

The page says: "Any requests to cache fewer than this number of tokens will be processed without caching, and no error is
returned." You find out only by the two zeros in the usage fields. If the prompt falls just short, the page advises that
"expanding the cached content to reach the threshold is often worthwhile", since reads are much cheaper than ordinary input.

### Where the breakpoint goes

Put it on the last block that is identical across the requests you want to share a cache. The page names the common mistake: a
large static system prompt followed by one block that carries a timestamp and the user's message. With the breakpoint on that last
block, the hash includes the timestamp, so request 2 never matches request 1: "You pay for a fresh cache write on every request
and never get a read." The fix is to move the breakpoint to the last stable block. The lookback does not rescue you, because it
"only finds entries that earlier requests wrote at their own breakpoints".

### The example

The example sends a 600-token policy as the system prompt to a scripted server that applies the prefix rule, a five-minute
lifetime and a minimum of 512 tokens (the figure for `claude-sonnet-5-5`). It runs seven requests and prints the three usage
fields. Rows 1 to 3 show a stable prefix and a lapse. Rows 4 to 7 compare a timestamp placed before the policy with one placed
after it.

<!-- example: m20-cache-hits tabs: python,typescript -->
```python
"""Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.

The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
"""
import json
import math

from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
POLICY = "Refund policy clause: items may be returned within 14 days. " * 40  # about 600 tokens
MARK = {"type": "ephemeral"}


def tokens(piece):
    return math.ceil(len(piece) / 4)


def blocks_of(body):
    """The request flattened in prefix order: tools, then system, then messages, each with its cache_control."""
    out = [(json.dumps(tool, sort_keys=True), tool.get("cache_control")) for tool in body.get("tools", [])]
    system = body.get("system", [])
    out += [(b["text"], b.get("cache_control")) for b in ([{"text": system}] if isinstance(system, str) else system)]
    for m in body["messages"]:
        content = m["content"] if isinstance(m["content"], list) else [{"text": m["content"]}]
        out += [(m["role"] + ": " + b["text"], b.get("cache_control")) for b in content]
    return out


class CacheSim:
    def __init__(self, minimum=512, ttl=300):
        self.minimum, self.ttl, self.clock, self.entries = minimum, ttl, 0, {}

    def __call__(self, body):
        blocks = blocks_of(body)
        marks = [i for i, (_, mark) in enumerate(blocks) if mark]
        key = lambda i: "\x00".join(t for t, _ in blocks[: i + 1])  # noqa: E731
        size = lambda i: sum(tokens(t) for t, _ in blocks[: i + 1])  # noqa: E731
        read = written = 0
        hit = None
        for i in reversed(marks):
            if self.entries.get(key(i), -1) > self.clock:
                read, hit = size(i), i
                self.entries[key(i)] = self.clock + self.ttl  # a hit refreshes the entry
                break
        if marks and hit != marks[-1] and size(marks[-1]) >= self.minimum:
            written = size(marks[-1]) - read
            self.entries[key(marks[-1])] = self.clock + self.ttl
        fresh = size(len(blocks) - 1) - read - written
        usage = {"input_tokens": fresh, "output_tokens": 20, "cache_read_input_tokens": read, "cache_creation_input_tokens": written,
                 "cache_creation": {"ephemeral_5m_input_tokens": written, "ephemeral_1h_input_tokens": 0}}
        return message([text("ok")], usage=usage)


def request(system, question):
    return dict(model=MODEL, max_tokens=50, system=system, messages=[{"role": "user", "content": question}])


def stable(question):
    return request([{"type": "text", "text": POLICY, "cache_control": MARK}], question)


def stamp_first(clock_label, question):
    return request([{"type": "text", "text": f"Current time: {clock_label}"}, {"type": "text", "text": POLICY, "cache_control": MARK}], question)


def stamp_last(clock_label, question):
    return request([{"type": "text", "text": POLICY, "cache_control": MARK}, {"type": "text", "text": f"Current time: {clock_label}"}], question)


def run(sim, label, body, wait=0):
    sim.clock += wait
    client, _ = scripted_client(sim)
    usage = client.messages.create(**body).usage
    print(f"{label:34} write {usage.cache_creation_input_tokens:4}  read {usage.cache_read_input_tokens:4}  fresh {usage.input_tokens:4}")
    return usage


def main():
    sim = CacheSim()
    run(sim, "1 stable system, first call", stable("Can I return a lamp?"))
    run(sim, "2 same system, new question", stable("Can I return a chair?"), wait=60)
    run(sim, "3 six minutes of silence", stable("Can I return a desk?"), wait=360)
    sim = CacheSim()
    run(sim, "4 timestamp first, 10:01", stamp_first("10:01", "Can I return a lamp?"))
    run(sim, "5 timestamp first, 10:02", stamp_first("10:02", "Can I return a lamp?"), wait=60)
    run(sim, "6 timestamp last, 10:03", stamp_last("10:03", "Can I return a lamp?"), wait=60)
    run(sim, "7 timestamp last, 10:04", stamp_last("10:04", "Can I return a lamp?"), wait=60)


if __name__ == "__main__":
    main()
```
```text
1 stable system, first call        write  600  read    0  fresh    7
2 same system, new question        write    0  read  600  fresh    7
3 six minutes of silence           write  600  read    0  fresh    7
4 timestamp first, 10:01           write  605  read    0  fresh    7
5 timestamp first, 10:02           write  605  read    0  fresh    7
6 timestamp last, 10:03            write  600  read    0  fresh   12
7 timestamp last, 10:04            write    0  read  600  fresh   12
```
```typescript
// Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.
// The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
// caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
// minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
// tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
export const POLICY = "Refund policy clause: items may be returned within 14 days. ".repeat(40); // about 600 tokens
const MARK = { type: "ephemeral" };

export const tokens = (piece: string) => Math.ceil(piece.length / 4);

type Piece = [string, unknown];

function blocksOf(body: any): Piece[] {
  const out: Piece[] = (body.tools ?? []).map((t: any): Piece => [JSON.stringify(t), t.cache_control]);
  const system = typeof body.system === "string" ? [{ text: body.system }] : (body.system ?? []);
  for (const b of system) out.push([b.text, b.cache_control]);
  for (const m of body.messages) {
    const content = Array.isArray(m.content) ? m.content : [{ text: m.content }];
    for (const b of content) out.push([`${m.role}: ${b.text}`, b.cache_control]);
  }
  return out;
}

export class CacheSim {
  clock = 0;
  entries = new Map<string, number>();
  minimum: number;
  ttl: number;
  constructor(minimum = 512, ttl = 300) {
    this.minimum = minimum;
    this.ttl = ttl;
  }

  reply(body: any) {
    const blocks = blocksOf(body);
    const marks = blocks.flatMap(([, mark], i) => (mark ? [i] : []));
    const key = (i: number) => blocks.slice(0, i + 1).map(([t]) => t).join("\0");
    const size = (i: number) => blocks.slice(0, i + 1).reduce((n, [t]) => n + tokens(t), 0);
    let read = 0;
    let written = 0;
    let hit: number | null = null;
    for (const i of [...marks].reverse()) {
      if ((this.entries.get(key(i)) ?? -1) > this.clock) {
        read = size(i);
        hit = i;
        this.entries.set(key(i), this.clock + this.ttl); // a hit refreshes the entry
        break;
      }
    }
    const last = marks.length ? marks[marks.length - 1] : null;
    if (last !== null && hit !== last && size(last) >= this.minimum) {
      written = size(last) - read;
      this.entries.set(key(last), this.clock + this.ttl);
    }
    const fresh = size(blocks.length - 1) - read - written;
    const usage = {
      input_tokens: fresh, output_tokens: 20, cache_read_input_tokens: read, cache_creation_input_tokens: written,
      cache_creation: { ephemeral_5m_input_tokens: written, ephemeral_1h_input_tokens: 0 },
    };
    return { body: message([text("ok")], "end_turn", usage as any) };
  }
}

const request = (system: unknown[], question: string) => ({ model: MODEL, max_tokens: 50, system, messages: [{ role: "user" as const, content: question }] });
const policyBlock = { type: "text", text: POLICY, cache_control: MARK };

export const stable = (question: string) => request([policyBlock], question);
export const stampFirst = (label: string, question: string) => request([{ type: "text", text: `Current time: ${label}` }, policyBlock], question);
export const stampLast = (label: string, question: string) => request([policyBlock, { type: "text", text: `Current time: ${label}` }], question);

export async function usageOf(sim: CacheSim, body: any, wait = 0): Promise<any> {
  sim.clock += wait;
  const fake = scriptedFetch([(sent: any) => sim.reply(sent)]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  return (await client.messages.create(body)).usage;
}

async function run(sim: CacheSim, label: string, body: any, wait = 0) {
  const u = await usageOf(sim, body, wait);
  console.log(`${label.padEnd(34)} write ${String(u.cache_creation_input_tokens).padStart(4)}  read ${String(u.cache_read_input_tokens).padStart(4)}  fresh ${String(u.input_tokens).padStart(4)}`);
}

async function main() {
  let sim = new CacheSim();
  await run(sim, "1 stable system, first call", stable("Can I return a lamp?"));
  await run(sim, "2 same system, new question", stable("Can I return a chair?"), 60);
  await run(sim, "3 six minutes of silence", stable("Can I return a desk?"), 360);
  sim = new CacheSim();
  await run(sim, "4 timestamp first, 10:01", stampFirst("10:01", "Can I return a lamp?"));
  await run(sim, "5 timestamp first, 10:02", stampFirst("10:02", "Can I return a lamp?"), 60);
  await run(sim, "6 timestamp last, 10:03", stampLast("10:03", "Can I return a lamp?"), 60);
  await run(sim, "7 timestamp last, 10:04", stampLast("10:04", "Can I return a lamp?"), 60);
}

if (import.meta.main) await main();
```
```text
1 stable system, first call        write  600  read    0  fresh    7
2 same system, new question        write    0  read  600  fresh    7
3 six minutes of silence           write  600  read    0  fresh    7
4 timestamp first, 10:01           write  605  read    0  fresh    7
5 timestamp first, 10:02           write  605  read    0  fresh    7
6 timestamp last, 10:03            write  600  read    0  fresh   12
7 timestamp last, 10:04            write    0  read  600  fresh   12
```
<!-- /example -->

Read the output row by row. Row 1 writes the 600 tokens. Row 2, a minute later, reads them and pays for only the new question
(7 fresh tokens). Row 3 comes six minutes later: the five-minute entry has lapsed, so it writes again. Rows 4 and 5 put the
timestamp first: the prefix differs each time, so each request writes 605 tokens and reads nothing. Rows 6 and 7 move the
timestamp after the breakpoint: row 6 writes once, row 7 reads, and the 12 fresh tokens are the question plus the timestamp. The
two usage patterns are the whole diagnosis: writes without reads mean the prefix keeps changing.

## Traps

1. **Reading `input_tokens` as the prompt size.** It counts only what follows the last breakpoint. Add the read and write
   fields to get the total.
2. **Expecting an error when the prompt is too short.** Below the minimum nothing is cached and nothing is raised. Check for the
   two zeros. Note that Haiku 4.5 needs 4,096 tokens where the others need 512.
3. **Marking the last block when it varies.** Automatic caching puts the breakpoint on the last cacheable block, which in a
   prompt that ends with a timestamp is the block that changes every time. Mark the end of the stable prefix explicitly.

## Quiz

1. A response reports `cache_read_input_tokens` of 8,000, `cache_creation_input_tokens` of 0 and `input_tokens` of 40. How many
   input tokens did the request carry?
   - **a**: 7,960
   - **b**: 8,000
   - **c**: 40
   - **d**: 8,040

2. A team marks a 1,500-token system prompt with `cache_control` on Claude Haiku 4.5 and sees both cache fields at 0, with no
   error. What explains it?
   - **a**: The text is under that model's 4,096 minimum, so it was handled as ordinary input
   - **b**: The text is over that model's minimum, so the API is quietly returning stored answers
   - **c**: The marker was ignored because system prompts are outside what the cache can hold
   - **d**: The five-minute lifetime had lapsed before the first request was even sent

3. A request puts a block with the current clock reading ahead of a large stable policy, with the marker on the policy. Every
   call writes everything and never reads. Why?
   - **a**: The clock block counts as a second marker, which triggers a fresh write on each turn
   - **b**: The hash covers the marked block, so the policy is compared by itself and the clock is ignored
   - **c**: All content before the boundary feeds the hash, so a changing line gives a new one
   - **d**: The hash is rebuilt from the user question, which differs on each turn of the chat

<details>
<summary>Answer key</summary>

1. **d**. The page gives "total_input_tokens = cache_read_input_tokens + cache_creation_input_tokens + input_tokens", so 8,000 + 0 + 40. *b* is ruled out because the page sum adds a third term, and "total_input_tokens = cache_read_input_tokens + cache_creation_input_tokens + input_tokens" includes the 40. *c* is ruled out because that field holds only "tokens after the last breakpoint, which are not cached". *a* is ruled out because the sum "total_input_tokens = cache_read_input_tokens + cache_creation_input_tokens + input_tokens" has no subtraction.
2. **a**. The page says "Any requests to cache fewer than this number of tokens will be processed without caching" and gives 4,096 for Claude Haiku 4.5. *b* is ruled out because 1,500 is below the minimum, and "the prompt was not cached" is what two zeros mean. *c* is ruled out because the page's hierarchy runs "up to and including the block designated with" the marker, and a system block can be that block. *d* is ruled out because "Row 1 writes the 600 tokens" on a first request, so there is no earlier entry to lapse.
3. **c**. The page says "The cache key is a hash of everything from the start of the request up to the marked block", so a changing block before it gives a new key. *b* is ruled out because that same sentence says the key is "a hash of everything from the start of the request", not of one block. *a* is ruled out because "Cache breakpoints themselves don't add any cost." and only a block carrying `cache_control` is a breakpoint. *d* is ruled out because "Row 2, a minute later, reads them and pays for only the new question".

</details>
