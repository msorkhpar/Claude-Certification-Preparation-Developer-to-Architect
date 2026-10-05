import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AssistantTurnsTest {
    @Test
    fun aSignalOfRiskIsRoutedToAPersonBeforeAnythingElse() {
        assertEquals("handoff:safety", route("I might hurt myself, please get me a human"))
        assertEquals("handoff:safety", route("this is an emergency", 5))
    }

    @Test
    fun aRequestForAPersonIsHonouredAndAStallNeedsTwoMisses() {
        assertEquals("handoff:requested", route("Can I speak to a person?"))
        assertEquals("answer", route("what?", 1))
        assertEquals("handoff:stalled", route("what?", 2))
    }

    @Test
    fun theWindowKeepsTheNewestTurnsThatFitAndAllThePinnedFacts() {
        val turns = listOf("one two", "three four five", "six")
        assertEquals(Window(listOf("three four five", "six"), 1, listOf("fact")), window(turns, 4, listOf("fact")))
        assertEquals(listOf("six"), window(turns, 3, listOf("fact")).kept)
        assertEquals(0, window(turns, 6, listOf("fact")).dropped)
    }

    @Test
    fun memoryIsReadPerCustomerAndOldFactsAreMarkedToVerify() {
        assertEquals(listOf(Recalled("address", "12 Elm Road", "current"), Recalled("plan", "Plus", "verify")), recall("ada", "2026-10-04"))
        assertEquals(listOf<Recalled>(), recall("bob", "2026-10-04"))
        assertEquals("current", recall("ada", "2026-10-20")[0].status)
        assertEquals("verify", recall("ada", "2026-10-21")[0].status)
    }
}
