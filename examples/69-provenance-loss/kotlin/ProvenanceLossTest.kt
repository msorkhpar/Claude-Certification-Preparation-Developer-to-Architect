import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProvenanceLossTest {
    private fun rows(vararg items: Triple<String, String, String>) = items.map { (value, source, date) -> Row("c", value, source, date) }

    private fun line(text: String, prefix: String) = text.lines().first { it.startsWith(prefix) }

    @Test
    fun theStatusTellsAgreementFromConflictAndChange() {
        assertEquals("agreed", status(rows(Triple("1", "A", "2024-01-01"), Triple("1", "B", "2024-02-01"))))
        assertEquals("conflict", status(rows(Triple("1", "A", "2024-01-01"), Triple("2", "B", "2024-01-01"))))
        assertEquals("changed", status(rows(Triple("1", "A", "2022-01-01"), Triple("2", "B", "2024-01-01"))))
    }

    @Test
    fun thePlainSummaryKeepsOneValueAndNoSources() {
        val summary = plainSummary(FINDINGS)
        assertTrue(summary.contains("market growth 2024: 12%") && !summary.split("growth forecast")[0].contains("9%"))
        assertEquals(0, sourcesNamed(summary, FINDINGS))
    }

    @Test
    fun theLedgerKeepsBothSidesOfAConflictWithTheirSources() {
        assertEquals("market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)", line(ledgerLines(FINDINGS), "market growth 2024"))
    }

    @Test
    fun aChangeOverTimeListsTheOlderValueFirst() {
        assertEquals("growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)", line(ledgerLines(FINDINGS), "growth forecast"))
    }

    @Test
    fun theLedgerNamesEverySource() {
        assertEquals(5, sourcesNamed(ledgerLines(FINDINGS), FINDINGS))
    }
}
