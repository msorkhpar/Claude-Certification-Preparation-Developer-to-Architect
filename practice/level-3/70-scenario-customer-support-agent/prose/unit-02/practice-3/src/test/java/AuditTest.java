import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditTest {
    private static Audit.Step step(String tool) {
        return new Audit.Step(tool, true, null);
    }

    private static Audit.Step step(String tool, boolean ok, String right) {
        return new Audit.Step(tool, ok, right);
    }

    private static Audit.Session session(List<Audit.Step> steps, String outcome, boolean needsHuman, int refund) {
        return new Audit.Session("s", steps, outcome, needsHuman, refund, 10000);
    }

    private static Audit.Session session(List<Audit.Step> steps) {
        return session(steps, "resolved", false, 0);
    }

    private static final List<Audit.Step> CLEAN = List.of(step("get_customer"), step("lookup_order"), step("process_refund"));

    private static List<Audit.Session> repeat(int n, Audit.Session s) {
        List<Audit.Session> all = new ArrayList<>();
        for (int i = 0; i < n; i++) all.add(s);
        return all;
    }

    @Test
    void m1_aMixedSetOfSessionsGetsEveryRateAndTheFirstFix() {
        var sessions = List.of(
            session(CLEAN, "resolved", false, 2000),
            session(List.of(step("lookup_order"), step("get_customer"), step("process_refund"))),
            session(List.of(step("get_customer")), "escalated", true, 0),
            session(List.of(step("get_customer")), "escalated", false, 0),
            session(CLEAN, "resolved", true, 0),
            session(List.of(step("get_customer", true, "lookup_order"), step("lookup_order"))));
        assertEquals(new Audit.Report(6, 4, 0.667, false, 1, 1, 1, 1, 0, "enforce_in_code"), Audit.audit(sessions));
    }

    @Test
    void e1_noSessionsGiveZeroRatesAndNoDiagnosis() {
        assertEquals(new Audit.Report(0, 0, 0.0, false, 0, 0, 0, 0, 0, "none"), Audit.audit(List.of()));
    }

    @Test
    void e2_aProtectedCallBeforeASuccessfulIdentityCheckIsASkippedPrerequisite() {
        assertEquals(0, Audit.audit(List.of(session(List.of(step("get_customer"), step("lookup_order"))))).skippedPrerequisite());
        assertEquals(1, Audit.audit(List.of(session(List.of(step("lookup_order"), step("get_customer"))))).skippedPrerequisite());
        assertEquals(1, Audit.audit(List.of(session(List.of(step("get_customer", false, null), step("process_refund"))))).skippedPrerequisite());
        assertEquals(2, Audit.audit(List.of(session(List.of(step("process_refund"))), session(List.of(step("lookup_order"))), session(CLEAN))).skippedPrerequisite());
    }

    @Test
    void e3_aRefundOverTheLimitCountsOnlyWhenItWasMade() {
        var made = Audit.audit(List.of(session(CLEAN, "resolved", false, 10001)));
        assertEquals(1, made.overLimitRefunds());
        assertEquals("enforce_in_code", made.diagnosis());
        assertEquals(0, Audit.audit(List.of(session(CLEAN, "resolved", false, 10000))).overLimitRefunds());
        var refused = Audit.audit(List.of(session(CLEAN, "escalated", true, 50000)));
        assertEquals(0, refused.overLimitRefunds());
        assertEquals("none", refused.diagnosis());
    }

    @Test
    void e4_moneyFirstThenToolDescriptionsThenEscalationCriteria() {
        var wrongTool = session(List.of(step("get_customer", true, "lookup_order")));
        var over = session(CLEAN, "escalated", false, 0);
        assertEquals("enforce_in_code", Audit.audit(List.of(session(List.of(step("process_refund"), step("get_customer"))), wrongTool, over)).diagnosis());
        assertEquals("rewrite_tool_descriptions", Audit.audit(List.of(wrongTool, wrongTool)).diagnosis());
        assertEquals("rewrite_tool_descriptions", Audit.audit(List.of(wrongTool, over)).diagnosis());
        assertEquals("write_escalation_criteria", Audit.audit(List.of(wrongTool, over, over)).diagnosis());
        assertEquals("write_escalation_criteria", Audit.audit(List.of(session(CLEAN, "resolved", true, 0))).diagnosis());
        assertEquals("none", Audit.audit(List.of(session(CLEAN))).diagnosis());
    }

    @Test
    void e5_theTargetBoundaryAndRoundingOfTheFirstContactRate() {
        var escalated = session(CLEAN, "escalated", true, 0);
        List<Audit.Session> fourOfFive = new ArrayList<>(repeat(4, session(CLEAN)));
        fourOfFive.add(escalated);
        assertEquals(0.8, Audit.audit(fourOfFive).fcr());
        assertTrue(Audit.audit(fourOfFive).meetsTarget());
        List<Audit.Session> threeOfFive = new ArrayList<>(repeat(3, session(CLEAN)));
        threeOfFive.addAll(repeat(2, escalated));
        assertEquals(0.6, Audit.audit(threeOfFive).fcr());
        assertFalse(Audit.audit(threeOfFive).meetsTarget());
        assertEquals(0.667, Audit.audit(List.of(session(CLEAN), session(CLEAN), escalated)).fcr());
        var mixed = Audit.audit(List.of(session(CLEAN, "escalated", false, 0), session(CLEAN, "escalated", false, 0), session(CLEAN, "resolved", true, 0)));
        assertEquals(2, mixed.overEscalated());
        assertEquals(1, mixed.underEscalated());
    }

    @Test
    void e6_aWrongToolCountsSessionsAndIgnoresStepsWithNoKnownRightTool() {
        var twice = session(List.of(step("get_customer", true, "lookup_order"), step("lookup_order", true, "process_refund")));
        var unknown = session(List.of(step("get_customer"), step("lookup_order", true, "lookup_order")));
        assertEquals(1, Audit.audit(List.of(twice, unknown, session(CLEAN))).wrongTool());
        assertEquals(2, Audit.audit(List.of(twice, twice)).wrongTool());
    }
}
