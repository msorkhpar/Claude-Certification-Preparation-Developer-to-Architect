private val log = System.getLogger("synthesis")

data class Finding(val claim: String, val value: String, val source: String, val date: String)

/** What a failed subagent returns: its type, the query, the partial findings and what to try instead. */
data class Failure(val type: String, val query: String, val partial: List<Finding>, val alternatives: List<String>)

/** A subagent's result: status "ok" with findings, or "error" with a failure. */
data class Result(val scope: String, val status: String, val findings: List<Finding>, val error: Failure?)

data class Source(val source: String, val date: String)

data class Claim(val claim: String, val value: String, val sources: List<Source>, val partial: Boolean)

data class Observed(val value: String, val source: String, val date: String)

data class Conflict(val claim: String, val values: List<Observed>)

data class Unresolved(val scope: String, val type: String, val query: String, val alternatives: List<String>)

data class Report(
    val status: String, val covered: List<String>, val gaps: List<String>, val partial: List<String>, val claims: List<Claim>,
    val conflicts: List<Conflict>, val errors: List<Unresolved>, val note: String,
)

private fun <T> MutableList<T>.addNew(item: T) {
    if (item !in this) add(item)
}

/** Every finding with its partial flag, in arrival order. */
private fun findings(results: List<Result>): List<Pair<Finding, Boolean>> {
    val seen = mutableListOf<Pair<Finding, Boolean>>()
    for (r in results) {
        if (r.status == "ok") r.findings.forEach { seen += it to false } else r.error!!.partial.forEach { seen += it to true }
    }
    return seen
}

/**
 * TODO 1 of 7 (unlocks e1, e5 and the coverage of every other case): split the required scopes into covered and gaps.
 * Receives the required scopes and the results. Returns (covered, gaps), both in the order of `required`: a scope is covered
 * when an "ok" result for it has at least one finding.
 * Example: required ["a", "b"], one ok result for "a" with a finding -> (["a"], ["b"])
 */
private fun coveredScopes(required: List<String>, results: List<Result>): Pair<List<String>, List<String>> = listOf<String>() to listOf()

/**
 * TODO 2 of 7 (unlocks e6): the sources of one claim.
 * Receives the (finding, partial) pairs of one claim. Returns a list of Source in arrival order, no duplicates.
 * Example: findings from s9, s1, s9 -> [Source("s9", ...), Source("s1", ...)]
 */
private fun sourcesOf(group: List<Pair<Finding, Boolean>>): List<Source> = listOf()

/**
 * TODO 3 of 7 (unlocks e3): what the sources said about a conflicting claim.
 * Receives the (finding, partial) pairs of one claim. Returns a list of Observed (value, source, date) in arrival order, no duplicates.
 * Example: s1 says "12", s2 says "14" twice -> [Observed("12", "s1", ...), Observed("14", "s2", ...)]
 */
private fun observedValues(group: List<Pair<Finding, Boolean>>): List<Observed> = listOf()

/**
 * TODO 4 of 7 (unlocks e4): is every finding of the claim from a partial list?
 * Receives the (finding, partial) pairs of one claim. Returns true only when all of them are partial.
 * Example: [(f, true), (f, false)] -> false
 */
private fun allPartial(group: List<Pair<Finding, Boolean>>): Boolean = false

/**
 * TODO 5 of 7 (unlocks e2 and e4): the errors that nothing made up for.
 * Receives the results and the covered scopes. Returns an Unresolved (scope, type, query, alternatives) for each "error" result whose scope is not covered.
 * Example: an error for "b" while only "a" is covered -> [Unresolved("b", "timeout", "qb", [...])]
 */
private fun unresolvedErrors(results: List<Result>, covered: List<String>): List<Unresolved> = listOf()

/**
 * TODO 6 of 7 (unlocks e4): the gaps for which a failed search still returned findings.
 * Receives the results and the gaps. Returns the gaps that have an "error" result with a non-empty partial list.
 * Example: gap "b" with an error that carries one partial finding -> ["b"]
 */
private fun partialScopes(results: List<Result>, gaps: List<String>): List<String> = listOf()

/**
 * TODO 7 of 7 (unlocks e1, e2 and e5): say what the report cannot cover.
 * Receives the gaps, the unresolved errors and the results. Returns "all scopes covered" for no gaps; otherwise "not covered: " and
 * each gap as "<scope> (<type> on '<query>')" for an unresolved error, "<scope> (not researched)" when no result names the scope,
 * or "<scope> (no findings)", joined by ", ".
 * Example: gaps ["b", "c"], an error for "b" (timeout on "qb"), no result for "c" -> "not covered: b (timeout on 'qb'), c (not researched)"
 */
private fun coverageNote(gaps: List<String>, errors: List<Unresolved>, results: List<Result>): String = ""

/** The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note. */
fun synthesize(required: List<String>, results: List<Result>): Report {
    log.log(System.Logger.Level.DEBUG, "synthesize input: {0}", results)
    val (covered, gaps) = coveredScopes(required, results)
    val claims = mutableListOf<Claim>()
    val conflicts = mutableListOf<Conflict>()
    for ((claim, group) in findings(results).groupBy { it.first.claim }.toSortedMap()) {
        val values = mutableListOf<String>()
        group.forEach { values.addNew(it.first.value) }
        if (values.size > 1) conflicts += Conflict(claim, observedValues(group))
        else claims += Claim(claim, values[0], sourcesOf(group), allPartial(group))
    }
    val errors = unresolvedErrors(results, covered)
    return Report(if (gaps.isEmpty()) "complete" else "partial", covered, gaps, partialScopes(results, gaps), claims, conflicts, errors, coverageNote(gaps, errors, results))
}
