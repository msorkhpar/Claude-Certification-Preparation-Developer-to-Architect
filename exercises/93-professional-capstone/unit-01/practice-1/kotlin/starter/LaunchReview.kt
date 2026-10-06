/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */

private val log = System.getLogger("launch_review")

private val SEVERITY = mapOf("high" to 0, "medium" to 1, "low" to 2)
private val DOMAINS = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7")

private typealias Add = (Boolean, String, String, String) -> Unit

/**
 * TODO 1 of 8 (unlocks e1, e4 and e7): the rules of domain P1.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: missing-feedback, autonomy-without-need and team-below-price from the statement's table. Example: missing-feedback: the flag feedback_loop is absent -> one finding `high P1 missing-feedback`.
 */
private fun p1Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
}

/**
 * TODO 2 of 8 (unlocks e4 and e8): the rules of domain P3.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: filter-after-ranking, stale-index, tool-bloat and agent-rights-only. Example: stale-index: the flag replace_on_change is absent -> one finding `high P3 stale-index`.
 */
private fun p3Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
}

/**
 * TODO 3 of 8 (unlocks e1, e3 and e4): the rules of domain P4.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: no-protected-segment, small-eval-set, no-way-back and big-bang-rollout. Example: no-way-back: the flag rollback is absent -> one finding `high P4 no-way-back`.
 */
private fun p4Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
}

/**
 * TODO 4 of 8 (unlocks e4, e7 and e8): the rules of domain P5.
 * Receives the flags and the numbers and reports each rule it finds through `add`. The rules: identifiers-reach-model, residency-unmet, audit-keeps-content, irreversible-without-person and retention-outside-window. Example: irreversible-without-person: irreversible_action present and human_step absent -> `high P5 irreversible-without-person`.
 */
private fun p5Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
}

private fun p2Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("volatile_prefix" in f, "medium", "P2", "volatile-prefix")
    add("model_measured" !in f, "low", "P2", "model-not-measured")
}

private fun p6Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("owner" !in f, "medium", "P6", "no-accountable-owner")
    add((n["latency_ms"] ?: 0) <= 0 || (n["availability_tenths"] ?: 0) <= 0, "medium", "P6", "sla-without-numbers")
    add("accuracy_stated" !in f, "low", "P6", "accuracy-unstated")
}

private fun p7Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add((n["team_size"] ?: 0) > 10 && "managed_settings" !in f, "medium", "P7", "unmanaged-team-settings")
}

/**
 * TODO 5 of 8 (unlocks e3): order the findings.
 * Receives the findings. Returns them ordered by severity (high, medium, low), then by domain, then by rule id in alphabetical order. Example: low P2 a, high P6 z, high P1 y -> high P1 y, high P6 z, low P2 a
 */
private fun order(found: List<String>): List<String> = found

/** The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule. */
fun launchReview(f: Set<String>, n: Map<String, Int>): List<String> {
    log.log(System.Logger.Level.DEBUG, "launchReview input: {0}", f)
    val found = mutableListOf<String>()
    val add: Add = { condition, severity, domain, rule -> if (condition) found.add("$severity $domain $rule") }
    for (rules in listOf(::p1Rules, ::p2Rules, ::p3Rules, ::p4Rules, ::p5Rules, ::p6Rules, ::p7Rules)) rules(f, n, add)
    return order(found)
}

/**
 * TODO 6 of 8 (unlocks e1 and e2): the verdict.
 * Receives the findings. Returns `reject` when any is high, `revise` when none is high and any is medium, `approve` otherwise. Example: [low P2 a] -> approve
 */
fun verdict(findings: List<String>): String {
    return ""
}

/**
 * TODO 7 of 8 (unlocks e5): the scorecard.
 * Receives the findings. Returns the seven counts of the findings of P1 to P7 in that order. Example: [high P1 a, low P7 c] -> [1, 0, 0, 0, 0, 0, 1]
 */
fun scorecard(findings: List<String>): List<Int> {
    return emptyList()
}

/**
 * TODO 8 of 8 (unlocks m1 and e6): the accuracy a design needs.
 * Receives the cost of an error and the cost of a check. Returns 100 minus the check cost as a percent of the error cost, the percent rounded up, never below 0, and 0 when an error costs nothing or less. Example: neededAccuracy(250, 5) -> 98
 */
fun neededAccuracy(errorCost: Int, reviewCost: Int): Int {
    return -1
}
