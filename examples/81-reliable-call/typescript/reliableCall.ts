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
