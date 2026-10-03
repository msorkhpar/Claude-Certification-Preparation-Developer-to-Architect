import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BatchesTest {
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private static Map<String, Object> item(String id, Object... extra) {
        Map<String, Object> params = map("model", MODEL, "max_tokens", 200,
                "messages", List.of(map("role", "user", "content", "Classify ticket " + id)));
        for (int i = 0; i < extra.length; i += 2) params.put((String) extra[i], extra[i + 1]);
        return map("id", id, "params", params);
    }

    private static String line(String customId, Map<String, Object> result) {
        return Json.stringify(map("custom_id", customId, "result", result));
    }

    private static Map<String, Object> message(String text, int input, int output, Object... extraUsage) {
        Map<String, Object> usage = map("input_tokens", input, "output_tokens", output);
        for (int i = 0; i < extraUsage.length; i += 2) usage.put((String) extraUsage[i], extraUsage[i + 1]);
        return map("type", "succeeded", "message", map("id", "msg_illustrative", "type", "message", "role", "assistant", "model", MODEL,
                "stop_reason", "end_turn", "content", List.of(map("type", "text", "text", text)), "usage", usage));
    }

    private static Map<String, Object> message(String text) {
        return message(text, 50, 7);
    }

    private static Map<String, Object> errored(String kind) {
        return map("type", "errored", "error", map("type", "error", "error", map("type", kind, "message", "x")));
    }

    /** The BatchError field the call throws, "crash" for another exception, null when it returns. */
    private static String failureOf(Supplier<Object> call) {
        try {
            call.get();
        } catch (BatchError e) {
            return e.field();
        } catch (RuntimeException e) {
            return "crash";
        }
        return null;
    }

    @SafeVarargs
    private static List<Map<String, Object>> built(Map<String, Object>... items) {
        List<Map<String, Object>> requests = Batches.buildRequests(List.of(items));
        return requests == null ? new ArrayList<>() : requests;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> field(Map<String, Object> done, String key) {
        return done == null ? List.of("no result") : (List<Object>) done.get(key);
    }

    private static List<Object> statuses(Map<String, Object> done) {
        List<Object> out = new ArrayList<>();
        if (done != null) for (Object o : field(done, "outcomes")) out.add(((Map<?, ?>) o).get("status"));
        return out;
    }

    @Test
    @SuppressWarnings("unchecked")
    void m1_resultsAreMatchedToRequestsByCustomIdNotByPosition() {
        List<Map<String, Object>> requests = built(item("t-1"), item("t-2"), item("t-3"));
        assertEquals(List.of("t-1", "t-2", "t-3"), requests.stream().map(r -> r.get("custom_id")).collect(Collectors.toList()));
        assertEquals(200, ((Map<String, Object>) requests.get(0).get("params")).get("max_tokens"));
        Map<String, Object> done = Batches.collect(requests, List.of(line("t-3", message("billing")), line("t-1", message("refund")), line("t-2", message("shipping"))));
        List<Object> got = new ArrayList<>();
        for (Object o : field(done, "outcomes")) {
            Map<String, Object> m = (Map<String, Object>) o;
            got.add(m.get("custom_id") + "/" + m.get("status") + "/" + m.get("text"));
        }
        assertEquals(List.of("t-1/succeeded/refund", "t-2/succeeded/shipping", "t-3/succeeded/billing"), got);
        assertEquals(List.of(List.of(), List.of(), List.of()), List.of(field(done, "retry"), field(done, "fix"), field(done, "unknown")));
    }

    @Test
    void e1_aCustomIdIs1To64SafeCharactersAndUnique() {
        for (String bad : new String[] {"", "has space", "dot.dot", "x".repeat(65), "naïve"}) {
            assertEquals("custom_id", failureOf(() -> Batches.buildRequests(List.of(item(bad)))), bad);
        }
        assertEquals("custom_id", failureOf(() -> Batches.buildRequests(List.of(item("a"), item("a")))));
        assertNull(failureOf(() -> Batches.buildRequests(List.of(item("x".repeat(64)), item("A_b-9")))));
    }

    @Test
    void e2_parametersABatchCannotTakeAreRefused() {
        assertEquals("params.stream", failureOf(() -> Batches.buildRequests(List.of(item("a", "stream", true)))));
        assertEquals("params.speed", failureOf(() -> Batches.buildRequests(List.of(item("a", "speed", "fast")))));
        assertEquals("params.max_tokens", failureOf(() -> Batches.buildRequests(List.of(map("id", "a", "params", map("model", MODEL, "max_tokens", 0, "messages", List.of()))))));
        assertNull(failureOf(() -> Batches.buildRequests(List.of(item("a", "stream", false)))));
    }

    @Test
    void e3_aBigJobIsCutInOrderByRequestCountAndBySize() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 7; i++) items.add(item("r" + i));
        List<Map<String, Object>> requests = Batches.buildRequests(items);
        if (requests == null) requests = new ArrayList<>();
        List<List<Map<String, Object>>> byCount = Batches.splitBatches(requests, 3, 1L << 40);
        List<List<Object>> ids = new ArrayList<>();
        if (byCount != null) for (List<Map<String, Object>> b : byCount) ids.add(b.stream().map(r -> r.get("custom_id")).collect(Collectors.toList()));
        assertEquals(List.of(List.of("r0", "r1", "r2"), List.of("r3", "r4", "r5"), List.of("r6")), ids);
        long size = requests.isEmpty() ? 0 : Json.stringify(requests.get(0)).getBytes(StandardCharsets.UTF_8).length;
        List<List<Map<String, Object>>> bySize = Batches.splitBatches(requests, 100_000, size * 2 + 5);
        List<Integer> counts = new ArrayList<>();
        List<Object> order = new ArrayList<>();
        if (bySize != null) for (List<Map<String, Object>> b : bySize) {
            counts.add(b.size());
            for (Map<String, Object> r : b) order.add(r.get("custom_id"));
        }
        assertEquals(List.of(2, 2, 2, 1), counts);
        assertEquals(List.of("r0", "r1", "r2", "r3", "r4", "r5", "r6"), order);
        List<List<Map<String, Object>>> none = Batches.splitBatches(new ArrayList<>());
        assertEquals(0, none == null ? -1 : none.size());
        final List<Map<String, Object>> all = requests;
        final long limit = size - 1;
        assertEquals("size", failureOf(() -> Batches.splitBatches(all, 100_000, limit)));
    }

    @Test
    void e4_invalidRequestsAreFixedAndTheRestAreRetried() {
        List<Map<String, Object>> requests = built(item("ok"), item("bad"), item("busy"), item("late"), item("stopped"));
        Map<String, Object> done = Batches.collect(requests, List.of(line("ok", message("fine")), line("bad", errored("invalid_request_error")),
                line("busy", errored("overloaded_error")), line("late", map("type", "expired")), line("stopped", map("type", "canceled"))));
        assertEquals(List.of("bad"), field(done, "fix"));
        assertEquals(List.of("busy", "late", "stopped"), field(done, "retry"));
        assertEquals(List.of("succeeded", "errored", "errored", "expired", "canceled"), statuses(done));
        Object second = done == null ? null : ((Map<?, ?>) field(done, "outcomes").get(1)).get("error_type");
        assertEquals("invalid_request_error", second);
    }

    @Test
    void e5_aRequestWithNoResultIsMissingAndAStrangerIsReported() {
        List<Map<String, Object>> requests = built(item("a"), item("b"), item("c"));
        Map<String, Object> done = Batches.collect(requests, List.of(line("c", message("three")), "", line("zzz", message("who")), line("a", message("one"))));
        assertEquals(List.of("succeeded", "missing", "succeeded"), statuses(done));
        assertEquals(List.of("b"), field(done, "retry"));
        assertEquals(List.of("zzz"), field(done, "unknown"));
        assertEquals(3, statuses(done).size());
    }

    @Test
    void e6_onlyRequestsThatSucceededCountTowardUsage() {
        List<Map<String, Object>> requests = built(item("a"), item("b"), item("c"));
        Map<String, Object> done = Batches.collect(requests, List.of(line("a", message("x", 100, 10, "cache_read_input_tokens", 400)),
                line("b", errored("api_error")), line("c", message("y", 30, 5, "cache_creation_input_tokens", 200))));
        assertEquals(map("input_tokens", 130L, "output_tokens", 15L, "cache_creation_input_tokens", 200L, "cache_read_input_tokens", 400L),
                done == null ? null : done.get("usage"));
    }
}
