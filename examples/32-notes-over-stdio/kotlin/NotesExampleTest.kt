import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class NotesExampleTest {
    @Test
    fun aBlankTitleIsAToolErrorAndDoesNotUseAnId() {
        val notes = Notes()
        val bad = notes.add(" ")
        val good = notes.add("A")
        assertTrue(bad.isError == true && "title is required" in words(bad))
        assertTrue(good.isError == false)
        assertEquals("Saved note 1: A", words(good))
    }

    @Test
    fun searchIsCaseInsensitiveAndSaysWhenNothingMatches() {
        val notes = Notes().also { it.add("Plan") }
        assertEquals("1. Plan", words(notes.search("PLAN", null)))
        assertEquals("No notes match \"zzz\"", words(notes.search("zzz", null)))
    }

    @Test
    fun aLimitThatIsNotAnIntegerIsAToolError() {
        val notes = Notes().also { it.add("Plan") }
        val bad = notes.search("plan", JsonPrimitive("two"))
        assertTrue(bad.isError == true)
        assertEquals("1. Plan", words(notes.search("plan", JsonPrimitive(3))))
    }

    @Test
    fun theCountAndThePromptFollowTheNotes() {
        val notes = Notes()
        val before = notes.count()
        notes.add("A")
        assertEquals(listOf("0 notes", "1 note"), listOf(before, notes.count()))
        assertEquals("Review these notes in a formal tone:\n- A", notes.review("formal"))
    }

    @Test
    fun theServerRegistersTwoToolsOneResourceAndOnePrompt() {
        val server = buildServer(Notes())
        assertEquals(setOf("add_note", "search_notes"), server.tools.keys)
        assertEquals(setOf("notes://count"), server.resources.keys)
        assertEquals(setOf("review_notes"), server.prompts.keys)
    }
}
