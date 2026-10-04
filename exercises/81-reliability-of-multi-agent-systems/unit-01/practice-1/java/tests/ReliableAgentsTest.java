import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ReliableAgentsTest {
    private static Map<String, Object> task(String id, String agent, List<String> needs, String fallback) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("agent", agent);
        t.put("key", "key-" + id);
        t.put("needs", needs);
        t.put("fallback", fallback);
        return t;
    }

    private static Map<String, Object> task(String id) {
        return task(id, "w", List.of(), null);
    }

    private static Map<String, Object> task(String id, String agent) {
        return task(id, agent, List.of(), null);
    }

    private static Map<String, Object> task(String id, List<String> needs) {
        return task(id, "w", needs, null);
    }

    private static final ReliableAgents.Agent ECHO = (key, inputs) -> key + "|" + String.join(",", new TreeMap<>(inputs).values());

    private static Map<String, Object> run(List<Map<String, Object>> plan, Map<String, ReliableAgents.Agent> agents, Map<String, String> store, int attempts, int threshold) {
        Map<String, Object> result = ReliableAgents.runPlan(plan, agents, store, attempts, threshold);
        assertNotNull(result, "runPlan returned nothing");
        return result;
    }

    private static Map<String, Object> run(List<Map<String, Object>> plan, Map<String, ReliableAgents.Agent> agents, Map<String, String> store) {
        return run(plan, agents, store, 3, 3);
    }

    @Test
    void m1_tasksRunInOrderAndReceiveTheResultsOfTheTasksTheyNeed() {
        Map<String, String> store = new LinkedHashMap<>();
        Map<String, Object> result = run(List.of(task("a"), task("b", List.of("a")), task("c", List.of("a", "b"))), Map.of("w", ECHO), store);
        Map<String, String> expected = Map.of("a", "key-a|", "b", "key-b|key-a|", "c", "key-c|key-a|,key-b|key-a|");
        assertEquals(expected, result.get("done"));
        assertEquals(Map.of("a", 1, "b", 1, "c", 1), result.get("attempts"));
        assertEquals(Map.of(), result.get("failed"));
        assertEquals(Map.of(), result.get("skipped"));
        assertEquals(List.of(), result.get("degraded"));
        assertEquals(List.of(), result.get("resumed"));
        assertEquals(expected, store);
    }

    @Test
    void e1_everyRetryOfATaskCarriesTheSameIdempotencyKey() {
        List<String> seen = new ArrayList<>();
        ReliableAgents.Agent flaky = (key, inputs) -> {
            seen.add(key);
            if (seen.size() < 3) throw new ReliableAgents.Transient("lost");
            return "ok";
        };
        Map<String, Object> result = run(List.of(task("t")), Map.of("w", flaky), new LinkedHashMap<>());
        assertEquals(List.of("key-t", "key-t", "key-t"), seen);
        assertEquals(Map.of("t", "ok"), result.get("done"));
        assertEquals(Map.of("t", 3), result.get("attempts"));
    }

    @Test
    void e2_retriesStopAtTheLimitAndAFatalFailureIsNotRetried() {
        ReliableAgents.Agent down = (key, inputs) -> {
            throw new ReliableAgents.Transient("down");
        };
        ReliableAgents.Agent bad = (key, inputs) -> {
            throw new ReliableAgents.Fatal("bad input");
        };
        Map<String, Object> result = run(List.of(task("a"), task("b", "x")), Map.of("w", down, "x", bad), new LinkedHashMap<>(), 3, 99);
        assertEquals(Map.of("a", "retries exhausted", "b", "fatal: bad input"), result.get("failed"));
        assertEquals(Map.of("a", 3, "b", 1), result.get("attempts"));
    }

    @Test
    void e3_aFailureStaysInsideItsBranchAndDependentsAreSkipped() {
        ReliableAgents.Agent agent = (key, inputs) -> {
            if (key.equals("key-a")) throw new ReliableAgents.Fatal("boom");
            return "ok";
        };
        Map<String, Object> result = run(List.of(task("a"), task("b", List.of("a")), task("c"), task("d", List.of("b"))), Map.of("w", agent), new LinkedHashMap<>());
        assertEquals(Map.of("c", "ok"), result.get("done"));
        assertEquals(Map.of("a", "fatal: boom"), result.get("failed"));
        assertEquals(Map.of("b", "dependency failed: a", "d", "dependency failed: b"), result.get("skipped"));
        assertEquals(Map.of("a", 1, "b", 0, "c", 1, "d", 0), result.get("attempts"));
    }

    @Test
    void e4_aBreakerStopsCallsToAnAgentThatKeepsFailingAndASuccessResetsIt() {
        List<String> calls = new ArrayList<>();
        ReliableAgents.Agent flaky = (key, inputs) -> {
            calls.add(key);
            throw new ReliableAgents.Transient("down");
        };
        ReliableAgents.Agent good = (key, inputs) -> "fine";
        List<Map<String, Object>> plan = List.of(task("t1", "flaky"), task("t2", "flaky"), task("t3", "flaky"), task("t4", "good"));
        Map<String, Object> result = run(plan, Map.of("flaky", flaky, "good", good), new LinkedHashMap<>(), 2, 3);
        assertEquals(Map.of("t1", "retries exhausted", "t2", "circuit open", "t3", "circuit open"), result.get("failed"));
        assertEquals(Map.of("t4", "fine"), result.get("done"));
        assertEquals(Map.of("t1", 2, "t2", 1, "t3", 0, "t4", 1), result.get("attempts"));
        assertEquals(3, calls.size());

        List<String> count = new ArrayList<>();
        ReliableAgents.Agent everyOther = (key, inputs) -> {
            count.add(key);
            if (count.size() % 2 == 1) throw new ReliableAgents.Transient("blip");
            return "ok";
        };
        Map<String, Object> healthy = run(List.of(task("a", "x"), task("b", "x")), Map.of("x", everyOther), new LinkedHashMap<>(), 2, 2);
        assertEquals(Map.of("a", "ok", "b", "ok"), healthy.get("done"));
        assertEquals(Map.of(), healthy.get("failed"));
    }

    @Test
    void e5_aFallbackDegradesOneTaskWithItsOwnKeyAndIsNotCheckpointed() {
        List<String> seen = new ArrayList<>();
        ReliableAgents.Agent primary = (key, inputs) -> {
            throw new ReliableAgents.Fatal("down");
        };
        ReliableAgents.Agent backup = (key, inputs) -> {
            seen.add(key);
            return "from backup";
        };
        ReliableAgents.Agent use = (key, inputs) -> "got " + inputs.get("s");
        Map<String, String> store = new LinkedHashMap<>();
        List<Map<String, Object>> plan = List.of(task("s", "primary", List.of(), "backup"), task("t", "use", List.of("s"), null));
        Map<String, Object> result = run(plan, Map.of("primary", primary, "backup", backup, "use", use), store);
        assertEquals(Map.of("s", "from backup", "t", "got from backup"), result.get("done"));
        assertEquals(List.of("s"), result.get("degraded"));
        assertEquals(Map.of(), result.get("failed"));
        assertEquals(List.of("key-s:fallback"), seen);
        assertEquals(java.util.Set.of("t"), store.keySet());
        Map<String, Object> bothDown = run(List.of(task("s", "primary", List.of(), "primary")), Map.of("primary", primary), new LinkedHashMap<>());
        assertEquals(Map.of("s", "fatal: down"), bothDown.get("failed"));
        assertEquals(List.of(), bothDown.get("degraded"));
    }

    @Test
    void e6_aSecondRunResumesFromTheCheckpointAndRetriesOnlyWhatFailed() {
        List<String> calls = new ArrayList<>();
        AtomicBoolean healthy = new AtomicBoolean(false);
        ReliableAgents.Agent agent = (key, inputs) -> {
            calls.add(key);
            if (key.equals("key-b") && !healthy.get()) throw new ReliableAgents.Transient("down");
            return "ok-" + key;
        };
        Map<String, String> store = new LinkedHashMap<>();
        List<Map<String, Object>> plan = List.of(task("a"), task("b", List.of("a")));
        Map<String, Object> first = run(plan, Map.of("w", agent), store, 2, 99);
        assertEquals(Map.of("b", "retries exhausted"), first.get("failed"));
        assertEquals(java.util.Set.of("a"), store.keySet());
        healthy.set(true);
        Map<String, Object> second = run(plan, Map.of("w", agent), store, 2, 99);
        assertEquals(List.of("a"), second.get("resumed"));
        assertEquals(Map.of("a", "ok-key-a", "b", "ok-key-b"), second.get("done"));
        assertEquals(Map.of("a", 0, "b", 1), second.get("attempts"));
        assertEquals(1, calls.stream().filter(k -> k.equals("key-a")).count());
    }

    @Test
    void e7_anUnexpectedCrashIsNotSwallowedAndKeepsTheWorkAlreadyCheckpointed() {
        ReliableAgents.Agent agent = (key, inputs) -> {
            if (key.equals("key-b")) throw new IllegalStateException("process died");
            return "ok";
        };
        Map<String, String> store = new LinkedHashMap<>();
        assertThrows(IllegalStateException.class, () -> run(List.of(task("a"), task("b"), task("c")), Map.of("w", agent), store));
        assertEquals(Map.of("a", "ok"), store);
    }
}
