import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class ToolLintTest {
    @Test
    fun theFirstSetBreaksTheRulesAndOverlaps() {
        assertEquals(listOf("no-boundary", "no-use-when", "short-description"), lint(POOR[0]))
        assertEquals(listOf("no-boundary", "no-use-when", "param-undescribed", "short-description"), lint(POOR[1]))
        assertEquals(0.71, (overlap(POOR[0], POOR[1]) * 100).roundToInt() / 100.0)
    }

    @Test
    fun theSplitSetIsCleanAndDoesNotOverlap() {
        assertEquals(listOf(emptyList<String>(), emptyList(), emptyList(), emptyList()), SPLIT.map { lint(it) })
        val max = SPLIT.withIndex().maxOf { (i, a) -> SPLIT.drop(i + 1).maxOfOrNull { overlap(a, it) } ?: 0.0 }
        assertTrue(max < 0.6)
    }

    @Test
    fun aPageCarriesACursorUntilTheLastRow() {
        val rows = (0 until 6).map { it.toString() }
        val first = page(rows, null, 4)
        assertEquals(listOf("0", "1", "2", "3"), first.rows)
        assertNotNull(first.cursor)
        assertTrue(first.note!!.startsWith("Showing 4 of 6"))
        val second = page(rows, first.cursor, 4)
        assertEquals(listOf("4", "5"), second.rows)
        assertNull(second.cursor)
        assertNull(second.note)
    }
}
