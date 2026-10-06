import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArchitectureReviewTest {
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> stages(Object... over) {
        Map<String, Object> s = map("input", List.of("parse"), "processing", List.of("classify", "route"), "output", List.of("validate", "send"), "feedback", List.of("review a sample"));
        for (int i = 0; i < over.length; i += 2) {
            if (over[i + 1] == null) s.remove((String) over[i]);
            else s.put((String) over[i], over[i + 1]);
        }
        return s;
    }

    private static Map<String, Object> design(Object... over) {
        Map<String, Object> d = map("name", "intake", "pattern", "workflow", "agents", 1, "cost", 3, "path_known", true, "parallel_independent", false, "shared_context", false,
                "needs_audit", true, "writes_without_approval", false, "stages", stages());
        for (int i = 0; i < over.length; i += 2) d.put((String) over[i], over[i + 1]);
        return d;
    }

    private static Map<String, Object> finding(String rule, String severity) {
        return map("rule", rule, "severity", severity);
    }

    private static List<Map<String, Object>> review(Map<String, Object> design) {
        List<Map<String, Object>> result = ArchitectureReview.review(design);
        assertNotNull(result, "review returned nothing");
        return result;
    }

    private static String verdict(List<Map<String, Object>> findings) {
        String result = ArchitectureReview.verdict(findings);
        assertNotNull(result, "verdict returned nothing");
        return result;
    }

    private static List<String> rules(Map<String, Object> design) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> f : review(design)) out.add((String) f.get("rule"));
        return out;
    }

    @Test
    void m1_aSoundDesignPassesReviewWithNoFindings() {
        assertEquals(List.of(), review(design()));
        assertEquals("approve", verdict(List.of()));
    }

    @Test
    void e1_aDesignWithoutAFeedbackLoopOrAStageIsRejected() {
        assertEquals(List.of(finding("no-feedback", "high")), review(design("stages", stages("feedback", List.of()))));
        assertEquals(List.of("no-feedback"), rules(design("stages", stages("feedback", null))));
        for (String stage : List.of("input", "processing")) assertEquals(List.of("missing-stage:" + stage), rules(design("stages", stages(stage, List.of()))));
        assertEquals(List.of("missing-stage:output"), rules(design("stages", stages("output", List.of()))));
    }

    @Test
    void e2_autonomyIsFlaggedOnlyWhenThePathIsKnown() {
        assertEquals(List.of(finding("autonomy-without-need", "medium")), review(design("pattern", "agent")));
        assertEquals(List.of("autonomy-without-need"), rules(design("pattern", "multi-agent", "agents", 1)));
        assertEquals(List.of(), rules(design("pattern", "agent", "path_known", false)));
        assertEquals(List.of(), rules(design("pattern", "augmented")));
    }

    @Test
    void e3_severalAgentsNeedIndependentPartsAndNoSharedContext() {
        assertEquals(List.of(finding("team-without-independence", "high")), review(design("pattern", "multi-agent", "path_known", false, "agents", 3, "shared_context", true, "parallel_independent", true)));
        assertEquals(List.of("team-without-independence"), rules(design("pattern", "multi-agent", "path_known", false, "agents", 3, "parallel_independent", false)));
        assertEquals(List.of(), rules(design("pattern", "multi-agent", "path_known", false, "agents", 3, "parallel_independent", true)));
        assertEquals(List.of(), rules(design("pattern", "agent", "path_known", false, "agents", 1, "shared_context", true)));
    }

    @Test
    void e4_anUnapprovedWriteIsAFindingOnlyWhenAnAuditIsNeeded() {
        assertEquals(List.of(finding("unapproved-write", "high")), review(design("writes_without_approval", true)));
        assertEquals(List.of(), rules(design("writes_without_approval", true, "needs_audit", false)));
        assertEquals(List.of(), rules(design("writes_without_approval", false)));
    }

    @Test
    void e5_outputThatNobodyValidatesIsFlagged() {
        assertEquals(List.of(finding("unvalidated-output", "medium")), review(design("stages", stages("output", List.of("send")))));
        assertEquals(List.of(), rules(design("stages", stages("output", List.of("validate")))));
    }

    @Test
    void e6_findingsAreOrderedBySeverityThenRuleAndTheVerdictFollowsTheWorst() {
        Map<String, Object> messy = design("pattern", "agent", "writes_without_approval", true, "stages", map("input", List.of("parse"), "processing", List.of("act"), "output", List.of("send"), "feedback", List.of()));
        assertEquals(List.of(finding("no-feedback", "high"), finding("unapproved-write", "high"), finding("autonomy-without-need", "medium"), finding("unvalidated-output", "medium")), review(messy));
        assertEquals("reject", verdict(review(messy)));
        assertEquals("revise", verdict(review(design("pattern", "agent"))));
        assertEquals("reject", verdict(List.of(finding("x", "medium"), finding("y", "high"))));
        assertEquals("revise", verdict(List.of(finding("x", "medium"))));
    }

    private static String pick(List<Map<String, Object>> designs) {
        return ArchitectureReview.cheapestAdequate(designs);
    }

    @Test
    void e7_theCheapestDesignThatIsNotRejectedWinsAndTiesGoByName() {
        Map<String, Object> cheapButRejected = design("name", "a-cheap", "cost", 1, "stages", stages("feedback", List.of()));
        Map<String, Object> revise = design("name", "b-revise", "cost", 2, "pattern", "agent");
        Map<String, Object> sound = design("name", "c-sound", "cost", 5);
        assertEquals("b-revise", pick(List.of(cheapButRejected, sound, revise)));
        assertEquals("alpha", pick(List.of(design("name", "zeta", "cost", 2), design("name", "alpha", "cost", 2), design("name", "mid", "cost", 4))));
        assertNull(pick(List.of(cheapButRejected)));
        assertNull(pick(List.of()));
    }
}
