import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuiltinToolsTest {
    private val text = "a = 1\nb = 1\n"

    @Test
    fun editIsAnExactReplacementThatNeedsOneMatch() {
        assertEquals(EditResult(true, "a = 2\nb = 1\n", 1), edit(text, "a = 1", "a = 2"))
        assertEquals(EditResult(false, error = "old_string appears 2 times"), edit(text, "= 1", "= 2"))
        assertEquals(EditResult(false, error = "old_string not found"), edit(text, "z", "y"))
        assertFalse(edit(text, "a = .", "x").ok) // no regex
    }

    @Test
    fun replaceAllChangesEveryMatch() {
        assertEquals(EditResult(true, "a = 2\nb = 2\n", 2), edit(text, "= 1", "= 2", replaceAll = true))
    }

    @Test
    fun thePlanWidensTheAnchorBeforeItRewritesTheFile() {
        assertEquals(Plan("edit", "a = 1"), planEdit(text, "a = 1"))
        assertEquals(Plan("edit", "b = 1"), planEdit(text, "= 1", anchors = listOf("b = 1")))
        assertEquals(Plan("replace_all", "= 1"), planEdit(text, "= 1", every = true))
        assertEquals(Plan("read_write", null), planEdit(text, "= 1", anchors = listOf("= 1")))
        assertEquals(Plan("read_again", null), planEdit(text, "z"))
    }

    @Test
    fun searchToolsAreDefaultOnWindowsOnlyAndNamedBackElsewhere() {
        assertEquals(listOf("Read", "Write", "Edit", "Bash", "Grep", "Glob"), toolSet("windows"))
        assertEquals(listOf("Read", "Write", "Edit", "Bash"), toolSet("linux"))
        assertEquals(listOf("Read", "Write", "Edit", "Bash", "Grep", "Glob"), toolSet("linux", allowedTools = listOf("Glob")))
        assertEquals(listOf("Read", "Grep"), toolSet("macos", tools = listOf("Read", "Grep")))
        assertEquals(listOf("Read", "Write", "Edit", "Grep", "Glob"), toolSet("wsl", disallowedTools = listOf("Bash")))
    }

    @Test
    fun aRuleCoversTheToolsItsNameImplies() {
        assertEquals(listOf("Read", "Grep", "Glob"), coveredBy("Read(x)"))
        assertEquals(listOf("Edit", "Write"), coveredBy("Edit(x)"))
        assertEquals(emptyList<String>(), coveredBy("Write(x)"))
        assertEquals(listOf("Bash"), coveredBy("Bash(ls *)"))
    }

    @Test
    fun aCallIsCheckedUnderTheRuleNameOfItsFamily() {
        assertEquals(listOf("Read", "Read", "Read", "Edit", "Edit", "Bash"), listOf("Read", "Grep", "Glob", "Edit", "Write", "Bash").map { ruleTool(it) })
    }
}
