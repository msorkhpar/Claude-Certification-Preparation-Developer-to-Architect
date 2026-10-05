/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */

private val log = System.getLogger("ledger")

val REQUIRED = listOf("claim", "value", "source", "date")
val KINDS = listOf("financial", "news", "technical")

data class Finding(val claim: String?, val value: String?, val source: String?, val date: String?)

data class Src(val source: String, val date: String)

data class Val(val value: String, val sources: List<Src>)

data class Entry(val claim: String, val status: String, val values: List<Val>)

data class Gap(val claim: String, val reason: String)

data class Coverage(val wellSupported: List<String>, val singleSource: List<String>, val changed: List<String>, val contested: List<String>, val gaps: List<Gap>)

private fun field(f: Finding, name: String): String = when (name) {
    "claim" -> f.claim
    "value" -> f.value
    "source" -> f.source
    else -> f.date
} ?: ""

fun checkFinding(finding: Finding): List<String> = REQUIRED.filter { field(finding, it).isBlank() }

fun merge(findings: List<Finding>): List<Entry> {
    log.log(System.Logger.Level.DEBUG, "merge input: {0}", findings)
    findings.forEachIndexed { i, f ->
        val missing = checkFinding(f)
        require(missing.isEmpty()) { "finding $i is missing ${missing.joinToString(", ")}" }
    }
    return findings.map { it.claim!! }.distinct().map { claim ->
        val group = findings.filter { it.claim == claim }
        val values = group.map { it.value!! }.distinct().map { value ->
            Val(value, group.filter { it.value == value }.map { Src(it.source!!, it.date!!) }.distinct())
        }
        when {
            values.size == 1 -> Entry(claim, "agreed", values)
            group.any { a -> group.any { b -> a.value != b.value && a.date == b.date } } -> Entry(claim, "conflict", values)
            else -> Entry(claim, "changed", values.sortedBy { v -> v.sources.minOf { it.date } })
        }
    }
}

fun coverageNote(planned: List<String>, merged: List<Entry>, unavailable: Map<String, String>): Coverage {
    val have = merged.map { it.claim }.toSet()
    return Coverage(
        merged.filter { it.status == "agreed" && it.values[0].sources.map { s -> s.source }.distinct().size >= 2 }.map { it.claim },
        merged.filter { it.status == "agreed" && it.values[0].sources.map { s -> s.source }.distinct().size < 2 }.map { it.claim },
        merged.filter { it.status == "changed" }.map { it.claim },
        merged.filter { it.status == "conflict" }.map { it.claim },
        planned.filter { it !in have }.map { Gap(it, unavailable[it] ?: "no source found") },
    )
}

fun render(entry: Entry, kind: String): String {
    require(kind in KINDS) { "unknown content type $kind" }
    val rows = entry.values.flatMap { v -> v.sources.map { s -> Triple(v.value, s.source, s.date) } }
    return when (kind) {
        "financial" -> (listOf("| Source | Date | Value |", "|---|---|---|") + rows.map { (v, s, d) -> "| $s | $d | $v |" }).joinToString("\n")
        "technical" -> (listOf("${entry.claim}:") + rows.map { (v, s, d) -> "- $v ($s, $d)" }).joinToString("\n")
        else -> "${entry.claim}: " + rows.joinToString("; ") { (v, s, d) -> "$v ($s, $d)" } + "." +
            when (entry.status) {
                "conflict" -> " The sources disagree."
                "changed" -> " The figures are from different dates."
                else -> ""
            }
    }
}
