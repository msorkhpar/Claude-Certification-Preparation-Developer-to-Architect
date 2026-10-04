import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StateManifestTest {
    @Test
    void theManifestIsWrittenAtTheStartAndUpdatedWhenTheAgentFinishes() {
        Map<String, String> fs = new LinkedHashMap<>();
        StateManifest.start(fs, "auth");
        assertEquals(Map.of("auth", new StateManifest.Entry("running", "state/auth.md")), StateManifest.readManifest(fs));
        assertFalse(fs.containsKey("state/auth.md"));
        StateManifest.finish(fs, "auth", List.of(new StateManifest.Finding("a fact", "x.py:1")));
        assertEquals(Map.of("auth", new StateManifest.Entry("done", "state/auth.md")), StateManifest.readManifest(fs));
        assertEquals("- a fact (x.py:1)", fs.get("state/auth.md"));
    }

    @Test
    void aCrashBeforeTheStateFileIsWrittenMeansARestart() {
        Map<String, String> fs = new LinkedHashMap<>();
        StateManifest.start(fs, "auth");
        StateManifest.finish(fs, "auth", List.of(new StateManifest.Finding("a fact", "x.py:1")));
        StateManifest.start(fs, "search");
        assertEquals(List.of(new StateManifest.Step("auth", "reuse"), new StateManifest.Step("search", "restart"), new StateManifest.Step("never_started", "restart")),
            StateManifest.recoveryPlan(fs, List.of("auth", "search", "never_started")));
    }

    @Test
    void aStateFileWithoutAFinishedManifestEntryIsResumed() {
        Map<String, String> fs = new LinkedHashMap<>();
        fs.put("state/manifest.txt", "billing|running|state/billing.md");
        fs.put("state/billing.md", "- half done (b.py:2)");
        assertEquals(List.of(new StateManifest.Step("billing", "resume")), StateManifest.recoveryPlan(fs, List.of("billing")));
    }

    @Test
    void theInjectedStateHoldsOnlyWhatNeedNotRunAgain() {
        Map<String, String> fs = new LinkedHashMap<>();
        StateManifest.start(fs, "auth");
        StateManifest.finish(fs, "auth", List.of(new StateManifest.Finding("a fact", "x.py:1")));
        StateManifest.start(fs, "search");
        assertEquals("auth:\n- a fact (x.py:1)", StateManifest.injectedState(fs, StateManifest.recoveryPlan(fs, List.of("auth", "search"))));
    }

    @Test
    void tokensRoundUpByFourCharacters() {
        assertEquals(List.of(0, 1, 2, 2), List.of(StateManifest.tokens(0), StateManifest.tokens(4), StateManifest.tokens(5), StateManifest.tokens("abcde")));
    }
}
