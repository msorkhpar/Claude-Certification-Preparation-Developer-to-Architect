import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A multi-agent run that survives injected failures: retries with one key, isolation, a breaker, a fallback and a checkpoint. See ../../statement.md. */
final class ReliableAgents {
    private static final System.Logger LOG = System.getLogger(ReliableAgents.class.getName());
    private ReliableAgents() {}

    /** A failure worth retrying: a timeout, a rate limit, a lost response. */
    static class Transient extends RuntimeException {
        Transient(String message) {
            super(message);
        }
    }

    /** A failure that retrying cannot fix. */
    static class Fatal extends RuntimeException {
        Fatal(String message) {
            super(message);
        }
    }

    interface Agent {
        String call(String key, Map<String, String> inputs);
    }

    /** The result and no reason, or no result and a reason. */
    record Outcome(String result, String reason) {}

    /** One call to an agent: status "ok", "transient" or "fatal" and the result or message. Any other exception is a crash and is not caught. */
    record Call(String status, String value) {}

    static Call callOnce(Map<String, Agent> agents, String name, String key, Map<String, String> inputs) {
        try {
            return new Call("ok", agents.get(name).call(key, inputs));
        } catch (Transient e) {
            return new Call("transient", null);
        } catch (Fatal e) {
            return new Call("fatal", e.getMessage());
        }
    }

    /** True when the agent has failed {@code threshold} calls in a row. */
    static boolean breakerOpen(Map<String, Integer> consecutive, String name, int threshold) {
        return consecutive.getOrDefault(name, 0) >= threshold;
    }

    /** A success resets the agent's count to zero; a failure adds one. */
    static void recordOutcome(Map<String, Integer> consecutive, String name, boolean ok) {
        if (ok) consecutive.put(name, 0);
        else consecutive.merge(name, 1, Integer::sum);
    }

    /** The result and no reason, or no result and a reason; every call to an agent is counted for the task. */
    static Outcome attempt(Map<String, Agent> agents, String name, String key, Map<String, String> inputs, Map<String, Integer> calls, String taskId,
                           Map<String, Integer> consecutive, int attempts, int threshold) {
        for (int n = 1; n <= attempts; n++) {
            if (breakerOpen(consecutive, name, threshold)) return new Outcome(null, "circuit open");
            calls.merge(taskId, 1, Integer::sum);
            Call call = callOnce(agents, name, key, inputs);
            recordOutcome(consecutive, name, call.status().equals("ok"));
            if (call.status().equals("ok")) return new Outcome(call.value(), null);
            if (call.status().equals("fatal")) return new Outcome(null, "fatal: " + call.value());
        }
        return new Outcome(null, "retries exhausted");
    }

    /** The first id in {@code needs} that is not done, or null. */
    static String missingDependency(List<String> needs, Map<String, String> done) {
        for (String n : needs) if (!done.containsKey(n)) return n;
        return null;
    }

    /** The key the fallback agent receives. */
    static String fallbackKey(String key) {
        return key + ":fallback";
    }

    /** True when the store already holds this task's result. */
    static boolean shouldResume(String tid, Map<String, String> store) {
        return store.containsKey(tid);
    }

    /** Write a final result to the store as soon as the task finishes; a degraded result is not final. */
    static void checkpoint(Map<String, String> store, String tid, String result, boolean degraded) {
        if (!degraded) store.put(tid, result);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> runPlan(List<Map<String, Object>> plan, Map<String, Agent> agents, Map<String, String> store, int attempts, int breakerThreshold) {
        LOG.log(System.Logger.Level.DEBUG, "runPlan input: {0}", plan);
        Map<String, String> done = new LinkedHashMap<>();
        Map<String, String> failed = new LinkedHashMap<>();
        Map<String, String> skipped = new LinkedHashMap<>();
        List<String> degraded = new ArrayList<>();
        List<String> resumed = new ArrayList<>();
        Map<String, Integer> calls = new LinkedHashMap<>();
        Map<String, Integer> consecutive = new HashMap<>();

        for (Map<String, Object> task : plan) {
            String tid = (String) task.get("id");
            calls.put(tid, 0);
            if (shouldResume(tid, store)) {
                done.put(tid, store.get(tid));
                resumed.add(tid);
                continue;
            }
            List<String> needs = task.get("needs") == null ? List.of() : (List<String>) task.get("needs");
            String missing = missingDependency(needs, done);
            if (missing != null) {
                skipped.put(tid, "dependency failed: " + missing);
                continue;
            }
            Map<String, String> inputs = new LinkedHashMap<>();
            for (String n : needs) inputs.put(n, done.get(n));
            Outcome outcome = attempt(agents, (String) task.get("agent"), (String) task.get("key"), inputs, calls, tid, consecutive, attempts, breakerThreshold);
            if (outcome.result() == null && task.get("fallback") != null) {
                Outcome second = attempt(agents, (String) task.get("fallback"), fallbackKey((String) task.get("key")), inputs, calls, tid, consecutive, attempts, breakerThreshold);
                if (second.result() != null) {
                    done.put(tid, second.result());
                    degraded.add(tid);
                    checkpoint(store, tid, second.result(), true);
                    continue;
                }
            }
            if (outcome.result() == null) {
                failed.put(tid, outcome.reason());
                continue;
            }
            done.put(tid, outcome.result());
            checkpoint(store, tid, outcome.result(), false);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("done", done);
        out.put("failed", failed);
        out.put("skipped", skipped);
        out.put("degraded", degraded);
        out.put("attempts", calls);
        out.put("resumed", resumed);
        return out;
    }
}
