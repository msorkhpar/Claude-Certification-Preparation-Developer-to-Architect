import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = System.getLogger("assistant_turns")

/**
 * The turn logic of a conversational assistant in miniature: a message is routed in code before the model sees it, the window keeps the pinned facts and the newest turns
 * that fit, and a memory that crosses sessions is read per customer and marked when it is old.
 *
 * The words are made up and the routing is a plain list of phrases: this example is about where each decision lives, not about what a model would say. The shapes (a route,
 * a window, a recalled fact with a status) are this course's design, not an Anthropic interface.
 */
data class Saved(val key: String, val value: String, val saved: String)

data class Window(val kept: List<String>, val dropped: Int, val facts: List<String>)

data class Recalled(val key: String, val value: String, val status: String)

val RISK = listOf("hurt myself", "end my life", "emergency")
val ASKS_FOR_PERSON = listOf("human", "a person", "an agent")
val STORE = mapOf("ada" to listOf(Saved("address", "12 Elm Road", "2026-09-20"), Saved("plan", "Plus", "2025-12-01")))

/** Decided in code, in this order: a signal of risk, a request for a person, a stalled conversation, otherwise the model answers. */
fun route(message: String, misses: Int = 0): String {
    log.log(System.Logger.Level.DEBUG, "route input: {0}", message)
    val text = message.lowercase()
    return when {
        RISK.any { it in text } -> "handoff:safety"
        ASKS_FOR_PERSON.any { it in text } -> "handoff:requested"
        misses >= 2 -> "handoff:stalled"
        else -> "answer"
    }
}

/** The pinned facts always stay; of the turns, the newest ones that fit the budget (counted in words) stay, as one block at the end. */
fun window(turns: List<String>, budget: Int, facts: List<String>): Window {
    val kept = mutableListOf<String>()
    var used = 0
    for (turn in turns.reversed()) {
        val words = turn.split(Regex("\\s+")).size
        if (used + words > budget) break
        kept.add(0, turn)
        used += words
    }
    return Window(kept, turns.size - kept.size, facts)
}

/** The facts saved for this customer only, each marked current or to be verified when it is older than the limit. */
fun recall(user: String, today: String, maxAgeDays: Int = 30): List<Recalled> =
    (STORE[user] ?: listOf()).map {
        val age = ChronoUnit.DAYS.between(LocalDate.parse(it.saved), LocalDate.parse(today))
        Recalled(it.key, it.value, if (age <= maxAgeDays) "current" else "verify")
    }

fun main() {
    for ((message, misses) in listOf("Where is my parcel?" to 0, "I want to talk to a human" to 0, "I feel like I might hurt myself" to 0, "what?" to 2)) {
        println("route '$message'" + (if (misses > 0) " after $misses misses" else "") + ": ${route(message, misses)}")
    }
    val turns = listOf("Hello", "My parcel has not arrived", "It was due on Monday", "Can you check the order", "Order 1234 please")
    val facts = listOf("order 1234: parcel due 2026-09-28", "address: 12 Elm Road")
    val w = window(turns, 12, facts)
    println("window: ${turns.size} turns, budget 12 words -> kept ${w.kept.size}, dropped ${w.dropped}, facts kept ${w.facts.size}")
    for (user in listOf("ada", "bob")) {
        val found = recall(user, "2026-10-04").joinToString(", ") { "${it.key}=${it.value} (${it.status})" }
        println("recall $user on 2026-10-04: " + found.ifEmpty { "nothing stored" })
    }
}
