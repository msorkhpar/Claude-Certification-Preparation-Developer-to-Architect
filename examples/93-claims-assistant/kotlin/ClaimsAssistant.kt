/**
 * A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.
 *
 * The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
 */
data class Chunk(val id: String, val doc: String, val version: Int, val text: String)

data class Request(val id: String, val text: String, val allowed: Set<String>, val consequence: String, val quote: String, val confidence: Int)

data class Case(val id: String, val segment: String, val oldOk: Boolean, val newOk: Boolean)

data class Trace(val request: String, val chunk: String, val outcome: String, val chars: Int)

data class Handled(val sent: String, val trace: Trace)

data class Tokenised(val sent: String, val vault: Map<String, String>)

val CURRENT = mapOf("policy" to 3, "contracts" to 1)
val INDEX = listOf(
    Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
    Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
    Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums."),
)
val STALE_INDEX = listOf(INDEX[0], Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX[2])

/** Identifiers become tokens before the text leaves the caller; the map from token to value stays here. */
fun tokenise(text: String): Tokenised {
    val vault = linkedMapOf<String, String>()
    val sent = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+").replace(text) { m ->
        vault.entries.firstOrNull { it.value == m.value }?.key ?: "<EMAIL_${vault.size + 1}>".also { vault[it] = m.value }
    }
    return Tokenised(sent, vault)
}

fun words(text: String): Set<String> = Regex("[a-z]+").findAll(text.lowercase()).map { it.value }.filter { it.length > 3 }.toSet()

/** The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence. */
fun retrieve(question: String, allowed: Set<String>, index: List<Chunk>): Chunk? {
    val mine = words(question)
    val scored = index.filter { it.doc in allowed }.map { c -> words(c.text).count { it in mine } to c }.sortedWith(compareBy({ -it.first }, { it.second.id }))
    return if (scored.isNotEmpty() && scored[0].first > 0) scored[0].second else null
}

/** One request through the chain; the outcome says why a request was held. */
fun handle(request: Request, index: List<Chunk>): Handled {
    val sent = tokenise(request.text).sent
    val chunk = retrieve(sent, request.allowed, index)
    val outcome = when {
        chunk == null -> "hold: no evidence"
        chunk.version != CURRENT.getValue(chunk.doc) -> "hold: stale evidence (${chunk.id} v${chunk.version}, current v${CURRENT.getValue(chunk.doc)})"
        request.quote !in chunk.text -> "hold: unsupported"
        request.consequence == "high" -> "human"
        else -> if (request.confidence >= 95) "auto" else "review"
    }
    return Handled(sent, Trace(request.id, if (chunk == null) "none" else "${chunk.id}@v${chunk.version}", outcome, sent.length))
}

/** Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere. */
fun keep(trace: Trace): Boolean = trace.outcome.startsWith("hold") || trace.outcome == "human"

/** A change ships only when no protected segment loses an answer and the losses do not outnumber the gains. */
fun release(cases: List<Case>, protectedSegments: Set<String>): String {
    val lost = cases.filter { it.oldOk && !it.newOk }
    val gained = cases.filter { it.newOk && !it.oldOk }
    val hit = lost.filter { it.segment in protectedSegments }.map { it.segment }.toSortedSet().toList()
    if (hit.isNotEmpty()) return "no-go: protected segment lost answers: " + hit.joinToString(", ")
    if (lost.size > gained.size) return "no-go: net loss: lost ${lost.size}, gained ${gained.size}"
    return "go: lost ${lost.size}, gained ${gained.size}"
}

fun main() {
    val water = "Water damage is covered up to 5,000 per claim."
    val late = "Claims must be reported within 30 days of the loss."
    val policy = setOf("policy")
    val requests = listOf(
        Request("r1", "How much does the policy cover for water damage?", policy, "low", water, 97) to INDEX,
        Request("r2", "How much does the policy cover for water damage?", policy, "low", water, 97) to STALE_INDEX,
        Request("r3", "Can I get a refund of 400 for water damage?", policy, "high", water, 99) to INDEX,
        Request("r4", "What is the partner commission?", policy, "low", water, 99) to INDEX,
        Request("r5", "I reported my claim from jo@example.com, how many days do I have?", policy, "low", late, 96) to INDEX,
    )
    val traces = mutableListOf<Trace>()
    for ((request, index) in requests) {
        val h = handle(request, index)
        traces.add(h.trace)
        println("${request.id}: sent='${h.sent}'; evidence=${h.trace.chunk}; outcome=${h.trace.outcome}")
    }
    println("traces kept: " + traces.filter { keep(it) }.joinToString(", ") { it.request })
    val cases = (1..6).map { Case("s$it", "status", it > 2, true) } + (1..4).map { Case("f$it", "refund", true, it != 4) } + listOf(Case("c1", "complaint", false, true), Case("c2", "complaint", true, true))
    println("release with refunds protected: " + release(cases, setOf("refund")))
    val fixed = cases.map { if (it.id == "f4") it.copy(newOk = true) else it }
    println("release after the refund fix: " + release(fixed, setOf("refund")))
}
