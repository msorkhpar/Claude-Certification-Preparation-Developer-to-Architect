/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */

val SEVERITY_ORDER = mapOf("high" to 0, "medium" to 1)
val AUTONOMOUS = listOf("agent", "multi-agent")

private fun finding(rule: String, severity: String): Map<String, Any?> = linkedMapOf("rule" to rule, "severity" to severity)

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

private fun empty(value: Any?): Boolean = value == null || (value is Collection<*> && value.isEmpty())

private fun truthy(value: Any?): Boolean = value == true

fun review(design: Map<String, Any?>): List<Map<String, Any?>>? {
    val stages = asMap(design["stages"])
    val findings = mutableListOf<Map<String, Any?>>()
    for (stage in listOf("input", "processing", "output")) {
        if (empty(stages[stage])) findings += finding("missing-stage:$stage", "high")
    }
    if (empty(stages["feedback"])) findings += finding("no-feedback", "high")
    val agents = (design["agents"] as Int?) ?: 1
    if (agents > 1 && (truthy(design["shared_context"]) || !truthy(design["parallel_independent"]))) findings += finding("team-without-independence", "high")
    if (truthy(design["writes_without_approval"]) && truthy(design["needs_audit"])) findings += finding("unapproved-write", "high")
    if (design["pattern"] in AUTONOMOUS && truthy(design["path_known"])) findings += finding("autonomy-without-need", "medium")
    if (!empty(stages["output"]) && !(stages["output"] as List<*>).contains("validate")) findings += finding("unvalidated-output", "medium")
    return findings.sortedWith(compareBy({ SEVERITY_ORDER.getValue(it["severity"] as String) }, { it["rule"] as String }))
}

fun verdict(findings: List<Map<String, Any?>>): String? {
    val severities = findings.map { it["severity"] }.toSet()
    return if ("high" in severities) "reject" else if ("medium" in severities) "revise" else "approve"
}

fun cheapestAdequate(designs: List<Map<String, Any?>>): String? {
    val adequate = designs.filter { verdict(review(it)!!) != "reject" }
    if (adequate.isEmpty()) return null
    return adequate.sortedWith(compareBy({ it["cost"] as Int }, { it["name"] as String })).first()["name"] as String
}
