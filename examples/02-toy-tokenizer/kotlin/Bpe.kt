/** A toy byte-pair tokenizer. It is not Claude's tokenizer: it shows why tokens are not words. */
data class Rule(val left: String, val right: String)

/** Learn merge rules: repeatedly join the most frequent adjacent pair (ties: first seen). */
fun train(corpus: String, merges: Int): List<Rule> {
    var words = corpus.trim().split(Regex("\\s+")).map { w -> w.map { it.toString() } }
    val rules = mutableListOf<Rule>()
    repeat(merges) {
        val pairs = LinkedHashMap<Rule, Int>()
        for (w in words) for (i in 0 until w.size - 1) pairs.merge(Rule(w[i], w[i + 1]), 1, Int::plus)
        if (pairs.isEmpty()) return rules
        val best = pairs.entries.fold(null as Map.Entry<Rule, Int>?) { top, e -> if (top == null || e.value > top.value) e else top }!!.key
        rules += best
        words = words.map { merge(it, best) }
    }
    return rules
}

fun merge(word: List<String>, pair: Rule): List<String> {
    val out = mutableListOf<String>()
    var i = 0
    while (i < word.size) {
        if (i + 1 < word.size && word[i] == pair.left && word[i + 1] == pair.right) {
            out += word[i] + word[i + 1]
            i += 2
        } else {
            out += word[i]
            i += 1
        }
    }
    return out
}

fun encode(word: String, rules: List<Rule>): List<String> = rules.fold(word.map { it.toString() }) { pieces, rule -> merge(pieces, rule) }

fun main() {
    val corpus = "low low low lower lower lowest newest newest widest widest"
    val rules = train(corpus, 6)
    println("merges: " + rules.joinToString(" ") { "${it.left}+${it.right}" })
    for (word in listOf("low", "lowest", "newer", "widest", "lowish")) {
        println("${word.padEnd(7)} -> ${encode(word, rules).joinToString(" | ")}")
    }
}
