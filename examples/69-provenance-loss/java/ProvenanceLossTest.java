import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProvenanceLossTest {
    private static List<ProvenanceLoss.Row> rows(String[]... items) {
        return java.util.Arrays.stream(items).map(i -> new ProvenanceLoss.Row("c", i[0], i[1], i[2])).toList();
    }

    private static String line(String text, String prefix) {
        return text.lines().filter(l -> l.startsWith(prefix)).findFirst().orElseThrow();
    }

    @Test
    void theStatusTellsAgreementFromConflictAndChange() {
        assertEquals("agreed", ProvenanceLoss.status(rows(new String[] {"1", "A", "2024-01-01"}, new String[] {"1", "B", "2024-02-01"})));
        assertEquals("conflict", ProvenanceLoss.status(rows(new String[] {"1", "A", "2024-01-01"}, new String[] {"2", "B", "2024-01-01"})));
        assertEquals("changed", ProvenanceLoss.status(rows(new String[] {"1", "A", "2022-01-01"}, new String[] {"2", "B", "2024-01-01"})));
    }

    @Test
    void thePlainSummaryKeepsOneValueAndNoSources() {
        String summary = ProvenanceLoss.plainSummary(ProvenanceLoss.FINDINGS);
        assertTrue(summary.contains("market growth 2024: 12%") && !summary.split("growth forecast")[0].contains("9%"));
        assertEquals(0, ProvenanceLoss.sourcesNamed(summary, ProvenanceLoss.FINDINGS));
    }

    @Test
    void theLedgerKeepsBothSidesOfAConflictWithTheirSources() {
        assertEquals("market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)", line(ProvenanceLoss.ledgerLines(ProvenanceLoss.FINDINGS), "market growth 2024"));
    }

    @Test
    void aChangeOverTimeListsTheOlderValueFirst() {
        assertEquals("growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)", line(ProvenanceLoss.ledgerLines(ProvenanceLoss.FINDINGS), "growth forecast"));
    }

    @Test
    void theLedgerNamesEverySource() {
        assertEquals(5, ProvenanceLoss.sourcesNamed(ProvenanceLoss.ledgerLines(ProvenanceLoss.FINDINGS), ProvenanceLoss.FINDINGS));
    }
}
