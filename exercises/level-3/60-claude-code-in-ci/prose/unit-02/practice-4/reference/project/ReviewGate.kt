import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("review_gate")

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
object ReviewGate {
    val SEVERITIES = listOf("low", "medium", "high")
    private val json = ObjectMapper()

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    fun reviewPrompt(diff: String, prior: List<Map<String, Any?>> = emptyList(), existingTests: List<String> = emptyList()): String {
        val lines = mutableListOf("<instructions>", "Review the change in <diff> against the criteria in the project instructions.", "Report only findings that are new or still unaddressed.")
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
        if (exitCode != 0) problems += "claude exited with status $exitCode"
        val envelope = try { json.readTree(stdout) } catch (e: Exception) { null }
        if (envelope == null || !envelope.isObject) return decision(1, emptyList(), problems + "the output is not a JSON object")
        val subtype = envelope.get("subtype")
        if (envelope.path("is_error").asBoolean(false) || subtype == null || !subtype.isTextual || subtype.asText() != "success") {
            problems += "the run ended with ${if (subtype == null || subtype.isNull) "None" else subtype.asText()}"
        }
        val output = envelope.get("structured_output")
        if (output == null || output.isNull) problems += "the result has no structured_output" else problems += SchemaCheck.schemaCheck(output, schema).map { "schema $it" }
        if (problems.isNotEmpty()) return decision(1, emptyList(), problems)
        val floor = SEVERITIES.indexOf(policy["min_severity"])
        val disabled = policy["disabled_categories"] as List<*>
        val failOn = policy["fail_on"] as List<*>
        val comments = output!!.get("findings")
            .filter { it.get("category").asText() !in disabled && SEVERITIES.indexOf(it.get("severity").asText()) >= floor }
            .map { linkedMapOf<String, Any?>("file" to it.get("file").asText(), "line" to it.get("line").asInt(), "severity" to it.get("severity").asText(), "body" to "${it.get("issue").asText()} Suggested fix: ${it.get("suggested_fix").asText()}") }
        return decision(if (comments.any { it["severity"] in failOn }) 1 else 0, comments, emptyList())
    }
}
