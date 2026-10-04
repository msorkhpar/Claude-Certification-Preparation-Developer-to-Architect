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

/** The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note. */
fun synthesize(required: List<String>, results: List<Result>): Report {
    val okScopes = results.filter { it.status == "ok" && it.findings.isNotEmpty() }.map { it.scope }
    val covered = required.filter { it in okScopes }
    val gaps = required.filter { it !in okScopes }
    val seen = mutableListOf<Pair<Finding, Boolean>>()
    for (r in results) {
        if (r.status == "ok") r.findings.forEach { seen += it to false } else r.error!!.partial.forEach { seen += it to true }
    }
    val claims = mutableListOf<Claim>()
    val conflicts = mutableListOf<Conflict>()
    for ((claim, group) in seen.groupBy { it.first.claim }.toSortedMap()) {
        val values = mutableListOf<String>()
        group.forEach { values.addNew(it.first.value) }
        if (values.size > 1) {
            val observed = mutableListOf<Observed>()
            group.forEach { (f, _) -> observed.addNew(Observed(f.value, f.source, f.date)) }
            conflicts += Conflict(claim, observed)
        } else {
            val sources = mutableListOf<Source>()
            group.forEach { (f, _) -> sources.addNew(Source(f.source, f.date)) }
            claims += Claim(claim, values[0], sources, group.all { it.second })
        }
    }
    val errors = results.filter { it.status == "error" && it.scope !in covered }.map { Unresolved(it.scope, it.error!!.type, it.error.query, it.error.alternatives) }
    val partial = gaps.filter { s -> results.any { it.status == "error" && it.scope == s && it.error!!.partial.isNotEmpty() } }
    val parts = gaps.map { g ->
        val failed = errors.firstOrNull { it.scope == g }
        when {
            failed != null -> "$g (${failed.type} on '${failed.query}')"
            results.none { it.scope == g } -> "$g (not researched)"
            else -> "$g (no findings)"
        }
    }
    return Report(if (gaps.isEmpty()) "complete" else "partial", covered, gaps, partial, claims, conflicts, errors, if (parts.isEmpty()) "all scopes covered" else "not covered: " + parts.joinToString(", "))
}
