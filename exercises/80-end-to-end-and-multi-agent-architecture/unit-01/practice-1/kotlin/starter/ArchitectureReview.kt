/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */

val SEVERITY_ORDER = mapOf("high" to 0, "medium" to 1) // high findings come first
val AUTONOMOUS = listOf("agent", "multi-agent")

fun review(design: Map<String, Any?>): List<Map<String, Any?>>? {
    // TODO: the findings of the rubric, each {rule, severity}, ordered by severity and then by rule.
    return null
}

fun verdict(findings: List<Map<String, Any?>>): String? {
    // TODO: "reject", "revise" or "approve" from the worst severity among the findings.
    return null
}

fun cheapestAdequate(designs: List<Map<String, Any?>>): String? {
    // TODO: the name of the cheapest design that is not rejected, or null.
    return null
}
