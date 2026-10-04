import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LedgerTest {
    private static <T> T got(T value) {
        assertNotNull(value, "the method returned nothing");
        return value;
    }

    private static Ledger.Finding f(String claim, String value, String source, String date) {
        return new Ledger.Finding(claim, value, source, date);
    }

    private static Ledger.Src s(String source, String date) {
        return new Ledger.Src(source, date);
    }

    private static Ledger.Val v(String value, Ledger.Src... sources) {
        return new Ledger.Val(value, List.of(sources));
    }

    private static Ledger.Entry entry(String claim, String status, Ledger.Val... values) {
        return new Ledger.Entry(claim, status, List.of(values));
    }

    private static List<Ledger.Entry> merge(Ledger.Finding... findings) {
        return got(Ledger.merge(List.of(findings)));
    }

    @Test
    void m1_findingsAreMergedPerClaimWithEverySourceKeptOnce() {
        List<Ledger.Entry> merged = merge(f("revenue 2023", "4.1B", "Annual report", "2024-02-01"), f("headcount", "910", "Press release", "2024-03-01"),
            f("revenue 2023", "4.1B", "Press release", "2024-02-03"), f("revenue 2023", "4.1B", "Annual report", "2024-02-01"));
        assertEquals(List.of(entry("revenue 2023", "agreed", v("4.1B", s("Annual report", "2024-02-01"), s("Press release", "2024-02-03"))), entry("headcount", "agreed", v("910", s("Press release", "2024-03-01")))), merged);
        assertEquals(List.of(), merge());
    }

    @Test
    void e1_aFindingWithoutASourceOrADateIsRefused() {
        assertEquals(List.of("source", "date"), got(Ledger.checkFinding(f("c", "v", "", " "))));
        assertEquals(List.of("claim"), got(Ledger.checkFinding(f(null, "v", "s", "2024-01-01"))));
        assertEquals(List.of(), got(Ledger.checkFinding(f("c", "v", "s", "2024-01-01"))));
        assertThrows(IllegalArgumentException.class, () -> Ledger.merge(List.of(f("c", "v", "s", "2024-01-01"), f("c", "w", "s", ""))));
        assertDoesNotThrow(() -> Ledger.merge(List.of(f("c", "v", "s", "2024-01-01"))));
    }

    @Test
    void e2_twoValuesFromTheSameDateAreAConflictThatKeepsBoth() {
        assertEquals(List.of(entry("market size", "conflict", v("12%", s("Firm A report", "2024-05-01")), v("9%", s("Firm B survey", "2024-05-01")))),
            merge(f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2024-05-01")));
    }

    @Test
    void e3_twoValuesFromDifferentDatesAreAChangeNotAConflict() {
        assertEquals(List.of(entry("market size", "changed", v("9%", s("Firm B survey", "2022-05-01")), v("12%", s("Firm A report", "2024-05-01")))),
            merge(f("market size", "12%", "Firm A report", "2024-05-01"), f("market size", "9%", "Firm B survey", "2022-05-01")));
    }

    @Test
    void e4_theCoverageNoteSeparatesWhatIsWellSupportedFromWhatIsNot() {
        List<Ledger.Entry> merged = merge(f("a", "1", "S1", "2024-01-01"), f("a", "1", "S2", "2024-01-02"), f("b", "2", "S1", "2024-01-01"), f("b", "2", "S1", "2024-02-01"),
            f("c", "3", "S1", "2024-01-01"), f("c", "4", "S2", "2022-01-01"), f("d", "5", "S1", "2024-01-01"), f("d", "6", "S2", "2024-01-01"));
        assertEquals(new Ledger.Coverage(List.of("a"), List.of("b"), List.of("c"), List.of("d"), List.of()), got(Ledger.coverageNote(List.of("a", "b", "c", "d"), merged, Map.of())));
    }

    @Test
    void e5_aPlannedClaimWithNoFindingIsAGapWithItsReason() {
        List<Ledger.Entry> merged = merge(f("a", "1", "S1", "2024-01-01"), f("z", "9", "S1", "2024-01-01"));
        Ledger.Coverage note = got(Ledger.coverageNote(List.of("q", "a", "r"), merged, Map.of("q", "the registry timed out")));
        assertEquals(List.of(new Ledger.Gap("q", "the registry timed out"), new Ledger.Gap("r", "no source found")), note.gaps());
        assertEquals(List.of("a", "z"), note.singleSource());
    }

    @Test
    void e6_financialDataIsRenderedAsATable() {
        Ledger.Entry e = entry("revenue 2023", "agreed", v("4.1B", s("Annual report", "2024-02-01"), s("Press release", "2024-02-03")));
        assertEquals("| Source | Date | Value |\n|---|---|---|\n| Annual report | 2024-02-01 | 4.1B |\n| Press release | 2024-02-03 | 4.1B |", got(Ledger.render(e, "financial")));
    }

    @Test
    void e7_newsIsRenderedAsProseThatSaysWhenSourcesDisagree() {
        Ledger.Entry agreed = entry("launch", "agreed", v("in May", s("Daily", "2024-05-02")));
        Ledger.Entry changed = entry("size", "changed", v("9%", s("B", "2022-05-01")), v("12%", s("A", "2024-05-01")));
        Ledger.Entry conflict = entry("size", "conflict", v("12%", s("A", "2024-05-01")), v("9%", s("B", "2024-05-01")));
        assertEquals("launch: in May (Daily, 2024-05-02).", got(Ledger.render(agreed, "news")));
        assertEquals("size: 9% (B, 2022-05-01); 12% (A, 2024-05-01). The figures are from different dates.", got(Ledger.render(changed, "news")));
        assertEquals("size: 12% (A, 2024-05-01); 9% (B, 2024-05-01). The sources disagree.", got(Ledger.render(conflict, "news")));
    }

    @Test
    void e8_technicalFindingsAreAListAndAnUnknownContentTypeIsRefused() {
        Ledger.Entry e = entry("rate limit", "agreed", v("100 per minute", s("API docs", "2024-06-01"), s("Changelog", "2024-06-05")));
        assertEquals("rate limit:\n- 100 per minute (API docs, 2024-06-01)\n- 100 per minute (Changelog, 2024-06-05)", got(Ledger.render(e, "technical")));
        assertNotEquals(got(Ledger.render(e, "technical")), got(Ledger.render(e, "financial")));
        assertThrows(IllegalArgumentException.class, () -> Ledger.render(e, "poem"));
    }
}
