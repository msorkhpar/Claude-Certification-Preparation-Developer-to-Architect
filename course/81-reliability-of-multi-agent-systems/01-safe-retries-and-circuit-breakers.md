# Safe retries and circuit breakers

**Level:** Architect Professional · **Module 81:** Reliability of multi-agent systems · **Page 1 of 2**
**Exams:** P1, P5

**After this page you can** say when a retry is safe and when it pays twice, design an idempotency key that is recorded together with the effect it guards, keep the same key on every retry of one task, and place a circuit breaker around an agent that keeps failing so that the run stops spending on it.

Checked on 2026-10-04 against Anthropic's engineering article "How we built our multi-agent research system", Anthropic's "Building effective agents", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domains 1 and 5. The example ran in the course's container in Python, TypeScript, Java and Kotlin with the same output. Nothing is called over a network: the failures are scripted and the clock is a number, so the output shows the safeguards and not a real system's failure rates.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 5 asks for reliability of context and tools, and domain 1 for a design that survives failure, so the exam asks what to do when a step times out, a response is lost or an agent keeps failing. *What Anthropic's article says:* its research system combines "the adaptability of AI agents built on Claude with deterministic safeguards like retry logic and regular checkpoints", and it reports that agents are stateful and that errors compound, so that minor failures can be catastrophic when nothing handles them. *How to read both:* the exam keys the deterministic answer, a safeguard in code outside the model, and rejects the answer that asks the model to be careful. Retry, key and breaker are patterns of ordinary distributed systems, applied here to agents; they are the course's teaching of the idea and not a feature of a Claude product.

## Why it matters

An agent calls a refund tool. The tool pays, and the response is lost on the way back. The agent sees a timeout, and the retry logic does what it was built to do: it tries again. The customer is refunded twice. Nothing misbehaved: the model reasoned correctly, the network failed as networks do, and the retry was exactly what the design asked for. A multi-agent system makes this worse, since more calls are made, in parallel, by components that cannot see one another's attempts. Reliability here is not about a better model. It is about what happens on the second attempt.

## The idea

### A retry is safe only when the repeat is recognised

Retrying a call that reads is harmless. Retrying a call that changes something (pays, sends, writes, books) repeats the change unless the tool can tell that this attempt is a repeat. The mechanism is an **idempotency key**: a value that identifies the intended action, sent with the call. The tool keeps the keys it has seen, and a second call with a known key returns the first result instead of acting again.

Three details decide whether the key works.

1. **Record the key together with the effect.** The key and the payment are saved in one step, in one transaction. If they were saved apart, a crash between the two leaves an effect with no key, or a key with no effect, and the next attempt either pays twice or never pays.
2. **Use the same key on every retry of one task.** The key names the intention, not the attempt. A new key for each attempt turns the retry into a new refund. The key is made once, before the first call, from what identifies the task (the order and the action), and it is stored with the task.
3. **The danger is the lost response.** A tool that fails before acting is safe to retry. The case that needs the key is the one in which the action happened and the answer did not arrive, which is why the example loses the response after the money has moved.

The key belongs to the tool's design as much as the agent's. When a tool that changes things offers no key, the architect wraps it with one, or does not let an agent retry it.

### Retries have limits, and not every failure is a retry

A retry helps with failures that pass: a timeout, a rate limit, a response that was lost. It does not help with one that does not: a refused request, a missing permission, an invalid input. The first kind is **transient**, the second **fatal**. A runner retries the first kind up to a fixed number of calls and then fails the task with a clear reason, and it never retries the second. An unlimited retry is a way to spend money while nothing changes.

### A circuit breaker stops the spending

Suppose an agent's backend is down. Every task that needs it retries, waits and fails, and a run of fifty tasks makes a hundred and fifty wasted calls. A **circuit breaker** is a small state machine around the agent that watches consecutive failures.

| State | What it does | How it changes |
|---|---|---|
| **Closed** | Calls go through | After a set number of failures in a row, it opens |
| **Open** | Calls are refused at once, without reaching the agent | After a cooldown, it moves to half-open |
| **Half-open** | One probe call goes through | A success closes it; a failure opens it again |

A success resets the count, so scattered failures do not open it. The count is of consecutive failures for one agent, not of all failures in the run: an agent that is healthy must not be cut off by a failing neighbour. When the breaker is open, the task fails fast with the reason `circuit open`, and the rest of the plan goes on (page 2).

### The example

<!-- example: m81-reliable-call tabs: python,typescript,java,kotlin -->
```python
"""A retried refund that must not pay twice, and a circuit breaker around a failing agent.

Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.
"""


class Transient(Exception):
    """A failure worth retrying: a timeout, a rate limit, a lost response."""


class Ledger:
    """The refunds actually paid, and the idempotency keys already used."""

    def __init__(self):
        self.paid = []
        self.keys = {}


def refund(ledger, key, order, amount, lose_response):
    """Pays once per key. The response may be lost after the money has moved, which is the dangerous case."""
    if key is not None and key in ledger.keys:
        return ledger.keys[key]
    ledger.paid.append((order, amount))
    receipt = f"refund-{len(ledger.paid)}"
    if key is not None:
        ledger.keys[key] = receipt
    if lose_response:
        raise Transient("response lost")
    return receipt


def retry(attempt, tries):
    """Calls attempt(n) up to `tries` times while it raises Transient; returns (result, calls made)."""
    for n in range(1, tries + 1):
        try:
            return attempt(n), n
        except Transient:
            if n == tries:
                raise


class Breaker:
    """Closed until `threshold` failures in a row, then open for `cooldown` seconds, then one probe (half open)."""

    def __init__(self, threshold, cooldown):
        self.threshold, self.cooldown = threshold, cooldown
        self.failures, self.opened_at = 0, None

    def state(self, now):
        if self.opened_at is None:
            return "closed"
        return "half-open" if now - self.opened_at >= self.cooldown else "open"

    def allow(self, now):
        return self.state(now) != "open"

    def record(self, ok, now):
        if ok:
            self.failures, self.opened_at = 0, None
        else:
            self.failures += 1
            if self.failures >= self.threshold or self.state(now) == "half-open":
                self.opened_at = now


def main():
    naive = Ledger()
    retry(lambda n: refund(naive, None, "order-7", 40, lose_response=(n == 1)), 2)
    print(f"retry without a key: {len(naive.paid)} refunds paid for one order")
    keyed = Ledger()
    receipt, calls = retry(lambda n: refund(keyed, "order-7:refund", "order-7", 40, lose_response=(n == 1)), 2)
    print(f"retry with a key: {len(keyed.paid)} refund paid for one order, {calls} calls, receipt {receipt}")
    breaker = Breaker(threshold=3, cooldown=30)
    reached = 0
    for now, healthy in [(0, False), (1, False), (2, False), (3, True), (40, True), (41, True)]:
        if not breaker.allow(now):
            print(f"t={now}: breaker {breaker.state(now)}, call refused without reaching the agent")
            continue
        reached += 1
        breaker.record(healthy, now)
        print(f"t={now}: call {'succeeded' if healthy else 'failed'}, breaker {breaker.state(now)}")
    print(f"the agent was reached {reached} times in 6 attempts")


if __name__ == "__main__":
    main()
```
```text
retry without a key: 2 refunds paid for one order
retry with a key: 1 refund paid for one order, 2 calls, receipt refund-1
t=0: call failed, breaker closed
t=1: call failed, breaker closed
t=2: call failed, breaker open
t=3: breaker open, call refused without reaching the agent
t=40: call succeeded, breaker closed
t=41: call succeeded, breaker closed
the agent was reached 5 times in 6 attempts
```
```typescript
// A retried refund that must not pay twice, and a circuit breaker around a failing agent.
//
// Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
// deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
// safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
// attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
// a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.

export class Transient extends Error {} // a failure worth retrying: a timeout, a rate limit, a lost response

/** The refunds actually paid, and the idempotency keys already used. */
export class Ledger {
  paid: Array<[string, number]> = [];
  keys = new Map<string, string>();
}

/** Pays once per key. The response may be lost after the money has moved, which is the dangerous case. */
export function refund(ledger: Ledger, key: string | null, order: string, amount: number, loseResponse: boolean): string {
  if (key !== null && ledger.keys.has(key)) return ledger.keys.get(key)!;
  ledger.paid.push([order, amount]);
  const receipt = `refund-${ledger.paid.length}`;
  if (key !== null) ledger.keys.set(key, receipt);
  if (loseResponse) throw new Transient("response lost");
  return receipt;
}

/** Calls attempt(n) up to `tries` times while it throws Transient; returns [result, calls made]. */
export function retry<T>(attempt: (n: number) => T, tries: number): [T, number] {
  for (let n = 1; ; n++) {
    try {
      return [attempt(n), n];
    } catch (error) {
      if (!(error instanceof Transient) || n >= tries) throw error;
    }
  }
}

/** Closed until `threshold` failures in a row, then open for `cooldown` seconds, then one probe (half open). */
export class Breaker {
  failures = 0;
  openedAt: number | null = null;
  threshold: number;
  cooldown: number;

  constructor(threshold: number, cooldown: number) {
    this.threshold = threshold;
    this.cooldown = cooldown;
  }

  state(now: number): string {
    if (this.openedAt === null) return "closed";
    return now - this.openedAt >= this.cooldown ? "half-open" : "open";
  }

  allow(now: number): boolean {
    return this.state(now) !== "open";
  }

  record(ok: boolean, now: number): void {
    if (ok) {
      this.failures = 0;
      this.openedAt = null;
    } else {
      this.failures += 1;
      if (this.failures >= this.threshold || this.state(now) === "half-open") this.openedAt = now;
    }
  }
}

function main() {
  const naive = new Ledger();
  retry((n) => refund(naive, null, "order-7", 40, n === 1), 2);
  console.log(`retry without a key: ${naive.paid.length} refunds paid for one order`);
  const keyed = new Ledger();
  const [receipt, calls] = retry((n) => refund(keyed, "order-7:refund", "order-7", 40, n === 1), 2);
  console.log(`retry with a key: ${keyed.paid.length} refund paid for one order, ${calls} calls, receipt ${receipt}`);
  const breaker = new Breaker(3, 30);
  let reached = 0;
  for (const [now, healthy] of [[0, false], [1, false], [2, false], [3, true], [40, true], [41, true]] as Array<[number, boolean]>) {
    if (!breaker.allow(now)) {
      console.log(`t=${now}: breaker ${breaker.state(now)}, call refused without reaching the agent`);
      continue;
    }
    reached += 1;
    breaker.record(healthy, now);
    console.log(`t=${now}: call ${healthy ? "succeeded" : "failed"}, breaker ${breaker.state(now)}`);
  }
  console.log(`the agent was reached ${reached} times in 6 attempts`);
}

if (import.meta.main) main();
```
```text
retry without a key: 2 refunds paid for one order
retry with a key: 1 refund paid for one order, 2 calls, receipt refund-1
t=0: call failed, breaker closed
t=1: call failed, breaker closed
t=2: call failed, breaker open
t=3: breaker open, call refused without reaching the agent
t=40: call succeeded, breaker closed
t=41: call succeeded, breaker closed
the agent was reached 5 times in 6 attempts
```
```java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * A retried refund that must not pay twice, and a circuit breaker around a failing agent.
 *
 * <p>Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
 * deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
 * safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
 * attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
 * a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.
 */
public final class ReliableCall {
    /** A failure worth retrying: a timeout, a rate limit, a lost response. */
    static final class Transient extends RuntimeException {
        Transient(String message) {
            super(message);
        }
    }

    /** The refunds actually paid, and the idempotency keys already used. */
    static final class Ledger {
        final List<String> paid = new ArrayList<>();
        final Map<String, String> keys = new HashMap<>();
    }

    record Attempt<T>(T result, int calls) {}

    /** Pays once per key. The response may be lost after the money has moved, which is the dangerous case. */
    static String refund(Ledger ledger, String key, String order, int amount, boolean loseResponse) {
        if (key != null && ledger.keys.containsKey(key)) return ledger.keys.get(key);
        ledger.paid.add(order + ":" + amount);
        String receipt = "refund-" + ledger.paid.size();
        if (key != null) ledger.keys.put(key, receipt);
        if (loseResponse) throw new Transient("response lost");
        return receipt;
    }

    /** Calls attempt(n) up to {@code tries} times while it throws Transient; returns the result and the calls made. */
    static <T> Attempt<T> retry(IntFunction<T> attempt, int tries) {
        for (int n = 1; ; n++) {
            try {
                return new Attempt<>(attempt.apply(n), n);
            } catch (Transient e) {
                if (n >= tries) throw e;
            }
        }
    }

    /** Closed until {@code threshold} failures in a row, then open for {@code cooldown} seconds, then one probe (half open). */
    static final class Breaker {
        private final int threshold;
        private final int cooldown;
        private int failures = 0;
        private Integer openedAt = null;

        Breaker(int threshold, int cooldown) {
            this.threshold = threshold;
            this.cooldown = cooldown;
        }

        String state(int now) {
            if (openedAt == null) return "closed";
            return now - openedAt >= cooldown ? "half-open" : "open";
        }

        boolean allow(int now) {
            return !state(now).equals("open");
        }

        void record(boolean ok, int now) {
            if (ok) {
                failures = 0;
                openedAt = null;
            } else {
                failures += 1;
                if (failures >= threshold || state(now).equals("half-open")) openedAt = now;
            }
        }
    }

    public static void main(String[] args) {
        Ledger naive = new Ledger();
        retry(n -> refund(naive, null, "order-7", 40, n == 1), 2);
        System.out.println("retry without a key: " + naive.paid.size() + " refunds paid for one order");
        Ledger keyed = new Ledger();
        Attempt<String> done = retry(n -> refund(keyed, "order-7:refund", "order-7", 40, n == 1), 2);
        System.out.println("retry with a key: " + keyed.paid.size() + " refund paid for one order, " + done.calls() + " calls, receipt " + done.result());
        Breaker breaker = new Breaker(3, 30);
        int reached = 0;
        int[] times = {0, 1, 2, 3, 40, 41};
        boolean[] healthy = {false, false, false, true, true, true};
        for (int i = 0; i < times.length; i++) {
            int now = times[i];
            if (!breaker.allow(now)) {
                System.out.println("t=" + now + ": breaker " + breaker.state(now) + ", call refused without reaching the agent");
                continue;
            }
            reached++;
            breaker.record(healthy[i], now);
            System.out.println("t=" + now + ": call " + (healthy[i] ? "succeeded" : "failed") + ", breaker " + breaker.state(now));
        }
        System.out.println("the agent was reached " + reached + " times in 6 attempts");
    }
}
```
```text
retry without a key: 2 refunds paid for one order
retry with a key: 1 refund paid for one order, 2 calls, receipt refund-1
t=0: call failed, breaker closed
t=1: call failed, breaker closed
t=2: call failed, breaker open
t=3: breaker open, call refused without reaching the agent
t=40: call succeeded, breaker closed
t=41: call succeeded, breaker closed
the agent was reached 5 times in 6 attempts
```
```kotlin
/**
 * A retried refund that must not pay twice, and a circuit breaker around a failing agent.
 *
 * Anthropic's multi-agent research write-up (read on 2026-10-04) says it combines "the adaptability of AI agents built on Claude with
 * deterministic safeguards like retry logic and regular checkpoints". This file shows two such safeguards on scripted failures. A retry is
 * safe only when the tool it repeats is idempotent: the refund tool records its idempotency key together with its effect, so a second
 * attempt with the same key returns the first result. A breaker stops calling an agent that keeps failing and lets one probe through after
 * a cooldown. Nothing is called over a network: the failures are scripted and the clock is a number.
 */
/** A failure worth retrying: a timeout, a rate limit, a lost response. */
class Transient(message: String) : RuntimeException(message)

/** The refunds actually paid, and the idempotency keys already used. */
class Ledger {
    val paid = mutableListOf<String>()
    val keys = mutableMapOf<String, String>()
}

/** Pays once per key. The response may be lost after the money has moved, which is the dangerous case. */
fun refund(ledger: Ledger, key: String?, order: String, amount: Int, loseResponse: Boolean): String {
    if (key != null && key in ledger.keys) return ledger.keys.getValue(key)
    ledger.paid += "$order:$amount"
    val receipt = "refund-${ledger.paid.size}"
    if (key != null) ledger.keys[key] = receipt
    if (loseResponse) throw Transient("response lost")
    return receipt
}

/** Calls attempt(n) up to `tries` times while it throws Transient; returns the result and the calls made. */
fun <T> retry(tries: Int, attempt: (Int) -> T): Pair<T, Int> {
    var n = 1
    while (true) {
        try {
            return attempt(n) to n
        } catch (e: Transient) {
            if (n >= tries) throw e
        }
        n++
    }
}

/** Closed until `threshold` failures in a row, then open for `cooldown` seconds, then one probe (half open). */
class Breaker(private val threshold: Int, private val cooldown: Int) {
    private var failures = 0
    private var openedAt: Int? = null

    fun state(now: Int): String {
        val opened = openedAt ?: return "closed"
        return if (now - opened >= cooldown) "half-open" else "open"
    }

    fun allow(now: Int) = state(now) != "open"

    fun record(ok: Boolean, now: Int) {
        if (ok) {
            failures = 0
            openedAt = null
        } else {
            failures += 1
            if (failures >= threshold || state(now) == "half-open") openedAt = now
        }
    }
}

fun main() {
    val naive = Ledger()
    retry(2) { n -> refund(naive, null, "order-7", 40, n == 1) }
    println("retry without a key: ${naive.paid.size} refunds paid for one order")
    val keyed = Ledger()
    val (receipt, calls) = retry(2) { n -> refund(keyed, "order-7:refund", "order-7", 40, n == 1) }
    println("retry with a key: ${keyed.paid.size} refund paid for one order, $calls calls, receipt $receipt")
    val breaker = Breaker(3, 30)
    var reached = 0
    for ((now, healthy) in listOf(0 to false, 1 to false, 2 to false, 3 to true, 40 to true, 41 to true)) {
        if (!breaker.allow(now)) {
            println("t=$now: breaker ${breaker.state(now)}, call refused without reaching the agent")
            continue
        }
        reached++
        breaker.record(healthy, now)
        println("t=$now: call ${if (healthy) "succeeded" else "failed"}, breaker ${breaker.state(now)}")
    }
    println("the agent was reached $reached times in 6 attempts")
}
```
```text
retry without a key: 2 refunds paid for one order
retry with a key: 1 refund paid for one order, 2 calls, receipt refund-1
t=0: call failed, breaker closed
t=1: call failed, breaker closed
t=2: call failed, breaker open
t=3: breaker open, call refused without reaching the agent
t=40: call succeeded, breaker closed
t=41: call succeeded, breaker closed
the agent was reached 5 times in 6 attempts
```
<!-- /example -->

The first two lines are the point. Without a key, a lost response and a retry pay twice. With the key recorded with the refund, the retry returns the first receipt, two calls were made, and one refund was paid. The breaker lines show three failures in a row opening it at `t=2`, a call at `t=3` refused without reaching the agent, and a probe at `t=40` after the cooldown that closes it. The agent was reached five times in six attempts: the breaker saved one call here, and in a run with a long outage it saves many more.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Generate a fresh key for each attempt so the tool cannot confuse them."** It is tempting because a fresh value looks unique and safe. The exam rejects it: a new key per attempt means the tool cannot recognise the repeat, so the retry pays again. The key names the intention and stays the same across attempts.
2. **"Tell the agent in its prompt never to refund the same order twice."** It is tempting because the instruction is easy to write. The exam rejects it: the agent cannot see an attempt whose response was lost, and a control that must hold is enforced in code outside the model.
3. **"Retry indefinitely until the call succeeds, because every failure is temporary."** It is tempting because transient failures are common. The exam rejects it: fatal failures never clear, an outage can last longer than any budget, and a breaker with a retry limit is what stops the spending.

## Quiz

1. Scenario: Tern Payments' transfer tool writes its idempotency key to a keys table and then makes the transfer as a second, separate step. A crash falls between the two steps, and after the restart a retry returns the stored receipt although no money moved. What design prevents this?
   - **a**: Delete stored keys that are older than a minute, so that a retry can proceed afresh
   - **b**: Make the agent confirm the account balance in its prompt before every retry
   - **c**: Write the transfer first and the key second, as two steps in that order
   - **d**: Persist both together as one indivisible operation

2. Scenario: Quillon Labs runs fifty tasks that depend on one search agent whose backend is down. Each task retries three times before failing. What limits the waste best?
   - **a**: A breaker that trips after repeated errors in a row and refuses calls for a pause
   - **b**: A larger retry count for each task, so the backend has more chances to come back
   - **c**: A faster model for the search agent, so that every failed call ends sooner
   - **d**: A rule that restarts the whole run from the first task whenever any task has failed

<details>
<summary>Answer key</summary>

1. **d**. The key and the effect are recorded in one step, so neither can exist without the other. *a* is ruled out because the page says to "Record the key together with the effect", and deleting keys breaks the recognition of repeats. *b* is ruled out because "a control that must hold is enforced in code outside the model". *c* is ruled out because it only reverses the gap: "a crash between the two leaves an effect with no key, or a key with no effect".
2. **a**. A breaker refuses calls at once while it is open and probes once after the cooldown. *b* is ruled out because "an unlimited retry is a way to spend money while nothing changes" and a larger count is the same mistake. *c* is ruled out because the waste is the count of calls, "a hundred and fifty wasted calls", which speed leaves in place. *d* is ruled out because a restart makes the same calls again, where the breaker means "the task fails fast with the reason `circuit open`".

</details>
