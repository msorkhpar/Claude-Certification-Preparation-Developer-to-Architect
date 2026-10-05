private val log = System.getLogger("deidentify")

/**
 * Tokenise identifiers before a model call, restore them locally, and log an audit entry that holds no content.
 *
 * The Claude documentation on API and data retention (read on 2026-10-04) says that HIPAA readiness "applies a broader set of privacy and
 * security safeguards" and that its protection covers message content, so the safest design keeps identifiers out of the message at all.
 * This file is a teaching model of that design, not a compliance control: patterns replace e-mail addresses and member numbers with tokens
 * that mean nothing to the model, the vault that maps tokens back stays in the caller, and the audit entry records sizes and counts, never
 * the prompt. The patterns do not find names, and the output shows that gap on purpose. No model is called.
 */
val PATTERNS = listOf("EMAIL" to Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "MEMBER" to Regex("\\bM-\\d{6}\\b"))

/** What the log keeps: the request id, the size of the prompt, how many distinct identifiers were tokenised. Never the prompt. */
data class Audit(val requestId: String, val chars: Int, val tokensIssued: Int, val promptStored: Boolean)

/** Replaces every match by a token; the same value always gets the same token. `vault` maps value to token and stays local. */
fun tokenise(text: String, vault: MutableMap<String, String>): String {
    log.log(System.Logger.Level.DEBUG, "tokenise input: {0}", text)
    var out = text
    for ((label, pattern) in PATTERNS) {
        out = pattern.replace(out) { match ->
            val value = match.value
            if (value !in vault) vault[value] = "<${label}_${vault.values.count { it.startsWith("<${label}_") } + 1}>"
            vault.getValue(value)
        }
    }
    return out
}

fun restore(text: String, vault: Map<String, String>): String = vault.entries.fold(text) { acc, (value, token) -> acc.replace(token, value) }

fun auditEntry(requestId: String, original: String, vault: Map<String, String>) = Audit(requestId, original.length, vault.size, false)

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val original = "Jane Doe (jane.doe@example.com, member M-204518) asks about claim 7781; jane.doe@example.com wrote twice."
    val vault = linkedMapOf<String, String>()
    val sent = tokenise(original, vault)
    println("sent to the model: $sent")
    val reply = "Please confirm <EMAIL_1> and <MEMBER_1> before we reopen the claim."
    println("model reply (illustrative): $reply")
    println("restored for the user: ${restore(reply, vault)}")
    val entry = auditEntry("req-001", original, vault)
    println("audit entry: request_id=${entry.requestId} chars=${entry.chars} tokens_issued=${entry.tokensIssued} prompt_stored=${py(entry.promptStored)}")
    println("raw values in the entry: ${py(vault.keys.any { entry.toString().contains(it) })}")
    println("gap: the name survives tokenising: ${py("Jane Doe" in sent)}")
}
