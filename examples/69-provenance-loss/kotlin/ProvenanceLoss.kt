/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
data class Row(val claim: String, val value: String, val source: String, val date: String)

val FINDINGS = listOf(
    Row("market growth 2024", "12%", "Firm A report", "2024-05-01"),
    Row("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
    Row("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
    Row("growth forecast", "9%", "Firm B survey", "2024-05-01"),
    Row("inflation 2023", "4%", "Firm A report", "2024-05-01"),
    Row("inflation 2023", "4%", "Trade paper", "2024-06-10"),
    Row("headcount", "910", "Press release", "2024-03-01"),
)

/** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
fun status(rows: List<Row>): String = when {
    rows.map { it.value }.distinct().size == 1 -> "agreed"
    rows.any { a -> rows.any { b -> a.value != b.value && a.date == b.date } } -> "conflict"
    else -> "changed"
}

/** One line per claim with the first value seen: short, and the sources are gone. */
fun plainSummary(findings: List<Row>): String = findings.map { it.claim }.distinct().joinToString("\n") { claim -> "$claim: ${findings.first { it.claim == claim }.value}" }

/** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
fun ledgerLines(findings: List<Row>): String = findings.map { it.claim }.distinct().joinToString("\n") { claim ->
    val group = findings.filter { it.claim == claim }
    val rows = if (status(group) == "changed") group.sortedBy { it.date } else group
    "$claim [${status(rows)}]: " + rows.joinToString("; ") { "${it.value} (${it.source}, ${it.date})" }
}

fun sourcesNamed(text: String, findings: List<Row>): Int = findings.filter { text.contains(it.source) }.map { it.source }.distinct().size

fun main() {
    val total = FINDINGS.map { it.source }.distinct().size
    println("findings: ${FINDINGS.size} from $total sources")
    val summary = plainSummary(FINDINGS)
    println("plain summary:")
    println(summary)
    println("sources named by the plain summary: ${sourcesNamed(summary, FINDINGS)} of $total")
    val ledger = ledgerLines(FINDINGS)
    println("ledger:")
    println(ledger)
    println("sources named by the ledger: ${sourcesNamed(ledger, FINDINGS)} of $total")
}
