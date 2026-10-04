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

/**
 * The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
 * Read statement.md for the shapes of a result and of the report, then replace the body of synthesize().
 */
fun synthesize(required: List<String>, results: List<Result>): Report = Report("", listOf(), listOf(), listOf(), listOf(), listOf(), listOf(), "")
