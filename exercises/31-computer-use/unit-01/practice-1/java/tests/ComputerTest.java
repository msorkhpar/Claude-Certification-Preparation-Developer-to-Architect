import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ComputerTest {
    private static final String NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed.";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Map<String, Object> makeScreen(int width, int height) {
        return map("width", width, "height", height, "cursor", list(0, 0), "typed", "", "log", list(),
                "elements", list(map("id", "search", "x", 1000, "y", 200, "w", 400, "h", 40, "risk", "none"), map("id", "pay", "x", 800, "y", 600, "w", 200, "h", 60, "risk", "payment")));
    }

    private static Map<String, Object> makeScreen() {
        return makeScreen(1920, 1080);
    }

    private static Map<String, Object> use(String id, String name, Object... input) {
        return map("type", "tool_use", "id", id, "name", name, "toolset_name", "computer", "input", map(input));
    }

    private static Map<String, Object> reply(List<Object> content, String stop) {
        return map("content", content, "stop_reason", stop);
    }

    private static Map<String, Object> toolReply(Object... blocks) {
        return reply(list(blocks), "tool_use");
    }

    private static Map<String, Object> says(String text) {
        return reply(list(map("type", "text", "text", text)), "end_turn");
    }

    @SuppressWarnings("unchecked")
    private static Object copy(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put((String) e.getKey(), copy(e.getValue()));
            return out;
        }
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>();
            for (Object o : l) out.add(copy(o));
            return out;
        }
        return v;
    }

    /** The model: returns the scripted replies in order and keeps a copy of every request. */
    private static final class Scripted implements Function<Map<String, Object>, Map<String, Object>> {
        final List<Map<String, Object>> replies = new ArrayList<>();
        final List<Map<String, Object>> seen = new ArrayList<>();

        @SafeVarargs
        Scripted(Map<String, Object>... replies) {
            this.replies.addAll(Arrays.asList(replies));
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> apply(Map<String, Object> request) {
            seen.add((Map<String, Object>) copy(request));
            return replies.isEmpty() ? says("script ran out") : replies.remove(0);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resultsOf(Scripted model, int index) {
        if (model.seen.size() <= index) return new ArrayList<>();
        List<Map<String, Object>> messages = (List<Map<String, Object>>) model.seen.get(index).get("messages");
        return (List<Map<String, Object>>) messages.get(messages.size() - 1).get("content");
    }

    private static Map<String, Object> orEmpty(Map<String, Object> m) {
        return m == null ? new LinkedHashMap<>() : m;
    }

    private static Outcome run(Map<String, Object> screen, String name, Map<String, Object> args, double scale) {
        Outcome o = Computer.perform(screen, name, args, scale, null);
        return o == null ? new Outcome("no result", false) : o;
    }

    private static Outcome run(Map<String, Object> screen, String name, Object... args) {
        return run(screen, name, map(args), Computer.scaleFor(1920, 1080));
    }

    @SuppressWarnings("unchecked")
    private static String dataOf(Outcome o) {
        try {
            return (String) ((Map<String, Object>) ((Map<String, Object>) ((List<Object>) o.content()).get(0)).get("source")).get("data");
        } catch (RuntimeException e) {
            return "no image";
        }
    }

    @Test
    void m1_theLoopRunsScaledActionsAndSendsEveryResultInOneMessage() {
        Map<String, Object> screen = makeScreen();
        Scripted model = new Scripted(toolReply(use("t1", "screenshot")),
                toolReply(use("t2", "left_click", "coordinate", list(894, 164)), use("t3", "type", "text", "weather"), use("t4", "screenshot")), says("Typed it."));
        Map<String, Object> result = orEmpty(Computer.runComputerLoop(model, screen));
        assertEquals(List.of("done", 3), List.of(result.getOrDefault("status", "none"), result.getOrDefault("turns", -1)));
        assertEquals(list(list("left_click", "search"), list("type", "weather")), screen.get("log"));
        assertEquals("weather", screen.get("typed"));
        assertEquals(3, model.seen.size());
        for (Map<String, Object> r : model.seen) {
            assertEquals(List.of(map("type", "computer_toolset_20260801")), r.get("tools"));
            assertEquals("claude-sonnet-5-5", r.get("model"));
            assertEquals(4096, ((Number) r.get("max_tokens")).intValue());
        }
        assertEquals(List.of(map("type", "tool_result", "tool_use_id", "t1", "toolset_name", "computer",
                "content", List.of(map("type", "image", "source", map("type", "base64", "media_type", "image/png", "data", "png:1429x804:0"))))), resultsOf(model, 1));
        List<Map<String, Object>> last = resultsOf(model, 2);
        assertEquals(List.of("t2", "t3", "t4"), last.stream().map(r -> r.get("tool_use_id")).toList());
        assertTrue(last.stream().noneMatch(r -> r.containsKey("is_error")));
        assertEquals(List.of("Clicked search", "Typed 7 characters"), last.stream().limit(2).map(r -> r.get("content")).toList());
        assertEquals(map("type", "image", "source", map("type", "base64", "media_type", "image/png", "data", "png:1429x804:2")), ((List<?>) last.get(2).get("content")).get(0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) model.seen.get(2).get("messages");
        assertEquals(List.of("user", "assistant", "user", "assistant", "user"), messages.stream().map(m -> m.get("role")).toList());
    }

    @Test
    void e1_theScreenIsScaledToWhatTheModelMaySeeAndClicksAreScaledBack() {
        assertEquals(1.0, Computer.scaleFor(1280, 800));
        assertEquals(1.0, Computer.scaleFor(1024, 768));
        assertEquals(0.744709, Computer.scaleFor(1920, 1080), 1e-5);
        assertEquals(0.558531, Computer.scaleFor(2560, 1440), 1e-5);
        assertEquals(1568.0 / 3000, Computer.scaleFor(3000, 100), 1e-9);
        assertArrayEquals(new int[] {1429, 804}, Computer.scaledSize(1920, 1080));
        assertArrayEquals(new int[] {1429, 804}, Computer.scaledSize(2560, 1440));
        assertArrayEquals(new int[] {1024, 768}, Computer.scaledSize(1024, 768));
        Map<String, Object> screen = makeScreen();
        assertArrayEquals(new int[] {1200, 220}, Computer.toScreen(894, 164, Computer.scaleFor(1920, 1080), screen));
        assertArrayEquals(new int[] {1919, 1079}, Computer.toScreen(5000, 5000, 1.0, screen));
        assertArrayEquals(new int[] {0, 0}, Computer.toScreen(-3, -3, 1.0, screen));
    }

    @Test
    void e2_aClickOnARiskyElementNeedsAPersonsConfirmation() {
        double scale = Computer.scaleFor(1920, 1080);
        List<Object> asked = new ArrayList<>();
        Function<Map<String, Object>, Boolean> yes = action -> {
            asked.add(action);
            return true;
        };
        List<Function<Map<String, Object>, Boolean>> refusals = new ArrayList<>();
        refusals.add(null);
        refusals.add(action -> false);
        for (Function<Map<String, Object>, Boolean> confirm : refusals) {
            Map<String, Object> screen = makeScreen();
            Outcome o = Computer.perform(screen, "left_click", map("coordinate", list(670, 469)), scale, confirm);
            assertTrue(o != null && o.isError() && String.valueOf(o.content()).startsWith("Declined: pay needs a person's confirmation (payment)"));
            assertEquals(list(), screen.get("log"));
            assertEquals(list(0, 0), screen.get("cursor"));
        }
        Map<String, Object> screen = makeScreen();
        Outcome ok = Computer.perform(screen, "double_click", map("coordinate", list(670, 469)), scale, yes);
        assertEquals(new Outcome("Clicked pay", false), ok);
        assertEquals(list(list("double_click", "pay")), screen.get("log"));
        assertEquals(List.of(map("action", "double_click", "element", "pay", "risk", "payment")), asked);
        asked.clear();
        assertEquals(new Outcome("Clicked search", false), Computer.perform(screen, "left_click", map("coordinate", list(894, 164)), scale, yes));
        assertEquals(0, asked.size());
        assertEquals(new Outcome("Clicked nothing", false), Computer.perform(screen, "left_click", map("coordinate", list(5, 5)), scale, yes));
    }

    @Test
    void e3_aFailedActionStopsTheRestOfItsBatch() {
        Map<String, Object> screen = makeScreen();
        Scripted model = new Scripted(toolReply(use("a", "left_click", "coordinate", list(894, 164)), use("b", "left_click", "coordinate", list(9999, 5)), use("c", "type", "text", "x"), use("d", "screenshot")), says("Stopped."));
        Computer.runComputerLoop(model, screen);
        List<Map<String, Object>> results = resultsOf(model, 1);
        assertEquals(List.of("a", "b", "c", "d"), results.stream().map(r -> r.get("tool_use_id")).toList());
        assertEquals(List.of(false, true, true, true), results.stream().map(r -> r.containsKey("is_error")).toList());
        assertTrue(String.valueOf(results.get(1).get("content")).contains("outside the screenshot"));
        assertEquals(NOT_EXECUTED, results.get(2).get("content"));
        assertEquals(NOT_EXECUTED, results.get(3).get("content"));
        assertTrue(results.stream().allMatch(r -> "computer".equals(r.get("toolset_name"))));
        assertEquals(list(list("left_click", "search")), screen.get("log"));
        assertEquals("", screen.get("typed"));
    }

    @Test
    void e4_invalidActionsComeBackAsErrorResultsWithAReason() {
        Map<String, Object> screen = makeScreen();
        assertEquals(new Outcome("Pressed ctrl+s", false), run(screen, "key", "text", "ctrl+s", "repeat", 2));
        assertTrue(run(screen, "key", "text", "Return", "repeat", 101).isError() && run(screen, "key", "repeat", 1).isError());
        assertEquals(new Outcome("Waited 2.5s", false), run(screen, "wait", "duration", 2.5));
        assertTrue(run(screen, "wait", "duration", 301).isError());
        assertEquals(new Outcome("Scrolled down 3", false), run(screen, "scroll", "scroll_direction", "down", "scroll_amount", 3));
        assertTrue(run(screen, "scroll", "scroll_direction", "sideways", "scroll_amount", 3).isError() && run(screen, "scroll", "scroll_direction", "up", "scroll_amount", 0).isError());
        assertEquals(new Outcome("Unknown action: teleport", true), run(screen, "teleport"));
        assertTrue(run(screen, "type").isError() && run(screen, "left_click", "coordinate", list(5000, 5)).isError());
        assertEquals(new Outcome("Moved", false), run(screen, "mouse_move", "coordinate", list(670, 469)));
        assertEquals(new Outcome("X=670,Y=469", false), run(screen, "cursor_position"));
        assertTrue(run(screen, "zoom", "region", list(0, 0, 5000, 5000)).isError() && run(screen, "zoom", "region", list(300, 200, 100, 100)).isError());
    }

    @Test
    void e5_theLoopEndsOnAStopReasonOrAtTheTurnLimit() {
        List<Map<String, Object>> endless = new ArrayList<>();
        for (int i = 0; i < 10; i++) endless.add(toolReply(use("t" + i, "screenshot")));
        Scripted model = new Scripted(endless.toArray(new Map[0]));
        Map<String, Object> result = orEmpty(Computer.runComputerLoop(model, makeScreen(), "claude-sonnet-5-5", 3, null));
        assertEquals(List.of("max_turns", 3, 3), List.of(result.getOrDefault("status", "none"), result.getOrDefault("turns", -1), model.seen.size()));
        assertEquals("refused", orEmpty(Computer.runComputerLoop(new Scripted(reply(list(map("type", "text", "text", "no")), "refusal")), makeScreen())).get("status"));
        assertEquals("truncated", orEmpty(Computer.runComputerLoop(new Scripted(reply(list(map("type", "text", "text", "cut")), "max_tokens")), makeScreen())).get("status"));
        Scripted paused = new Scripted(reply(list(map("type", "text", "text", "working")), "pause_turn"), says("done"));
        Map<String, Object> again = orEmpty(Computer.runComputerLoop(paused, makeScreen()));
        assertEquals(List.of("done", 2), List.of(again.getOrDefault("status", "none"), again.getOrDefault("turns", -1)));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> second = paused.seen.size() > 1 ? (List<Map<String, Object>>) paused.seen.get(1).get("messages") : new ArrayList<>();
        assertEquals(List.of("user", "assistant"), second.stream().map(m -> m.get("role")).toList());
    }

    private static Map<String, Object> shot(int n) {
        return map("type", "image", "source", map("type", "base64", "media_type", "image/png", "data", "png:" + n));
    }

    private static List<Object> contents(List<Map<String, Object>> msgs) {
        List<Object> out = new ArrayList<>();
        if (msgs == null) return out;
        for (Map<String, Object> m : msgs) if ("user".equals(m.get("role")) && m.get("content") instanceof List<?> l) out.add(((Map<?, ?>) l.get(0)).get("content"));
        return out;
    }

    @Test
    @SuppressWarnings("unchecked")
    void e6_oldScreenshotsAreReplacedSoTheContextDoesNotFillUp() {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(map("role", "user", "content", "task"));
        for (int n = 0; n < 5; n++) {
            messages.add(map("role", "assistant", "content", list(use("t" + n, "screenshot"))));
            messages.add(map("role", "user", "content", list(map("type", "tool_result", "tool_use_id", "t" + n, "toolset_name", "computer", "content", list(shot(n))))));
        }
        messages.add(map("role", "assistant", "content", list(use("k", "type", "text", "x"))));
        messages.add(map("role", "user", "content", list(map("type", "tool_result", "tool_use_id", "k", "content", "Typed 1 characters"))));
        List<Map<String, Object>> before = (List<Map<String, Object>>) copy(messages);
        List<Map<String, Object>> pruned = Computer.pruneScreenshots(messages, 2);
        assertEquals(before, messages);
        List<Object> note = List.of(map("type", "text", "text", "[screenshot removed]"));
        assertEquals(List.of(note, note, note, List.of(shot(3)), List.of(shot(4)), "Typed 1 characters"), contents(pruned));
        assertTrue(pruned != null && pruned.size() == messages.size() && pruned.get(0).equals(messages.get(0)));
        assertEquals(messages, Computer.pruneScreenshots(messages, 9));
        List<Object> none = contents(Computer.pruneScreenshots(messages, 0));
        assertEquals(List.of(note, note, note, note, note), none.size() >= 5 ? none.subList(0, 5) : none);
    }

    @Test
    void e7_screenshotsAndZoomsAreSentAtTheSizeTheModelMaySee() {
        int[][] sizes = {{1920, 1080}, {2560, 1440}, {1280, 800}};
        String[] want = {"1429x804", "1429x804", "1280x800"};
        for (int i = 0; i < sizes.length; i++) {
            Outcome o = run(makeScreen(sizes[i][0], sizes[i][1]), "screenshot", map(), Computer.scaleFor(sizes[i][0], sizes[i][1]));
            assertFalse(o.isError());
            assertEquals("png:" + want[i] + ":0", dataOf(o));
        }
        Map<String, Object> screen = makeScreen();
        Outcome zoom = run(screen, "zoom", "region", list(0, 0, 300, 200));
        assertFalse(zoom.isError());
        assertEquals("png:403x269:0", dataOf(zoom));
        run(screen, "left_click", "coordinate", list(5, 5));
        assertEquals("png:1429x804:1", dataOf(run(screen, "screenshot")));
    }
}
