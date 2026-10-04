import static harness.Scripted.map;
import static harness.Scripted.message;
import static harness.Scripted.text;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import harness.Scripted;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScreenLoopTest {
    @Test
    void theScaleComesFromThePixelBudgetForALargeScreenAndClicksMapBack() {
        double scale = ScreenLoop.scaleFor(2560, 1440);
        assertEquals(0.5585, Math.round(scale * 10000) / 10000.0);
        assertEquals(1429, (int) (2560 * scale));
        assertEquals(804, (int) (1440 * scale));
        assertEquals(1.0, ScreenLoop.scaleFor(1280, 720));
        ScreenLoop.Screen screen = ScreenLoop.newScreen();
        assertArrayEquals(new int[] {0, 0}, ScreenLoop.toScreen(0, 0, scale, screen));
        assertArrayEquals(new int[] {2559, 1439}, ScreenLoop.toScreen(10_000, 10_000, scale, screen));
    }

    @Test
    void aRiskyClickIsDeclinedUnlessAPersonSaysYes() {
        ScreenLoop.Screen screen = ScreenLoop.newScreen();
        double scale = ScreenLoop.scaleFor(2560, 1440);
        Map<String, Object> click = map("coordinate", List.of((int) Math.rint(2150 * scale), (int) Math.rint(1240 * scale)));
        ScreenLoop.Outcome declined = ScreenLoop.perform(screen, "left_click", click, scale, null);
        assertEquals("Declined: buy needs a person's confirmation (payment)", declined.content().string().get());
        assertTrue(declined.isError());
        assertFalse(ScreenLoop.perform(screen, "left_click", click, scale, action -> action.get("risk").equals("payment")).isError());
        assertEquals(List.of(List.of("click", "buy")), screen.log);
    }

    @Test
    void aFailureHaltsTheRestOfItsBatchWithTheDocumentedText() {
        ScreenLoop.Screen screen = ScreenLoop.newScreen();
        Scripted.Rig rig = Scripted.client(
            message(List.of(ScreenLoop.call("t1", "left_click", map("coordinate", List.of(900, 900))), ScreenLoop.call("t2", "mystery", map()), ScreenLoop.call("t3", "type", map("text", "x"))), "tool_use"),
            message(List.of(text("ok"))));
        assertEquals("done", ScreenLoop.runLoop(rig.client(), screen, null, 6).status());
        JsonNode messages = rig.http().requests.get(1).get("messages");
        JsonNode results = messages.get(messages.size() - 1).get("content");
        assertEquals(List.of(false, true, true), List.of(results.get(0).path("is_error").asBoolean(false), results.get(1).path("is_error").asBoolean(false), results.get(2).path("is_error").asBoolean(false)));
        assertEquals(ScreenLoop.HALT, results.get(2).get("content").asText());
        assertEquals("", screen.typed.toString());
        results.forEach(r -> assertEquals("computer", r.get("toolset_name").asText()));
    }
}
