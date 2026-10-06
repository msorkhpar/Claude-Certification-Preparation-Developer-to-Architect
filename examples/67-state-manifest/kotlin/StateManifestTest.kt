import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StateManifestTest {
    @Test
    fun theManifestIsWrittenAtTheStartAndUpdatedWhenTheAgentFinishes() {
        val fs = linkedMapOf<String, String>()
        start(fs, "auth")
        assertEquals(mapOf("auth" to Entry("running", "state/auth.md")), readManifest(fs))
        assertFalse("state/auth.md" in fs)
        finish(fs, "auth", listOf(Finding("a fact", "x.py:1")))
        assertEquals(mapOf("auth" to Entry("done", "state/auth.md")), readManifest(fs))
        assertEquals("- a fact (x.py:1)", fs["state/auth.md"])
    }

    @Test
    fun aCrashBeforeTheStateFileIsWrittenMeansARestart() {
        val fs = linkedMapOf<String, String>()
        start(fs, "auth")
        finish(fs, "auth", listOf(Finding("a fact", "x.py:1")))
        start(fs, "search")
        assertEquals(listOf(Step("auth", "reuse"), Step("search", "restart"), Step("never_started", "restart")), recoveryPlan(fs, listOf("auth", "search", "never_started")))
    }

    @Test
    fun aStateFileWithoutAFinishedManifestEntryIsResumed() {
        val fs = mapOf("state/manifest.txt" to "billing|running|state/billing.md", "state/billing.md" to "- half done (b.py:2)")
        assertEquals(listOf(Step("billing", "resume")), recoveryPlan(fs, listOf("billing")))
    }

    @Test
    fun theInjectedStateHoldsOnlyWhatNeedNotRunAgain() {
        val fs = linkedMapOf<String, String>()
        start(fs, "auth")
        finish(fs, "auth", listOf(Finding("a fact", "x.py:1")))
        start(fs, "search")
        assertEquals("auth:\n- a fact (x.py:1)", injectedState(fs, recoveryPlan(fs, listOf("auth", "search"))))
    }

    @Test
    fun tokensRoundUpByFourCharacters() {
        assertEquals(listOf(0, 1, 2, 2), listOf(tokens(0), tokens(4), tokens(5), tokens("abcde")))
    }
}
