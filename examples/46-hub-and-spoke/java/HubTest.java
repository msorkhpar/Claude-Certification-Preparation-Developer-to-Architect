import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static harness.Scripted.toolUse;
import static org.junit.jupiter.api.Assertions.*;

import harness.Scripted;
import java.util.List;
import org.junit.jupiter.api.Test;

class HubTest {
    @Test
    void thePlanIsReadFromTheToolCallAndNoChoiceIsForced() {
        var subtasks = Hub.SUBTASKS.stream().map(s -> map("scope", s.scope(), "brief", s.brief())).toList();
        Scripted.Rig rig = Scripted.client(message(List.of(toolUse("t", "plan", map("subtasks", subtasks))), "tool_use"));
        assertEquals(Hub.SUBTASKS, Hub.plan(rig.client(), "q"));
        assertFalse(rig.http().requests.get(0).has("tool_choice"));
        assertTrue(rig.http().requests.get(0).at("/messages/0/content").asText().contains("plan tool"));
    }

    @Test
    void aSubagentRequestHoldsItsBriefAndTheRolePromptOnly() {
        Scripted.Rig rig = Scripted.client(message(List.of(text(Hub.REPORTS.get(0)))));
        Hub.runSubagent(rig.client(), Hub.SUBTASKS.get(0).brief());
        var request = rig.http().requests.get(0);
        assertEquals(Hub.SUBAGENT_SYSTEM, request.get("system").asText());
        assertEquals(1, request.get("messages").size());
        assertEquals("user", request.at("/messages/0/role").asText());
        assertEquals(Hub.SUBTASKS.get(0).brief(), request.at("/messages/0/content").asText());
        assertFalse(request.has("tools"));
    }

    @Test
    void theSynthesisRequestHoldsEveryFinding() {
        Scripted.Rig rig = Scripted.client(message(List.of(text("done"))));
        Hub.synthesize(rig.client(), "q", List.of(new String[] {"chips", Hub.REPORTS.get(0)}, new String[] {"cars", Hub.REPORTS.get(1)}));
        String body = rig.http().requests.get(0).toString();
        assertTrue(body.contains("CHIPS-REPORT") && body.contains("CARS-REPORT"));
        assertFalse(body.contains("RATES-REPORT"));
    }
}
