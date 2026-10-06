import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RecoveryTest {
    private fun <T : Any> got(value: T?): T {
        assertNotNull(value, "the function returned nothing")
        return value!!
    }

    private fun agent(name: String, status: String) = AgentEntry(name, "state/$name.md", status)

    private fun manifest(vararg agents: AgentEntry) = got(buildManifest(agents.toList()))

    @Test
    fun m1_aFindingIsRecordedOncePerAreaAndFactInFirstSeenOrder() {
        var found = got(addFinding(emptyList(), "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12"))
        found = got(addFinding(found, "billing", "invoices use cents", "billing/Money.java:5"))
        val again = got(addFinding(found, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99"))
        assertEquals(found, again)
        assertEquals(listOf("auth", "billing"), again.map { it.area })
        assertEquals(3, got(addFinding(found, "billing", "a different fact", "x:1")).size)
        assertEquals(3, got(addFinding(found, "auth", "invoices use cents", "x:1")).size)
        assertEquals(2, found.size)
    }

    @Test
    fun e1_theScratchpadGroupsFindingsUnderTheirArea() {
        var found = got(addFinding(emptyList(), "auth", "f1", "a:1"))
        found = got(addFinding(found, "billing", "f2", "b:2"))
        found = got(addFinding(found, "auth", "f3", "a:3"))
        assertEquals("## auth\n- f1 (a:1)\n- f3 (a:3)\n\n## billing\n- f2 (b:2)", renderScratchpad(found))
        assertEquals("", renderScratchpad(emptyList()))
    }

    @Test
    fun e2_theManifestListsEveryAgentWithItsStateFileAndStatusAndRefusesBadInput() {
        val m = manifest(agent("search", "running"), agent("auth", "done"), agent("billing", "failed"))
        assertEquals(Manifest(1, listOf(agent("auth", "done"), agent("billing", "failed"), agent("search", "running"))), m)
        assertThrows(IllegalArgumentException::class.java) { buildManifest(listOf(agent("auth", "done"), agent("auth", "running"))) }
        assertThrows(IllegalArgumentException::class.java) { buildManifest(listOf(agent("auth", "paused"))) }
    }

    @Test
    fun e3_aFinishedAgentWithItsStateFileIsReusedAndNotRunAgain() {
        assertEquals(listOf(Action("auth", "reuse")), resumePlan(manifest(agent("auth", "done")), setOf("state/auth.md")))
    }

    @Test
    fun e4_aRunningOrFailedAgentWithAStateFileIsResumedFromIt() {
        val m = manifest(agent("billing", "failed"), agent("search", "running"))
        assertEquals(listOf(Action("billing", "resume"), Action("search", "resume")), resumePlan(m, setOf("state/billing.md", "state/search.md")))
    }

    @Test
    fun e5_anAgentWhoseStateFileIsMissingIsRestartedFromScratch() {
        val m = manifest(agent("auth", "done"), agent("search", "running"))
        assertEquals(listOf(Action("auth", "reuse"), Action("search", "restart")), resumePlan(m, setOf("state/auth.md")))
        assertEquals(listOf(Action("auth", "restart"), Action("search", "restart")), resumePlan(m, emptySet()))
    }

    @Test
    fun e6_theResumePromptCarriesTheTaskAndTheStateLinesAndNothingElse() {
        assertEquals("Map the search module.\n\nState from the last run:\n- indexer.py done\n- ranker.py not started\nContinue from the first unfinished step.", resumePrompt("Map the search module.", listOf("indexer.py done", "ranker.py not started")))
        assertEquals("Map the search module.", resumePrompt("Map the search module.", emptyList()))
    }

    @Test
    fun e7_theCompactCommandNamesWhatToKeep() {
        assertEquals("/compact Focus on the list of files read, open questions", compactCommand(listOf("the list of files read", "open questions")))
        assertEquals("/compact", compactCommand(emptyList()))
    }
}
