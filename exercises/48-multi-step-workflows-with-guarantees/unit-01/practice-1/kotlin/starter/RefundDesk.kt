/** A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md. Results are JSON-like maps. */

typealias Backend = Map<String, (Map<String, Any?>) -> Map<String, Any?>>

class RefundDesk(private val backend: Backend, private val limitCents: Int = 10000) {
    // TODO: keep what the desk has verified, looked up, refunded and blocked.

    fun state(): Map<String, Any?>? {
        // TODO: a snapshot of the desk's state, as described in the statement.
        return null
    }

    fun call(name: String, args: Map<String, Any?>): Map<String, Any?>? {
        // TODO: check the prerequisites in code, then call the backend, and return {content, is_error, blocked}.
        return null
    }

    fun handoff(reason: String): Map<String, Any?>? {
        // TODO: the structured hand-off a person needs to take the case over.
        return null
    }
}
