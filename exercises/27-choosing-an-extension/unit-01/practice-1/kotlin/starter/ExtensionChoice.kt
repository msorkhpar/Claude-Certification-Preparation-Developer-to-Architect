/** Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md. */

data class Choice(val mechanism: String, val reason: String)

fun choose(situation: Map<String, Any>): Choice? {
    // TODO: the mechanism and the reason code for a situation (a map whose missing keys take the defaults in the statement).
    return null
}
