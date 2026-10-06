import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecoveryTest {
    private static <T> T got(T value) {
        assertNotNull(value, "the method returned nothing");
        return value;
    }

    private static Recovery.AgentEntry agent(String name, String status) {
        return new Recovery.AgentEntry(name, "state/" + name + ".md", status);
    }

    private static Recovery.Manifest manifest(Recovery.AgentEntry... agents) {
        return got(Recovery.buildManifest(List.of(agents)));
    }

    @Test
    void m1_aFindingIsRecordedOncePerAreaAndFactInFirstSeenOrder() {
        List<Recovery.Finding> found = got(Recovery.addFinding(List.of(), "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12"));
        found = got(Recovery.addFinding(found, "billing", "invoices use cents", "billing/Money.java:5"));
        List<Recovery.Finding> again = got(Recovery.addFinding(found, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99"));
        assertEquals(found, again);
        assertEquals(List.of("auth", "billing"), again.stream().map(Recovery.Finding::area).toList());
        assertEquals(3, got(Recovery.addFinding(found, "billing", "a different fact", "x:1")).size());
        assertEquals(3, got(Recovery.addFinding(found, "auth", "invoices use cents", "x:1")).size());
        assertEquals(2, found.size());
    }

    @Test
    void e1_theScratchpadGroupsFindingsUnderTheirArea() {
        List<Recovery.Finding> found = got(Recovery.addFinding(List.of(), "auth", "f1", "a:1"));
        found = got(Recovery.addFinding(found, "billing", "f2", "b:2"));
        found = got(Recovery.addFinding(found, "auth", "f3", "a:3"));
        assertEquals("## auth\n- f1 (a:1)\n- f3 (a:3)\n\n## billing\n- f2 (b:2)", Recovery.renderScratchpad(found));
        assertEquals("", Recovery.renderScratchpad(List.of()));
    }

    @Test
    void e2_theManifestListsEveryAgentWithItsStateFileAndStatusAndRefusesBadInput() {
        Recovery.Manifest m = manifest(agent("search", "running"), agent("auth", "done"), agent("billing", "failed"));
        assertEquals(new Recovery.Manifest(1, List.of(agent("auth", "done"), agent("billing", "failed"), agent("search", "running"))), m);
        assertThrows(IllegalArgumentException.class, () -> Recovery.buildManifest(List.of(agent("auth", "done"), agent("auth", "running"))));
        assertThrows(IllegalArgumentException.class, () -> Recovery.buildManifest(List.of(agent("auth", "paused"))));
    }

    @Test
    void e3_aFinishedAgentWithItsStateFileIsReusedAndNotRunAgain() {
        assertEquals(List.of(new Recovery.Action("auth", "reuse")), Recovery.resumePlan(manifest(agent("auth", "done")), Set.of("state/auth.md")));
    }

    @Test
    void e4_aRunningOrFailedAgentWithAStateFileIsResumedFromIt() {
        Recovery.Manifest m = manifest(agent("billing", "failed"), agent("search", "running"));
        assertEquals(List.of(new Recovery.Action("billing", "resume"), new Recovery.Action("search", "resume")), Recovery.resumePlan(m, Set.of("state/billing.md", "state/search.md")));
    }

    @Test
    void e5_anAgentWhoseStateFileIsMissingIsRestartedFromScratch() {
        Recovery.Manifest m = manifest(agent("auth", "done"), agent("search", "running"));
        assertEquals(List.of(new Recovery.Action("auth", "reuse"), new Recovery.Action("search", "restart")), Recovery.resumePlan(m, Set.of("state/auth.md")));
        assertEquals(List.of(new Recovery.Action("auth", "restart"), new Recovery.Action("search", "restart")), Recovery.resumePlan(m, Set.of()));
    }

    @Test
    void e6_theResumePromptCarriesTheTaskAndTheStateLinesAndNothingElse() {
        assertEquals("Map the search module.\n\nState from the last run:\n- indexer.py done\n- ranker.py not started\nContinue from the first unfinished step.",
            Recovery.resumePrompt("Map the search module.", List.of("indexer.py done", "ranker.py not started")));
        assertEquals("Map the search module.", Recovery.resumePrompt("Map the search module.", List.of()));
    }

    @Test
    void e7_theCompactCommandNamesWhatToKeep() {
        assertEquals("/compact Focus on the list of files read, open questions", Recovery.compactCommand(List.of("the list of files read", "open questions")));
        assertEquals("/compact", Recovery.compactCommand(List.of()));
    }
}
