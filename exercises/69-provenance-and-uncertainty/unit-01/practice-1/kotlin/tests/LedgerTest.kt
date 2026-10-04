import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LedgerTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun f(claim: String?, value: String, source: String, date: String) = Finding(claim, value, source, date)

    private fun s(source: String, date: String) = Src(source, date)

    private fun v(value: String, vararg sources: Src) = Val(value, sources.toList())

    private fun entry(claim: String, status: String, vararg values: Val) = Entry(claim, status, values.toList())

    private fun merged(vararg findings: Finding) = got(merge(findings.toList()))

    @Test
    fun m1_findingsAreMergedPerClaimWithEverySourceKeptOnce() {
        val result = merged(f("revenue 2023", "4.1B", "Annual report", "2024-02-01"), f("headcount", "910", "Press release", "2024-03-01"),
            f("revenue 2023", "4.1B", "Press release", "2024-02-03"), f("revenue 2023", "4.1B", "Annual report", "2024-02-01"))
        assertEquals(listOf(entry("revenue 2023", "agreed", v("4.1B", s("Annual report", "2024-02-01"), s("Press release", "2024-02-03"))), entry("headcount", "agreed", v("910", s("Press release", "2024-03-01")))), result)
        assertEquals(emptyList<Entry>(), merged())
    }

    @Test
    fun e1_aFindingWithoutASourceOrADateIsRefused() {
        assertEquals(listOf("source", "date"), got(checkFinding(f("c", "v", "", " "))))
        assertEquals(listOf("claim"), got(checkFinding(f(null, "v", "s", "2024-01-01"))))
        assertEquals(emptyList<String>(), got(checkFinding(f("c", "v", "s", "2024-01-01"))))
        assertThrows(IllegalArgumentException::class.java) { merge(listOf(f("c", "v", "s", "2024-01-01"), f("c", "w", "s", ""))) }
        assertDoesNotThrow { merge(listOf(f("c", "v", "s", "2024-01-01"))) }
    }

    @Test
    fun e2_twoValuesFromTheSameDateAreAConflictThatKeepsBoth() {
        assertEquals(listOf(entry("market size", "conflict", v("12%", s("Firm A report", "2024-05-01")), v("9%", s("Firm B survey", "2024-05-01")))),
            merged(f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2024-05-01")))
    }

    @Test
    fun e3_twoValuesFromDifferentDatesAreAChangeNotAConflict() {
        assertEquals(listOf(entry("market size", "changed", v("9%", s("Firm B survey", "2022-05-01")), v("12%", s("Firm A report", "2024-05-01")))),
            merged(f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2022-05-01")))
    }

    @Test
    fun e4_theCoverageNoteSeparatesWhatIsWellSupportedFromWhatIsNot() {
        val result = merged(f("a", "1", "S1", "2024-01-01"), f("a", "1", "S2", "2024-01-02"), f("b", "2", "S1", "2024-01-01"), f("b", "2", "S1", "2024-02-01"),
            f("c", "3", "S1", "2024-01-01"), f("c", "4", "S2", "2022-01-01"), f("d", "5", "S1", "2024-01-01"), f("d", "6", "S2", "2024-01-01"))
        assertEquals(Coverage(listOf("a"), listOf("b"), listOf("c"), listOf("d"), emptyList()), got(coverageNote(listOf("a", "b", "c", "d"), result, emptyMap())))
    }

    @Test
    fun e5_aPlannedClaimWithNoFindingIsAGapWithItsReason() {
        val result = merged(f("a", "1", "S1", "2024-01-01"), f("z", "9", "S1", "2024-01-01"))
        val note = got(coverageNote(listOf("q", "a", "r"), result, mapOf("q" to "the registry timed out")))
        assertEquals(listOf(Gap("q", "the registry timed out"), Gap("r", "no source found")), note.gaps)
        assertEquals(listOf("a", "z"), note.singleSource)
    }

    @Test
    fun e6_financialDataIsRenderedAsATable() {
        val e = entry("revenue 2023", "agreed", v("4.1B", s("Annual report", "2024-02-01"), s("Press release", "2024-02-03")))
        assertEquals("| Source | Date | Value |\n|---|---|---|\n| Annual report | 2024-02-01 | 4.1B |\n| Press release | 2024-02-03 | 4.1B |", got(render(e, "financial")))
    }

    @Test
    fun e7_newsIsRenderedAsProseThatSaysWhenSourcesDisagree() {
        val agreed = entry("launch", "agreed", v("in May", s("Daily", "2024-05-02")))
        val changed = entry("size", "changed", v("9%", s("B", "2022-05-01")), v("12%", s("A", "2024-05-01")))
        val conflict = entry("size", "conflict", v("12%", s("A", "2024-05-01")), v("9%", s("B", "2024-05-01")))
        assertEquals("launch: in May (Daily, 2024-05-02).", got(render(agreed, "news")))
        assertEquals("size: 9% (B, 2022-05-01); 12% (A, 2024-05-01). The figures are from different dates.", got(render(changed, "news")))
        assertEquals("size: 12% (A, 2024-05-01); 9% (B, 2024-05-01). The sources disagree.", got(render(conflict, "news")))
    }

    @Test
    fun e8_technicalFindingsAreAListAndAnUnknownContentTypeIsRefused() {
        val e = entry("rate limit", "agreed", v("100 per minute", s("API docs", "2024-06-01"), s("Changelog", "2024-06-05")))
        assertEquals("rate limit:\n- 100 per minute (API docs, 2024-06-01)\n- 100 per minute (Changelog, 2024-06-05)", got(render(e, "technical")))
        assertNotEquals(got(render(e, "technical")), got(render(e, "financial")))
        assertThrows(IllegalArgumentException::class.java) { render(e, "poem") }
    }
}
