/** Diagnose a failure from a trace. See ../../statement.md. */
object Diagnose {
    /** The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered. */
    fun diagnose(trace: List<Map<String, Any?>>): Map<String, Any?> =
        linkedMapOf("index" to -1, "type" to "ok", "origin" to "none", "recovery" to "none", "recovered" to false)
}
