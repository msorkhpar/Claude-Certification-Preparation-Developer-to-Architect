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

// TODO 1 of 8 (finish this to pass e1): the provenance check. Receives one finding. Return the names, in REQUIRED order,
//   of the fields that are absent, empty or only white space. Example: source "" and date " " -> [source, date].
fun checkFinding(finding: Finding): List<String> = REQUIRED.filter { field(finding, it).isEmpty() }

fun merge(findings: List<Finding>): List<Entry> {
    log.log(System.Logger.Level.DEBUG, "merge input: {0}", findings)
    findings.forEachIndexed { i, f ->
        val missing = checkFinding(f)
        require(missing.isEmpty()) { "finding $i is missing ${missing.joinToString(", ")}" }
    }
    return findings.map { it.claim!! }.distinct().map { claim ->
        val group = findings.filter { it.claim == claim }
        val values = group.map { it.value!! }.distinct().map { value ->
            // TODO 2 of 8 (finish this to pass m1): the sources of a value. When a finding repeats a source and date
            //   pair that the value already holds, do not add it again; otherwise add the pair last. Example: Annual
            //   report 2024-02-01 reported twice -> kept once.
            Val(value, group.filter { it.value == value }.map { Src(it.source!!, it.date!!) })
        }
        // TODO 3 of 8 (finish this to pass e2, e3): the status of a claim. Receives the group of findings and its
        //   distinct values. One value -> agreed. Several values where two different values share a date -> conflict (keep
        //   all). Several values with no shared date -> changed, with the values ordered by their earliest date. Example:
        //   12% and 9% both on 2024-05-01 -> conflict.
        Entry(claim, "agreed", values)
    }
}

fun coverageNote(planned: List<String>, merged: List<Entry>, unavailable: Map<String, String>): Coverage {
    val have = merged.map { it.claim }.toSet()
    return Coverage(
        // TODO 4 of 8 (finish this to pass e4): the split of the agreed claims. For an agreed claim, add it to
        //   well_supported when its value comes from at least two different sources, otherwise to single_source. Example:
        //   two sources -> well_supported; one -> single_source.
        emptyList(),
        merged.filter { it.status == "agreed" }.map { it.claim },
        merged.filter { it.status == "changed" }.map { it.claim },
        merged.filter { it.status == "conflict" }.map { it.claim },
        // TODO 5 of 8 (finish this to pass e5): the gaps. Receives the planned claims, the merged entries and the
        //   reasons for unavailable claims. Return one {claim, reason} for each planned claim that has no merged entry,
        //   with the reason from `unavailable` or "no source found". Example: planned [a, b], merged a -> gap b.
        emptyList(),
    )
}

fun render(entry: Entry, kind: String): String {
    require(kind in KINDS) { "unknown content type $kind" }
    val rows = entry.values.flatMap { v -> v.sources.map { s -> Triple(v.value, s.source, s.date) } }
    return when (kind) {
        // TODO 6 of 8 (finish this to pass e6): the financial rendering. Return a Markdown table: the header "| Source |
        //   Date | Value |", the line "|---|---|---|" and one row "| source | date | value |" for each source of each
        //   value. Example: one source -> three lines.
        // TODO 7 of 8 (finish this to pass e8): the technical rendering. Return the claim followed by a colon, then one
        //   line "- value (source, date)" for each source of each value. Example: one source -> two lines.
        // TODO 8 of 8 (finish this to pass e7): the news ending. After the prose, add " The sources disagree." for a
        //   conflict and " The figures are from different dates." for a changed claim. Example: conflict -> the prose ends
        //   with that sentence.
        else -> "${entry.claim}: " + rows.joinToString("; ") { (v, s, d) -> "$v ($s, $d)" } + "."
    }
}
