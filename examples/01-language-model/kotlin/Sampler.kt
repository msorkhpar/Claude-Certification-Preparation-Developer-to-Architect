import kotlin.math.exp

/** A toy next-token sampler. It is not Claude: it only shows what temperature does. */
val TOKENS = listOf("blue", " clear", " falling", "green")
val LOGITS = listOf(4.0, 2.5, 1.0, -1.0)

/** Turn scores into probabilities. Lower temperature sharpens, higher flattens. */
fun softmax(logits: List<Double>, temperature: Double): List<Double> {
    val scaled = logits.map { it / temperature }
    val top = scaled.max()
    val exps = scaled.map { exp(it - top) }
    val total = exps.sum()
    return exps.map { it / total }
}

/** A tiny seeded random generator, the same in every language of this course. */
class Lcg(seed: Long) {
    private var state = seed.mod(1L shl 32)

    fun next(): Double {
        state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
        return state / 4294967296.0
    }
}

fun sample(probs: List<Double>, rng: Lcg): Int {
    val u = rng.next()
    var acc = 0.0
    for ((i, p) in probs.withIndex()) {
        acc += p
        if (u < acc) return i
    }
    return probs.lastIndex
}

fun greedy(probs: List<Double>): Int = probs.indices.maxBy { probs[it] }

fun main() {
    for (t in listOf(0.5, 1.0, 2.0)) {
        val probs = softmax(LOGITS, t)
        println("T=$t: " + TOKENS.indices.joinToString("  ") { "%s=%.3f".format(TOKENS[it].trim(), probs[it]) })
    }
    println("greedy: " + TOKENS[greedy(softmax(LOGITS, 1.0))])
    for (t in listOf(0.2, 1.0, 2.0)) {
        val probs = softmax(LOGITS, t)
        val rng = Lcg(7)
        println("T=$t ten draws: " + List(10) { TOKENS[sample(probs, rng)].trim() }.joinToString(" "))
    }
}
