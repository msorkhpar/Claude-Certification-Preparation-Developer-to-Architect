/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
/** name, sentiment, asked for a person, policy silent, what a careful person would do */
data class Case(val name: String, val sentiment: String, val asked: Boolean, val policySilent: Boolean, val truth: String)

data class Account(val id: String, val lastOrder: Int)

data class Example(val text: String, val decision: String, val why: String)

val CASES = listOf(
    Case("price match with another shop", "calm", false, true, "escalate"),
    Case("wrong colour, standard exchange", "angry", false, false, "resolve"),
    Case("calm request to speak to a person", "calm", true, false, "escalate"),
    Case("password reset", "frustrated", false, false, "resolve"),
    Case("refund for an item bought elsewhere", "calm", false, true, "escalate"),
    Case("angry, wants a person now", "angry", true, false, "escalate"),
)

fun bySentiment(c: Case): String = if (c.sentiment == "calm") "resolve" else "escalate"

fun byCriteria(c: Case): String = if (c.asked || c.policySilent) "escalate" else "resolve"

fun errors(rule: (Case) -> String): List<Int> = CASES.withIndex().filter { rule(it.value) != it.value.truth }.map { it.index + 1 }

/** The heuristic the guide rejects: choose the account with the latest order. */
fun pickMostRecent(matches: List<Account>): String = matches.maxByOrNull { it.lastOrder }!!.id

/** What the guide asks for: no choice, a request for something that tells the matches apart. */
fun askForIdentifier(matches: Int, fields: List<String>): String = "I found $matches accounts for that name. Please give me one of: " + fields.joinToString(", ") + "."

/** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
fun escalationSection(criteria: List<String>, examples: List<Example>): String {
    val lines = mutableListOf("Escalate to a person when:") + criteria.map { "- $it" } + listOf("", "Examples:") + examples.map { "Customer: \"${it.text}\" -> ${it.decision} (${it.why})" }
    return lines.joinToString("\n")
}

fun main() {
    CASES.forEachIndexed { i, c -> println("case ${i + 1} (${c.name}): sentiment rule ${bySentiment(c)}, criteria ${byCriteria(c)}, careful person ${c.truth}") }
    println("sentiment rule routed ${errors(::bySentiment).size} of ${CASES.size} wrongly: cases " + errors(::bySentiment).joinToString(", "))
    println("criteria routed ${errors(::byCriteria).size} of ${CASES.size} wrongly")
    val matches = listOf(Account("c1", 20260901), Account("c2", 20260915))
    println("heuristic: the agent acts on ${pickMostRecent(matches)} although the customer may be c1")
    println(askForIdentifier(matches.size, listOf("the email on the account", "the postcode")))
    println(escalationSection(listOf("the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"),
        listOf(Example("Can you match the price on another site?", "escalate", "the policy only covers our own prices"), Example("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"))))
}
