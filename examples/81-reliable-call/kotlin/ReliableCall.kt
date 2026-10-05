private val log = System.getLogger("reliable_call")

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
    log.log(System.Logger.Level.DEBUG, "refund input: {0}", key)
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
