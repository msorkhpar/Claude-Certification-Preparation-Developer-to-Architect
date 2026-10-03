/** An injection-resistant tool gate. See ../../statement.md. */
class Gate(private val root: String, private val allowedHosts: List<String>, private val allowedEmailDomains: List<String>) {
    companion object {
        /** The names of the injection signals found in the text, in the fixed order override, role-tag, exfiltrate, reveal. */
        fun screen(text: String): List<String> = emptyList()

        /** A tool_result block that carries untrusted text as one JSON string, or an error result when the text is flagged. */
        fun wrapUntrusted(toolUseId: String, source: String, content: String): Map<String, Any?> =
            linkedMapOf("type" to "tool_result", "tool_use_id" to toolUseId, "content" to content)

        /** Secrets become [SECRET], addresses [EMAIL] and card numbers that pass the Luhn check [CARD]. */
        fun redact(text: String): String = text

        /** The PreToolUse hook answer for a decision: exit code, standard output, standard error. */
        fun hookResponse(decision: Map<String, Any?>): Map<String, Any?> = linkedMapOf("exit_code" to 0, "stdout" to "", "stderr" to "")
    }

    val isTainted: Boolean get() = false

    fun audit(): List<Map<String, Any?>> = emptyList()

    fun markUntrusted(source: String) {}

    fun decide(actor: String, tool: String, args: Map<String, Any?>): Map<String, Any?> = linkedMapOf("decision" to "allow", "reason" to "ok")

    fun alerts(): List<Map<String, Any?>> = emptyList()
}
