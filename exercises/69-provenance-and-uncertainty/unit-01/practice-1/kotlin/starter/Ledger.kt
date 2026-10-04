/** Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md. */

val REQUIRED = listOf("claim", "value", "source", "date")
val KINDS = listOf("financial", "news", "technical")

data class Finding(val claim: String?, val value: String?, val source: String?, val date: String?)

data class Src(val source: String, val date: String)

data class Val(val value: String, val sources: List<Src>)

data class Entry(val claim: String, val status: String, val values: List<Val>)

data class Gap(val claim: String, val reason: String)

data class Coverage(val wellSupported: List<String>, val singleSource: List<String>, val changed: List<String>, val contested: List<String>, val gaps: List<Gap>)

fun checkFinding(finding: Finding): List<String>? {
    // TODO: the names of the required fields that are missing or blank, in the order of REQUIRED.
    return null
}

fun merge(findings: List<Finding>): List<Entry>? {
    // TODO: one entry per claim, with its values, their sources and a status; refuse an incomplete finding with an IllegalArgumentException.
    return null
}

fun coverageNote(planned: List<String>, merged: List<Entry>, unavailable: Map<String, String>): Coverage? {
    // TODO: which claims are well supported, single-source, changed, contested, and which planned claims are gaps.
    return null
}

fun render(entry: Entry, kind: String): String? {
    // TODO: a table for financial data, prose for news, a bulleted list for technical findings.
    return null
}
