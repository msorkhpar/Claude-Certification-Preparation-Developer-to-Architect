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

    /**
     * TODO 1 of 7 (unlocks e4): is the breaker of this agent open?
     * Receives the map of consecutive failures per agent, the agent name and the threshold. Returns true when the agent's count has reached the threshold
     * (a name that is not in the map has a count of 0). Example: breakerOpen(Map.of("w", 3), "w", 3) -> true, breakerOpen(Map.of(), "w", 3) -> false
     */
    static boolean breakerOpen(Map<String, Integer> consecutive, String name, int threshold) {
        return false;
    }

    /**
     * TODO 2 of 7 (unlocks e4): update the consecutive-failure count of an agent after a call.
     * Receives the map of consecutive failures, the agent name and whether the call succeeded. Sets the count to 0 on a success and adds one on a failure.
     * Example: after a failure then a success then a failure, the count is 1
     */
    static void recordOutcome(Map<String, Integer> consecutive, String name, boolean ok) {}

    /**
     * TODO 3 of 7 (unlocks m1, e1, e2 and e4): call an agent for a task, retrying a transient failure.
     * Receives the agents, the agent name, the task's key and inputs, the per-task call counts, the task id, the breaker counts, the attempt limit and the breaker
     * threshold. Up to {@code attempts} times: if the breaker is open return Outcome(null, "circuit open") without calling; otherwise add one to {@code calls} for the task, make the
     * call with {@code callOnce} (always the same key), record the outcome and return Outcome(result, null) on "ok"; return Outcome(null, "fatal: <message>") on "fatal" without
     * retrying; retry on "transient". After the last attempt return Outcome(null, "retries exhausted").
     * Example: an agent that fails once with Transient and then answers "ok" -> Outcome("ok", null) with two calls counted
     */
    static Outcome attempt(Map<String, Agent> agents, String name, String key, Map<String, String> inputs, Map<String, Integer> calls, String taskId,
                           Map<String, Integer> consecutive, int attempts, int threshold) {
        return new Outcome(null, "retries exhausted");
    }

    /**
     * TODO 4 of 7 (unlocks e3): the first dependency that did not finish.
     * Receives the ids a task needs and the map of finished results. Returns the first id in {@code needs} (in order) that is not in {@code done}, or null when all are.
     * Example: missingDependency(List.of("a", "b"), Map.of("a", "x")) -> "b"
     */
    static String missingDependency(List<String> needs, Map<String, String> done) {
        return null;
    }

    /**
     * TODO 5 of 7 (unlocks e5): the idempotency key for the fallback agent.
     * Receives the task's key. Returns it followed by {@code :fallback}, so that a repeat of the fallback is recognised without colliding with the primary.
     * Example: fallbackKey("key-s") -> "key-s:fallback"
     */
    static String fallbackKey(String key) {
        return key;
    }

    /**
     * TODO 6 of 7 (unlocks e6): is this task already checkpointed?
     * Receives a task id and the store of checkpointed results. Returns true when the store holds the task, so that no agent is called for it.
     * Example: shouldResume("a", Map.of("a", "ok")) -> true
     */
    static boolean shouldResume(String tid, Map<String, String> store) {
        return false;
    }

    /**
     * TODO 7 of 7 (unlocks m1, e5, e6 and e7): write a finished task's result to the store.
     * Receives the store, the task id, the result and whether the result is degraded (it came from the fallback). Writes the result to the store unless it is
     * degraded, so that a later run tries the primary again. It is called as soon as each task finishes, so a crash later in the run loses nothing finished.
     * Example: checkpoint(store, "a", "ok", false) -> store is {a=ok}; with degraded true the store is unchanged
     */
    static void checkpoint(Map<String, String> store, String tid, String result, boolean degraded) {}

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
