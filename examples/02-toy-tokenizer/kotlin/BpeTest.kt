import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BpeTest {
    private val corpus = "low low low lower lower lowest newest newest widest widest"

    @Test
    fun aFrequentWordBecomesOneTokenAndARareOneSplits() {
        val rules = train(corpus, 6)
        assertEquals(listOf("low"), encode("low", rules))
        assertTrue(encode("lowish", rules).size > 1)
    }

    @Test
    fun anUnseenWordStillEncodesWithKnownPieces() {
        assertEquals("newer", encode("newer", train(corpus, 6)).joinToString(""))
    }

    @Test
    fun noMergesMeansOneTokenPerCharacter() {
        assertEquals(listOf("l", "o", "w"), encode("low", train(corpus, 0)))
    }
}
