/** Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md. */

private val log = System.getLogger("launch_review")

private val SEVERITY = mapOf("high" to 0, "medium" to 1, "low" to 2)
private val DOMAINS = listOf("P1", "P2", "P3", "P4", "P5", "P6", "P7")

private typealias Add = (Boolean, String, String, String) -> Unit

private fun p1Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("feedback_loop" !in f, "high", "P1", "missing-feedback")
    add(("agent" in f || "team" in f) && "path_known" in f, "medium", "P1", "autonomy-without-need")
    add("team" in f && (n["team_value_chats"] ?: 0) < 15, "medium", "P1", "team-below-price")
}

private fun p3Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("filter_after_ranking" in f, "high", "P3", "filter-after-ranking")
    add("replace_on_change" !in f, "high", "P3", "stale-index")
    add((n["tool_tokens"] ?: 0) > 10000 && "deferral" !in f, "medium", "P3", "tool-bloat")
    add("agent_rights_only" in f, "high", "P3", "agent-rights-only")
}

private fun p4Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("protected_segment" !in f, "medium", "P4", "no-protected-segment")
    add((n["eval_cases"] ?: 0) < 20, "low", "P4", "small-eval-set")
    add("rollback" !in f, "high", "P4", "no-way-back")
    add((n["rollout_stages"] ?: 0) < 3, "medium", "P4", "big-bang-rollout")
}

private fun p5Rules(f: Set<String>, n: Map<String, Int>, add: Add) {
    add("pii_reaches_model" in f, "high", "P5", "identifiers-reach-model")
    add("residency_unmet" in f, "high", "P5", "residency-unmet")
    add("audit_keeps_content" in f, "medium", "P5", "audit-keeps-content")
    add("irreversible_action" in f && "human_step" !in f, "high", "P5", "irreversible-without-person")
    add((n["retain_days"] ?: 0) < (n["floor_days"] ?: 0) || (n["retain_days"] ?: 0) > (n["ceiling_days"] ?: 0), "medium", "P5", "retention-outside-window")
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

private fun order(found: List<String>): List<String> =
    found.sortedWith(compareBy({ SEVERITY.getValue(it.split(" ")[0]) }, { it.split(" ")[1] }, { it.split(" ")[2] }))

/** The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule. */
fun launchReview(f: Set<String>, n: Map<String, Int>): List<String> {
    log.log(System.Logger.Level.DEBUG, "launchReview input: {0}", f)
    val found = mutableListOf<String>()
    val add: Add = { condition, severity, domain, rule -> if (condition) found.add("$severity $domain $rule") }
    for (rules in listOf(::p1Rules, ::p2Rules, ::p3Rules, ::p4Rules, ::p5Rules, ::p6Rules, ::p7Rules)) rules(f, n, add)
    return order(found)
}

/** reject for any high finding, revise for any medium one, otherwise approve. */
fun verdict(findings: List<String>): String {
    if (findings.any { it.startsWith("high ") }) return "reject"
    if (findings.any { it.startsWith("medium ") }) return "revise"
    return "approve"
}

/** The number of findings in each domain, P1 to P7. */
fun scorecard(findings: List<String>): List<Int> = DOMAINS.map { d -> findings.count { it.split(" ")[1] == d } }

/** The break-even accuracy in whole percent, rounded up on the cost side; 0 when an error costs nothing or no more than a check. */
fun neededAccuracy(errorCost: Int, reviewCost: Int): Int {
    if (errorCost <= 0) return 0
    val needed = (100 * reviewCost + errorCost - 1) / errorCost
    return maxOf(0, 100 - needed)
}
