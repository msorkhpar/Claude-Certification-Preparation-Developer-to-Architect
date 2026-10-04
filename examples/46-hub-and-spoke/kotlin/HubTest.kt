import harness.Scripted
import harness.Scripted.map
import harness.Scripted.message
import harness.Scripted.text
import harness.Scripted.toolUse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HubTest {
    @Test
    fun thePlanIsReadFromTheToolCallAndNoChoiceIsForced() {
        val rig = Scripted.client(message(listOf(toolUse("t", "plan", map("subtasks", SUBTASKS.map { map("scope", it.scope, "brief", it.brief) }))), "tool_use"))
        assertEquals(SUBTASKS, plan(rig.client(), "q"))
        assertFalse(rig.http().requests[0].has("tool_choice"))
        assertTrue("plan tool" in rig.http().requests[0].at("/messages/0/content").asText())
    }

    @Test
    fun aSubagentRequestHoldsItsBriefAndTheRolePromptOnly() {
        val rig = Scripted.client(message(listOf(text(REPORTS[0]))))
        runSubagent(rig.client(), SUBTASKS[0].brief)
        val request = rig.http().requests[0]
        assertEquals(SUBAGENT_SYSTEM, request["system"].asText())
        assertEquals(1, request["messages"].size())
        assertEquals("user", request.at("/messages/0/role").asText())
        assertEquals(SUBTASKS[0].brief, request.at("/messages/0/content").asText())
        assertFalse(request.has("tools"))
    }

    @Test
    fun theSynthesisRequestHoldsEveryFinding() {
        val rig = Scripted.client(message(listOf(text("done"))))
        synthesize(rig.client(), "q", listOf("chips" to REPORTS[0], "cars" to REPORTS[1]))
        val body = rig.http().requests[0].toString()
        assertTrue("CHIPS-REPORT" in body && "CARS-REPORT" in body)
        assertFalse("RATES-REPORT" in body)
    }
}
