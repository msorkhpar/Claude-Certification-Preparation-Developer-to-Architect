import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("review_gate")

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
object ReviewGate {
    val SEVERITIES = listOf("low", "medium", "high")
    private val json = ObjectMapper()

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    fun reviewPrompt(diff: String, prior: List<Map<String, Any?>> = emptyList(), existingTests: List<String> = emptyList()): String {
        // TODO 1 of 9 (finish this to pass e4): the first instructions of the prompt. Add the sentence "Report only
        //   findings that are new or still unaddressed." as the third line of the instructions, after the line that points
        //   to the project criteria. Example: the prompt starts with <instructions>, the review line, then this sentence.
        val lines = mutableListOf("<instructions>", "Review the change in <diff> against the criteria in the project instructions.")
        if (prior.isNotEmpty()) lines += "Do not repeat a finding listed in <already_reported>."
        if (existingTests.isNotEmpty()) lines += "Do not suggest a test for a behaviour that an existing test in <existing_tests> already covers."
        lines += "</instructions>"
        if (prior.isNotEmpty()) lines += listOf("<already_reported>") + prior.map { "- ${it["file"]}:${it["line"]} [${it["category"]}] ${it["issue"]}" } + "</already_reported>"
        if (existingTests.isNotEmpty()) lines += listOf("<existing_tests>") + existingTests.map { "- $it" } + "</existing_tests>"
        lines += listOf("<diff>", diff, "</diff>")
        return lines.joinToString("\n")
    }

    private fun decision(exit: Int, comments: List<Map<String, Any?>>, problems: List<String>): Map<String, Any?> = linkedMapOf("exit" to exit, "comments" to comments, "problems" to problems)

    /** policy: min_severity, disabled_categories and fail_on. Returns exit, comments and problems. */
    fun gate(stdout: String, exitCode: Int, schema: JsonNode, policy: Map<String, Any?>): Map<String, Any?> {
        log.log(System.Logger.Level.DEBUG, "gate input: {0}", stdout)
        val problems = mutableListOf<String>()
        // TODO 2 of 9 (finish this to pass e1): the exit status. When claude exited with a status other than 0, add the
        //   problem "claude exited with status N" (the job fails whatever the output looks like). Example: exit 2 and a
        //   perfect answer -> exit 1 and that problem.
        val envelope = try { json.readTree(stdout) } catch (e: Exception) { null }
        if (envelope == null || !envelope.isObject) return decision(1, emptyList(), problems + "the output is not a JSON object")
        val subtype = envelope.get("subtype")
        if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual || subtype.asText() != "success") {
            problems += "the run ended with ${if (subtype == null || subtype.isNull) "None" else subtype.asText()}"
        }
        val output = envelope.get("structured_output")
        // TODO 3 of 9 (finish this to pass e2): the schema check. Check the structured output against the schema with
        //   the provided helper and add one problem per error, as "schema " followed by the error (which names the path).
        //   Example: severity "critical" -> "schema $.findings[0].severity: ...".
        if (output == null || output.isNull) problems += "the result has no structured_output"
        if (problems.isNotEmpty()) return decision(1, emptyList(), problems)
        val floor = SEVERITIES.indexOf(policy["min_severity"])
        val disabled = policy["disabled_categories"] as List<*>
        val failOn = policy["fail_on"] as List<*>
        // TODO 4 of 9 (finish this to pass m1): the comments of a valid answer. Keep each finding whose severity is at
        //   or above policy min_severity (the order is low, medium, high) and whose category is not in
        //   disabled_categories. Return one comment {file, line, severity, body} per kept finding, in order, with the body
        //   "ISSUE Suggested fix: FIX". Example: a medium bug at api.py:12 with floor medium -> one comment.
        val comments = emptyList<Map<String, Any?>>()
        // TODO 5 of 9 (finish this to pass e3): the decision. The job fails (exit 1) when at least one posted comment
        //   has a severity listed in policy fail_on; otherwise it only comments (exit 0). Example: fail_on [high], one
        //   posted medium comment -> exit 0.
        return decision(0, comments, emptyList())
    }
}
