/** Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md. */

data class Choice(val mechanism: String, val reason: String, val intervalMinutes: Int)

fun choose(job: Map<String, Any>): Choice {
    // TODO: the mechanism, the reason code and the interval in minutes for a job (a map whose missing keys take the defaults in the statement).
    return Choice("", "", 0)
}
