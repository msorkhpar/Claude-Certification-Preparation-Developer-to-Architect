import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolLintTest {
    @Test
    void theFirstSetBreaksTheRulesAndOverlaps() {
        assertEquals(List.of("no-boundary", "no-use-when", "short-description"), ToolLint.lint(ToolLint.POOR.get(0)));
        assertEquals(List.of("no-boundary", "no-use-when", "param-undescribed", "short-description"), ToolLint.lint(ToolLint.POOR.get(1)));
        assertEquals(0.71, Math.round(ToolLint.overlap(ToolLint.POOR.get(0), ToolLint.POOR.get(1)) * 100) / 100.0);
    }

    @Test
    void theSplitSetIsCleanAndDoesNotOverlap() {
        for (ToolLint.Tool tool : ToolLint.SPLIT) assertEquals(List.of(), ToolLint.lint(tool));
        double max = 0;
        for (int i = 0; i < ToolLint.SPLIT.size(); i++) for (int j = i + 1; j < ToolLint.SPLIT.size(); j++) max = Math.max(max, ToolLint.overlap(ToolLint.SPLIT.get(i), ToolLint.SPLIT.get(j)));
        assertTrue(max < 0.6);
    }

    @Test
    void aPageCarriesACursorUntilTheLastRow() {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 6; i++) rows.add(String.valueOf(i));
        ToolLint.Page first = ToolLint.page(rows, null, 4);
        assertEquals(List.of("0", "1", "2", "3"), first.rows());
        assertNotNull(first.cursor());
        assertTrue(first.note().startsWith("Showing 4 of 6"));
        ToolLint.Page second = ToolLint.page(rows, first.cursor(), 4);
        assertEquals(List.of("4", "5"), second.rows());
        assertNull(second.cursor());
        assertNull(second.note());
    }
}
