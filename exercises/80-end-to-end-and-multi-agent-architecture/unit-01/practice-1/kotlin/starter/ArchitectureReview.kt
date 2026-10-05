/** An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md. Designs and findings are JSON-like maps. */
private val log = System.getLogger("architecture_review")

val SEVERITY_ORDER = mapOf("high" to 0, "medium" to 1)
val AUTONOMOUS = listOf("agent", "multi-agent")

private fun finding(rule: String, severity: String): Map<String, Any?> = linkedMapOf("rule" to rule, "severity" to severity)

@Suppress("UNCHECKED_CAST")
private fun asMap(value: Any?): Map<String, Any?> = (value as Map<String, Any?>?) ?: emptyMap()

private fun empty(value: Any?): Boolean = value == null || (value is Collection<*> && value.isEmpty())

private fun truthy(value: Any?): Boolean = value == true

/**
 * TODO 1 of 8 (unlocks e1): the findings about absent stages.
 * Receives the design's `stages` map. Returns a list of findings (use `finding(rule, severity)`): `missing-stage:input`, `missing-stage:processing` and
 * `missing-stage:output` for each of those stages that is absent or empty, then `no-feedback` when `feedback` is absent or empty; every one with severity `high`.
 * Example: {input: [parse], processing: [act], output: [send]} -> [{rule: no-feedback, severity: high}]
 */
fun stageFindings(stages: Map<String, Any?>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 2 of 8 (unlocks e3): the finding about a team.
 * Receives the design. Returns [`team-without-independence`, severity `high`] when there is more than one agent (`agents`, default 1) and either
 * `shared_context` is true or `parallel_independent` is not true; otherwise an empty list.
 * Example: {agents: 3, shared_context: true, parallel_independent: true} -> one finding; {agents: 1, shared_context: true} -> []
 */
fun teamFindings(design: Map<String, Any?>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 3 of 8 (unlocks e4): the finding about an unapproved write.
 * Receives the design. Returns [`unapproved-write`, severity `high`] when `writes_without_approval` and `needs_audit` are both true; otherwise an empty list.
 * Example: {writes_without_approval: true, needs_audit: false} -> []
 */
fun writeFindings(design: Map<String, Any?>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 4 of 8 (unlocks e2): the finding about autonomy.
 * Receives the design. Returns [`autonomy-without-need`, severity `medium`] when `pattern` is in `AUTONOMOUS` and `path_known` is true; otherwise an empty list.
 * Example: {pattern: agent, path_known: false} -> []
 */
fun autonomyFindings(design: Map<String, Any?>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 5 of 8 (unlocks e5): the finding about unvalidated output.
 * Receives the design's `stages` map. Returns [`unvalidated-output`, severity `medium`] when the `output` stage is present and does not contain `validate`;
 * otherwise an empty list (an absent output stage is already reported as a missing stage).
 * Example: {output: [send]} -> one finding; {output: [validate, send]} -> []
 */
fun outputFindings(stages: Map<String, Any?>): List<Map<String, Any?>> = emptyList()

/**
 * TODO 6 of 8 (unlocks e6): order the findings.
 * Receives a list of findings. Returns them ordered by severity (`high` before `medium`, see `SEVERITY_ORDER`) and then by rule name.
 * Example: [medium autonomy-without-need, high no-feedback] -> the high one first
 */
fun orderFindings(findings: List<Map<String, Any?>>): List<Map<String, Any?>> = findings

/**
 * TODO 7 of 8 (unlocks m1 and e6): the verdict.
 * Receives a list of findings. Returns `reject` when any is `high`, otherwise `revise` when any is `medium`, otherwise `approve`.
 * Example: verdict(emptyList()) -> "approve"
 */
fun verdict(findings: List<Map<String, Any?>>): String? = ""

fun review(design: Map<String, Any?>): List<Map<String, Any?>>? {
    log.log(System.Logger.Level.DEBUG, "review input: {0}", design)
    val stages = asMap(design["stages"])
    return orderFindings(stageFindings(stages) + teamFindings(design) + writeFindings(design) + autonomyFindings(design) + outputFindings(stages))
}

/**
 * TODO 8 of 8 (unlocks e7): the cheapest design that is not rejected.
 * Receives a list of designs, each with `name` and `cost`. Returns the name of the one with the lowest `cost` among those whose `verdict(review(d))` is not
 * `reject`; equal costs go to the lower name; `null` when none qualifies or the list is empty.
 * Example: costs 2 (name "zeta") and 2 (name "alpha"), both sound -> "alpha"
 */
fun cheapestAdequate(designs: List<Map<String, Any?>>): String? = null
