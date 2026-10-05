/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */
private val log = System.getLogger("architecture_review")

val SEVERITY_ORDER = mapOf("high" to 0, "medium" to 1)
val AUTONOMOUS = listOf("agent", "multi-agent")

private fun finding(rule: String, severity: String): Map<String, Any?> = linkedMapOf("rule" to rule, "severity" to severity)

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

private fun empty(value: Any?): Boolean = value == null || (value is Collection<*> && value.isEmpty())

private fun truthy(value: Any?): Boolean = value == true

/** missing-stage:<stage> for each absent stage, then no-feedback when the feedback stage is absent; all high. */
fun stageFindings(stages: Map<String, Any?>): List<Map<String, Any?>> {
    val found = listOf("input", "processing", "output").filter { empty(stages[it]) }.map { finding("missing-stage:$it", "high") }.toMutableList()
    if (empty(stages["feedback"])) found += finding("no-feedback", "high")
    return found
}

/** team-without-independence (high) for more than one agent with a shared context or parts that are not independent. */
fun teamFindings(design: Map<String, Any?>): List<Map<String, Any?>> {
    val agents = (design["agents"] as Int?) ?: 1
    return if (agents > 1 && (truthy(design["shared_context"]) || !truthy(design["parallel_independent"]))) listOf(finding("team-without-independence", "high")) else emptyList()
}

/** unapproved-write (high) when the design writes without approval and an audit is needed. */
fun writeFindings(design: Map<String, Any?>): List<Map<String, Any?>> =
    if (truthy(design["writes_without_approval"]) && truthy(design["needs_audit"])) listOf(finding("unapproved-write", "high")) else emptyList()

/** autonomy-without-need (medium) when an agent or a team is used on a known path. */
fun autonomyFindings(design: Map<String, Any?>): List<Map<String, Any?>> =
    if (design["pattern"] in AUTONOMOUS && truthy(design["path_known"])) listOf(finding("autonomy-without-need", "medium")) else emptyList()

/** unvalidated-output (medium) when the output stage is present and has no validate step. */
fun outputFindings(stages: Map<String, Any?>): List<Map<String, Any?>> =
    if (!empty(stages["output"]) && !(stages["output"] as List<*>).contains("validate")) listOf(finding("unvalidated-output", "medium")) else emptyList()

/** High first, then by rule name. */
fun orderFindings(findings: List<Map<String, Any?>>): List<Map<String, Any?>> =
    findings.sortedWith(compareBy({ SEVERITY_ORDER.getValue(it["severity"] as String) }, { it["rule"] as String }))

fun verdict(findings: List<Map<String, Any?>>): String? {
    val severities = findings.map { it["severity"] }.toSet()
    return if ("high" in severities) "reject" else if ("medium" in severities) "revise" else "approve"
}

fun review(design: Map<String, Any?>): List<Map<String, Any?>>? {
    log.log(System.Logger.Level.DEBUG, "review input: {0}", design)
    val stages = asMap(design["stages"])
    return orderFindings(stageFindings(stages) + teamFindings(design) + writeFindings(design) + autonomyFindings(design) + outputFindings(stages))
}

fun cheapestAdequate(designs: List<Map<String, Any?>>): String? {
    val adequate = designs.filter { verdict(review(it)!!) != "reject" }
    if (adequate.isEmpty()) return null
    return adequate.sortedWith(compareBy({ it["cost"] as Int }, { it["name"] as String })).first()["name"] as String
}
