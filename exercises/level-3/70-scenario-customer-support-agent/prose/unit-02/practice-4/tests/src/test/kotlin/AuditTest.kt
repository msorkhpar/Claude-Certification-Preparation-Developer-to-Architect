import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AuditTest {
    private fun session(steps: List<Step>, outcome: String = "resolved", needsHuman: Boolean = false, refund: Int = 0) = Session("s", steps, outcome, needsHuman, refund, 10000)

    private val clean = listOf(Step("get_customer"), Step("lookup_order"), Step("process_refund"))

    @Test
    fun m1_aMixedSetOfSessionsGetsEveryRateAndTheFirstFix() {
        val sessions = listOf(
            session(clean, refund = 2000),
            session(listOf(Step("lookup_order"), Step("get_customer"), Step("process_refund"))),
            session(listOf(Step("get_customer")), "escalated", true),
            session(listOf(Step("get_customer")), "escalated"),
            session(clean, needsHuman = true),
            session(listOf(Step("get_customer", rightTool = "lookup_order"), Step("lookup_order"))),
        )
        assertEquals(Report(6, 4, 0.667, false, 1, 1, 1, 1, 0, "enforce_in_code"), audit(sessions))
    }

    @Test
    fun e1_noSessionsGiveZeroRatesAndNoDiagnosis() {
        assertEquals(Report(0, 0, 0.0, false, 0, 0, 0, 0, 0, "none"), audit(listOf()))
    }

    @Test
    fun e2_aProtectedCallBeforeASuccessfulIdentityCheckIsASkippedPrerequisite() {
        assertEquals(0, audit(listOf(session(listOf(Step("get_customer"), Step("lookup_order"))))).skippedPrerequisite)
        assertEquals(1, audit(listOf(session(listOf(Step("lookup_order"), Step("get_customer"))))).skippedPrerequisite)
        assertEquals(1, audit(listOf(session(listOf(Step("get_customer", ok = false), Step("process_refund"))))).skippedPrerequisite)
        assertEquals(2, audit(listOf(session(listOf(Step("process_refund"))), session(listOf(Step("lookup_order"))), session(clean))).skippedPrerequisite)
    }

    @Test
    fun e3_aRefundOverTheLimitCountsOnlyWhenItWasMade() {
        val made = audit(listOf(session(clean, refund = 10001)))
        assertEquals(1, made.overLimitRefunds)
        assertEquals("enforce_in_code", made.diagnosis)
        assertEquals(0, audit(listOf(session(clean, refund = 10000))).overLimitRefunds)
        val refused = audit(listOf(session(clean, "escalated", true, 50000)))
        assertEquals(0, refused.overLimitRefunds)
        assertEquals("none", refused.diagnosis)
    }

    @Test
    fun e4_moneyFirstThenToolDescriptionsThenEscalationCriteria() {
        val wrongTool = session(listOf(Step("get_customer", rightTool = "lookup_order")))
        val over = session(clean, "escalated")
        assertEquals("enforce_in_code", audit(listOf(session(listOf(Step("process_refund"), Step("get_customer"))), wrongTool, over)).diagnosis)
        assertEquals("rewrite_tool_descriptions", audit(listOf(wrongTool, wrongTool)).diagnosis)
        assertEquals("rewrite_tool_descriptions", audit(listOf(wrongTool, over)).diagnosis)
        assertEquals("write_escalation_criteria", audit(listOf(wrongTool, over, over)).diagnosis)
        assertEquals("write_escalation_criteria", audit(listOf(session(clean, needsHuman = true))).diagnosis)
        assertEquals("none", audit(listOf(session(clean))).diagnosis)
    }

    @Test
    fun e5_theTargetBoundaryAndRoundingOfTheFirstContactRate() {
        val escalated = session(clean, "escalated", true)
        val fourOfFive = audit(List(4) { session(clean) } + escalated)
        assertEquals(0.8, fourOfFive.fcr)
        assertTrue(fourOfFive.meetsTarget)
        val threeOfFive = audit(List(3) { session(clean) } + List(2) { escalated })
        assertEquals(0.6, threeOfFive.fcr)
        assertFalse(threeOfFive.meetsTarget)
        assertEquals(0.667, audit(listOf(session(clean), session(clean), escalated)).fcr)
        val mixed = audit(listOf(session(clean, "escalated"), session(clean, "escalated"), session(clean, needsHuman = true)))
        assertEquals(2, mixed.overEscalated)
        assertEquals(1, mixed.underEscalated)
    }

    @Test
    fun e6_aWrongToolCountsSessionsAndIgnoresStepsWithNoKnownRightTool() {
        val twice = session(listOf(Step("get_customer", rightTool = "lookup_order"), Step("lookup_order", rightTool = "process_refund")))
        val unknown = session(listOf(Step("get_customer"), Step("lookup_order", rightTool = "lookup_order")))
        assertEquals(1, audit(listOf(twice, unknown, session(clean))).wrongTool)
        assertEquals(2, audit(listOf(twice, twice)).wrongTool)
    }
}
