import com.fasterxml.jackson.databind.JsonNode

/** The decision of a review job and the prompt of a review run. See ../../statement.md. */
object ReviewGate {
    val SEVERITIES = listOf("low", "medium", "high")

    /** The prompt of one review run. prior: maps with file, line, category and issue; existingTests: test names. */
    fun reviewPrompt(diff: String, prior: List<Map<String, Any?>> = emptyList(), existingTests: List<String> = emptyList()): String? {
        // TODO: instructions, the findings already reported, the tests that exist, and the diff last.
        return null
    }

    /** policy: min_severity, disabled_categories and fail_on. Returns a map with exit, comments and problems. */
    fun gate(stdout: String, exitCode: Int, schema: JsonNode, policy: Map<String, Any?>): Map<String, Any?>? {
        // TODO: decide the job from the exit status and the JSON the run printed: {"exit", "comments", "problems"}. SchemaCheck.schemaCheck is provided.
        return null
    }
}
